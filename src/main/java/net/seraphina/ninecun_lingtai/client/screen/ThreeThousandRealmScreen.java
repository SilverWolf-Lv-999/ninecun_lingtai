package net.seraphina.ninecun_lingtai.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.seraphina.ninecun_lingtai.common.network.ModNetwork;
import net.seraphina.ninecun_lingtai.common.network.packet.TravelToThreeThousandRealmPacket;
import net.seraphina.ninecun_lingtai.common.realm.ThreeThousandRealmCatalogEntry;
import net.seraphina.ninecun_lingtai.common.realm.ThreeThousandRealmCatalogSnapshot;
import net.seraphina.ninecun_lingtai.common.realm.ThreeThousandRealmCatalogStatus;
import org.lwjgl.glfw.GLFW;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class ThreeThousandRealmScreen extends Screen {
    private static final int PANEL_COLOR = 0xF2181B24;
    private static final int PANEL_BORDER_COLOR = 0xFF75644D;
    private static final int ROW_COLOR = 0xD9282C38;
    private static final int ROW_HOVER_COLOR = 0xE23A3E4A;
    private static final int ROW_SELECTED_COLOR = 0xE252493A;
    private static final int PRIMARY_TEXT_COLOR = 0xFFF0E5CA;
    private static final int SECONDARY_TEXT_COLOR = 0xFFB9B2A4;
    private static final int MUTED_TEXT_COLOR = 0xFF817D75;
    private static final int ERROR_TEXT_COLOR = 0xFFFF7777;
    private static final int PROGRESS_TRACK_COLOR = 0xFF2B2D35;
    private static final int PROGRESS_FILL_COLOR = 0xFFD9B968;
    private static final int ROW_HEIGHT = 32;
    private static final int WIDGET_HEIGHT = 20;
    private static final int MAXIMUM_PANEL_WIDTH = 420;
    private static final int MAXIMUM_PANEL_HEIGHT = 320;

    private ThreeThousandRealmCatalogSnapshot thisCatalogSnapshot;
    private List<ThreeThousandRealmCatalogEntry> thisFilteredRealms = List.of();
    private EditBox thisSearchBox;
    private Button thisTravelButton;
    private Button thisCloseButton;
    private ResourceLocation thisSelectedRealmId;
    private String thisSearchQuery = "";
    private boolean thisTravelPending;
    private int thisScrollRow;
    private int thisPanelLeft;
    private int thisPanelTop;
    private int thisPanelWidth;
    private int thisPanelHeight;

    public ThreeThousandRealmScreen(ThreeThousandRealmCatalogSnapshot catalogSnapshot) {
        super(Component.translatable("screen.ninecun_lingtai.three_thousand_realms.title"));
        this.thisCatalogSnapshot = catalogSnapshot;
        refreshFilteredRealms();
    }

    @Override
    protected void init() {
        this.thisPanelWidth = Math.min(MAXIMUM_PANEL_WIDTH, Math.max(280, this.width - 20));
        this.thisPanelHeight = Math.min(MAXIMUM_PANEL_HEIGHT, Math.max(220, this.height - 20));
        this.thisPanelLeft = (this.width - this.thisPanelWidth) / 2;
        this.thisPanelTop = (this.height - this.thisPanelHeight) / 2;

        this.thisSearchBox = new EditBox(
                this.font,
                this.thisPanelLeft + 10,
                this.thisPanelTop + 33,
                this.thisPanelWidth - 20,
                WIDGET_HEIGHT,
                Component.translatable("screen.ninecun_lingtai.three_thousand_realms.search")
        );
        this.thisSearchBox.setMaxLength(80);
        this.thisSearchBox.setHint(
                Component.translatable("screen.ninecun_lingtai.three_thousand_realms.search")
        );
        this.thisSearchBox.setValue(this.thisSearchQuery);
        this.thisSearchBox.setResponder(query -> {
            this.thisSearchQuery = query;
            this.thisScrollRow = 0;
            refreshFilteredRealms();
        });
        this.addRenderableWidget(this.thisSearchBox);

        int footerTop = this.thisPanelTop + this.thisPanelHeight - 25;
        int buttonWidth = Math.min(132, (this.thisPanelWidth - 24) / 2);
        int buttonGap = 4;
        int buttonLeft = this.thisPanelLeft + this.thisPanelWidth
                - buttonWidth * 2 - buttonGap - 10;
        this.thisCloseButton = Button.builder(
                        Component.translatable("screen.ninecun_lingtai.close"),
                        button -> onClose()
                )
                .bounds(buttonLeft, footerTop, buttonWidth, WIDGET_HEIGHT)
                .build();
        this.thisTravelButton = Button.builder(
                        Component.translatable("screen.ninecun_lingtai.three_thousand_realms.travel"),
                        button -> travelToSelectedRealm()
                )
                .bounds(
                        buttonLeft + buttonWidth + buttonGap,
                        footerTop,
                        buttonWidth,
                        WIDGET_HEIGHT
                )
                .build();
        this.addRenderableWidget(this.thisCloseButton);
        this.addRenderableWidget(this.thisTravelButton);
        updateWidgetState();
    }

    @Override
    public void tick() {
        if (this.thisSearchBox != null) {
            this.thisSearchBox.tick();
        }
    }

    @Override
    public void render(
            GuiGraphics guiGraphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        this.renderBackground(guiGraphics);
        int panelRight = this.thisPanelLeft + this.thisPanelWidth;
        int panelBottom = this.thisPanelTop + this.thisPanelHeight;
        guiGraphics.fill(
                this.thisPanelLeft,
                this.thisPanelTop,
                panelRight,
                panelBottom,
                PANEL_BORDER_COLOR
        );
        guiGraphics.fill(
                this.thisPanelLeft + 1,
                this.thisPanelTop + 1,
                panelRight - 1,
                panelBottom - 1,
                PANEL_COLOR
        );
        guiGraphics.drawCenteredString(
                this.font,
                this.title,
                this.width / 2,
                this.thisPanelTop + 8,
                PRIMARY_TEXT_COLOR
        );

        if (this.thisCatalogSnapshot.getStatus() == ThreeThousandRealmCatalogStatus.LOADING) {
            renderLoadingState(guiGraphics);
            if (!this.thisFilteredRealms.isEmpty()) {
                renderRealmList(guiGraphics, mouseX, mouseY);
            }
        } else if (this.thisCatalogSnapshot.getStatus() == ThreeThousandRealmCatalogStatus.FAILED) {
            renderFailedState(guiGraphics);
        } else {
            renderRealmList(guiGraphics, mouseX, mouseY);
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderRealmTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT
                && this.thisCatalogSnapshot.getStatus() == ThreeThousandRealmCatalogStatus.READY
                && mouseX >= this.thisPanelLeft + 10
                && mouseX < this.thisPanelLeft + this.thisPanelWidth - 10
                && mouseY >= getListTop()
                && mouseY < getListBottom()) {
            int visibleRow = ((int) mouseY - getListTop()) / ROW_HEIGHT;
            int realmIndex = this.thisScrollRow + visibleRow;
            if (realmIndex >= 0 && realmIndex < this.thisFilteredRealms.size()) {
                this.thisSelectedRealmId = this.thisFilteredRealms.get(realmIndex).getRealmId();
                updateWidgetState();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (this.thisCatalogSnapshot.getStatus() != ThreeThousandRealmCatalogStatus.READY
                || mouseY < getListTop()
                || mouseY >= getListBottom()) {
            return super.mouseScrolled(mouseX, mouseY, delta);
        }
        int maximumScrollRow = Math.max(0, this.thisFilteredRealms.size() - getVisibleRows());
        if (maximumScrollRow <= 0) {
            return super.mouseScrolled(mouseX, mouseY, delta);
        }
        this.thisScrollRow = Mth.clamp(
                this.thisScrollRow - (int) Math.signum(delta),
                0,
                maximumScrollRow
        );
        return true;
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(null);
        }
    }

    public void updateCatalog(ThreeThousandRealmCatalogSnapshot catalogSnapshot) {
        this.thisCatalogSnapshot = catalogSnapshot;
        refreshFilteredRealms();
        if (this.thisSelectedRealmId != null
                && this.thisFilteredRealms.stream()
                .noneMatch(realm -> realm.getRealmId().equals(this.thisSelectedRealmId))) {
            this.thisSelectedRealmId = null;
        }
        if (this.thisSearchBox != null) {
            updateWidgetState();
        }
    }

    public void handleTravelResult(boolean successful) {
        if (successful) {
            onClose();
            return;
        }
        this.thisTravelPending = false;
        updateWidgetState();
    }

    private void renderLoadingState(GuiGraphics guiGraphics) {
        int progressTop = this.thisFilteredRealms.isEmpty()
                ? this.thisPanelTop + this.thisPanelHeight / 2 - 8
                : this.thisPanelTop + 32;
        int progressLeft = this.thisPanelLeft + 26;
        int progressRight = this.thisPanelLeft + this.thisPanelWidth - 26;
        float progress = (float) this.thisCatalogSnapshot.getCompletedRealmCount()
                / this.thisCatalogSnapshot.getTargetRealmCount();
        int filledWidth = Math.round((progressRight - progressLeft) * Mth.clamp(progress, 0.0F, 1.0F));
        guiGraphics.drawCenteredString(
                this.font,
                Component.translatable("screen.ninecun_lingtai.three_thousand_realms.loading"),
                this.width / 2,
                progressTop - 18,
                SECONDARY_TEXT_COLOR
        );
        guiGraphics.fill(progressLeft, progressTop, progressRight, progressTop + 6, PROGRESS_TRACK_COLOR);
        if (filledWidth > 0) {
            guiGraphics.fill(
                    progressLeft,
                    progressTop,
                    progressLeft + filledWidth,
                    progressTop + 6,
                    PROGRESS_FILL_COLOR
            );
        }
        guiGraphics.drawCenteredString(
                this.font,
                Component.translatable(
                        "screen.ninecun_lingtai.three_thousand_realms.progress",
                        this.thisCatalogSnapshot.getCompletedRealmCount(),
                        this.thisCatalogSnapshot.getTargetRealmCount()
                ),
                this.width / 2,
                progressTop + 12,
                MUTED_TEXT_COLOR
        );
    }

    private void renderFailedState(GuiGraphics guiGraphics) {
        guiGraphics.drawCenteredString(
                this.font,
                Component.translatable("screen.ninecun_lingtai.three_thousand_realms.failed"),
                this.width / 2,
                this.thisPanelTop + this.thisPanelHeight / 2 - 4,
                ERROR_TEXT_COLOR
        );
        guiGraphics.drawCenteredString(
                this.font,
                Component.translatable("screen.ninecun_lingtai.three_thousand_realms.retry_hint"),
                this.width / 2,
                this.thisPanelTop + this.thisPanelHeight / 2 + 12,
                MUTED_TEXT_COLOR
        );
    }

    private void renderRealmList(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int listTop = getListTop();
        int listBottom = getListBottom();
        int visibleRows = getVisibleRows();
        int maximumScrollRow = Math.max(0, this.thisFilteredRealms.size() - visibleRows);
        this.thisScrollRow = Mth.clamp(this.thisScrollRow, 0, maximumScrollRow);

        if (this.thisCatalogSnapshot.getStatus() == ThreeThousandRealmCatalogStatus.READY) {
            guiGraphics.drawString(
                    this.font,
                    Component.translatable(
                            "screen.ninecun_lingtai.three_thousand_realms.count",
                            this.thisFilteredRealms.size()
                    ),
                    this.thisPanelLeft + 11,
                    this.thisPanelTop + 20,
                    SECONDARY_TEXT_COLOR,
                    false
            );
        }
        guiGraphics.enableScissor(
                this.thisPanelLeft + 8,
                listTop,
                this.thisPanelLeft + this.thisPanelWidth - 8,
                listBottom
        );
        for (int visibleRow = 0; visibleRow < visibleRows; visibleRow++) {
            int realmIndex = this.thisScrollRow + visibleRow;
            if (realmIndex >= this.thisFilteredRealms.size()) {
                break;
            }
            ThreeThousandRealmCatalogEntry realm = this.thisFilteredRealms.get(realmIndex);
            int rowTop = listTop + visibleRow * ROW_HEIGHT;
            boolean hovered = mouseX >= this.thisPanelLeft + 10
                    && mouseX < this.thisPanelLeft + this.thisPanelWidth - 10
                    && mouseY >= rowTop
                    && mouseY < rowTop + ROW_HEIGHT - 3;
            int rowColor = realm.getRealmId().equals(this.thisSelectedRealmId)
                    ? ROW_SELECTED_COLOR
                    : (hovered ? ROW_HOVER_COLOR : ROW_COLOR);
            guiGraphics.fill(
                    this.thisPanelLeft + 10,
                    rowTop,
                    this.thisPanelLeft + this.thisPanelWidth - 10,
                    rowTop + ROW_HEIGHT - 3,
                    rowColor
            );
            String displayName = truncate(
                    realm.getDisplayName(),
                    this.thisPanelWidth - 34
            );
            guiGraphics.drawString(
                    this.font,
                    displayName,
                    this.thisPanelLeft + 17,
                    rowTop + 5,
                    PRIMARY_TEXT_COLOR,
                    false
            );
            guiGraphics.drawString(
                    this.font,
                    truncate(realm.getRealmId().toString(), this.thisPanelWidth - 34),
                    this.thisPanelLeft + 17,
                    rowTop + 18,
                    MUTED_TEXT_COLOR,
                    false
            );
        }
        guiGraphics.disableScissor();

        if (this.thisFilteredRealms.isEmpty()) {
            if (this.thisCatalogSnapshot.getStatus() != ThreeThousandRealmCatalogStatus.LOADING) {
                guiGraphics.drawCenteredString(
                        this.font,
                        Component.translatable(
                                this.thisSearchQuery.isBlank()
                                        ? "screen.ninecun_lingtai.three_thousand_realms.empty"
                                        : "screen.ninecun_lingtai.three_thousand_realms.no_match"
                        ),
                        this.width / 2,
                        listTop + 16,
                        MUTED_TEXT_COLOR
                );
            }
        }
        renderScrollBar(guiGraphics, visibleRows, listTop, listBottom);
    }

    private void renderScrollBar(
            GuiGraphics guiGraphics,
            int visibleRows,
            int listTop,
            int listBottom
    ) {
        int maximumScrollRow = Math.max(0, this.thisFilteredRealms.size() - visibleRows);
        if (maximumScrollRow <= 0) {
            return;
        }
        int trackLeft = this.thisPanelLeft + this.thisPanelWidth - 7;
        int trackHeight = listBottom - listTop;
        int thumbHeight = Math.max(12, trackHeight * visibleRows / this.thisFilteredRealms.size());
        int thumbTop = listTop + (trackHeight - thumbHeight) * this.thisScrollRow / maximumScrollRow;
        guiGraphics.fill(trackLeft, listTop, trackLeft + 2, listBottom, PROGRESS_TRACK_COLOR);
        guiGraphics.fill(trackLeft, thumbTop, trackLeft + 2, thumbTop + thumbHeight, PROGRESS_FILL_COLOR);
    }

    private void renderRealmTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        ThreeThousandRealmCatalogEntry realm = getRealmAt(mouseX, mouseY);
        if (realm != null) {
            guiGraphics.renderTooltip(
                    this.font,
                    Component.literal(realm.getRealmId().toString()),
                    mouseX,
                    mouseY
            );
        }
    }

    private ThreeThousandRealmCatalogEntry getRealmAt(double mouseX, double mouseY) {
        if (this.thisCatalogSnapshot.getStatus() != ThreeThousandRealmCatalogStatus.READY
                || mouseX < this.thisPanelLeft + 10
                || mouseX >= this.thisPanelLeft + this.thisPanelWidth - 10
                || mouseY < getListTop()
                || mouseY >= getListBottom()) {
            return null;
        }
        int visibleRow = ((int) mouseY - getListTop()) / ROW_HEIGHT;
        if ((int) mouseY - getListTop() - visibleRow * ROW_HEIGHT >= ROW_HEIGHT - 3) {
            return null;
        }
        int realmIndex = this.thisScrollRow + visibleRow;
        return realmIndex >= 0 && realmIndex < this.thisFilteredRealms.size()
                ? this.thisFilteredRealms.get(realmIndex)
                : null;
    }

    private void travelToSelectedRealm() {
        if (this.thisSelectedRealmId == null || this.thisTravelPending) {
            return;
        }
        this.thisTravelPending = true;
        updateWidgetState();
        ModNetwork.sendToServer(new TravelToThreeThousandRealmPacket(this.thisSelectedRealmId));
    }

    private void refreshFilteredRealms() {
        String normalizedQuery = this.thisSearchQuery.trim().toLowerCase(Locale.ROOT);
        this.thisFilteredRealms = this.thisCatalogSnapshot.getRealms().stream()
                .filter(realm -> normalizedQuery.isEmpty()
                        || realm.getDisplayName().toLowerCase(Locale.ROOT).contains(normalizedQuery)
                        || realm.getRealmId().toString().contains(normalizedQuery))
                .sorted(Comparator.comparing(ThreeThousandRealmCatalogEntry::getDisplayName))
                .toList();
    }

    private void updateWidgetState() {
        boolean catalogReady = this.thisCatalogSnapshot.getStatus()
                == ThreeThousandRealmCatalogStatus.READY;
        this.thisSearchBox.visible = catalogReady;
        this.thisSearchBox.active = catalogReady;
        this.thisTravelButton.setMessage(
                Component.translatable(
                        this.thisTravelPending
                                ? "screen.ninecun_lingtai.three_thousand_realms.travel_loading"
                                : "screen.ninecun_lingtai.three_thousand_realms.travel"
                )
        );
        this.thisTravelButton.visible = catalogReady;
        this.thisTravelButton.active = catalogReady
                && this.thisSelectedRealmId != null
                && !this.thisTravelPending;
    }

    private int getListTop() {
        return this.thisPanelTop + 58;
    }

    private int getListBottom() {
        return this.thisPanelTop + this.thisPanelHeight - 32;
    }

    private int getVisibleRows() {
        return Math.max(1, (getListBottom() - getListTop()) / ROW_HEIGHT);
    }

    private String truncate(String text, int maximumWidth) {
        if (this.font.width(text) <= maximumWidth) {
            return text;
        }
        return this.font.plainSubstrByWidth(
                text,
                Math.max(0, maximumWidth - this.font.width("..."))
        ) + "...";
    }
}
