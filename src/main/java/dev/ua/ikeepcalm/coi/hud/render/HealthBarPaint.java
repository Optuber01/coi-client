package dev.ua.ikeepcalm.coi.hud.render;

import dev.ua.ikeepcalm.coi.ui.CoiStyle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.Locale;

/**
 * Every pixel of the Beyonder health element, one method per {@link HealthStyle}.
 * <p>
 * <b>Absorption is never invisible.</b> Gold continues past the main fill while there is room
 * and is drawn <em>over</em> the fill's right-hand end when there is not. Clamping it to
 * {@code w - fillW} gives zero width at full pool, so the one state a shield most needs to
 * announce itself in showed nothing.
 */
public class HealthBarPaint {

    /**
     * Where the fill reaches the alarm colour and the overlay's pulse starts: one cue, one number.
     */
    public static final float LOW_FRACTION = 0.30f;

    /** Where the fill starts warming up toward {@link #LOW_FRACTION}. */
    private static final float ALERT_START_FRACTION = 0.60f;

    private static final int BORDER_COLOR = 0xCC000000;
    /** The fill blends between these by how much pool is left, so full health is not the loudest. */
    private static final int CALM_TOP = 0xFFA83A3A;
    private static final int CALM_BOTTOM = 0xFF4E1212;
    private static final int ALERT_TOP = 0xFFFF4444;
    private static final int ALERT_BOTTOM = 0xFF8E1414;

    private static final int ABSORB_TOP = 0xFFFFD75E;
    private static final int ABSORB_BOTTOM = 0xFFB07C0C;
    private static final int ABSORB_TEXT = 0xFFFFD75E;
    private static final int TEXT_COLOR = 0xFFFFFFFF;

    private static final int ORNATE_BORDER = 0xF005050A;
    private static final int BEVEL_SHADE = 0x66000000;
    private static final int BEVEL_LIGHT = 0x38FFFFFF;
    private static final int BRACKET_LEN = 4;

    private static final int PIP_COUNT = 10;
    private static final int PIP_GAP = 1;

    private static final int ABSORB_TEXT_GAP = 3;

    private HealthBarPaint() {
    }

    /**
     * @param absorption already converted into pool units by the caller
     * @param flash      1 &rarr; 0 white wash right after a drop
     * @param pulse      0 &rarr; 1 &rarr; 0 brightening while the pool is low
     */
    public static void draw(GuiGraphicsExtractor ctx, HealthStyle style, int x, int y, int w, int h,
                            double current, double max, double absorption, float flash, float pulse) {
        switch (style) {
            case HEARTS -> hearts(ctx, x, y, w, h, current, max, absorption);
            case BAR -> bar(ctx, x, y, w, h, current, max, absorption, flash, pulse);
            case ORNATE -> ornate(ctx, x, y, w, h, current, max, absorption, flash, pulse);
            case PIPS -> pips(ctx, x, y, w, h, current, max, absorption, flash, pulse);
        }
    }

    /**
     * The readout alone. The box is where the <em>numbers</em> go, not the hearts: vanilla owns
     * their position and this style deliberately does not move them. No flash and no pulse —
     * vanilla's hearts already have their own, and a second differently-timed one reads as a bug.
     */
    private static void hearts(GuiGraphicsExtractor ctx, int x, int y, int w, int h,
                               double current, double max, double absorption) {
        Font font = Minecraft.getInstance().font;
        Component readout = readout(font, current, max, w);
        Component absorb = absorbText(absorption);

        int width = font.width(readout);
        if (absorb != null) width += ABSORB_TEXT_GAP + font.width(absorb);

        int textX = x + (w - width) / 2;
        int textY = y + (h - font.lineHeight) / 2 + 1;
        ctx.text(font, readout, textX, textY, TEXT_COLOR, true);
        if (absorb != null) {
            ctx.text(font, absorb, textX + font.width(readout) + ABSORB_TEXT_GAP, textY, ABSORB_TEXT, true);
        }
    }

    private static void bar(GuiGraphicsExtractor ctx, int x, int y, int w, int h,
                            double current, double max, double absorption, float flash, float pulse) {
        CoiBar.frame(ctx, x, y, w, h, BORDER_COLOR);

        int fillW = CoiBar.lerpWidth(current, max, w);
        fillPool(ctx, x, y, h, fillW, current, max, pulse);

        int[] span = absorbSpan(x, w, fillW, absorption, max);
        if (span != null) CoiBar.fill(ctx, span[0], y, h, span[1], ABSORB_TOP, ABSORB_BOTTOM);

        flash(ctx, x, y, w, h, flash);
        readoutInside(ctx, x, y, w, h, current, max);
    }

