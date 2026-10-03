package dev.ua.ikeepcalm.coi.screen.title;

import dev.ua.ikeepcalm.coi.config.ClientStateStore;
import dev.ua.ikeepcalm.coi.config.HudConfig;
import dev.ua.ikeepcalm.coi.screen.TitleScreenHaunt;
import dev.ua.ikeepcalm.coi.screen.menu.MenuTheme;
import dev.ua.ikeepcalm.coi.ui.CoiStyle;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.TitleScreen;

/**
 * The gate, the time base, the geometry and the one accent the whole scene shares.
 * {@link #accent()} being a single value is the point: the void, the ring, the wordmark and the
 * button rails sicken together instead of four classes each holding an opinion about how red
 * things have got. {@code coiTitleScreen} false falls every hook through to vanilla.
 */
public class TitleTakeover {

    /**
     * Desaturated on purpose: a fully saturated red would read as "error", not as spoiled.
     */
    private static final int SPOILED = 0xFFA85C63;
    /**
     * Below this the ring stops shrinking with the window and simply crops.
     */
    private static final int MIN_RADIUS = 90;
    /**
     * The title screen can sit unrendered for minutes; an unclamped delta finishes every tween
     * in one step, so the wheel is found already rearranged around the parked cursor.
     */
    private static final float MAX_FRAME_MS = 100f;
    /**
     * Render thread only.
     */
    private static long lastFrameAt;
    private static float frameDelta;
    private TitleTakeover() {
    }

    public static boolean active() {
        return HudConfig.getSettings().coiTitleScreen;
    }

    /**
     * The logo, splash and button hooks are shared with the rest of the game, so both halves
     * of this matter.
     */
    public static boolean onTitleScreen() {
        return active() && Minecraft.getInstance().gui.screen() instanceof TitleScreen;
    }

    /**
     * 0..1, the same figure the haunting uses — so the hallucinations toggle gates the
     * takeover's horror for free and a clean player never sees any of it.
     */
    public static float corruption() {
        return TitleScreenHaunt.intensity();
    }

    public static int accent() {
        return MenuTheme.lerpArgb(CoiStyle.ACCENT, SPOILED, corruption());
    }

    public static long now() {
        return System.currentTimeMillis();
    }

    public static boolean steady() {
        return HudConfig.getSettings().epilepsyMode;
    }

    /**
     * The one easing chokepoint, and the only reader of the frame delta. Under
     * {@code epilepsyMode} it returns the target outright.
     */
    public static float approach(float current, float target, float durationMs) {
        if (steady() || durationMs <= 0) return target;
        float step = frameDelta / durationMs;
        return step >= 1f ? target : current + (target - current) * step;
    }

    /**
     * Empty when nothing is lit — a fresh install, not a missing value.
     */
    public static String litPathway() {
        return ClientStateStore.getLastPathway();
    }

    public static Geometry geometry(int w, int h) {
        int radius = Math.max(MIN_RADIUS, Math.round(Math.min(w, h) * 0.46f) - 24);
        return new Geometry(w / 2, Math.round(h * 0.46f), radius);
    }

    /**
     * Drawn in place of vanilla's panorama, so the widgets still land on top. The cursor arrives
     * already in gui-scaled pixels, which is the space the wheel works in.
     * <p>
     * The <em>only</em> place the frame delta may be advanced: {@link #drawSplash} also runs once
     * a frame, and counting the same gap twice would halve every tween.
     */
    public static void drawScene(GuiGraphicsExtractor graphics, int w, int h, int mouseX, int mouseY) {
        long now = now();
        frameDelta = lastFrameAt == 0 ? 0f : Math.min(MAX_FRAME_MS, now - lastFrameAt);
        lastFrameAt = now;

        Geometry geo = geometry(w, h);
        int accent = accent();
        float corruption = corruption();
        TitleScene.draw(graphics, w, h, geo, accent, corruption);
        PathwayWheel.draw(graphics, geo, accent, corruption, mouseX, mouseY);
        TitleLogo.draw(graphics, Minecraft.getInstance().font, w, h, geo, accent);
    }

    public static void drawSplash(GuiGraphicsExtractor graphics, Font font, int w, int h) {
        TitleLogo.drawSplash(graphics, font, w, h, geometry(w, h), accent());
    }

    /**
     * Computed once per draw and handed down, so three collaborators cannot re-derive it and
     * drift. {@code cy} is above the middle, so the button column falls inside the circle.
     */
    public record Geometry(int cx, int cy, int radius) {
    }
}
