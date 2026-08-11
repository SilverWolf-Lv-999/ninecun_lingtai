package com.seraphina.ninecun_lingtai.utility;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.seraphina.ninecun_lingtai.api.technique.TechniqueAssessment;
import net.minecraft.util.GsonHelper;

public final class AiResponseUtility {

    public static TechniqueAssessment parseTechniqueAssessment(String responseContent) {
        String json = removeMarkdownFence(responseContent.trim());
        JsonElement jsonElement = JsonParser.parseString(json);
        JsonObject jsonObject = GsonHelper.convertToJsonObject(jsonElement, "technique assessment");
        int score = GsonHelper.getAsInt(jsonObject, "score");
        String feedback = GsonHelper.getAsString(jsonObject, "feedback", "");
        try {
            return new TechniqueAssessment(score, feedback);
        } catch (IllegalArgumentException exception) {
            throw new JsonParseException(exception.getMessage(), exception);
        }
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