    private static void ornate(GuiGraphicsExtractor ctx, int x, int y, int w, int h,
                               double current, double max, double absorption, float flash, float pulse) {
        CoiBar.frame(ctx, x, y, w, h, ORNATE_BORDER);

        int fillW = CoiBar.lerpWidth(current, max, w);
        fillPool(ctx, x, y, h, fillW, current, max, pulse);

        // Inset a pixel all round, so the bevel stays visible through the gold
        int[] span = absorbSpan(x + 1, w - 2, Math.max(0, fillW - 1), absorption, max);
        if (span != null) CoiBar.fill(ctx, span[0], y + 1, h - 2, span[1], ABSORB_TOP, ABSORB_BOTTOM);

        bevel(ctx, x, y, w, h);
        flash(ctx, x, y, w, h, flash);
        brackets(ctx, x, y, w, h);
        readoutInside(ctx, x, y, w, h, current, max);
    }

    /** The gaps are cut <em>after</em> every fill, so a notch shows through red and gold alike. */
    private static void pips(GuiGraphicsExtractor ctx, int x, int y, int w, int h,
                             double current, double max, double absorption, float flash, float pulse) {
        CoiBar.frame(ctx, x, y, w, h, BORDER_COLOR);

        float urgency = urgency(current, max);
        int top = lerpColor(CALM_TOP, ALERT_TOP, urgency);
        int bottom = lerpColor(CALM_BOTTOM, ALERT_BOTTOM, urgency);
        double filled = max <= 0 ? 0 : Mth.clamp(current / max, 0, 1) * PIP_COUNT;

        for (int i = 0; i < PIP_COUNT; i++) {
            int px = pipStart(x, w, i);
            int pw = pipEnd(x, w, i) - px;
            int part = (int) Math.round(Mth.clamp(filled - i, 0, 1) * pw);
            if (part <= 0) continue;
            CoiBar.fill(ctx, px, y, h, part, top, bottom);
            if (pulse > 0) ctx.fill(px, y, px + part, y + h, CoiBar.withAlpha(0x38FFFFFF, pulse));
        }

        // Gold grows inward from the right-hand end: "on top of" the pool, not part of it
        double shield = max <= 0 ? 0 : Mth.clamp(absorption / max, 0, 1) * PIP_COUNT;
        for (int i = PIP_COUNT - 1; i >= 0 && shield > 0; i--) {
            int px = pipStart(x, w, i);
            int pw = pipEnd(x, w, i) - px;
            int part = (int) Math.round(Mth.clamp(shield - (PIP_COUNT - 1 - i), 0, 1) * pw);
            if (part <= 0) break;
            CoiBar.fill(ctx, px + pw - part, y, h, part, ABSORB_TOP, ABSORB_BOTTOM);
        }

        for (int i = 1; i < PIP_COUNT; i++) {
            ctx.fill(pipEnd(x, w, i - 1), y, pipStart(x, w, i), y + h, BORDER_COLOR);
        }

        flash(ctx, x, y, w, h, flash);
        readoutInside(ctx, x, y, w, h, current, max);
    }

    /**
     * Running divisions of {@code w + PIP_GAP}, so segment 0 starts exactly at {@code x} and
     * segment 9 ends exactly at {@code x + w} whatever the width divides into — rounding each
     * pip down separately leaves the row short of the box the layout editor reports.
     */
    private static int pipStart(int x, int w, int i) {
        return x + i * (w + PIP_GAP) / PIP_COUNT;
    }

    private static int pipEnd(int x, int w, int i) {
        return x + (i + 1) * (w + PIP_GAP) / PIP_COUNT - PIP_GAP;
    }

    private static void fillPool(GuiGraphicsExtractor ctx, int x, int y, int h, int fillW,
                                 double current, double max, float pulse) {
        float urgency = urgency(current, max);
        CoiBar.fill(ctx, x, y, h, fillW,
                lerpColor(CALM_TOP, ALERT_TOP, urgency),
                lerpColor(CALM_BOTTOM, ALERT_BOTTOM, urgency));
        if (pulse > 0 && fillW > 0) {
            ctx.fill(x, y, x + fillW, y + h, CoiBar.withAlpha(0x38FFFFFF, pulse));
        }
    }

