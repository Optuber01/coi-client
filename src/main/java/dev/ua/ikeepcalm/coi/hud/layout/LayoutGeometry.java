package dev.ua.ikeepcalm.coi.hud.layout;

import dev.ua.ikeepcalm.coi.hud.HudAnchor;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;

import java.util.function.Consumer;
import java.util.function.IntConsumer;

/**
 * The arithmetic every layout element shares: a dragged position back into an anchor plus
 * offsets, the snap distances, and the padding a bar's label and frame add around its fill.
 */
public class LayoutGeometry {

    /**
     * How close to the centre line an element lands before it snaps to CENTER / a zero offset.
     */
    public static final int CENTER_SNAP = 12;

    /** The margin baked into {@link HudAnchor#resolve}. */
    public static final int EDGE_MARGIN = HudAnchor.MARGIN;

    /** The label line every bar hangs above itself, plus the 1px frame. */
    public static final int BAR_LABEL_PAD = 11;
    public static final int BAR_FRAME_PAD = 1;

    private LayoutGeometry() {
    }

    /**
     * A dragged fill origin back into anchor + offsets. The anchor follows the half of the
     * screen the bar landed in, so it keeps its distance from that edge when the window resizes.
     */
    public static void applyAnchoredMove(int pointX, int pointY, int barW, int barH, int w, int h,
                                         Consumer<String> anchor, IntConsumer xOffset, IntConsumer yOffset) {
        applyAnchoredMove(pointX, pointY, barW, w, h, pointY + barH / 2 < h / 2, anchor, xOffset, yOffset);
    }

    /** Same, for stacks taller than one bar, which decide their own top/bottom half. */
    public static void applyAnchoredMove(int pointX, int pointY, int barW, int w, int h, boolean top,
                                         Consumer<String> anchor, IntConsumer xOffset, IntConsumer yOffset) {
        int px = Mth.clamp(pointX, 0, Math.max(0, w - barW));
        int py = Mth.clamp(pointY, 0, Math.max(0, h));
        boolean center = Math.abs(px + barW / 2 - w / 2) <= CENTER_SNAP;
        boolean right = !center && px + barW / 2 > w / 2;

        anchor.accept((top ? "TOP_" : "BOTTOM_") + (center ? "CENTER" : right ? "RIGHT" : "LEFT"));

        int xo;
        if (center) {
            xo = px - (w - barW) / 2;
            if (Math.abs(xo) <= CENTER_SNAP) xo = 0;
        } else if (right) {
            xo = px - (w - barW - EDGE_MARGIN);
        } else {
            xo = px - EDGE_MARGIN;
        }
        xOffset.accept(xo);
        yOffset.accept(top ? py : h - py);
    }

    /** Marks a slot that only fills once the server sends something. */
    public static void ghostOutline(GuiGraphicsExtractor ctx, int x, int y, int w, int h, int color) {
        for (int i = 0; i < w; i += 4) {
            int x1 = Math.min(x + w, x + i + 2);
            ctx.fill(x + i, y, x1, y + 1, color);
            ctx.fill(x + i, y + h - 1, x1, y + h, color);
        }
        for (int i = 0; i < h; i += 4) {
            int y1 = Math.min(y + h, y + i + 2);
            ctx.fill(x, y + i, x + 1, y1, color);
            ctx.fill(x + w - 1, y + i, x + w, y1, color);
        }
    }
}
