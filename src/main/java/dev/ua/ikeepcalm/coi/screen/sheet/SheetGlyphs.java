package dev.ua.ikeepcalm.coi.screen.sheet;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Drawn rather than blitted: 16px is the floor for the mod's bitmap artwork, and the sheet needs
 * marks at 16 and 32. An 8×8 grid blown up by whole pixels is exact at both — a 16px box gives
 * 2px cells, a 32px box 4px ones.
 */
public class SheetGlyphs {

    /**
     * The same {@code CoiIcons.GLYPHS} names a server-authored document uses.
     */
    public static final String GLYPH_HEALTH = "health";
    public static final String GLYPH_GROWTH = "growth";
    public static final String GLYPH_RESTORE = "restore";
    public static final String GLYPH_WARD = "ward";
    public static final String GLYPH_DIVINATION = "divination";
    public static final String GLYPH_DEFENSE = "defense";
    public static final String GLYPH_SOUL = "soul";
    public static final String GLYPH_POWER = "power";
    public static final String GLYPH_RESIST = "resist";
    public static final String GLYPH_AUTHORITY = "authority";
    public static final String GLYPH_COOLDOWN = "cooldown";

    private static final int GRID = 8;
    /** Crossed blades: a contest, not a chair. Deliberately nothing like {@link #THRONE} — the
     * two must not read as the same door. */
    public static final String[] BLADES = {
            "X......X",
            "XX....XX",
            ".XX..XX.",
            "..XXXX..",
            "...XX...",
            "..XXXX..",
            ".XX..XX.",
            "XX....XX"
    };
    private static int bandBottom = Integer.MAX_VALUE;

    public static final String[] HEART = {
            ".XX..XX.",
            "XXXXXXXX",
            "XXXXXXXX",
            "XXXXXXXX",
            ".XXXXXX.",
            "..XXXX..",
            "...XX...",
            "........"
    };

    public static final String[] FLASK = {
            "..XXXX..",
            "...XX...",
            "...XX...",
            "..XXXX..",
            ".XX..XX.",
            "XX....XX",
            "XX....XX",
            ".XXXXXX."
    };

    public static final String[] HOURGLASS = {
            "XXXXXXXX",
            ".XXXXXX.",
            "..XXXX..",
            "...XX...",
            "...XX...",
            "..XXXX..",
            ".XXXXXX.",
            "XXXXXXXX"
    };

    public static final String[] CHURCH = {
            "...XX...",
            "...XX...",
            ".XXXXXX.",
            "...XX...",
            "..XXXX..",
            ".XXXXXX.",
            "XXXXXXXX",
            "XX.XX.XX"
    };

    public static final String[] SPARK = {
            "...XX...",
            "...XX...",
            "..XXXX..",
            "XXXXXXXX",
            "XXXXXXXX",
            "..XXXX..",
            "...XX...",
            "...XX..."
    };

    public static final String[] BEAST = {
            "XX....XX",
            "XX....XX",
            "XXXXXXXX",
            "XXXXXXXX",
            "X.XXXX.X",
            "XXXXXXXX",
            ".XXXXXX.",
            "..XXXX.."
    };

    public static final String[] GEM = {
            ".XXXXXX.",
            "XXXXXXXX",
            "XXXXXXXX",
            ".XXXXXX.",
            ".XXXXXX.",
            "..XXXX..",
            "..XXXX..",
            "...XX..."
    };

    public static final String[] CROWN = {
            "........",
            "X..XX..X",
            "X..XX..X",
            "XX.XX.XX",
            "XXXXXXXX",
            "XXXXXXXX",
            "XXXXXXXX",
            "........"
    };

    public static final String[] PIN = {
            "..XXXX..",
            ".XXXXXX.",
            "XX....XX",
            "XX....XX",
            ".XXXXXX.",
            "..XXXX..",
            "...XX...",
            "...XX..."
    };

    public static final String[] THRONE = {
            "X......X",
            "XX....XX",
            "XXXXXXXX",
            "XXXXXXXX",
            "XXXXXXXX",
            "XXXXXXXX",
            "XX....XX",
            "XX....XX"
    };
    /** The band {@link #drawFilled} is painting, in absolute screen pixels. Render thread only. */
    private static int bandTop = Integer.MIN_VALUE;

    public static final String[] PILLARS = {
            "...XX...",
            "..XXXX..",
            ".XXXXXX.",
            "XXXXXXXX",
            "XX.XX.XX",
            "XX.XX.XX",
            "XX.XX.XX",
            "XXXXXXXX"
    };

    public static final String[] BLOCKS = {
            "........",
            "XXXXXXXX",
            "XXXXXXXX",
            "XX.XX.XX",
            "XXXXXXXX",
            "XXXXXXXX",
            "XX.XX.XX",
            "........"
    };

    public static final String[] WARNING = {
            "...XX...",
            "..XXXX..",
            "..X..X..",
            ".XX..XX.",
            ".X.XX.X.",
            "XX.XX.XX",
            "XX....XX",
            "XXXXXXXX"
    };

    public static final String[] LOCK = {
            "..XXXX..",
            ".XX..XX.",
            ".XX..XX.",
            "XXXXXXXX",
            "XXX..XXX",
            "XXX..XXX",
            "XXXXXXXX",
            "........"
    };

    private SheetGlyphs() {
    }

    public static void draw(GuiGraphicsExtractor ctx, String[] glyph, int x, int y, int size, int argb) {
        int cell = Math.max(1, size / GRID);
        int span = cell * GRID;
        int originX = x + (size - span) / 2;
        int originY = y + (size - span) / 2;
        for (int row = 0; row < GRID; row++) {
            String line = glyph[row];
            for (int col = 0; col < GRID; col++) {
                if (line.charAt(col) != 'X') continue;
                rect(ctx, originX + col * cell, originY + row * cell, cell, cell, argb);
            }
        }
    }

    public static void drawFilled(GuiGraphicsExtractor ctx, String[] glyph, int x, int y, int size,
                                  float fill, int emptyArgb, int fullArgb) {
        draw(ctx, glyph, x, y, size, emptyArgb);
        int filled = Math.round(size * Math.clamp(fill, 0f, 1f));
        if (filled <= 0) return;
        bandTop = y + size - filled;
        bandBottom = y + size;
        draw(ctx, glyph, x, y, size, fullArgb);
        bandTop = Integer.MIN_VALUE;
        bandBottom = Integer.MAX_VALUE;
    }

    /** Clipped to the fill band rather than scissored: a scissor resolves in window pixels and
     * would cut the wrong rows under a pose transform. */
    private static void rect(GuiGraphicsExtractor ctx, int x, int y, int w, int h, int argb) {
        int top = Math.max(y, bandTop);
        int bottom = Math.min(y + h, bandBottom);
        if (bottom > top) ctx.fill(x, top, x + w, bottom, argb);
    }

}
