package dev.ua.ikeepcalm.coi.hud;

import dev.ua.ikeepcalm.coi.config.HudConfig;
import dev.ua.ikeepcalm.coi.hud.render.CoiBar;
import net.minecraft.util.Mth;

import java.util.Arrays;
import java.util.BitSet;

/**
 * Per-element HUD transparency — {@link HudScale}'s other half: ambient render state pushed
 * around one element's draw, render-thread only. It cannot be a pose push, because
 * {@code ctx.pose()} is a 2D matrix stack with no colour channel; opacity has to be applied per
 * colour through {@link #apply(int)}. That is also the hazard: one colour that skips
 * {@code apply} stays solid while the rest fades, which reads as a bug rather than a setting.
 */
public class HudOpacity {

    /**
     * Factors this close to 1 skip the work entirely.
     */
    private static final float EPSILON = 1e-4f;

    private static final BitSet PUSHED = new BitSet();
    /**
     * The factor each real push replaced, indexed by its depth.
     */
    private static float[] previous = new float[8];
    private static int depth = 0;

    /**
     * The factor every {@link #apply} multiplies by; 1 outside any push.
     */
    private static float factor = 1f;

    private HudOpacity() {
    }

    public static float clamp(float opacity) {
        return Mth.clamp(opacity, HudConfig.MIN_ELEMENT_OPACITY, HudConfig.MAX_ELEMENT_OPACITY);
    }

    /**
     * Nested pushes multiply, so a nested piece can never come out more solid than its container.
     */
    public static void push(float opacity) {
        float o = clamp(opacity);
        boolean real = Math.abs(o - 1f) >= EPSILON;
        PUSHED.set(depth, real);
        if (real) {
            if (depth >= previous.length) previous = Arrays.copyOf(previous, depth * 2);
            previous[depth] = factor;
            factor *= o;
        }
        depth++;
    }

    public static void pop() {
        if (depth <= 0) return;
        depth--;
        if (PUSHED.get(depth)) factor = previous[depth];
    }

    /**
     * The raw factor, for callers that hand an alpha rather than a colour (CoiBar's faded overloads).
     */
    public static float current() {
        return factor;
    }

    /**
     * The colour a draw inside a push should use: the argb's own alpha times the ambient factor.
     */
    public static int apply(int argb) {
        if (factor >= 1f - EPSILON) return argb;
        return CoiBar.withAlpha(argb, factor);
    }
}
