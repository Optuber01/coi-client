package dev.ua.ikeepcalm.coi.screen;

import dev.ua.ikeepcalm.coi.ui.CoiStyle;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * For uniform-height rows only, so the thumb is a plain proportion of the viewport. The ability
 * picker's rows are unequal, so its handle is walked in pixels and is deliberately not this one.
 */
public class ScrollbarPainter {

    private static final int WIDTH = 3;
    private static final int MIN_THUMB_H = 16;

    private ScrollbarPainter() {
    }

    /**
     * @param trackX left edge of the 3px track; draws nothing when everything already fits
     */
    public static void draw(GuiGraphicsExtractor graphics, int trackX, int top, int bottom,
                            int contentHeight, double scrollOffset, double maxScroll) {
        if (maxScroll <= 0) return;

        int viewportH = bottom - top;
        graphics.fill(trackX, top, trackX + WIDTH, bottom, CoiStyle.SCROLL_TRACK);

        int thumbH = Math.max(MIN_THUMB_H, viewportH * viewportH / contentHeight);
        int thumbY = top + (int) ((viewportH - thumbH) * (scrollOffset / maxScroll));
        graphics.fill(trackX, thumbY, trackX + WIDTH, thumbY + thumbH, CoiStyle.BORDER);
    }
}
