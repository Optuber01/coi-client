package dev.ua.ikeepcalm.coi.screen.sheet;

import dev.ua.ikeepcalm.coi.domain.beyonder.model.SheetState;
import dev.ua.ikeepcalm.coi.screen.menu.MenuTheme;
import dev.ua.ikeepcalm.coi.ui.CoiStyle;

/**
 * A row picks a {top, bottom, text} palette rather than a colour, so the four stacked gauges
 * stay distinguishable as any one of them changes.
 */
final class SheetPalette {

    public static final int SYMBOL_EMPTY = 0xFF4A4A56;

    public static final int[][] STAGE_COLORS = {
            {0xFF00FFCC, 0xFF00AA88, 0xFF00FFCC},
            {0xFFFFAA00, 0xFFCC7700, 0xFFFFAA00},
            {0xFFDD2222, 0xFF991111, 0xFFDD2222},
            {0xFFFF0000, 0xFF8B0000, 0xFFFF0055},
            {0xFF993399, 0xFF3A3A3A, 0xFF993399}
    };

    public static final int[][] TIRED_COLORS = {
            {0xFF9A9AA2, 0xFF6E6E76, 0xFF9A9AA2},
            {0xFFC9B27A, 0xFF8E7C50, 0xFFC9B27A},
            {0xFFE0A83C, 0xFF9A6E1E, 0xFFE0A83C},
            {0xFFE07C1E, 0xFF9A4E0E, 0xFFE07C1E},
            {0xFFD64518, 0xFF8B2A0A, 0xFFD64518}
    };

    public static final int[] SPIRIT_COLORS = {0xFFB3CFEC, 0xFF5D8FC2, 0xFF9FC4E8};
    public static final int RED = MenuTheme.DANGER;
    public static final int VIOLET = 0xFFC9A0E8;
    public static final int AMBER = MenuTheme.WARN;

    private SheetPalette() {
    }

    /** The pathway names the card's accent, falling back to gold before the first packet. */
    public static int accent() {
        return SheetState.hasData() ? SheetState.pathwayArgb() : CoiStyle.ACCENT;
    }

    public static int[] healthColors(double fraction) {
        if (fraction > 0.75) return new int[]{0xFF4CE05A, 0xFF2A8F34, 0xFF4CE05A};
        if (fraction > 0.50) return new int[]{0xFFE8E04C, 0xFF9A9424, 0xFFE8E04C};
        if (fraction > 0.25) return new int[]{0xFFE0A83C, 0xFF9A6E1E, 0xFFE0A83C};
        return new int[]{RED, 0xFF8F2A2A, RED};
    }
}
