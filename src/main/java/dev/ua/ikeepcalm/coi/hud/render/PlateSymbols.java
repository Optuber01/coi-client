package dev.ua.ikeepcalm.coi.hud.render;

import dev.ua.ikeepcalm.coi.domain.effect.visual.EffectPaint;
import dev.ua.ikeepcalm.coi.hud.HudOpacity;
import dev.ua.ikeepcalm.coi.hud.HudScale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The fillable gauge symbols on the character plate. Each sheet is one
 * {@value #SIZE}×{@value #SIZE} full-colour sprite drawn three times over itself: the whole
 * thing under {@link #EMPTY_RGB}, its bottom {@code fill} rows under the caller's wash, then
 * the rows a ceiling puts out of reach under {@link #CAPPED_RGB}.
 * <p>
 * <b>The partial fills are sub-rect blits, not a scissor.</b> A scissor resolves in window
 * pixels and would cut the wrong rows once the plate sits inside a {@link HudScale} push; a
 * sub-rect composes with whatever transform is already on the pose.
 */
public class PlateSymbols {

    /**
     * Sheet edge and drawn size at once, so every blit is 1:1 and the artwork never resamples.
     */
    public static final int SIZE = 32;

    private static final int SHEET_W = SIZE;
    private static final int SHEET_H = SIZE;

    /** Wash for "draw the artwork in its own colours". */
    public static final int NO_WASH = 0xFFFFFF;

    public static final Identifier BRAIN = sheet("symbol_brain");
    public static final Identifier MASK = sheet("symbol_mask");

    /** A multiply, so it dims the artwork toward its own shadow rather than flattening it to grey. */
    private static final int EMPTY_RGB = 0x4A4A56;
    /** Kept close to {@link #EMPTY_RGB} so it reads as damage, never as a value. */
    private static final int CAPPED_RGB = 0x40202A;

    /** Cached per reload; an absent sheet degrades to {@link #fallback} rather than blitting nothing. */
    private static final Map<Identifier, Boolean> PRESENT = new ConcurrentHashMap<>();

    private PlateSymbols() {
    }

    private static Identifier sheet(String name) {
        return Identifier.fromNamespaceAndPath("coi-client", "textures/gui/hud/" + name + ".png");
    }

    /**
     * @param fill 0..1 of the symbol's height
     * @param cap  the highest fraction the fill can ever reach (1 for no ceiling); the rows
     *             above it are drawn dead, which is how permanent madness shows as lost headroom
     * @param wash multiplied over the filled rows; {@link #NO_WASH} keeps the art's own colours
     */
    public static void draw(GuiGraphicsExtractor ctx, Identifier symbol, int x, int y,
                            float fill, float cap, int wash) {
        float f = Mth.clamp(fill, 0f, 1f);
        float c = Mth.clamp(cap, 0f, 1f);
        if (!present(symbol)) {
            fallback(ctx, x, y, f, c, wash);
            return;
        }

        // Every tint is the blit's own alpha channel, so each goes through HudOpacity
        body(ctx, symbol, x, y, HudOpacity.apply(EffectPaint.argb(EMPTY_RGB, 255)));
        fillUp(ctx, symbol, x, y, f, HudOpacity.apply(EffectPaint.argb(wash, 255)));
        // Last, so it also covers any fill a stale value pushed above the ceiling
        capBand(ctx, symbol, x, y, c, HudOpacity.apply(EffectPaint.argb(CAPPED_RGB, 255)));
    }

    private static void body(GuiGraphicsExtractor ctx, Identifier symbol, int x, int y, int argb) {
        ctx.blit(RenderPipelines.GUI_TEXTURED, symbol, x, y, 0f, 0f, SIZE, SIZE, SHEET_W, SHEET_H, argb);
    }

    /** Source rows {@code v = SIZE - h .. SIZE}, so the fill rises rather than squashing. */
    private static void fillUp(GuiGraphicsExtractor ctx, Identifier symbol, int x, int y, float fraction, int argb) {
        int h = Math.round(SIZE * fraction);
        if (h <= 0) return;
        int v = SIZE - h;
        ctx.blit(RenderPipelines.GUI_TEXTURED, symbol, x, y + v, 0f, v, SIZE, h, SIZE, h, SHEET_W, SHEET_H, argb);
    }

    private static void capBand(GuiGraphicsExtractor ctx, Identifier symbol, int x, int y, float cap, int argb) {
        int h = SIZE - Math.round(SIZE * cap);
        if (h <= 0) return;
        ctx.blit(RenderPipelines.GUI_TEXTURED, symbol, x, y, 0f, 0f, SIZE, h, SIZE, h, SHEET_W, SHEET_H, argb);
    }

    private static boolean present(Identifier symbol) {
        Boolean cached = PRESENT.get(symbol);
        if (cached != null) return cached;
        boolean found;
        try {
            Minecraft client = Minecraft.getInstance();
            found = client != null && client.getResourceManager() != null
                    && client.getResourceManager().getResource(symbol).isPresent();
        } catch (Exception e) {
            found = false;
        }
        PRESENT.put(symbol, found);
        return found;
    }

    public static void clearCache() {
        PRESENT.clear();
    }

    /** Stand-in for a sheet the packs do not define, so the row still reads as a gauge. */
    private static void fallback(GuiGraphicsExtractor ctx, int x, int y, float fill, float cap, int wash) {
        int left = x + 3;
        int right = x + SIZE - 3;
        int top = y + 2;
        int bottom = y + SIZE - 2;
        int span = bottom - top;

        roundedRect(ctx, left, top, right, bottom, HudOpacity.apply(EffectPaint.argb(EMPTY_RGB, 255)));
        roundedRect(ctx, left, bottom - Math.round(span * fill), right, bottom,
                HudOpacity.apply(EffectPaint.argb(wash, 255)));
        roundedRect(ctx, left, top, right, bottom - Math.round(span * cap),
                HudOpacity.apply(EffectPaint.argb(CAPPED_RGB, 255)));
    }

    private static void roundedRect(GuiGraphicsExtractor ctx, int left, int top, int right, int bottom, int argb) {
        if (bottom <= top || right - left < 2) return;
        ctx.fill(left + 1, top, right - 1, top + 1, argb);
        if (bottom - top > 2) ctx.fill(left, top + 1, right, bottom - 1, argb);
        if (bottom - top > 1) ctx.fill(left + 1, bottom - 1, right - 1, bottom, argb);
    }
}
