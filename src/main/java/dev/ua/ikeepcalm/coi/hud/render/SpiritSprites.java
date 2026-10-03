package dev.ua.ikeepcalm.coi.hud.render;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/**
 * The three sprites the spirituality bar is built from, plus the pixel geometry gluing them
 * together. Every blit takes an ARGB tint, so the whole composite can fade as one.
 */
public class SpiritSprites {

    private static final Identifier FILL =
            Identifier.fromNamespaceAndPath("coi-client", "textures/gui/hud/spirituality_fill.png");
    private static final Identifier FRAME =
            Identifier.fromNamespaceAndPath("coi-client", "textures/gui/hud/spirituality_frame.png");
    private static final Identifier FRAME_CRITICAL =
            Identifier.fromNamespaceAndPath("coi-client", "textures/gui/hud/spirituality_frame_critical.png");

    /** spirituality_fill.png is 182×5; its opaque rows are 0..4. */
    public static final int FILL_W = 182;
    public static final int FILL_H = 5;

    /** Both frame PNGs are 184×25; the rails live on rows 9..15. */
    public static final int FRAME_W = 184;
    public static final int FRAME_H = 25;

    /** Frame origin relative to the fill's top-left, so rails y=9..15 wrap rows 0..4. */
    public static final int FRAME_DX = -1;
    public static final int FRAME_DY = -9;

    /**
     * The flask bulb inside spirituality_frame.png: rows 16..22, x 84..99, measured off the PNG.
     */
    private static final int BULB_U = 84;
    private static final int BULB_V = 16;
    private static final int BULB_W = 16;
    private static final int BULB_H = 7;

    private SpiritSprites() {
    }

    public static void fill(GuiGraphicsExtractor ctx, int x, int y, int fillW, int argb) {
        if (fillW <= 0) return;
        int w = Math.min(fillW, FILL_W);
        ctx.blit(RenderPipelines.GUI_TEXTURED, FILL, x, y, 0f, 0f, w, FILL_H, w, FILL_H, FILL_W, FILL_H, argb);
    }

    /** Drawn at the fill's origin; the frame offset is applied here. */
    public static void frame(GuiGraphicsExtractor ctx, int fillX, int fillY, boolean critical, int argb) {
        if ((argb >>> 24) == 0) return;
        Identifier texture = critical ? FRAME_CRITICAL : FRAME;
        ctx.blit(RenderPipelines.GUI_TEXTURED, texture, fillX + FRAME_DX, fillY + FRAME_DY,
                0f, 0f, FRAME_W, FRAME_H, FRAME_W, FRAME_H, argb);
    }

    /** {@code ratio} 1 draws nothing; 0 dims the whole bulb. */
    public static void flaskLevel(GuiGraphicsExtractor ctx, int fillX, int fillY, float ratio, int argb) {
        int emptyRows = Math.round((1f - Math.clamp(ratio, 0f, 1f)) * BULB_H);
        if (emptyRows <= 0 || (argb >>> 24) == 0) return;
        int x = fillX + FRAME_DX + BULB_U;
        int y = fillY + FRAME_DY + BULB_V;
        ctx.blit(RenderPipelines.GUI_TEXTURED, FRAME, x, y, BULB_U, BULB_V, BULB_W, emptyRows,
                BULB_W, emptyRows, FRAME_W, FRAME_H, argb);
    }
}
