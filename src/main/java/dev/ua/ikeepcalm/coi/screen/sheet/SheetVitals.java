package dev.ua.ikeepcalm.coi.screen.sheet;

import dev.ua.ikeepcalm.coi.domain.beyonder.model.SheetState;
import dev.ua.ikeepcalm.coi.hud.render.PlateSymbols;
import dev.ua.ikeepcalm.coi.screen.menu.MenuGauges;
import dev.ua.ikeepcalm.coi.screen.menu.MenuTheme;
import dev.ua.ikeepcalm.coi.ui.CoiStyle;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;

import static dev.ua.ikeepcalm.coi.screen.sheet.SheetContext.*;
import static dev.ua.ikeepcalm.coi.screen.sheet.SheetGlyphs.GLYPH_HEALTH;
import static dev.ua.ikeepcalm.coi.screen.sheet.SheetMetrics.*;
import static dev.ua.ikeepcalm.coi.screen.sheet.SheetPalette.*;

final class SheetVitals {

    private SheetVitals() {
    }

    static int draw(SheetContext ctx, GuiGraphicsExtractor graphics, int x, int y, int w) {
        boolean floors = SheetState.permanentFloor() > 0 || SheetState.godhoodFloor() > 0;
        int ry = ctx.section(graphics, x, y, w, "screen.coi.sheet_sec_vitals", GLYPH_HEALTH);

        ry = healthRow(ctx, graphics, x, ry, w) + ctx.expandedGap(VITAL_GAP);
        ry = spiritualityRow(ctx, graphics, x, ry, w) + ctx.expandedGap(VITAL_GAP);
        ry = madnessRow(ctx, graphics, x, ry, w, floors) + ctx.expandedGap(VITAL_GAP);
        return tirednessRow(ctx, graphics, x, ry, w);
    }

    private static int healthRow(SheetContext ctx, GuiGraphicsExtractor graphics, int x, int y, int w) {
        double max = Math.max(1.0, SheetState.maxHealth());
        double fraction = Math.clamp(SheetState.health() / max, 0, 1);
        int[] palette = healthColors(fraction);
        boolean low = fraction <= 0.25;
        Component sub = low
                ? Component.translatable("screen.coi.sheet_health_low")
                : Component.translatable("screen.coi.sheet_health_sub", round0(fraction * 100));
        return SheetRows.vital(ctx, graphics, x, y, w,
                (g, sx, sy) -> SheetGlyphs.drawFilled(g, SheetGlyphs.HEART, sx, sy, SYMBOL,
                        (float) fraction, SYMBOL_EMPTY, palette[0]),
                I18n.get("screen.coi.sheet_health"),
                num(SheetState.health()) + " / " + num(max), palette[2],
                SheetRows.bar(fraction, palette[0]),
                sub, low ? RED : CoiStyle.TEXT_MUTED, null);
    }

    private static int spiritualityRow(SheetContext ctx, GuiGraphicsExtractor graphics, int x, int y, int w) {
        double max = Math.max(1, SheetState.maxSpirituality());
        double fraction = Math.clamp(SheetState.spirituality() / max, 0, 1);
        boolean low = fraction < 0.30;
        Component sub = low
                ? Component.translatable("screen.coi.sheet_spirit_low")
                : Component.translatable("screen.coi.sheet_spirit_sub", round0(fraction * 100));
        return SheetRows.vital(ctx, graphics, x, y, w,
                (g, sx, sy) -> SheetGlyphs.drawFilled(g, SheetGlyphs.FLASK, sx, sy, SYMBOL,
                        (float) fraction, SYMBOL_EMPTY, low ? RED : SPIRIT_COLORS[0]),
                I18n.get("screen.coi.sheet_spirituality"),
                SheetState.spirituality() + " / " + SheetState.maxSpirituality(),
                low ? RED : SPIRIT_COLORS[2],
                SheetRows.bar(fraction, low ? RED : SPIRIT_COLORS[0]),
                sub, low ? RED : CoiStyle.TEXT_MUTED, null);
    }

