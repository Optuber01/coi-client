package dev.ua.ikeepcalm.coi.screen.sheet;

final class SheetMetrics {

    static final int GAP = 4;
    static final int ICON = 16;
    static final int SMALL_ICON = 12;
    static final int HEADING_H = 15;
    static final int LINE = 11;
    static final int SYMBOL = 32;

    static final float HOVER_MS = 120;

    /**
     * A vitals row is as tall as its symbol; one carrying a second muted line grows by one.
     */
    static final int VITAL_ROW_H = SYMBOL;
    static final int VITAL_GAP = 4;
    static final int BAR_H = 6;

    static final int LEDGER_ROW_H = 14;
    static final int CHIP_GAP = 3;
    static final int NAV_CARD_H = 30;
    static final int PARA_LINE = 10;

    static final int TWO_COLUMN_MIN = 320;

    /**
     * 3x the two-column threshold's per-card share, not 1.5x the threshold itself.
     */
    static final int THREE_COLUMN_MIN = 540;


    private SheetMetrics() {
    }

    static boolean inBox(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
