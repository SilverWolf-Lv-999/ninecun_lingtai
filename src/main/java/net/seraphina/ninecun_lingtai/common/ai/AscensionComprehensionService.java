package net.seraphina.ninecun_lingtai.common.ai;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import net.minecraft.util.GsonHelper;
import net.seraphina.ninecun_lingtai.api.ai.AiProvider;
import net.seraphina.ninecun_lingtai.api.technique.TechniqueAssessment;
import net.seraphina.ninecun_lingtai.common.ascension.AscensionQuestion;
import net.seraphina.ninecun_lingtai.utility.AiConfigUtility;
import net.seraphina.ninecun_lingtai.utility.AiResponseUtility;
import net.seraphina.ninecun_lingtai.utility.AscensionPromptUtility;
import net.seraphina.ninecun_lingtai.utility.SoulInjuryUtility;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public final class AscensionComprehensionService {
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15L))
            .build();

    /**
     * 带魂伤扣分的评估入口：在服务端主线程取样扣分值，assessment 返回后调整评分
     * （设计文档 R6：不动 prompt，扣分后仍按原判定阈值）。
     */
    public static CompletableFuture<TechniqueAssessment> evaluate(
            AscensionQuestion question,
            String profileContext,
            String answer,
            ServerPlayer player
    ) {
        double penalty = SoulInjuryUtility.getComprehensionPenalty(player);
        return evaluate(question, profileContext, answer)
                .thenApply(assessment -> SoulInjuryUtility.applyComprehensionPenalty(assessment, penalty));
    }

    public static CompletableFuture<TechniqueAssessment> evaluate(
            AscensionQuestion question,
            String profileContext,
            String answer
    ) {
        AiProvider provider;
        provider = AiConfigUtility.getProvider();
        String apiKey;
        String model;
        try {
            apiKey = AiConfigUtility.requireApiKey();
            model = AiConfigUtility.requireModel();
        } catch (IllegalStateException exception) {
            return CompletableFuture.failedFuture(exception);
        }

        JsonObject requestBody;
        try {
            requestBody = createRequestBody(question, profileContext, answer, model);
        } catch (RuntimeException exception) {
            return CompletableFuture.failedFuture(exception);
        }
        HttpRequest request = HttpRequest.newBuilder(provider.getChatCompletionsEndpoint())
                .timeout(Duration.ofSeconds(60L))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(requestBody)))
                .build();
        return HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(AscensionComprehensionService::parseResponse);
    }

    private static JsonObject createRequestBody(
            AscensionQuestion question,
            String profileContext,
            String answer,
            String model
    ) {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", model);
        JsonArray messages = new JsonArray();
        messages.add(createMessage(
                "system",
                AscensionPromptUtility.createSystemPrompt(question, profileContext)
        ));
        messages.add(createMessage("user", AscensionPromptUtility.createUserPrompt(answer)));
        requestBody.add("messages", messages);

        JsonObject responseFormat = new JsonObject();
        responseFormat.addProperty("type", "json_object");
        requestBody.add("response_format", responseFormat);
        return requestBody;
    }

    private static JsonObject createMessage(String role, String content) {
        JsonObject message = new JsonObject();
        message.addProperty("role", role);
        message.addProperty("content", content);
        return message;
    }

    private static TechniqueAssessment parseResponse(HttpResponse<String> response) {
        return getTechniqueAssessment(response);
    }

    @NotNull
    public static TechniqueAssessment getTechniqueAssessment(HttpResponse<String> response) {
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new CompletionException(new IllegalStateException(
                    "AI provider returned HTTP " + response.statusCode()
            ));

        }
        try {
            JsonObject responseJson = JsonParser.parseString(response.body()).getAsJsonObject();
            JsonArray choices = GsonHelper.getAsJsonArray(responseJson, "choices");
            if (choices.isEmpty()) {
                throw new JsonParseException("AI response contains no choices");
            }
            JsonObject choice = GsonHelper.convertToJsonObject(choices.get(0), "AI choice");
            JsonObject message = GsonHelper.getAsJsonObject(choice, "message");
            JsonElement content = message.get("content");
            if (content == null || !content.isJsonPrimitive()) {
                throw new JsonParseException("AI response message has no text content");
            }
            return AiResponseUtility.parseTechniqueAssessment(content.getAsString());
        } catch (RuntimeException exception) {
            throw new CompletionException(new JsonParseException("Unable to parse AI provider response", exception));
        }
    }
}
