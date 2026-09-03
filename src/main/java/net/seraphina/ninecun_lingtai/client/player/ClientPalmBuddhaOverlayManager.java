package net.seraphina.ninecun_lingtai.client.player;

import net.minecraft.client.Minecraft;
import net.seraphina.ninecun_lingtai.client.utility.PalmBuddhaOverlayGeometryUtility;

/**
 * Controls the non-Screen Palm Buddha member overlay.
 *
 * <p>The overlay is a single shader-backed surface. It never creates a
 * replacement world, camera, entity copy, or a collection of vanilla
 * buttons.</p>
 */
public final class ClientPalmBuddhaOverlayManager {
    private static boolean thisActive;
    private static int thisSelectedRow = -1;

    private ClientPalmBuddhaOverlayManager() {
    }

    public static void open() {
        if (Minecraft.getInstance().player == null) {
            return;
        }
        thisActive = true;
        thisSelectedRow = -1;
        ClientPalmBuddhaMemberManager.reset();
        ClientPalmBuddhaMemberManager.requestFirstPage();
    }

    public static void close() {
        thisActive = false;
        thisSelectedRow = -1;
    }

    public static boolean isActive() {
        return thisActive && Minecraft.getInstance().player != null;
    }

    public static void nextPage() {
        if (!isActive()) {
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
        if (!isActive()) {
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

    /** Selects one visible member row; -1 means that the click hit no row. */
    public static void selectMemberAt(double mouseX, double mouseY) {
        if (!isActive()) {
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

    public static void reset() {
        thisActive = false;
        thisSelectedRow = -1;
        ClientPalmBuddhaMemberManager.reset();
    }
}
