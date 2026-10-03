package dev.ua.ikeepcalm.coi.hud;

import net.minecraft.util.Mth;

/**
 * The corner math, in one place, so the overlays and the tour's spotlights cannot drift apart.
 */
public enum HudAnchor {
    TOP_LEFT,
    TOP_CENTER,
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_CENTER,
    BOTTOM_RIGHT;

    /** The margin a LEFT/RIGHT anchored element sits at with a zero offset. */
    public static final int MARGIN = 10;

    /** Anything unrecognised (or null) reads as {@link #TOP_LEFT}. */
    public static HudAnchor parse(String value) {
        if (value == null) return TOP_LEFT;
        try {
            return valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return TOP_LEFT;
        }
    }

    /** Whether y is measured down from the top; stacked bars use it to decide which way to grow. */
    public boolean isTop() {
        return this == TOP_LEFT || this == TOP_CENTER || this == TOP_RIGHT;
    }

    public boolean isCenter() {
        return this == TOP_CENTER || this == BOTTOM_CENTER;
    }

    public boolean isRight() {
        return this == TOP_RIGHT || this == BOTTOM_RIGHT;
    }

    /**
     * @param topY          y for the TOP_* anchors
     * @param bottomYOffset distance up from the screen bottom for the BOTTOM_* anchors
     * @return {@code {x, y}}, x clamped so the bar stays on screen
     */
    public int[] resolve(int screenW, int screenH, int barW, int xOffset, int topY, int bottomYOffset) {
        int x;
        if (isCenter()) {
            x = (screenW - barW) / 2;
        } else if (isRight()) {
            x = screenW - barW - MARGIN;
        } else {
            x = MARGIN;
        }
        int y = isTop() ? topY : screenH - bottomYOffset;
        x = Mth.clamp(x + xOffset, 0, Math.max(0, screenW - barW));
        return new int[]{x, y};
    }
}
