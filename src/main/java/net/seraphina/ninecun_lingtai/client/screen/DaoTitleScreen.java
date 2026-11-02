package net.seraphina.ninecun_lingtai.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.seraphina.ninecun_lingtai.api.cultivation.CultivationRealm;
import net.seraphina.ninecun_lingtai.api.daotitle.DaoTitleSuffix;
import net.seraphina.ninecun_lingtai.client.player.ClientDaoTitleManager;
import net.seraphina.ninecun_lingtai.common.network.ModNetwork;
import net.seraphina.ninecun_lingtai.common.network.packet.SetDaoTitlePacket;
import net.seraphina.ninecun_lingtai.common.player.DaoTitleSnapshot;
import net.seraphina.ninecun_lingtai.utility.DaoTitleUtility;

public final class DaoTitleScreen extends Screen {
    private EditBox customNameBox;
    private DaoTitleSuffix selectedSuffix = DaoTitleSuffix.IMMORTAL;
    private Button suffixButton;
    private Button saveButton;
    private boolean initializedFromSnapshot;

    public DaoTitleScreen() {
        super(Component.translatable("screen.ninecun_lingtai.dao_title.title"));
    }

    @Override
    protected void init() {
        int panelWidth = 300;
        int left = (this.width - panelWidth) / 2;
        this.customNameBox = new EditBox(this.font, left + 20, this.height / 2 - 48, panelWidth - 40, 20,
                Component.translatable("screen.ninecun_lingtai.dao_title.custom_name"));
        this.customNameBox.setMaxLength(32);
        DaoTitleSnapshot snapshot = ClientDaoTitleManager.getLocalSnapshot();
        if (snapshot != null) {
            this.customNameBox.setValue(snapshot.customName());
            this.selectedSuffix = snapshot.immortalSuffix();
            this.initializedFromSnapshot = true;
        }
        this.addRenderableWidget(this.customNameBox);
        this.suffixButton = this.addRenderableWidget(Button.builder(getSuffixText(), button -> cycleSuffix())
                .bounds(left + 20, this.height / 2 - 18, panelWidth - 40, 20).build());
        this.saveButton = this.addRenderableWidget(Button.builder(
                        Component.translatable("screen.ninecun_lingtai.dao_title.save"),
                        button -> save())
                .bounds(left + 20, this.height / 2 + 45, panelWidth - 40, 20).build());
        updateState();
        this.setInitialFocus(this.customNameBox);
    }

    @Override
    public void tick() {
        super.tick();
        DaoTitleSnapshot snapshot = ClientDaoTitleManager.getLocalSnapshot();
        if (!this.initializedFromSnapshot && snapshot != null) {
            this.customNameBox.setValue(snapshot.customName());
            this.selectedSuffix = snapshot.immortalSuffix();
            this.suffixButton.setMessage(getSuffixText());
            this.initializedFromSnapshot = true;
        }
        updateState();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        int panelWidth = 300;
        int left = (this.width - panelWidth) / 2;
        int top = this.height / 2 - 105;
        guiGraphics.fill(left, top, left + panelWidth, top + 190, 0xF0181B24);
        guiGraphics.fill(left, top, left + panelWidth, top + 1, 0xFF75644D);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, top + 12, 0xFFF0E5CA);
        guiGraphics.drawString(this.font, Component.translatable("screen.ninecun_lingtai.dao_title.custom_name"), left + 20, top + 34, 0xFFB9B2A4, false);
        guiGraphics.drawString(this.font, Component.translatable("screen.ninecun_lingtai.dao_title.suffix"), left + 20, top + 65, 0xFFB9B2A4, false);
        guiGraphics.drawString(this.font, Component.translatable("screen.ninecun_lingtai.dao_title.preview"), left + 20, top + 105, 0xFFB9B2A4, false);
        DaoTitleSnapshot snapshot = ClientDaoTitleManager.getLocalSnapshot();
        Component previewText;
        if (snapshot == null) {
            previewText = Component.literal("...");
        } else {
            String preview = DaoTitleUtility.getDisplayTitle(new DaoTitleSnapshot(
                    this.customNameBox == null ? snapshot.customName() : this.customNameBox.getValue(),
                    this.selectedSuffix,
                    snapshot.realm()
            ));
            previewText = preview.isBlank()
                    ? Component.translatable("screen.ninecun_lingtai.dao_title.locked")
                    : DaoTitleUtility.formatName(Minecraft.getInstance().player.getName(), preview);
        }
        guiGraphics.drawCenteredString(this.font, previewText, this.width / 2, top + 122, 0xFFE7D6B1);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void cycleSuffix() {
        DaoTitleSuffix[] suffixes = DaoTitleSuffix.values();
        this.selectedSuffix = suffixes[(this.selectedSuffix.ordinal() + 1) % suffixes.length];
        this.suffixButton.setMessage(getSuffixText());
    }

    private Component getSuffixText() {
        return Component.translatable("screen.ninecun_lingtai.dao_title.suffix_value", this.selectedSuffix.getChineseName());
    }

    private void updateState() {
        DaoTitleSnapshot snapshot = ClientDaoTitleManager.getLocalSnapshot();
        boolean unlocked = snapshot != null && snapshot.isUnlocked();
        boolean immortal = unlocked && snapshot.realm() == CultivationRealm.IMMORTAL;
        if (this.suffixButton != null) {
            this.suffixButton.active = immortal;
        }
        if (this.saveButton != null) {
            this.saveButton.active = unlocked;
        }
    }

    private void save() {
        if (this.saveButton == null || !this.saveButton.active) {
            return;
        }
        ModNetwork.sendToServer(new SetDaoTitlePacket(this.customNameBox.getValue(), this.selectedSuffix.ordinal()));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
