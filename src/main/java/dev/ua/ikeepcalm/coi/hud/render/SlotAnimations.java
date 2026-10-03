package dev.ua.ikeepcalm.coi.hud.render;

import dev.ua.ikeepcalm.coi.domain.effect.visual.EffectPaint;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * The three moments a slot animates: the cast punch, the cast ring, the ready glint.
 */
final class SlotAnimations {

    private static final long CAST_POP_MS = 320;
    private static final long CAST_RING_MS = 420;
    private static final long READY_FLASH_MS = 650;

    private static final float POP_AMOUNT = 0.13f;

    private long castTime;
    private long readyFlashTime;

    void triggerCast() {
        castTime = System.currentTimeMillis();
    }

    void triggerReadyFlash() {
        readyFlashTime = System.currentTimeMillis();
    }

    /** @return true when a matrix was pushed and the caller must pop it */
    boolean pushCastPop(GuiGraphicsExtractor context, int x, int y, int size, long now) {
        if (castTime <= 0 || now - castTime >= CAST_POP_MS) return false;

        float castP = (now - castTime) / (float) CAST_POP_MS;
        float scale = 1f + POP_AMOUNT * (float) Math.sin(Math.PI * castP);
        var pose = context.pose();
        pose.pushMatrix();
        pose.translate(x + size / 2f, y + size / 2f);
        pose.scale(scale, scale);
        pose.translate(-(x + size / 2f), -(y + size / 2f));
        return true;
    }

    void renderCastRing(GuiGraphicsExtractor context, int x, int y, int size, long now, int pathwayRgb) {
        if (castTime <= 0) return;
        float p = (now - castTime) / (float) CAST_RING_MS;
        if (p >= 1f) return;

        float ease = EffectPaint.easeOutCubic(p);

        int expand = 2 + (int) (13 * ease);
        int a = (int) (210 * (1f - p));
        context.outline(x - expand, y - expand, size + expand * 2, size + expand * 2,
                EffectPaint.argb(pathwayRgb, a));

        int inner = 1 + (int) (7 * ease);
        context.outline(x - inner, y - inner, size + inner * 2, size + inner * 2,
                EffectPaint.argb(0xFFFFFF, a / 2));
    }

    void renderReadyFlash(GuiGraphicsExtractor context, int x, int y, int size, long now) {
        if (readyFlashTime <= 0) return;
        float p = (now - readyFlashTime) / (float) READY_FLASH_MS;
        if (p >= 1f) {
            readyFlashTime = 0;
            return;
        }

        int fade = (int) (200 * (1f - p));
        context.outline(x - 2, y - 2, size + 4, size + 4, EffectPaint.argb(0x88FF88, fade));
        context.fill(x, y, x + size, y + size, EffectPaint.argb(0xAAFFAA, fade / 4));

        float sweep = EffectPaint.smoothstep(p);
        int bandCx = (int) (x - size * 0.6f + size * 2.2f * sweep);
        context.enableScissor(x, y, x + size, y + size);
        var pose = context.pose();
        pose.pushMatrix();
        pose.translate(bandCx, y + size / 2f);
        pose.rotate(-0.45f);
        context.fill(-2, -size, 2, size, 0xB0FFFFFF);
        context.fill(-6, -size, -2, size, 0x50FFFFFF);
        context.fill(2, -size, 6, size, 0x50FFFFFF);
        pose.popMatrix();
        context.disableScissor();
    }
}
