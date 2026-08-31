package net.seraphina.ninecun_lingtai.client.screen;

import net.seraphina.ninecun_lingtai.ModMain;
import net.seraphina.ninecun_lingtai.common.network.ModNetwork;
import net.seraphina.ninecun_lingtai.common.network.packet.SetSoulSenseFilterPacket;
import net.seraphina.ninecun_lingtai.common.soul.SoulSenseTargetCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.EnumMap;
import java.util.Map;

public final class SoulSenseFilterScreen extends Screen {
    private static final int BUTTON_WIDTH = 150;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_SPACING = 24;

    private final Map<SoulSenseTargetCategory, Button> categoryButtons =
            new EnumMap<>(SoulSenseTargetCategory.class);
    private int filterMask;

    public SoulSenseFilterScreen(int filterMask) {
        super(Component.translatable("screen." + ModMain.MOD_ID + ".soul_sense_filter"));
        this.filterMask = filterMask;
    }

    @Override
    protected void init() {
        SoulSenseTargetCategory[] categories = SoulSenseTargetCategory.values();
        int columnCount = 2;
        int rowCount = (categories.length + columnCount - 1) / columnCount;
        int totalWidth = columnCount * BUTTON_WIDTH + (columnCount - 1) * 10;
        int startX = (this.width - totalWidth) / 2;
        int startY = (this.height - rowCount * BUTTON_SPACING - 40) / 2 + 16;

        for (int index = 0; index < categories.length; index++) {
            SoulSenseTargetCategory category = categories[index];
            int column = index % columnCount;
            int row = index / columnCount;
            Button button = Button.builder(
                            getCategoryLabel(category),
                            pressed -> toggleCategory(category)
                    )
                    .bounds(
                            startX + column * (BUTTON_WIDTH + 10),
                            startY + row * BUTTON_SPACING,
                            BUTTON_WIDTH,
                            BUTTON_HEIGHT
                    )
                    .build();
            this.categoryButtons.put(category, button);
            addRenderableWidget(button);
        }

        int footerY = startY + rowCount * BUTTON_SPACING + 8;
        addRenderableWidget(Button.builder(
                        Component.translatable("screen." + ModMain.MOD_ID + ".soul_sense_filter.all"),
                        pressed -> setMask(SoulSenseTargetCategory.getFullMask())
                )
                .bounds(startX, footerY, 96, BUTTON_HEIGHT)
                .build());
        addRenderableWidget(Button.builder(
                        Component.translatable("screen." + ModMain.MOD_ID + ".soul_sense_filter.none"),
                        pressed -> setMask(0)
                )
                .bounds(startX + 106, footerY, 96, BUTTON_HEIGHT)
                .build());
        addRenderableWidget(Button.builder(
                        Component.translatable("gui.done"),
                        pressed -> onClose()
                )
                .bounds(startX + totalWidth - 96, footerY, 96, BUTTON_HEIGHT)
                .build());
    }

    private void toggleCategory(SoulSenseTargetCategory category) {
        setMask(this.filterMask ^ category.getMaskBit());
    }

    private void setMask(int updatedMask) {
        this.filterMask = updatedMask & SoulSenseTargetCategory.getFullMask();
        ModNetwork.sendToServer(new SetSoulSenseFilterPacket(this.filterMask));
        refreshLabels();
    }

    private void refreshLabels() {
        for (Map.Entry<SoulSenseTargetCategory, Button> entry : this.categoryButtons.entrySet()) {
            entry.getValue().setMessage(getCategoryLabel(entry.getKey()));
        }
    }

    private Component getCategoryLabel(SoulSenseTargetCategory category) {
        boolean enabled = (this.filterMask & category.getMaskBit()) != 0;
        String stateKey = enabled
                ? "screen." + ModMain.MOD_ID + ".soul_sense_filter.on"
                : "screen." + ModMain.MOD_ID + ".soul_sense_filter.off";
        return Component.translatable(
                stateKey,
                Component.translatable(
                        "soul_sense_category." + ModMain.MOD_ID + "." + category.getSerializedName()
                )
        );
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(
                this.font,
                this.title,
                this.width / 2,
                24,
                0xFFFFFF
        );
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
