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
import net.seraphina.ninecun_lingtai.common.ascension.AscensionQuestion;
import net.seraphina.ninecun_lingtai.utility.AiConfigUtility;
import net.seraphina.ninecun_lingtai.utility.AscensionPromptUtility;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public final class AscensionQuestionGenerationService {
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15L))
            .build();
    private static final int MAXIMUM_TITLE_LENGTH = 128;
    private static final int MAXIMUM_PROMPT_LENGTH = 4_096;
    private static final int MAXIMUM_GRADING_FOCUS_LENGTH = 1_024;

    public static CompletableFuture<List<AscensionQuestion>> generate(String profileContext) {
        AiProvider provider = AiConfigUtility.getProvider();
        String apiKey;
        String model;
        int questionCount = AscensionPromptUtility.chooseQuestionCount();
        UUID generationNonce = UUID.randomUUID();
        try {
            apiKey = AiConfigUtility.requireApiKey();
            model = AiConfigUtility.requireModel();
        } catch (IllegalStateException exception) {
            return CompletableFuture.failedFuture(exception);
        }

        JsonObject requestBody;
        try {
            requestBody = createRequestBody(profileContext, questionCount, generationNonce, model);
        } catch (RuntimeException exception) {
            return CompletableFuture.failedFuture(exception);
        }
        HttpRequest request = HttpRequest.newBuilder(provider.getChatCompletionsEndpoint())
                .timeout(Duration.ofSeconds(90L))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(requestBody)))
                .build();
        return HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> parseResponse(response, questionCount));
    }

    private static JsonObject createRequestBody(
            String profileContext,
            int questionCount,
            UUID generationNonce,
            String model
    ) {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", model);
        requestBody.addProperty("temperature", 1.2D);
        JsonArray messages = new JsonArray();
        messages.add(createMessage(
                "system",
                AscensionPromptUtility.createQuestionGenerationSystemPrompt(
                        profileContext,
                        questionCount,
                        generationNonce
                )
        ));
        messages.add(createMessage(
                "user",
                AscensionPromptUtility.createQuestionGenerationUserPrompt()
        ));
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

    private static List<AscensionQuestion> parseResponse(
            HttpResponse<String> response,
            int expectedQuestionCount
    ) {
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
            return parseQuestionSet(content.getAsString(), expectedQuestionCount);
        } catch (RuntimeException exception) {
            throw new CompletionException(
                    new JsonParseException("Unable to parse AI-generated ascension questions", exception)
            );
        }
    }

    private static List<AscensionQuestion> parseQuestionSet(
            String responseContent,
            int expectedQuestionCount
    ) {
        String json = removeMarkdownFence(responseContent.trim());
        JsonElement jsonElement = JsonParser.parseString(json);
        JsonObject jsonObject = GsonHelper.convertToJsonObject(jsonElement, "ascension question set");
        JsonArray questionElements = GsonHelper.getAsJsonArray(jsonObject, "questions");
        if (questionElements.size() != expectedQuestionCount) {
            throw new JsonParseException(
                    "AI generated " + questionElements.size()
                            + " questions, expected " + expectedQuestionCount
            );
        }

        List<AscensionQuestion> questions = new ArrayList<>(questionElements.size());
        Set<String> prompts = new HashSet<>();
        for (JsonElement questionElement : questionElements) {
            JsonObject questionObject = GsonHelper.convertToJsonObject(
                    questionElement,
                    "ascension question"
            );
            String title = requireGeneratedText(
                    GsonHelper.getAsString(questionObject, "title"),
                    MAXIMUM_TITLE_LENGTH,
                    "title"
            );
            String prompt = requireGeneratedText(
                    GsonHelper.getAsString(questionObject, "prompt"),
                    MAXIMUM_PROMPT_LENGTH,
                    "prompt"
            );
            String gradingFocus = requireGeneratedText(
                    GsonHelper.getAsString(questionObject, "gradingFocus"),
                    MAXIMUM_GRADING_FOCUS_LENGTH,
                    "gradingFocus"
            );
            if (!prompts.add(normalizeForComparison(prompt))) {
                throw new JsonParseException("AI generated duplicate ascension questions");
            }
            questions.add(new AscensionQuestion(title, prompt, gradingFocus));
        }
        return List.copyOf(questions);
    }

    private static String requireGeneratedText(String value, int maximumLength, String fieldName) {
        String normalizedValue = value == null ? "" : value.trim();
        if (normalizedValue.isEmpty()) {
            throw new JsonParseException("Generated ascension question " + fieldName + " is blank");
        }
        if (normalizedValue.length() > maximumLength) {
            throw new JsonParseException(
                    "Generated ascension question " + fieldName + " is too long"
            );
        }
        return normalizedValue;
    }

    private static String normalizeForComparison(String value) {
        return value.replaceAll("\\s+", "");
    }

    private static String removeMarkdownFence(String content) {
        if (!content.startsWith("```") || !content.endsWith("```")) {
            return content;
        }
        int firstLineEnd = content.indexOf('\n');
        if (firstLineEnd < 0) {
            return content;
        }
        return content.substring(firstLineEnd + 1, content.length() - 3).trim();
    }
}
