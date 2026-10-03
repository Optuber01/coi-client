package dev.ua.ikeepcalm.coi.hud.render;

import dev.ua.ikeepcalm.coi.domain.effect.visual.EffectPaint;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.Random;

/**
 * The stage vignettes, the VHS tearing and the glyphs that eat into a label. All of it runs off
 * one {@linkplain #bursting cadence}, so the screen tearing, the bar's slices and its static
 * come and go together. The screen effects cover the whole window, so a caller draws them
 * <em>outside</em> any per-element scale push.
 */
public class MadnessCorruption {

    private static final String GLITCH_GLYPHS = "#%&@!?/\\";

    private static final long BURST_CYCLE_MS = 650;
    private static final long BURST_LEN_MS = 250;

    /**
     * 64-bit golden ratio: spreads a coarse timestamp over the seed space.
     */
    public static final long SEED_MIX = 0x9E3779B97F4A7C15L;

    private MadnessCorruption() {
    }

    public static boolean bursting(long time) {
        return time % BURST_CYCLE_MS < BURST_LEN_MS;
    }

    public static void screenEffects(GuiGraphicsExtractor ctx, int w, int h, int stage) {
        if (stage < 2) return; // No screen effects for stages 0 and 1

        long time = System.currentTimeMillis();

        if (stage == 2) {
            EffectPaint.vignette(ctx, w, h, 0x1A0000, 70, 0.13f, 0.15f);
        } else if (stage == 3) {
            float pulse = (float) Math.sin(time * 0.008) * 0.25f + 0.75f;
            EffectPaint.vignette(ctx, w, h, 0x660000, (int) (185 * pulse), 0.22f, 0.25f);
        } else {
            float pulse = (float) Math.sin(time * 0.015) * 0.15f + 0.85f;
            EffectPaint.vignette(ctx, w, h, 0x2A082E, (int) (235 * pulse), 0.28f, 0.31f);

            glitchLines(ctx, w, h, 0.85f * pulse);
        }
    }

    private static void glitchLines(GuiGraphicsExtractor ctx, int w, int h, float intensity) {
        long elapsed = System.currentTimeMillis();
        if (!bursting(elapsed)) return;

        Random rng = new Random((elapsed / 60) * SEED_MIX);
        int lineCount = 3 + (int) (intensity * 4);
        float alpha = 0.6f + 0.4f * intensity;

        for (int i = 0; i < lineCount; i++) {
            int y = rng.nextInt(h);
            int bh = 1 + rng.nextInt(3);
            int type = rng.nextInt(4);

            switch (type) {
                case 0 -> {
                    int a = (int) (90 * alpha);
                    ctx.fill(0, y, w, y + bh, a << 24);
                }
                case 1 -> {
                    int a = (int) (40 * alpha);
                    ctx.fill(0, y, w, y + bh, (a << 24) | 0xFFFFFF);
                }
                case 2 -> {
                    int ra = (int) (50 * alpha);
                    ctx.fill(0, y, w, y + 1, (ra << 24) | 0x8A0E8E);
                }
                case 3 -> {
                    int splitX = w / 3 + rng.nextInt(w / 3);
                    int a = (int) (35 * alpha);
                    ctx.fill(splitX, y, w, y + bh, (a << 24) | 0x222222);
                }
            }
        }
    }

    /** Re-rolled every 90ms, so the corruption crawls rather than strobing per frame. */
    public static String corruptText(String text, long time) {
        Random rng = new Random((time / 90) * SEED_MIX);
        char[] chars = text.toCharArray();
        int hits = 1 + rng.nextInt(3);
        for (int i = 0; i < hits; i++) {
            int idx = rng.nextInt(chars.length);
            if (chars[idx] != ' ') {
                chars[idx] = GLITCH_GLYPHS.charAt(rng.nextInt(GLITCH_GLYPHS.length()));
            }
        }
        return new String(chars);
    }
}
