package net.seraphina.ninecun_lingtai.client.utility;

/** Shared layout and hit-test math for the shader-backed Palm Buddha overlay. */
public final class PalmBuddhaOverlayGeometryUtility {
    public static final int MAXIMUM_VISIBLE_ROWS = 12;

    private static final float MAXIMUM_PANEL_WIDTH = 560.0F;
    private static final float MAXIMUM_PANEL_HEIGHT = 360.0F;
    private static final float PANEL_MARGIN = 12.0F;
    private static final float HEADER_HEIGHT = 58.0F;
    private static final float FOOTER_HEIGHT = 42.0F;
    private static final float CONTENT_HORIZONTAL_MARGIN = 20.0F;

    private PalmBuddhaOverlayGeometryUtility() {
    }

    public static PanelBounds calculatePanelBounds(int screenWidth, int screenHeight) {
        float panelWidth = Math.min(
                MAXIMUM_PANEL_WIDTH,
                Math.max(220.0F, screenWidth - PANEL_MARGIN * 2.0F)
        );
        float panelHeight = Math.min(
                MAXIMUM_PANEL_HEIGHT,
                Math.max(240.0F, screenHeight - PANEL_MARGIN * 2.0F)
        );
        return new PanelBounds(
                (screenWidth - panelWidth) * 0.5F,
                (screenHeight - panelHeight) * 0.5F,
                panelWidth,
                panelHeight
        );
    }

    public static int findMemberRow(
            PanelBounds panelBounds,
            double mouseX,
            double mouseY,
            int memberCount
    ) {
        if (memberCount <= 0 || mouseX < panelBounds.left() + CONTENT_HORIZONTAL_MARGIN
                || mouseX >= panelBounds.right() - CONTENT_HORIZONTAL_MARGIN
                || mouseY < panelBounds.rowsTop()
                || mouseY >= panelBounds.rowsBottom()) {
            return -1;
        }
        int row = (int) ((mouseY - panelBounds.rowsTop()) / panelBounds.rowHeight());
        return row >= 0 && row < Math.min(MAXIMUM_VISIBLE_ROWS, memberCount) ? row : -1;
    }

    public static float rowTextTop(PanelBounds panelBounds, int row) {
        return panelBounds.rowsTop() + row * panelBounds.rowHeight() + 6.0F;
    }

    public record PanelBounds(float left, float top, float width, float height) {
        public float right() {
            return this.left + this.width;
        }

        public float bottom() {
            return this.top + this.height;
        }

        public float rowsTop() {
            return this.top + HEADER_HEIGHT;
        }

        public float rowsBottom() {
            return this.bottom() - FOOTER_HEIGHT;
        }

        public float rowHeight() {
            return (this.rowsBottom() - this.rowsTop()) / MAXIMUM_VISIBLE_ROWS;
        }
    }
}
