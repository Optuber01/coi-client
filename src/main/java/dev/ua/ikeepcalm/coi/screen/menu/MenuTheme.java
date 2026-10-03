package dev.ua.ikeepcalm.coi.screen.menu;

import dev.ua.ikeepcalm.coi.domain.effect.visual.EffectPaint;
import dev.ua.ikeepcalm.coi.domain.menu.model.MenuComponent;
import dev.ua.ikeepcalm.coi.ui.CoiStyle;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.Locale;

/**
 * Every colour and hand-drawn primitive the menus and the character sheet share. Nothing here is
 * a vanilla widget sprite: a stone-grey vanilla button in the middle of a {@link CoiStyle} card
 * reads as a different program.
 */
public class MenuTheme {

    public static final int INFO = 0xFF7FC8FF;
    public static final int SUCCESS = 0xFF5FD35F;
    public static final int WARN = 0xFFE0A83C;
    public static final int DANGER = 0xFFE04C4C;

    public static final int SURFACE = 0x18FFFFFF;
    public static final int SURFACE_HOVER = 0x30FFFFFF;
    public static final int RULE = 0x28FFFFFF;

    public static final int BORDER_OFF = 0xFF2A2A32;

    private static final int ON_ACCENT = 0xFF14141A;

    /**
     * 16, not the font's bare 8+2: a pill sized to its text alone reads as a cramped label.
     */
    public static final int CHIP_H = 16;

    /**
     * Centred text baseline for a {@link #CHIP_H} pill. One helper because four boxes use it —
     * chips, badges, grid-cell badges, hero badges — and they drifted apart the last time the
     * height changed. Minecraft glyphs carry their ink in the top 7 rows of an 8-row cell, so
     * centring on 8 lands the ink a touch high on purpose.
     */
    public static int chipTextY(int y) {
        return y + (CHIP_H - 8) / 2;
    }

    private MenuTheme() {
    }

    // --- Colours ---

    public static int textColor(MenuComponent.TextStyle style, int accentArgb) {
        return switch (style) {
            case BODY -> CoiStyle.TEXT_BODY;
            case MUTED -> CoiStyle.TEXT_MUTED;
            case HEADING -> accentArgb;
            case WARN -> WARN;
            case DANGER -> DANGER;
            case SUCCESS -> SUCCESS;
        };
    }

    public static int toastColor(String style, int accentArgb) {
        return switch (style == null ? "" : style.toLowerCase(Locale.ROOT)) {
            case "success" -> SUCCESS;
            case "warn", "warning" -> WARN;
            case "error", "danger" -> DANGER;
            case "info" -> INFO;
            default -> accentArgb;
        };
    }

    public static int argb(int rgb, int fallbackArgb) {
        return rgb != 0 ? 0xFF000000 | rgb : fallbackArgb;
    }

    public static int shade(int argb, float amount) {
        int target = amount >= 0 ? 0xFF : 0x00;
        float f = Math.abs(amount);
        int r = Math.round(((argb >> 16) & 0xFF) * (1 - f) + target * f);
        int g = Math.round(((argb >> 8) & 0xFF) * (1 - f) + target * f);
        int b = Math.round((argb & 0xFF) * (1 - f) + target * f);
        return (argb & 0xFF000000) | (r << 16) | (g << 8) | b;
    }

    public static int withAlpha(int argb, float alpha) {
        return EffectPaint.argb(argb & 0xFFFFFF, Math.round(Math.clamp(alpha, 0f, 1f) * 255));
    }

