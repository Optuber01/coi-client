package dev.ua.ikeepcalm.coi.screen.menu;

import dev.ua.ikeepcalm.coi.domain.effect.visual.EffectPaint;

import net.minecraft.client.gui.GuiGraphicsExtractor;

public class MenuGauges {

    /**
     * The capped band is darker than the empty track on purpose: "not filled yet" and "can
     * never fill" have to read as different things.
     */
    static final int GAUGE_TRACK = 0x50000000;
    static final int CAPPED = 0x90000000;

    private MenuGauges() {
    }

    public static void gauge(GuiGraphicsExtractor g, int x, int y, int w, int h, double fraction, int argb) {
        if (w <= 0 || h <= 0) return;
        g.fill(x, y, x + w, y + h, GAUGE_TRACK);
        int filled = (int) Math.round(Math.clamp(fraction, 0.0, 1.0) * w);
        if (filled > 0) g.fill(x, y, x + filled, y + h, argb);
        g.fill(x, y, x + w, y + 1, 0x18FFFFFF);
    }

    public static void gauge(GuiGraphicsExtractor g, int x, int y, int w, int h,
                             double fraction, int argb, double cap) {
        if (w <= 0 || h <= 0) return;
        double ceiling = Math.clamp(cap, 0.0, 1.0);
        gauge(g, x, y, w, h, Math.min(Math.clamp(fraction, 0.0, 1.0), ceiling), argb);
        capBand(g, x, y, w, h, ceiling, argb);
    }

    public static void capBand(GuiGraphicsExtractor g, int x, int y, int w, int h, double cap, int argb) {
        int capX = x + (int) Math.round(Math.clamp(cap, 0.0, 1.0) * w);
        if (capX >= x + w) return;
        g.fill(capX, y, x + w, y + h, CAPPED);
        g.fill(capX, y, capX + 1, y + h, MenuTheme.withAlpha(argb, 0.55f));
    }

    public static void segments(GuiGraphicsExtractor g, int x, int y, int w, int h,
                                double fraction, int argb, int count) {
        if (w <= 0 || h <= 0 || count <= 0) return;
        double value = Math.clamp(fraction, 0.0, 1.0);
        int gap = w >= count * 3 ? 1 : 0;
        int span = w - (count - 1) * gap;
        for (int i = 0; i < count; i++) {
            int sx = x + i * span / count + i * gap;
            int sw = (i + 1) * span / count - i * span / count;
            g.fill(sx, y, sx + sw, y + h, GAUGE_TRACK);
            double lit = Math.clamp(value * count - i, 0.0, 1.0);
            int filled = (int) Math.round(lit * sw);
            if (filled > 0) g.fill(sx, y, sx + filled, y + h, argb);
        }
    }

    public static void ring(GuiGraphicsExtractor g, int centreX, int centreY, int radius,
                            int thickness, double fraction, int argb) {
        if (radius <= thickness) return;
        int steps = Math.max(32, radius * 4);
        double value = Math.clamp(fraction, 0.0, 1.0);
        float mid = radius - thickness / 2f;
        int track = MenuTheme.withAlpha(argb, 0.14f);
        for (int i = 0; i < steps; i++) {
            double a0 = -Math.PI / 2 + i * (Math.PI * 2) / steps;
            double a1 = -Math.PI / 2 + (i + 1) * (Math.PI * 2) / steps;
            boolean lit = (i + 1) / (double) steps <= value + 1e-6;
            EffectPaint.line(g,
                    (float) (centreX + Math.cos(a0) * mid), (float) (centreY + Math.sin(a0) * mid),
                    (float) (centreX + Math.cos(a1) * mid), (float) (centreY + Math.sin(a1) * mid),
                    lit ? argb : track, thickness);
        }
    }
}