    /** @return {@code {x, width}}, or null when there is no absorption to draw */
    private static int[] absorbSpan(int x, int w, int fillW, double absorption, double max) {
        int absorbW = Math.min(w, CoiBar.lerpWidth(absorption, max, w));
        if (absorbW <= 0) return null;
        int start = Math.min(x + fillW, x + w - absorbW);
        return new int[]{Math.max(x, start), absorbW};
    }

    private static void bevel(GuiGraphicsExtractor ctx, int x, int y, int w, int h) {
        ctx.fill(x, y, x + w, y + 1, BEVEL_SHADE);
        ctx.fill(x, y, x + 1, y + h, BEVEL_SHADE);
        ctx.fill(x, y + h - 1, x + w, y + h, BEVEL_LIGHT);
        ctx.fill(x + w - 1, y, x + w, y + h, BEVEL_LIGHT);
    }

    /** Length is capped at half the box, so the brackets do not meet at a small scale. */
    private static void brackets(GuiGraphicsExtractor ctx, int x, int y, int w, int h) {
        int gold = CoiStyle.ACCENT;
        int lx = Math.min(BRACKET_LEN, w / 2);
        int ly = Math.min(BRACKET_LEN, h / 2);
        int x0 = x - 1, y0 = y - 1, x1 = x + w + 1, y1 = y + h + 1;

        ctx.fill(x0, y0, x0 + lx, y0 + 1, gold);
        ctx.fill(x0, y0, x0 + 1, y0 + ly, gold);
        ctx.fill(x1 - lx, y0, x1, y0 + 1, gold);
        ctx.fill(x1 - 1, y0, x1, y0 + ly, gold);
        ctx.fill(x0, y1 - 1, x0 + lx, y1, gold);
        ctx.fill(x0, y1 - ly, x0 + 1, y1, gold);
        ctx.fill(x1 - lx, y1 - 1, x1, y1, gold);
        ctx.fill(x1 - 1, y1 - ly, x1, y1, gold);
    }

    private static void flash(GuiGraphicsExtractor ctx, int x, int y, int w, int h, float flash) {
        if (flash > 0) ctx.fill(x, y, x + w, y + h, CoiBar.withAlpha(0xB0FFFFFF, flash));
    }

    /** Inside the fill, not above it: the bar has the hearts' footprint and nothing to spare. */
    private static void readoutInside(GuiGraphicsExtractor ctx, int x, int y, int w, int h,
                                      double current, double max) {
        Font font = Minecraft.getInstance().font;
        Component readout = readout(font, current, max, w);
        int textX = x + (w - font.width(readout)) / 2;
        ctx.text(font, readout, textX, y + (h - font.lineHeight) / 2 + 1, TEXT_COLOR, true);
    }

    /**
     * The widest readout that still fits, measured rather than assumed: a four-digit pool at a
     * scale of 0.5 makes "1,234 / 1,750" wider than the box.
     */
    private static Component readout(Font font, double current, double max, int w) {
        int room = w - 4;
        Component grouped = Component.translatable("hud.coi.health_readout",
                format(current, true), format(max, true));
        if (font.width(grouped) <= room) return grouped;

        Component plain = Component.translatable("hud.coi.health_readout",
                format(current, false), format(max, false));
        if (font.width(plain) <= room) return plain;

        return Component.translatable("hud.coi.health_current", format(current, false));
    }

    /** @return null when the shield rounds to nothing */
    private static Component absorbText(double absorption) {
        long shield = Math.round(absorption);
        if (shield <= 0) return null;
        return Component.translatable("hud.coi.health_absorb", format(absorption, true));
    }

    /** 0 while the pool is comfortable, 1 once it is low. */
    private static float urgency(double current, double max) {
        if (max <= 0) return 0f;
        float fraction = Mth.clamp((float) (current / max), 0f, 1f);
        return Mth.clamp((ALERT_START_FRACTION - fraction)
                / (ALERT_START_FRACTION - LOW_FRACTION), 0f, 1f);
    }

    /** Alpha is taken from {@code a} rather than interpolated; both ends are opaque. */
    private static int lerpColor(int a, int b, float t) {
        int ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        int r = ar + Math.round((br - ar) * t);
        int g = ag + Math.round((bg - ag) * t);
        int bl = ab + Math.round((bb - ab) * t);
        return (a & 0xFF000000) | (r << 16) | (g << 8) | bl;
    }

    private static String format(double value, boolean grouped) {
        return String.format(Locale.ROOT, grouped ? "%,d" : "%d", Math.max(0L, Math.round(value)));
    }
}
