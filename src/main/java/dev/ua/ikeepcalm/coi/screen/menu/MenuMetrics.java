package dev.ua.ikeepcalm.coi.screen.menu;

/**
 * The geometry a menu document is drawn to, and the one hit test everything uses.
 */
final class MenuMetrics {

    /**
     * Card geometry is shared with the character sheet — see {@code CoiStyle.cardWidth}.
     */
    static final int ICON = 16;
    static final int SMALL_ICON = 12;
    static final int GAP = 4;
    static final int MIN_TILE = 18;
    static final int FIELD_H = 18;

    static final int MIN_PANEL_W = 72;

    static final int HERO_ICON = 32;
    static final int HERO_RING = 28;
    static final int STAT_RING = 24;

    static final int RAIL_X = 5;
    static final int RAIL_TEXT_X = 18;

    static final float HOVER_MS = 120;
    static final float GAUGE_MS = 200;

    private MenuMetrics() {
    }

    static boolean inBox(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
