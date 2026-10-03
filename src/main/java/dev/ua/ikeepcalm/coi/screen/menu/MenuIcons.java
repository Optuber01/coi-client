package dev.ua.ikeepcalm.coi.screen.menu;

import dev.ua.ikeepcalm.coi.domain.ability.model.Pathways;
import dev.ua.ikeepcalm.coi.domain.menu.model.MenuIcon;
import dev.ua.ikeepcalm.coi.ui.AbilityIcons;
import dev.ua.ikeepcalm.coi.ui.CoiIcons;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

import java.util.UUID;

/**
 * The five places a document's icon can come from. Only {@code glyph} ships in this jar and so
 * cannot be defeated by the player's resource pack — which is why a server names it for anything
 * conceptual.
 */
public class MenuIcons {

    private static final float WATERMARK_ALPHA = 0.06f;

    /** The edge of the pathway-emblem bitmaps the watermark scales up from. */
    private static final int EMBLEM = 9;

    private static final int SKIN_SHEET = 64;

    private MenuIcons() {
    }

    public static void watermark(GuiGraphicsExtractor g, Font font, MenuIcon icon,
                                 int x, int y, int size) {
        if (icon.kind() != MenuIcon.Kind.PATHWAY || !icon.present()) return;
        int color = MenuTheme.withAlpha(0xFF000000 | Pathways.pathwayRgb(icon.value()), WATERMARK_ALPHA);
        var pose = g.pose();
        pose.pushMatrix();
        pose.translate(x, y);
        pose.scale(size / (float) EMBLEM, size / (float) EMBLEM);
        CoiIcons.drawPathwayEmblem(g, font, icon.value(), 0, 0, color);
        pose.popMatrix();
    }

    /**
     * Draws whichever of the three icon sources the document named.
     *
     * @return false when nothing was drawn, so the caller can close the gap
     */
    public static boolean draw(GuiGraphicsExtractor g, Font font, MenuIcon icon, int x, int y, int size, float alpha) {
        if (!icon.present()) return false;
        return switch (icon.kind()) {
            case PATHWAY -> {
                CoiIcons.drawPathwayArtwork(g, font, icon.value(), x, y, size, alpha);
                yield true;
            }
            case ITEM -> AbilityIcons.drawItemModel(g, icon.value(), x, y, size);
            // ITEM alone silently draws *nothing* when the pack does not define the model,
            // which is how ability tiles came out blank
            case ABILITY -> {
                AbilityIcons.draw(g, icon.value(), x, y, size, Math.round(Math.clamp(alpha, 0f, 1f) * 255));
                yield true;
            }
            case HEAD -> drawHead(g, icon.value(), x, y, size);
            // authored at 16; below that it blurs, so callers with small slots pass their
            // own drawn glyphs instead
            case GLYPH -> {
                Identifier id = CoiIcons.glyph(icon.value());
                yield id != null && CoiIcons.draw(g, id, x, y, size, alpha);
            }
            case NONE -> false;
        };
    }

    private static boolean drawHead(GuiGraphicsExtractor g, String uuid, int x, int y, int size) {
        Minecraft client = Minecraft.getInstance();
        if (client.getConnection() == null) return false;
        PlayerInfo info;
        try {
            info = client.getConnection().getPlayerInfo(UUID.fromString(uuid));
        } catch (IllegalArgumentException e) {
            return false;
        }
        if (info == null) return false;
        Identifier skin = info.getSkin().body().texturePath();
        g.blit(RenderPipelines.GUI_TEXTURED, skin, x, y, 8f, 8f, size, size, 8, 8,
                SKIN_SHEET, SKIN_SHEET, 0xFFFFFFFF);
        g.blit(RenderPipelines.GUI_TEXTURED, skin, x, y, 40f, 8f, size, size, 8, 8,
                SKIN_SHEET, SKIN_SHEET, 0xFFFFFFFF);
        return true;
    }
}
