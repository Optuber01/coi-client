package dev.ua.ikeepcalm.coi.screen.title;

import dev.ua.ikeepcalm.coi.domain.ability.model.Pathways;
import dev.ua.ikeepcalm.coi.screen.TitleScreenHaunt;
import dev.ua.ikeepcalm.coi.screen.menu.MenuTheme;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.Locale;

/**
 * The wordmark is laid out against <em>vanilla's own first button row</em>, {@code height / 4 + 48}
 * in {@code TitleScreen.init}, never against a fraction of the screen height: the mod's logo is far
 * larger than vanilla's, so a percentage that clears the buttons at 16:9 puts them through the
 * middle of it in a short window. The band runs from under the wheel's apex (so the wordmark sits
 * inside the ring) down to that row, and the logo is fitted by both its width cap and the band's
 * height. On a window too short for both, clearing the buttons wins.
 */
public class TitleLogo {

    private static final Identifier LOGO =
            Identifier.fromNamespaceAndPath("coi-client", "textures/gui/title/lom_logo.png");

    /**
     * The shipped art, trimmed to its content. Every size keeps this ratio.
     */
    private static final int LOGO_TEX_W = 512;
    private static final int LOGO_TEX_H = 339;

    private static final int MAX_LOGO_W = 280;
    private static final float WIDTH_FRACTION = 0.42f;

    private static final int TOP_MARGIN = 12;
    /** Half a lit emblem plus air, so the wheel's apex clears the wordmark. */
    private static final int EMBLEM_CLEARANCE = 26;
    /** Reserved whether or not a caption is drawn. */
    private static final int CAPTION_BAND = 16;
    private static final int BUTTON_CLEARANCE = 10;
    private static final int MIN_LOGO_W = 72;
    private static final int MIN_BAND = 48;

    /** Vanilla's splash tilt, kept so the line still reads as the splash. */
    private static final float SPLASH_TILT = -0.34906584f;

    private record Layout(int x, int y, int w, int h) {
    }

    private TitleLogo() {
    }

    private static Layout layout(int w, int h, TitleTakeover.Geometry geo) {
        // TitleScreen.init: the first button row.
        int buttonsTop = h / 4 + 48;
        // on a short window the apex is below where the band has to start, and clearing the
        // buttons wins over clearing an emblem
        int latest = Math.max(TOP_MARGIN, buttonsTop - MIN_BAND - CAPTION_BAND - BUTTON_CLEARANCE);
        int top = Math.min(Math.max(TOP_MARGIN, geo.cy() - geo.radius() + EMBLEM_CLEARANCE), latest);
        int band = Math.max(24, buttonsTop - top - CAPTION_BAND - BUTTON_CLEARANCE);
        int width = Math.min(Math.min(MAX_LOGO_W, Math.round(w * WIDTH_FRACTION)),
                band * LOGO_TEX_W / LOGO_TEX_H);
        width = Math.max(MIN_LOGO_W, Math.min(width, w - 16));
        int height = width * LOGO_TEX_H / LOGO_TEX_W;
        return new Layout((w - width) / 2, top + Math.max(0, (band - height) / 2), width, height);
    }

    static void draw(GuiGraphicsExtractor graphics, Font font, int w, int h,
                     TitleTakeover.Geometry geo, int accent) {
        Layout layout = layout(w, h, geo);
        graphics.blit(RenderPipelines.GUI_TEXTURED, LOGO,
                layout.x(), layout.y(), 0f, 0f, layout.w(), layout.h(),
                LOGO_TEX_W, LOGO_TEX_H, LOGO_TEX_W, LOGO_TEX_H,
                wordmarkTint(accent));
        caption(graphics, font, w, layout);
    }

    private static int wordmarkTint(int accent) {
        float corruption = TitleTakeover.corruption();
        return MenuTheme.withAlpha(MenuTheme.lerpArgb(0xFFFFFFFF, accent, corruption * 0.6f),
                1f - 0.25f * corruption);
    }

    /** Nothing persisted means nothing drawn — an empty line is a worse answer than none. */
    private static void caption(GuiGraphicsExtractor graphics, Font font, int w, Layout layout) {
        String pathway = Pathways.normalizePathway(TitleTakeover.litPathway());
        if (pathway.isEmpty()) return;
        String text = spaced(Pathways.formatPathwayName(pathway).toUpperCase(Locale.ROOT));
        int color = MenuTheme.withAlpha(0xFF000000 | Pathways.pathwayRgb(pathway), 0.85f);
        graphics.text(font, text, (w - font.width(text)) / 2, layout.y() + layout.h() + 4, color, true);
    }

    private static String spaced(String text) {
        StringBuilder out = new StringBuilder(text.length() * 2);
        for (int i = 0; i < text.length(); i++) {
            if (i > 0) out.append(' ');
            out.append(text.charAt(i));
        }
        return out.toString();
    }

    /**
     * Off the wordmark's own top-right corner; vanilla's fixed {@code (width / 2 + 123, 69)}
     * lands inside a logo this size. The line comes from {@link TitleScreenHaunt}, which decides
     * it once per launch — re-rolling here would give a different one every frame.
     */
    static void drawSplash(GuiGraphicsExtractor graphics, Font font, int w, int h,
                           TitleTakeover.Geometry geo, int accent) {
        Component splash = TitleScreenHaunt.splashText();
        if (splash == null) return;
        Layout layout = layout(w, h, geo);
        int textW = font.width(splash);

        float pulse = 1.8f;
        if (!TitleTakeover.steady()) {
            pulse -= Math.abs((float) Math.sin(TitleTakeover.now() % 1000L / 1000f * Math.PI * 2) * 0.1f);
        }
        float scale = pulse * 100f / (textW + 32);

        var pose = graphics.pose();
        pose.pushMatrix();
        pose.translate(layout.x() + layout.w() * 0.92f, layout.y() + layout.h() * 0.30f);
        pose.rotate(SPLASH_TILT);
        pose.scale(scale, scale);
        graphics.text(font, splash, -textW / 2, -8, accent, true);
        pose.popMatrix();
    }
}
