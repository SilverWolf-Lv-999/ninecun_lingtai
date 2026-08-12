package net.seraphina.ninecun_lingtai.common.ai;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import net.seraphina.ninecun_lingtai.api.ai.AiProvider;
import net.seraphina.ninecun_lingtai.api.technique.TechniqueAssessment;
import net.seraphina.ninecun_lingtai.api.technique.TechniqueDefinition;
import net.seraphina.ninecun_lingtai.utility.AiConfigUtility;
import net.seraphina.ninecun_lingtai.utility.AiResponseUtility;
import net.seraphina.ninecun_lingtai.utility.TechniquePromptUtility;
import net.minecraft.util.GsonHelper;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public final class TechniqueComprehensionService {
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15L))
            .build();

    private TechniqueComprehensionService() {
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
