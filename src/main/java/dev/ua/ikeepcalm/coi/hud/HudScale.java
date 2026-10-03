package dev.ua.ikeepcalm.coi.hud;

import dev.ua.ikeepcalm.coi.config.HudConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;

import java.util.BitSet;

/**
 * Per-element HUD scaling. <b>The one rule:</b> every element scales about its own fill origin —
 * the point its {@code anchor()} returns — so growing it never teleports it away from where the
 * player dropped it, and draw calls inside the push keep their absolute screen coordinates.
 * Geometry reported <em>outside</em> the push (anchor resolution, layout bounds) must go through
 * {@link #size} instead, or the anchors and the on-screen clamp drift.
 * <p>
 * Render-thread only: the push/pop pairing is a static stack.
 */
public class HudScale {

    /**
     * Scales this close to 1 skip the matrix work entirely.
     */
    private static final float EPSILON = 1e-4f;

    /**
     * Which open levels actually touched the pose, so {@link #pop} unwinds only those.
     */
    private static final BitSet PUSHED = new BitSet();
    private static int depth = 0;

    private HudScale() {
    }

    public static float clamp(float scale) {
        return Mth.clamp(scale, HudConfig.MIN_ELEMENT_SCALE, HudConfig.MAX_ELEMENT_SCALE);
    }

    /**
     * A pixel measurement at this scale, for any constant that leaves the push.
     */
    public static int size(int base, float scale) {
        return Math.round(base * clamp(scale));
    }

    public static void push(GuiGraphicsExtractor ctx, int originX, int originY, float scale) {
        float s = clamp(scale);
        boolean real = Math.abs(s - 1f) >= EPSILON;
        PUSHED.set(depth, real);
        depth++;
        if (!real) return;

        var pose = ctx.pose();
        pose.pushMatrix();
        pose.translate(originX, originY);
        pose.scale(s, s);
        pose.translate(-originX, -originY);
    }

    public static void pop(GuiGraphicsExtractor ctx) {
        if (depth <= 0) return;
        depth--;
        if (PUSHED.get(depth)) ctx.pose().popMatrix();
    }
}
