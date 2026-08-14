package net.seraphina.ninecun_lingtai.client;

import net.seraphina.ninecun_lingtai.common.item.ItemFontStyle;

import java.util.Objects;

public record ItemFontTheme(
        ItemFontStyle primaryStyle,
        ItemFontStyle accentStyle,
        int rank
) {
    public ItemFontTheme {
        Objects.requireNonNull(primaryStyle, "primaryStyle");
        Objects.requireNonNull(accentStyle, "accentStyle");
        rank = Math.max(0, Math.min(rank, 9));
    }

    public static ItemFontTheme of(ItemFontStyle style) {
        return new ItemFontTheme(style, style, 0);
    }

    public static ItemFontTheme withGrade(
            ItemFontStyle primaryStyle,
            ItemFontStyle accentStyle,
            int rank
    ) {
        return new ItemFontTheme(primaryStyle, accentStyle, rank);
    }
}
