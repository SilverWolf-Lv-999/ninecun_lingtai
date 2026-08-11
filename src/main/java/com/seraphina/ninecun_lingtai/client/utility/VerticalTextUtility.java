package com.seraphina.ninecun_lingtai.client.utility;

import java.util.ArrayList;
import java.util.List;

public final class VerticalTextUtility {

    public static List<List<String>> splitIntoColumns(String content, int maximumRows) {
        if (maximumRows <= 0) {
            throw new IllegalArgumentException("maximumRows must be positive");
        }
        String normalizedContent = content.replace("\r\n", "\n").replace('\r', '\n');
        String[] logicalLines = normalizedContent.split("\n", -1);
        List<List<String>> columns = new ArrayList<>();
        for (String logicalLine : logicalLines) {
            List<String> symbols = logicalLine.codePoints()
                    .mapToObj(codePoint -> new String(Character.toChars(codePoint)))
                    .toList();
            if (symbols.isEmpty()) {
                columns.add(List.of());
                continue;
            }
            for (int symbolIndex = 0; symbolIndex < symbols.size(); symbolIndex += maximumRows) {
                columns.add(List.copyOf(symbols.subList(
                        symbolIndex,
                        Math.min(symbolIndex + maximumRows, symbols.size())
                )));
            }
        }
        return List.copyOf(columns);
    }
}
