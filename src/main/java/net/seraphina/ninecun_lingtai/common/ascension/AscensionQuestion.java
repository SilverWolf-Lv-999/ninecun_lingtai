package net.seraphina.ninecun_lingtai.common.ascension;

import java.util.Objects;

public record AscensionQuestion(
        String title,
        String prompt,
        String gradingFocus
) {
    public AscensionQuestion {
        title = requireText(title, "title");
        prompt = requireText(prompt, "prompt");
        gradingFocus = requireText(gradingFocus, "gradingFocus");
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName);
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
