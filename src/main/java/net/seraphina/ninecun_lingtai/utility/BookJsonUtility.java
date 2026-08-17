package net.seraphina.ninecun_lingtai.utility;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;

public final class BookJsonUtility {
    public static BookMetadata parse(ResourceLocation bookId, JsonElement jsonElement) {
        JsonObject jsonObject = GsonHelper.convertToJsonObject(jsonElement, "book definition");
        String title = getRequiredNonBlankString(jsonObject, "title");
        String author = GsonHelper.getAsString(jsonObject, "author", "");
        ResourceLocation content = parseResourceLocation(
                getRequiredNonBlankString(jsonObject, "content"),
                "content"
        );
        return new BookMetadata(bookId, title, author, content);
    }

    private static ResourceLocation parseResourceLocation(String value, String fieldName) {
        ResourceLocation resourceLocation = ResourceLocation.tryParse(value);
        if (resourceLocation == null) {
            throw new JsonParseException("Invalid resource id for " + fieldName + ": " + value);
        }
        return resourceLocation;
    }

    private static String getRequiredNonBlankString(JsonObject jsonObject, String fieldName) {
        String value = GsonHelper.getAsString(jsonObject, fieldName);
        if (value.isBlank()) {
            throw new JsonParseException(fieldName + " must not be blank");
        }
        return value;
    }

    public record BookMetadata(
            ResourceLocation id,
            String title,
            String author,
            ResourceLocation content
    ) {
    }
}
