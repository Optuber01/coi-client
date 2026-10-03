package dev.ua.ikeepcalm.coi.screen.ability;

/**
 * These unequal row heights are why every scroll calculation in the picker is in pixels.
 */
final class PickerMetrics {

    /** Rows are no longer uniform: only ability rows carry two text lines. */
    static final int ROW_H = 26;
    static final int HEADER_H = 13;
    static final int UNBIND_H = 18;
    static final int MESSAGE_H = 18;

    static final int PAD = 8;
    static final int SEARCH_H = 20;
    static final int ICON = 16;
    /** Shared text column for both lines of an ability row, clear of the icon. */
    static final int TEXT_INSET = PAD + ICON + 4;
    /** Second column of the meta line; widened when the cost badge overruns it. */
    static final int COOLDOWN_COL = 44;

    static final int MIN_PANEL_W = 260;
    static final int MAX_PANEL_W = 360;

    private PickerMetrics() {
    }
}