    /**
     * The one row the plate and the sheet deliberately disagree on: the plate's brain fills
     * with <em>sanity</em>, this one with madness, because the bar beside it splits the same way.
     */
    private static int madnessRow(SheetContext ctx, GuiGraphicsExtractor graphics, int x, int y, int w,
                                  boolean floors) {
        int stage = SheetState.madnessStage();
        int[] palette = STAGE_COLORS[stage];
        double madness = SheetState.madness();
        // until stage 2 the artwork keeps its own colours and only the bar carries the state
        int wash = stage < 2 ? PlateSymbols.NO_WASH : palette[0] & 0xFFFFFF;
        return SheetRows.vital(ctx, graphics, x, y, w,
                (g, sx, sy) -> PlateSymbols.draw(g, PlateSymbols.BRAIN, sx, sy,
                        (float) (madness / 100.0), 1f, wash),
                I18n.get("screen.coi.sheet_madness"),
                pct(madness) + " · " + I18n.get("screen.coi.sheet_stage_" + stage), palette[2],
                madnessBar(palette),
                Component.translatable("screen.coi.sheet_stage_desc_" + stage), palette[2],
                floors ? floorNote() : null);
    }

    private static Component floorNote() {
        double permanent = SheetState.permanentFloor();
        double godhood = SheetState.godhoodFloor();
        if (permanent > 0 && godhood > 0) {
            return Component.translatable("screen.coi.sheet_floor_both", round0(permanent), round0(godhood));
        }
        return permanent > 0
                ? Component.translatable("screen.coi.sheet_floor_perm", round0(permanent))
                : Component.translatable("screen.coi.sheet_floor_godhood", round0(godhood));
    }

    private static int tirednessRow(SheetContext ctx, GuiGraphicsExtractor graphics, int x, int y, int w) {
        int stage = SheetState.tirednessStage();
        int[] palette = TIRED_COLORS[stage];
        double tiredness = Math.clamp(SheetState.tiredness(), 0, 100);
        return SheetRows.vital(ctx, graphics, x, y, w,
                (g, sx, sy) -> SheetGlyphs.drawFilled(g, SheetGlyphs.HOURGLASS, sx, sy, SYMBOL,
                        (float) (tiredness / 100.0), SYMBOL_EMPTY, palette[0]),
                I18n.get("screen.coi.sheet_tiredness"),
                pct(tiredness) + " · " + I18n.get("screen.coi.sheet_tired_" + stage), palette[2],
                SheetRows.bar(tiredness / 100.0, palette[0]),
                Component.translatable("screen.coi.sheet_tired_desc_" + stage), CoiStyle.TEXT_MUTED, null);
    }

    private static SheetRows.BarPainter madnessBar(int[] palette) {
        double madness = SheetState.madness();
        double permanent = SheetState.permanentFloor();
        double godhood = SheetState.godhoodFloor();
        return (graphics, x, y, w) -> {
            MenuGauges.gauge(graphics, x, y, w, BAR_H, madness / 100.0, palette[0]);
            int filled = (int) Math.round(Math.clamp(madness / 100.0, 0, 1) * w);
            int permEnd = Math.min((int) Math.round(Math.clamp(permanent / 100.0, 0, 1) * w), filled);
            int godEnd = Math.min(permEnd + (int) Math.round(Math.clamp(godhood / 100.0, 0, 1) * w), filled);
            // from y + 1, so the gauge's own top highlight survives the overdraw
            if (permEnd > 0) {
                graphics.fill(x, y + 1, x + permEnd, y + BAR_H, palette[1]);
            }
            if (godEnd > permEnd) {
                graphics.fill(x + permEnd, y + 1, x + godEnd, y + BAR_H,
                        MenuTheme.withAlpha(palette[0], 0.6f));
            }
        };
    }
}
