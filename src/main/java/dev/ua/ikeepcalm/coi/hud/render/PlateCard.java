package dev.ua.ikeepcalm.coi.hud.render;

import dev.ua.ikeepcalm.coi.domain.ability.model.Pathways;
import dev.ua.ikeepcalm.coi.domain.effect.visual.EffectPaint;
import dev.ua.ikeepcalm.coi.hud.HudOpacity;
import dev.ua.ikeepcalm.coi.ui.CoiIcons;
import dev.ua.ikeepcalm.coi.ui.CoiStyle;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Locale;

/**
 * The character plate's paint: the card, its header and one row per gauge or reserve.
 * <p>
 * <b>Every colour drawn here goes through {@link HudOpacity#apply}</b> — fills, outlines, text
 * colours and the tints handed to {@code blit} alike. One that skips it leaves a piece of the
 * card solid while the rest fades, which reads as a bug rather than as a setting. Outside the
 * overlay's push {@code apply} is the identity, so this costs nothing.
 */
public class PlateCard {

    /**
     * Every interior column below is measured off this, so the plate has one number to tune.
     */
    public static final int CARD_W = 168;

    public static final int PAD = 6;

    public static final int HEADER_H = 30;
    public static final int HEAD = 24;
    public static final int HEAD_GAP = 6;
    public static final int HEADER_LINE_2 = 14;
    private static final int SKIN_SHEET = 64;
    private static final int CREST_GAP = 3;

    public static final int ROW_H = PlateSymbols.SIZE;
    public static final int ROW_GAP = 2;
    private static final int VALUE_W = 34;
    private static final int GAUGE_BAR_X = PAD + PlateSymbols.SIZE + 6;
    private static final int GAUGE_BAR_W = CARD_W - PAD - VALUE_W - GAUGE_BAR_X;
    private static final int GAUGE_BAR_H = 6;

    public static final int DIVIDER_GAP = 4;
    public static final int RES_ROW_H = 11;
    private static final int RES_VALUE_W = 30;
    private static final int RES_BAR_W = 56;
    private static final int RES_BAR_H = 3;
    private static final int RES_BAR_X = CARD_W - PAD - RES_VALUE_W - 4 - RES_BAR_W;

    private static final int BORDER = 0xCC000000;
    private static final int DIVIDER = 0x304D535C;
    private static final int VALUE_COLOR = 0xFFE0E0E0;

    private static final int GRANT_RISE = 5;

    private PlateCard() {
    }

    /** The one place the stacking is defined, so {@link #draw} and the editor cannot drift. */
    public static int height(int gaugeCount, int reserveCount) {
        int h = PAD + HEADER_H;
        h += gaugeCount * (ROW_GAP + ROW_H);
        if (reserveCount > 0) {
            h += DIVIDER_GAP + 1 + DIVIDER_GAP + reserveCount * RES_ROW_H;
        }
        return h + PAD;
    }

    public record Reserve(String label, float fill, int rgb, Component value) {
    }

    /**
     * Restates {@code CoiStyle.drawCard}'s three draws rather than calling it, because
     * {@code ui/} seeing the ambient alpha would mean a {@code ui -> hud} import. The three
     * colours still come from {@code CoiStyle}.
     */
    public static void drawChrome(GuiGraphicsExtractor ctx, int x, int y, int w, int h) {
        ctx.fillGradient(x, y, x + w, y + h, HudOpacity.apply(0xCC191C21), HudOpacity.apply(0xB0101217));
        ctx.outline(x, y, w, h, HudOpacity.apply(0x60434951));
    }

    public static void draw(GuiGraphicsExtractor ctx, Font font, int x, int y,
                            AbstractClientPlayer player, String name, String pathway, int sequence,
                            List<Gauge> gauges, List<Reserve> reserves) {
        drawChrome(ctx, x, y, CARD_W, height(gauges.size(), reserves.size()));
        drawHeader(ctx, font, x, y, player, name, pathway, sequence);

        int rowY = drawGauges(ctx, font, x, y + PAD + HEADER_H, gauges);
        if (reserves.isEmpty()) return;

        rowY = drawDivider(ctx, x, rowY);
        for (Reserve reserve : reserves) {
            drawReserve(ctx, font, x, rowY, reserve);
            rowY += RES_ROW_H;
        }
    }

