package dev.ua.ikeepcalm.coi.screen.sheet;

import dev.ua.ikeepcalm.coi.config.HudConfig;
import dev.ua.ikeepcalm.coi.domain.menu.model.MenuIcon;
import dev.ua.ikeepcalm.coi.screen.menu.MenuIcons;
import dev.ua.ikeepcalm.coi.screen.menu.MenuTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

import static dev.ua.ikeepcalm.coi.screen.sheet.SheetMetrics.HEADING_H;
import static dev.ua.ikeepcalm.coi.screen.sheet.SheetMetrics.SMALL_ICON;
import static dev.ua.ikeepcalm.coi.screen.sheet.SheetPalette.accent;

/**
 * What a section is given to draw itself with. The sheet's draw <em>is</em> its layout — every
 * section returns the y it reached — so the hit boxes and the hover are collected on the way down
 * the page and mouse events reuse the last frame's. The sections themselves hold no state.
 */
final class SheetContext {

    /** A bare whole number: the {@code %} sign belongs to the lang string. */
    static String round0(double value) {
        return String.format(Locale.ROOT, "%.0f", value);
    }

    private final Font font;
    private final Consumer<String> opener;
    private final List<Hit> hits = new ArrayList<>();

    private Component hoveredTip;
    private int viewTop;
    private int viewBottom;
    private float frameDelta;
    private boolean compact;
    private int spareHeight;
    private int spareGaps;

    SheetContext(Font font, Consumer<String> opener) {
        this.font = font;
        this.opener = opener;
    }

    void beginFrame(float delta, int top, int bottom, boolean compactLayout) {
        this.frameDelta = delta;
        this.viewTop = top;
        this.viewBottom = bottom;
        this.compact = compactLayout;
        hits.clear();
        hoveredTip = null;
    }

    Font font() {
        return font;
    }

    int viewTop() {
        return viewTop;
    }

    int viewBottom() {
        return viewBottom;
    }

    List<Hit> hits() {
        return hits;
    }

    void addHit(Hit hit) {
        hits.add(hit);
    }

    void hover(Component tip) {
        hoveredTip = tip;
    }

    Component hoveredTip() {
        return hoveredTip;
    }

    void open(String target) {
        opener.accept(target);
    }

    static AbstractClientPlayer player() {
        Minecraft client = Minecraft.getInstance();
        return client == null ? null : client.player;
    }

    int sectionGap() {
        return compact ? 4 : 8;
    }

    void distributeSpace(int pixels, int gaps) {
        spareHeight = Math.max(0, pixels);
        spareGaps = gaps;
    }

    int expandedGap(int minimum) {
        if (spareGaps <= 0) return minimum;
        int extra = spareHeight / spareGaps--;
        spareHeight -= extra;
        return minimum + extra;
    }

    /** The one easing chokepoint; under {@code epilepsyMode} it returns the target outright. */
    float approach(float current, float target, float durationMs) {
        if (HudConfig.getSettings().epilepsyMode || durationMs <= 0) return target;
        float step = frameDelta / durationMs;
        return step >= 1f ? target : current + (target - current) * step;
    }

    int section(GuiGraphicsExtractor graphics, int x, int y, int w, String key, String glyph) {
        if (graphics == null) return y + HEADING_H;
        int accent = accent();
        int left = x;
        if (MenuIcons.draw(graphics, font(), new MenuIcon(MenuIcon.Kind.GLYPH, glyph),
                left, y + 1, SMALL_ICON, 1f)) {
            left += SMALL_ICON + 3;
        }
        String caption = trim(I18n.get(key), x + w - left);
        graphics.text(font(), caption, left, y + 3, 0xFFE4D9BF, false);
        int used = font().width(caption);
        MenuTheme.headingRule(graphics, left + used + 5, y + 6, x + w, accent);
        return y + HEADING_H;
    }

    String trim(String text, int maxW) {
        if (text == null) return "";
        if (maxW <= 0) return "";
        if (this.font.width(text) <= maxW) return text;
        return this.font.plainSubstrByWidth(text, Math.max(0, maxW - this.font.width("…"))) + "…";
    }

    static String num(double value) {
        return String.format(Locale.ROOT, "%,.0f", value);
    }

    static String pct(double value) {
        return String.format(Locale.ROOT, "%.1f%%", value);
    }

    /**
     * Rebuilt every frame during the draw, in absolute screen coordinates.
     */
    record Hit(int x, int y, int w, int h, Runnable action, Component tip) {
        boolean contains(double mx, double my) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }
    }
}
