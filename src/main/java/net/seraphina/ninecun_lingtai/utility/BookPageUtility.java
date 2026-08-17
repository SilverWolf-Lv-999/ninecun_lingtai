package net.seraphina.ninecun_lingtai.utility;

import java.util.ArrayList;
import java.util.List;

public final class BookPageUtility {
    public static final int MAXIMUM_PAGE_CONTENT_LENGTH = 1000;
    private static final int MINIMUM_BOUNDARY_SEARCH_LENGTH = MAXIMUM_PAGE_CONTENT_LENGTH / 2;

    public static List<String> splitIntoPages(String content) {
        String normalizedContent = normalizeLineEndings(content);
        if (normalizedContent.isBlank()) {
            return List.of("");
        }

        List<String> pages = new ArrayList<>();
        int pageStart = 0;
        while (pageStart < normalizedContent.length()) {
            int pageEnd = Math.min(
                    normalizedContent.length(),
                    pageStart + MAXIMUM_PAGE_CONTENT_LENGTH
            );
            if (pageEnd < normalizedContent.length()) {
                pageEnd = findPageBoundary(normalizedContent, pageStart, pageEnd);
            }
            if (pageEnd <= pageStart) {
                pageEnd = Math.min(
                        normalizedContent.length(),
                        pageStart + MAXIMUM_PAGE_CONTENT_LENGTH
                );
            }

            pages.add(normalizedContent.substring(pageStart, pageEnd).strip());
            pageStart = skipLeadingWhitespace(normalizedContent, pageEnd);
        }
        return List.copyOf(pages);
    }

    private static String normalizeLineEndings(String content) {
        String normalizedContent = content == null ? "" : content.replace("\r\n", "\n").replace('\r', '\n');
        return normalizedContent.startsWith("\uFEFF") ? normalizedContent.substring(1) : normalizedContent;
    }

    private static int findPageBoundary(String content, int pageStart, int preferredEnd) {
        int minimumBoundary = pageStart + MINIMUM_BOUNDARY_SEARCH_LENGTH;
        for (int index = preferredEnd; index > minimumBoundary; index--) {
            char character = content.charAt(index - 1);
            if (character == '\n') {
                return index;
            }
        }
        for (int index = preferredEnd; index > minimumBoundary; index--) {
            char character = content.charAt(index - 1);
            if (isSentenceBoundary(character) || Character.isWhitespace(character)) {
                return index;
            }
        }
        return preferredEnd;
    }

    private static int skipLeadingWhitespace(String content, int index) {
        int nextIndex = index;
        while (nextIndex < content.length() && Character.isWhitespace(content.charAt(nextIndex))) {
            nextIndex++;
        }
        return nextIndex;
    }

    private static boolean isSentenceBoundary(char character) {
        return character == '。'
                || character == '！'
                || character == '？'
                || character == '；'
                || character == '.'
                || character == '!'
                || character == '?'
                || character == ';';
    }
}