    /** @return the y the rows reached */
    public static int drawGauges(GuiGraphicsExtractor ctx, Font font, int x, int rowY, List<Gauge> gauges) {
        for (Gauge gauge : gauges) {
            rowY += ROW_GAP;
            drawGauge(ctx, font, x, rowY, gauge);
            rowY += ROW_H;
        }
        return rowY;
    }

    /** @return the y the first reserve row starts at */
    public static int drawDivider(GuiGraphicsExtractor ctx, int x, int rowY) {
        rowY += DIVIDER_GAP;
        ctx.fill(x + PAD, rowY, x + CARD_W - PAD, rowY + 1, HudOpacity.apply(DIVIDER));
        return rowY + 1 + DIVIDER_GAP;
    }

    /** Face {@code u=8,v=8} and hat {@code u=40,v=8}, off the 64×64 skin sheet. */
    public static void drawHead(GuiGraphicsExtractor ctx, AbstractClientPlayer player, int x, int y) {
        if (player == null) {
            ctx.fill(x, y, x + HEAD, y + HEAD, HudOpacity.apply(EffectPaint.argb(0x2A2A32, 255)));
            return;
        }
        Identifier skin = player.getSkin().body().texturePath();
        // The one blit whose tint is otherwise plain white — the easiest to leave un-faded
        int tint = HudOpacity.apply(0xFFFFFFFF);
        ctx.blit(RenderPipelines.GUI_TEXTURED, skin, x, y, 8f, 8f, HEAD, HEAD, 8, 8,
                SKIN_SHEET, SKIN_SHEET, tint);
        ctx.blit(RenderPipelines.GUI_TEXTURED, skin, x, y, 40f, 8f, HEAD, HEAD, 8, 8,
                SKIN_SHEET, SKIN_SHEET, tint);
    }

    public static void drawHeader(GuiGraphicsExtractor ctx, Font font, int x, int y,
                                   AbstractClientPlayer player, String name, String pathway, int sequence) {
        int rgb = Pathways.pathwayRgb(pathway);
        CoiIcons.drawPathwayArtwork(ctx, font, pathway, x + CARD_W - 39, y + 3, 32,
                HudOpacity.current() * 0.12f);
        int headX = x + PAD;
        int headY = y + PAD + (HEADER_H - HEAD) / 2;
        drawHead(ctx, player, headX, headY);

        int textX = headX + HEAD + HEAD_GAP;
        int textW = CARD_W - PAD - (textX - x);
        ctx.text(font, trim(font, name, textW), textX, y + PAD, HudOpacity.apply(CoiStyle.TEXT_BODY), true);

        if (pathway == null || pathway.isEmpty()) return;
        drawPathwayLine(ctx, font, textX, y + PAD + HEADER_LINE_2, pathway, sequence,
                Pathways.pathwayRgb(pathway));
    }

    public static void drawPathwayLine(GuiGraphicsExtractor ctx, Font font, int x, int y,
                                       String pathway, int sequence, int rgb) {
        int argb = HudOpacity.apply(EffectPaint.argb(rgb, 255));
        CoiIcons.drawPathwayArtwork(ctx, font, pathway, x, y - 1, 10, HudOpacity.current());
        int emblemW = 10;
        int textX = x + emblemW + CREST_GAP;
        String upper = pathway.toUpperCase(Locale.ROOT).replace('_', ' ');
        int available = CARD_W - PAD * 2 - HEAD - HEAD_GAP - emblemW - CREST_GAP;
        int suffixWidth = sequence >= 0 ? font.width(Component.translatable("hud.coi.plate_pathway", "", sequence)) : 0;
        upper = trim(font, upper, Math.max(0, available - suffixWidth));
        Component caption = sequence >= 0
                ? Component.translatable("hud.coi.plate_pathway", upper, sequence)
                : Component.translatable("hud.coi.plate_pathway_only", upper);
        ctx.text(font, caption, textX, y, argb, true);
    }