    public static int lerpArgb(int from, int to, float t) {
        float f = Math.clamp(t, 0f, 1f);
        int a = Math.round(((from >>> 24) & 0xFF) * (1 - f) + ((to >>> 24) & 0xFF) * f);
        int r = Math.round(((from >> 16) & 0xFF) * (1 - f) + ((to >> 16) & 0xFF) * f);
        int g = Math.round(((from >> 8) & 0xFF) * (1 - f) + ((to >> 8) & 0xFF) * f);
        int b = Math.round((from & 0xFF) * (1 - f) + (to & 0xFF) * f);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static int surface(float hoverT) {
        return lerpArgb(SURFACE, SURFACE_HOVER, hoverT);
    }

    public static int deltaColor(String delta) {
        if (delta.startsWith("+")) return SUCCESS;
        if (delta.startsWith("-") || delta.startsWith("−")) return DANGER;
        return CoiStyle.TEXT_MUTED;
    }

    // --- Primitives ---

    public static void heading(GuiGraphicsExtractor g, Font font, String title, int x, int y, int w, int accentArgb) {
        int used = headingCaption(g, font, title, x, y, w, accentArgb);
        headingRule(g, x + used + 5, y + 3, x + w, accentArgb);
    }

    public static int headingCaption(GuiGraphicsExtractor g, Font font, String title,
                                     int x, int y, int w, int accentArgb) {
        if (title.isEmpty() || w <= 0) return 0;
        String caption = spaced(title.toUpperCase(Locale.ROOT));
        int textW = Math.min(font.width(caption), w);
        g.text(font, font.plainSubstrByWidth(caption, w), x, y, accentArgb, false);
        return textW;
    }

    public static void headingRule(GuiGraphicsExtractor g, int x, int y, int rightX, int accentArgb) {
        if (x < rightX) g.fill(x, y, rightX, y + 1, withAlpha(accentArgb, 0.35f));
    }

    /** The small-caps trick: the vanilla font has no small caps and no letter-spacing. */
    private static String spaced(String text) {
        StringBuilder out = new StringBuilder(text.length() * 2);
        for (int i = 0; i < text.length(); i++) {
            if (i > 0) out.append(' ');
            out.append(text.charAt(i));
        }
        return out.toString();
    }

    public static void hairline(GuiGraphicsExtractor g, int x, int y, int w) {
        if (w <= 0) return;
        g.fill(x, y, x + w, y + 1, RULE);
    }

    public static void labelledRule(GuiGraphicsExtractor g, Font font, String label, int x, int y, int w) {
        String text = font.plainSubstrByWidth(label, w);
        g.text(font, text, x, y, CoiStyle.TEXT_MUTED, false);
        int ruleX = x + font.width(text) + 5;
        hairline(g, ruleX, y + 4, x + w - ruleX);
    }

    /** Four corner pixels knocked out — as much rounding as reads honestly at this scale. */
    public static void panel(GuiGraphicsExtractor g, int x, int y, int w, int h, int fill, int border) {
        if (w <= 0 || h <= 0) return;
        g.fill(x + 1, y, x + w - 1, y + h, fill);
        g.fill(x, y + 1, x + 1, y + h - 1, fill);
        g.fill(x + w - 1, y + 1, x + w, y + h - 1, fill);
        if (border != 0) {
            g.fill(x + 1, y, x + w - 1, y + 1, border);
            g.fill(x + 1, y + h - 1, x + w - 1, y + h, border);
            g.fill(x, y + 1, x + 1, y + h - 1, border);
            g.fill(x + w - 1, y + 1, x + w, y + h - 1, border);
        }
    }

    public static void button(GuiGraphicsExtractor g, Font font, int x, int y, int w, int h,
                              MenuComponent.Button button, int accentArgb, boolean hovered, boolean pressed) {
        boolean enabled = button.enabled();
        int fill;
        int border;
        int label;
        switch (button.style()) {
            case PRIMARY -> {
                fill = withAlpha(accentArgb, 0.85f);
                border = accentArgb;
                label = ON_ACCENT;
            }
            case DANGER -> {
                fill = 0xCC7A2626;
                border = DANGER;
                label = 0xFFFFE4E4;
            }
            case SUCCESS -> {
                fill = 0xCC1F5C33;
                border = SUCCESS;
                label = 0xFFE4FFEA;
            }
            case GHOST -> {
                fill = hovered ? SURFACE : 0;
                border = hovered ? CoiStyle.BORDER : 0;
                label = hovered ? CoiStyle.TEXT_BODY : CoiStyle.TEXT_MUTED;
            }
            case SECONDARY -> {
                fill = SURFACE;
                border = CoiStyle.BORDER;
                label = CoiStyle.TEXT_BODY;
            }
            default -> {
                fill = SURFACE;
                border = CoiStyle.BORDER;
                label = CoiStyle.TEXT_BODY;
            }
        }

        if (!enabled) {
            fill = 0x14FFFFFF;
            border = BORDER_OFF;
            label = CoiStyle.INACTIVE;
        } else if (pressed) {
            fill = shade(fill, -0.25f);
        } else if (hovered) {
            fill = shade(fill, 0.14f);
            border = shade(border, 0.2f);
        }

        panel(g, x, y, w, h, fill, border);

        int textY = y + (button.desc().isEmpty() ? (h - 8) / 2 : 4) + (enabled && pressed ? 1 : 0);
        int inset = 6;
        int iconW = 0;
        if (button.icon().present()) {
            MenuIcons.draw(g, font, button.icon(), x + inset, y + (h - 12) / 2, 12, enabled ? 1f : 0.4f);
            iconW = 15;
        }

        int textX = x + inset + iconW;
        int textW = w - inset * 2 - iconW;
        if (button.desc().isEmpty() && iconW == 0) {
            String text = font.plainSubstrByWidth(button.label(), textW);
            g.text(font, text, x + (w - font.width(text)) / 2, textY, label, false);
        } else {
            g.text(font, font.plainSubstrByWidth(button.label(), textW), textX, textY, label, false);
            if (!button.desc().isEmpty()) {
                int descColor = enabled ? CoiStyle.TEXT_MUTED : shade(CoiStyle.INACTIVE, -0.2f);
                g.text(font, font.plainSubstrByWidth(button.desc(), textW), textX, textY + 10, descColor, false);
            }
        }
    }

    public static int badge(GuiGraphicsExtractor g, Font font, String text, int rightX, int y, int rgb, int accentArgb) {
        if (text.isEmpty()) return 0;
        int color = argb(rgb, accentArgb);
        int textW = font.width(text);
        int w = textW + 8;
        int x = rightX - w;
        panel(g, x, y, w, CHIP_H, withAlpha(color, 0.18f), withAlpha(color, 0.55f));
        g.text(font, text, x + 4, chipTextY(y), color, false);
        return w;
    }

    public static void toggle(GuiGraphicsExtractor g, int x, int y, boolean on, boolean enabled, int accentArgb) {
        int trackW = 20;
        int trackH = 10;
        int track = !enabled ? 0x18FFFFFF : on ? withAlpha(accentArgb, 0.45f) : 0x22FFFFFF;
        int border = !enabled ? BORDER_OFF : on ? accentArgb : CoiStyle.BORDER;
        panel(g, x, y, trackW, trackH, track, border);
        int knobX = on ? x + trackW - 9 : x + 1;
        int knob = !enabled ? CoiStyle.INACTIVE : on ? accentArgb : CoiStyle.TEXT_MUTED;
        g.fill(knobX, y + 1, knobX + 8, y + trackH - 1, knob);
    }

    /**
     * Drawn rather than typed: unicode ✔/✘ are unreliable in the vanilla font. {@code PENDING} is
     * a ring rather than a dimmed cross — "not yet" must not look like "failed".
     *
     * @param rgb the server's override, or 0 for the state's own colour
     */
    public static void check(GuiGraphicsExtractor g, int x, int y, MenuComponent.CheckState state, int rgb) {
        int color = argb(rgb, checkColor(state));
        switch (state) {
            case OK -> {
                EffectPaint.line(g, x + 1, y + 4, x + 3, y + 6, color, 1);
                EffectPaint.line(g, x + 3, y + 6, x + 7, y + 1, color, 1);
            }
            case NO -> {
                EffectPaint.line(g, x + 1, y + 1, x + 7, y + 6, color, 1);
                EffectPaint.line(g, x + 1, y + 6, x + 7, y + 1, color, 1);
            }
            case PENDING -> panel(g, x + 1, y, 7, 7, 0, color);
        }
    }

    public static int checkColor(MenuComponent.CheckState state) {
        return switch (state) {
            case OK -> SUCCESS;
            case NO -> DANGER;
            case PENDING -> CoiStyle.TEXT_MUTED;
        };
    }

    public static int checkLabelColor(MenuComponent.CheckState state) {
        return state == MenuComponent.CheckState.OK ? CoiStyle.TEXT_BODY : checkColor(state);
    }

    public static void chevron(GuiGraphicsExtractor g, int x, int y, int color) {
        EffectPaint.line(g, x + 4, y, x, y + 4, color, 1);
        EffectPaint.line(g, x, y + 4, x + 4, y + 8, color, 1);
    }

    public static void cross(GuiGraphicsExtractor g, int x, int y, int color) {
        EffectPaint.line(g, x, y, x + 7, y + 7, color, 1);
        EffectPaint.line(g, x, y + 7, x + 7, y, color, 1);
    }

    public static void disclosure(GuiGraphicsExtractor g, int x, int y, float openT, int color) {
        double angle = Math.clamp(openT, 0f, 1f) * (Math.PI / 2);
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        float cx = x + 3f;
        float cy = y + 3.5f;
        // vertex and both arms rotate about the glyph's centre; rotating only the arms swings
        // the caret open around the wrong point
        float vx = (float) (cx + 2.5 * cos);
        float vy = (float) (cy + 2.5 * sin);
        for (int arm = -1; arm <= 1; arm += 2) {
            double ax = -2.5;
            double ay = arm * 3.0;
            EffectPaint.line(g, vx, vy,
                    (float) (cx + ax * cos - ay * sin), (float) (cy + ax * sin + ay * cos), color, 1);
        }
    }

    public static void stepMarker(GuiGraphicsExtractor g, Font font, int centreX, int centreY,
                                  String number, Boolean done, int accentArgb) {
        boolean filled = Boolean.TRUE.equals(done);
        int color = done == null ? CoiStyle.TEXT_MUTED
                : filled ? accentArgb : withAlpha(accentArgb, 0.55f);
        if (number.isEmpty()) {
            if (filled) {
                g.fill(centreX - 2, centreY - 2, centreX + 3, centreY + 3, color);
            } else {
                panel(g, centreX - 3, centreY - 3, 7, 7, CoiStyle.CARD_BG, color);
            }
            return;
        }
        panel(g, centreX - 5, centreY - 5, 11, 11,
                filled ? withAlpha(color, 0.85f) : CoiStyle.CARD_BG, color);
        int textW = font.width(number);
        g.text(font, number, centreX - textW / 2, centreY - 3, filled ? ON_ACCENT : color, false);
    }

    public static void rail(GuiGraphicsExtractor g, int x, int top, int bottom, int accentArgb) {
        if (bottom <= top) return;
        g.fill(x, top, x + 1, bottom, withAlpha(accentArgb, 0.25f));
    }

    public static int chipWidth(Font font, MenuComponent.Chip chip) {
        int w = 8 + font.width(chip.label());
        if (chip.icon().present()) w += 12;
        if (!chip.value().isEmpty()) w += 4 + font.width(chip.value());
        return w;
    }

    public static void chip(GuiGraphicsExtractor g, Font font, MenuComponent.Chip chip,
                            int x, int y, int accentArgb, float hoverT) {
        int color = argb(chip.rgb(), accentArgb);
        int w = chipWidth(font, chip);
        panel(g, x, y, w, CHIP_H, surface(hoverT), withAlpha(color, 0.35f));
        int textX = x + 4;
        int textY = chipTextY(y);
        if (chip.icon().present()) {
            MenuIcons.draw(g, font, chip.icon(), textX, y + (CHIP_H - 9) / 2, 9, 1f);
            textX += 12;
        }
        g.text(font, chip.label(), textX, textY, CoiStyle.TEXT_MUTED, false);
        textX += font.width(chip.label());
        if (!chip.value().isEmpty()) {
            g.text(font, chip.value(), textX + 4, textY, color, false);
        }
    }

    /** Everything inside keeps its own coordinate space; the caller only knows where text starts. */
    public static void scaledText(GuiGraphicsExtractor g, Font font, String text,
                                  int x, int y, float scale, int color, boolean shadow) {
        if (text.isEmpty()) return;
        var pose = g.pose();
        pose.pushMatrix();
        pose.translate(x, y);
        pose.scale(scale, scale);
        g.text(font, text, 0, 0, color, shadow);
        pose.popMatrix();
    }

    public static void scrollbar(GuiGraphicsExtractor g, int x, int top, int trackH,
                                 int handleY, int handleH, int width, int accentArgb, boolean grabbed) {
        g.fill(x, top, x + width, top + trackH, CoiStyle.SCROLL_TRACK);
        g.fill(x, handleY, x + width, handleY + handleH, withAlpha(accentArgb, grabbed ? 0.85f : 0.55f));
    }

    /** The vanilla {@code EditBox}'s black fill in a white border reads as a hole punched through
     * this card, so the box is rendered borderless on top of this instead. */
    public static void field(GuiGraphicsExtractor g, int x, int y, int w, int h,
                             boolean focused, int accentArgb) {
        panel(g, x, y, w, h, focused ? SURFACE_HOVER : SURFACE,
                focused ? accentArgb : CoiStyle.BORDER);
    }

}
