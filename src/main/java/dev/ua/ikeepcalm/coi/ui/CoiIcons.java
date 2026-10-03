package dev.ua.ikeepcalm.coi.ui;

import dev.ua.ikeepcalm.coi.domain.ability.model.Pathways;
import dev.ua.ikeepcalm.coi.domain.effect.visual.EffectPaint;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The mod's small first-party GUI icons, and the one way to draw them. <b>Each ships at exactly
 * the size it is drawn at</b>, so every blit is 1:1 and the artwork never resamples — which is
 * why the sizes are constants here and not a caller's argument; a new size means a new file.
 * {@link #draw} returns false for an icon no loaded pack defines, and every caller falls back.
 */
public class CoiIcons {

    /**
     * <b>16 is the floor for this artwork, measured rather than preferred.</b> The sources are
     * detailed 64px images, not pixel art upscaled from a small grid, so any reduction below
     * 16px averages the detail away and reads as a blur. Narrower slots keep drawn glyphs.
     */
    public static final Identifier COG = icon("cog");
    public static final int COG_SIZE = 16;

    private static final int WHITE = 0xFFFFFFFF;

    public static final int CREST = 7;

    private static final Map<Identifier, Boolean> PRESENT = new ConcurrentHashMap<>();

    private CoiIcons() {
    }

    private static Identifier icon(String name) {
        return Identifier.fromNamespaceAndPath("coi-client", "textures/gui/icons/" + name + ".png");
    }

    /**
     * Server-named, so <b>sanitised to {@code [a-z0-9_]}</b>: a document must not be able to
     * walk out of the icon folder or name a texture in another namespace.
     *
     * @return null when nothing legible is left of the name
     */
    public static Identifier glyph(String name) {
        if (name == null || name.isEmpty()) return null;
        StringBuilder clean = new StringBuilder(name.length());
        for (int i = 0; i < name.length() && clean.length() < 48; i++) {
            char c = Character.toLowerCase(name.charAt(i));
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_') clean.append(c);
        }
        return clean.isEmpty() ? null : icon(clean.toString());
    }

    /** Every glyph this build ships, so the debug screen can show what a server may name. */
    public static final String[] GLYPHS = {
            "alliance", "authority", "cooldown", "cost", "damage", "defense", "divination",
            "flame", "growth", "health", "magic", "power", "regen", "resist", "restore",
            "rites", "saturation", "sequence", "slot_empty", "slot_filled", "soul",
            "spirit", "spirituality", "uniqueness", "ward"
    };

    /**
     * @return false when no loaded pack defines it, so the caller can fall back
     */
    public static boolean draw(GuiGraphicsExtractor ctx, Identifier icon, int x, int y, int size) {
        return draw(ctx, icon, x, y, size, WHITE);
    }

    /** The tint is a multiply: it can dim or colour an icon, never brighten it. */
    public static boolean draw(GuiGraphicsExtractor ctx, Identifier icon, int x, int y, int size, int argb) {
        if (!present(icon)) return false;
        ctx.blit(RenderPipelines.GUI_TEXTURED, icon, x, y, 0f, 0f, size, size, size, size, argb);
        return true;
    }

    public static boolean draw(GuiGraphicsExtractor ctx, Identifier icon, int x, int y, int size, float alpha) {
        return draw(ctx, icon, x, y, size, EffectPaint.argb(0xFFFFFF, Math.round(Math.clamp(alpha, 0f, 1f) * 255)));
    }

    /**
     * The mod ships real art for all 25 pathways, so the diamond crest is only a fallback for
     * one {@code Pathways} does not know.
     *
     * @return the width drawn, so the caller can place the caption after it
     */
    public static int drawPathwayEmblem(GuiGraphicsExtractor ctx, Font font, String pathway,
                                        int x, int y, int argb) {
        Component emblem = Pathways.pathwayEmblem(pathway);
        if (emblem != null) {
            ctx.text(font, emblem, x, y, argb, false);
            return font.width(emblem);
        }
        drawCrest(ctx, x, y + 1, argb);
        return CREST;
    }

    /** The emblem's authored height — the bitmaps behind the pathway font are 9px. */
    public static final int EMBLEM = 9;

    /** Full artwork for menu slots; retain the font route for inline text and missing textures. */
    public static void drawPathwayArtwork(GuiGraphicsExtractor ctx, Font font, String pathway,
                                          int x, int y, int size, float alpha) {
        Identifier texture = Pathways.qualityEmblemTexture(pathway);
        if (texture == null || !present(texture)) texture = Pathways.emblemTexture(pathway);
        int opacity = Math.round(Math.clamp(alpha, 0f, 1f) * 255);
        if (texture != null && present(texture)) {
            // Unit source dimensions map the complete image to UV 0..1 at any source resolution.
            ctx.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 0f, 0f,
                    size, size, 1, 1, 1, 1, EffectPaint.argb(0xFFFFFF, opacity));
        } else {
            drawPathwayEmblem(ctx, font, pathway, x, y, size,
                    EffectPaint.argb(Pathways.pathwayRgb(pathway), opacity));
        }
    }

    /**
     * The plain overload always draws at the font's own 9px, which is a speck inside a 32px
     * hero block. Whole-number scales stay crisp (9->18, 9->27); anything else is a blur.
     *
     * @return the width drawn, so the caller can place a caption after it
     */
    public static int drawPathwayEmblem(GuiGraphicsExtractor ctx, Font font, String pathway,
                                        int x, int y, int size, int argb) {
        if (size <= EMBLEM) return drawPathwayEmblem(ctx, font, pathway, x, y, argb);
        float scale = size / (float) EMBLEM;
        var pose = ctx.pose();
        pose.pushMatrix();
        pose.translate(x, y);
        pose.scale(scale, scale);
        int drawn = drawPathwayEmblem(ctx, font, pathway, 0, 0, argb);
        pose.popMatrix();
        return Math.round(drawn * scale);
    }

    /** Row by row, so it stays a diamond instead of the rounded blob a circle becomes here. */
    private static void drawCrest(GuiGraphicsExtractor ctx, int x, int y, int argb) {
        int mid = CREST / 2;
        for (int row = 0; row < CREST; row++) {
            int spread = mid - Math.abs(row - mid);
            ctx.fill(x + mid - spread, y + row, x + mid + spread + 1, y + row + 1, argb);
        }
        ctx.fill(x + mid, y + mid, x + mid + 1, y + mid + 1, 0xC0000000);
    }

    private static boolean present(Identifier icon) {
        Boolean cached = PRESENT.get(icon);
        if (cached != null) return cached;
        boolean found;
        try {
            Minecraft client = Minecraft.getInstance();
            found = client != null && client.getResourceManager() != null
                    && client.getResourceManager().getResource(icon).isPresent();
        } catch (Exception e) {
            found = false;
        }
        PRESENT.put(icon, found);
        return found;
    }

    public static void clearCache() {
        PRESENT.clear();
    }
}
