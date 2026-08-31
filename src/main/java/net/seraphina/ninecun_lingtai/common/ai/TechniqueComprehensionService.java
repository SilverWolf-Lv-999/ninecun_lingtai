package net.seraphina.ninecun_lingtai.common.ai;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.seraphina.ninecun_lingtai.api.ai.AiProvider;
import net.seraphina.ninecun_lingtai.api.technique.TechniqueAssessment;
import net.seraphina.ninecun_lingtai.api.technique.TechniqueDefinition;
import net.seraphina.ninecun_lingtai.utility.AiConfigUtility;
import net.seraphina.ninecun_lingtai.utility.SoulInjuryUtility;
import net.seraphina.ninecun_lingtai.utility.TechniquePromptUtility;
import net.minecraft.server.level.ServerPlayer;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import static net.seraphina.ninecun_lingtai.common.ai.AscensionComprehensionService.getTechniqueAssessment;

public final class TechniqueComprehensionService {
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15L))
            .build();

    /**
     * 带魂伤扣分的评估入口：在服务端主线程取样扣分值，assessment 返回后调整评分
     * （设计文档 R6：不动 prompt，扣分后仍按原 minimum_learning_score 判定）。
     */
    public static CompletableFuture<TechniqueAssessment> evaluate(
            TechniqueDefinition technique,
            String playerUnderstanding,
            ServerPlayer player
    ) {
        double penalty = SoulInjuryUtility.getComprehensionPenalty(player);
        return evaluate(technique, playerUnderstanding)
                .thenApply(assessment -> SoulInjuryUtility.applyComprehensionPenalty(assessment, penalty));
    }

    public static CompletableFuture<TechniqueAssessment> evaluate(
            TechniqueDefinition technique,
            String playerUnderstanding
    ) {
        AiProvider provider = AiConfigUtility.getProvider();
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
            requestBody = createRequestBody(technique, playerUnderstanding, model);
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
                .thenApply(TechniqueComprehensionService::parseResponse);
    }

    private static JsonObject createRequestBody(
            TechniqueDefinition technique,
            String playerUnderstanding,
            String model
    ) {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", model);
        JsonArray messages = new JsonArray();
        messages.add(createMessage("system", TechniquePromptUtility.createSystemPrompt(technique)));
        messages.add(createMessage("user", TechniquePromptUtility.createUserPrompt(playerUnderstanding)));
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
}
