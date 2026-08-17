package net.seraphina.ninecun_lingtai.client.tooltip;

import net.minecraft.world.inventory.tooltip.TooltipComponent;

import java.util.Objects;

public final class BookTooltipComponent implements TooltipComponent {
    private final String title;
    private final String author;

    public BookTooltipComponent(String title, String author) {
        this.title = Objects.requireNonNullElse(title, "");
        this.author = Objects.requireNonNullElse(author, "");
    }

    public String getTitle() {
        return this.title;
    }

    public String getAuthor() {
        return this.author;
    }
}