    public static void drawGauge(GuiGraphicsExtractor ctx, Font font, int x, int rowY, Gauge gauge) {
        PlateSymbols.draw(ctx, gauge.symbol(), x + PAD, rowY, gauge.fill(), gauge.cap(), gauge.wash());

        int barX = x + GAUGE_BAR_X;
        int barY = rowY + (ROW_H - GAUGE_BAR_H) / 2;
        // CoiBar owns colours the caller cannot reach by dimming what it passes in, so the
        // fade goes in as a factor
        float fade = HudOpacity.current();
        CoiBar.frame(ctx, barX, barY, GAUGE_BAR_W, GAUGE_BAR_H, BORDER, fade);
        CoiBar.fill(ctx, barX, barY, GAUGE_BAR_H, CoiBar.lerpWidth(gauge.fill(), 1.0, GAUGE_BAR_W),
                EffectPaint.argb(gauge.rgb(), 255), darken(gauge.rgb()), fade);
        drawCeiling(ctx, barX, barY, gauge.cap());

        int right = x + CARD_W - PAD;
        int valueArgb = HudOpacity.apply(EffectPaint.argb(gauge.rgb(), 255));
        if (gauge.sub() == null) {
            ctx.text(font, gauge.value(), right - font.width(gauge.value()), rowY + (ROW_H - font.lineHeight) / 2, valueArgb, true);
            return;
        }
        ctx.text(font, gauge.value(), right - font.width(gauge.value()), rowY + 6, valueArgb, true);
        ctx.text(font, gauge.sub(), right - font.width(gauge.sub()), rowY + 17,
                HudOpacity.apply(CoiStyle.TEXT_MUTED), true);
    }

    /**
     * Drawn outside {@link #draw} because it leaves the card.
     *
     * @param rowIndex which gauge row the popup belongs to, counting from 1
     */
    public static void drawGrantPopup(GuiGraphicsExtractor ctx, Font font, int x, int y,
                                      int rowIndex, int rgb, int granted, float progress) {
        Component text = Component.translatable("hud.coi.acting_gain", granted);
        int rowY = y + PAD + HEADER_H + rowIndex * (ROW_GAP + ROW_H) - ROW_H;
        int textY = rowY - 2 - (int) (GRANT_RISE * progress);
        ctx.text(font, text, x + CARD_W - PAD - font.width(text), textY,
                HudOpacity.apply(EffectPaint.argb(rgb, (int) (255 * (1f - progress)))), true);
    }

    private static void drawCeiling(GuiGraphicsExtractor ctx, int barX, int barY, float cap) {
        if (cap >= 1f) return;
        int capX = barX + CoiBar.lerpWidth(cap, 1.0, GAUGE_BAR_W);
        ctx.fill(capX, barY, barX + GAUGE_BAR_W, barY + GAUGE_BAR_H, HudOpacity.apply(0x90000000));
        ctx.fill(capX, barY - 1, capX + 1, barY + GAUGE_BAR_H + 1, HudOpacity.apply(0xDDFFFFFF));
    }

    public static void drawReserve(GuiGraphicsExtractor ctx, Font font, int x, int rowY, Reserve reserve) {
        ctx.text(font, trim(font, reserve.label(), RES_BAR_X - PAD - 4), x + PAD, rowY + 1,
                HudOpacity.apply(CoiStyle.TEXT_BODY), true);

        int barX = x + RES_BAR_X;
        int barY = rowY + 4;
        float fade = HudOpacity.current();
        CoiBar.frame(ctx, barX, barY, RES_BAR_W, RES_BAR_H, BORDER, fade);
        CoiBar.fill(ctx, barX, barY, RES_BAR_H, CoiBar.lerpWidth(reserve.fill(), 1.0, RES_BAR_W),
                EffectPaint.argb(reserve.rgb(), 255), darken(reserve.rgb()), fade);

        int right = x + CARD_W - PAD;
        ctx.text(font, reserve.value(), right - font.width(reserve.value()), rowY + 1,
                HudOpacity.apply(VALUE_COLOR), true);
    }

    /**
     * @param wash multiplied over the symbol's filled part; {@link PlateSymbols#NO_WASH} leaves
     *             the artwork's own colours alone
     */
    public record Gauge(Identifier symbol, float fill, float cap, int rgb, int wash,
                        Component value, Component sub) {
        public Gauge(Identifier symbol, float fill, float cap, int rgb, int wash, Component value) {
            this(symbol, fill, cap, rgb, wash, value, null);
        }
    }

    private static int darken(int rgb) {
        int r = (int) (((rgb >> 16) & 0xFF) * 0.55f);
        int g = (int) (((rgb >> 8) & 0xFF) * 0.55f);
        int b = (int) ((rgb & 0xFF) * 0.55f);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private static String trim(Font font, String text, int maxW) {
        if (text == null) return "";
        if (font.width(text) <= maxW) return text;
        return font.plainSubstrByWidth(text, Math.max(0, maxW - font.width("…"))) + "…";
    }
}
