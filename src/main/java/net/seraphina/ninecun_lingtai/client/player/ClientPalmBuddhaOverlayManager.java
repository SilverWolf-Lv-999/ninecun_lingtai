package net.seraphina.ninecun_lingtai.client.player;

import net.minecraft.client.Minecraft;
import net.seraphina.ninecun_lingtai.client.utility.PalmBuddhaOverlayGeometryUtility;
import net.seraphina.ninecun_lingtai.common.network.ModNetwork;
import net.seraphina.ninecun_lingtai.common.network.packet.ReleasePalmBuddhaMemberPacket;
import net.seraphina.ninecun_lingtai.common.spell.palm_buddha.PalmBuddhaMemberSnapshot;
import net.seraphina.ninecun_lingtai.common.spell.palm_buddha.PalmBuddhaReleaseResult;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Controls the non-Screen Palm Buddha member overlay.
 *
 * <p>The overlay is a single shader-backed surface. It never creates a
 * replacement world, camera, entity copy, or a collection of vanilla
 * buttons.</p>
 */
public final class ClientPalmBuddhaOverlayManager {
    private static final int RELEASE_RESULT_TIMEOUT_TICKS = 80;

    private static boolean thisActive;
    private static int thisSelectedRow = -1;
    private static UUID thisPendingReleaseMemberId;
    private static int thisPendingReleaseTicks;

    private ClientPalmBuddhaOverlayManager() {
    }

    public static void open() {
        if (Minecraft.getInstance().player == null) {
            return;
        }
        thisActive = true;
        thisSelectedRow = -1;
        thisPendingReleaseMemberId = null;
        thisPendingReleaseTicks = 0;
        ClientPalmBuddhaReleaseFeedbackManager.reset();
        ClientPalmBuddhaMemberManager.reset();
        ClientPalmBuddhaMemberManager.requestFirstPage();
    }

    public static void close() {
        thisActive = false;
        thisSelectedRow = -1;
        thisPendingReleaseMemberId = null;
        thisPendingReleaseTicks = 0;
        ClientPalmBuddhaReleaseFeedbackManager.reset();
    }

    public static boolean isActive() {
        return thisActive && Minecraft.getInstance().player != null;
    }

    public static void nextPage() {
        if (!isActive() || isInteractionLocked()) {
            return;
        }
        int pageCount = ClientPalmBuddhaMemberManager.getCurrentPage().pageCount();
        int currentPage = ClientPalmBuddhaMemberManager.getCurrentPage().pageIndex();
        if (pageCount > 0 && currentPage + 1 < pageCount) {
            thisSelectedRow = -1;
            ClientPalmBuddhaMemberManager.requestPage(currentPage + 1);
        }
    }

    public static void previousPage() {
        if (!isActive() || isInteractionLocked()) {
            return;
        }
        int currentPage = ClientPalmBuddhaMemberManager.getCurrentPage().pageIndex();
        if (currentPage > 0) {
            thisSelectedRow = -1;
            ClientPalmBuddhaMemberManager.requestPage(currentPage - 1);
        }
    }

    /** Consumes only the few navigation key mappings used by this overlay. */
    public static boolean handleKeybinds() {
        if (!isActive()) {
            return false;
        }
        Minecraft minecraft = Minecraft.getInstance();
        boolean consumed = false;
        while (minecraft.options.keyLeft.consumeClick()) {
            previousPage();
            consumed = true;
        }
        while (minecraft.options.keyRight.consumeClick()) {
            nextPage();
            consumed = true;
        }
        while (minecraft.options.keyInventory.consumeClick()) {
            // Consume the vanilla inventory binding without opening a Screen;
            // this overlay is closed only by the direct Escape Mixin path.
            consumed = true;
        }
        return consumed;
    }

    public static int getSelectedRow() {
        return isActive() ? thisSelectedRow : -1;
    }

    /**
     * Returns whether the selected member can receive the single release gesture.
     * This is only a client-side visual guard; the server remains authoritative.
     */
    public static boolean isSelectedMemberReleasable() {
        return !isInteractionLocked() && getSelectedMember()
                .map(member -> member.online() && member.inExpectedRealm())
                .orElse(false);
    }

    /**
     * Sends only the selected member UUID.  No position, dimension, or other
     * client-authored state is included in the release request.
     */
    public static boolean releaseSelectedMemberAt(double mouseX, double mouseY) {
        if (!isActive() || isInteractionLocked() || thisSelectedRow < 0) {
            return false;
        }
        Minecraft minecraft = Minecraft.getInstance();
        PalmBuddhaOverlayGeometryUtility.PanelBounds panelBounds =
                PalmBuddhaOverlayGeometryUtility.calculatePanelBounds(
                        minecraft.getWindow().getGuiScaledWidth(),
                        minecraft.getWindow().getGuiScaledHeight()
                );
        int clickedRow = PalmBuddhaOverlayGeometryUtility.findMemberRow(
                panelBounds,
                mouseX,
                mouseY,
                ClientPalmBuddhaMemberManager.getCurrentPage().members().size()
        );
        if (clickedRow != thisSelectedRow) {
            return false;
        }
        PalmBuddhaMemberSnapshot member = getSelectedMember().orElse(null);
        if (member == null || !member.online() || !member.inExpectedRealm()) {
            return false;
        }
        ModNetwork.sendToServer(new ReleasePalmBuddhaMemberPacket(member.memberId()));
        thisPendingReleaseMemberId = member.memberId();
        thisPendingReleaseTicks = RELEASE_RESULT_TIMEOUT_TICKS;
        return true;
    }

    /**
     * 接收服务端的放生结果。刷新请求只在结果抵达后发出，避免客户端先显示过期摘要。
     */
    public static void acceptReleaseResult(
            UUID memberId,
            boolean successful,
            PalmBuddhaReleaseResult.FailureReason failureReason
    ) {
        if (!isActive()
                || thisPendingReleaseMemberId == null
                || !Objects.equals(thisPendingReleaseMemberId, memberId)) {
            return;
        }
        thisPendingReleaseMemberId = null;
        thisPendingReleaseTicks = 0;
        thisSelectedRow = -1;
        ClientPalmBuddhaReleaseFeedbackManager.accept(memberId, successful, failureReason);
    }

    /** Advances the short feedback animation and refreshes the page after it ends. */
    public static void tick() {
        if (!isActive()) {
            return;
        }
        if (isReleasePending()) {
            thisPendingReleaseTicks--;
            if (thisPendingReleaseTicks <= 0) {
                acceptReleaseResult(
                        thisPendingReleaseMemberId,
                        false,
                        PalmBuddhaReleaseResult.FailureReason.REQUEST_TIMEOUT
                );
            }
        }
        boolean feedbackWasActive = ClientPalmBuddhaReleaseFeedbackManager.isActive();
        ClientPalmBuddhaReleaseFeedbackManager.tick();
        if (feedbackWasActive && !ClientPalmBuddhaReleaseFeedbackManager.isActive()) {
            ClientPalmBuddhaMemberManager.requestPage(
                    ClientPalmBuddhaMemberManager.getCurrentPage().pageIndex()
            );
        }
    }

    public static boolean isReleasePending() {
        return isActive() && thisPendingReleaseMemberId != null;
    }

    private static boolean isInteractionLocked() {
        return isReleasePending() || ClientPalmBuddhaReleaseFeedbackManager.isActive();
    }

    public static float getReleaseFeedbackType() {
        if (!ClientPalmBuddhaReleaseFeedbackManager.isActive()) {
            return 0.0F;
        }
        return ClientPalmBuddhaReleaseFeedbackManager.isSuccessful() ? 1.0F : -1.0F;
    }

    public static float getReleaseFeedbackProgress(float partialTicks) {
        return ClientPalmBuddhaReleaseFeedbackManager.getProgress(partialTicks);
    }

    public static Optional<net.minecraft.network.chat.Component> getReleaseFeedbackMessage() {
        return ClientPalmBuddhaReleaseFeedbackManager.isActive()
                ? Optional.of(ClientPalmBuddhaReleaseFeedbackManager.getMessage())
                : Optional.empty();
    }

    /** Converts physical window coordinates before applying the selected-row release gesture. */
    public static void releaseSelectedMemberAtWindowCoordinates(double windowX, double windowY) {
        if (!isActive()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        double guiX = windowX * minecraft.getWindow().getGuiScaledWidth() / minecraft.getWindow().getWidth();
        double guiY = windowY * minecraft.getWindow().getGuiScaledHeight() / minecraft.getWindow().getHeight();
        releaseSelectedMemberAt(guiX, guiY);
    }

    /** Selects one visible member row; -1 means that the click hit no row. */
    public static void selectMemberAt(double mouseX, double mouseY) {
        if (!isActive() || isInteractionLocked()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        PalmBuddhaOverlayGeometryUtility.PanelBounds panelBounds =
                PalmBuddhaOverlayGeometryUtility.calculatePanelBounds(
                        minecraft.getWindow().getGuiScaledWidth(),
                        minecraft.getWindow().getGuiScaledHeight()
                );
        thisSelectedRow = PalmBuddhaOverlayGeometryUtility.findMemberRow(
                panelBounds,
                mouseX,
                mouseY,
                ClientPalmBuddhaMemberManager.getCurrentPage().members().size()
        );
    }

    /** Converts physical window coordinates to GUI-scaled coordinates before selecting a row. */
    public static void selectMemberAtWindowCoordinates(double windowX, double windowY) {
        if (!isActive()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        double guiX = windowX * minecraft.getWindow().getGuiScaledWidth() / minecraft.getWindow().getWidth();
        double guiY = windowY * minecraft.getWindow().getGuiScaledHeight() / minecraft.getWindow().getHeight();
        selectMemberAt(guiX, guiY);
    }

    private static Optional<PalmBuddhaMemberSnapshot> getSelectedMember() {
        if (!isActive() || thisSelectedRow < 0) {
            return Optional.empty();
        }
        var members = ClientPalmBuddhaMemberManager.getCurrentPage().members();
        if (thisSelectedRow >= members.size()) {
            return Optional.empty();
        }
        return Optional.of(members.get(thisSelectedRow));
    }

    public static void reset() {
        thisActive = false;
        thisSelectedRow = -1;
        thisPendingReleaseMemberId = null;
        thisPendingReleaseTicks = 0;
        ClientPalmBuddhaReleaseFeedbackManager.reset();
        ClientPalmBuddhaMemberManager.reset();
    }
}
