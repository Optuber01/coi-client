package dev.ua.ikeepcalm.coi.screen.sheet;

import dev.ua.ikeepcalm.coi.domain.beyonder.model.SheetState;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

import static dev.ua.ikeepcalm.coi.screen.sheet.SheetContext.round0;
import static dev.ua.ikeepcalm.coi.screen.sheet.SheetGlyphs.*;
import static dev.ua.ikeepcalm.coi.screen.sheet.SheetPalette.*;

final class SheetConditions {

    private SheetConditions() {
    }

    static int draw(SheetContext ctx, GuiGraphicsExtractor graphics, int x, int y, int w) {
        List<SheetChips.Chip> chips = conditionChips();
        if (chips.isEmpty()) return y;

        int ry = ctx.section(graphics, x, y, w, "screen.coi.sheet_sec_conditions", GLYPH_WARD);
        ry = SheetChips.draw(ctx, graphics, x, ry, SheetChips.layout(ctx, chips, w));
        return SheetChips.explanations(ctx, graphics, x, ry, w, chips) + ctx.sectionGap();
    }

    private static List<SheetChips.Chip> conditionChips() {
        List<SheetChips.Chip> chips = new ArrayList<>();
        SheetState.Gauge lifeAndDeath = SheetState.lifeAndDeath();
        if (lifeAndDeath.present()) {
            chips.add(new SheetChips.Chip(I18n.get("screen.coi.sheet_life_death"),
                    I18n.get("screen.coi.sheet_percent", round0(Math.clamp(lifeAndDeath.value(), 0, 1) * 100)),
                    VIOLET, GLYPH_SOUL,
                    Component.translatable("screen.coi.sheet_life_death_desc")));
        }
        SheetState.Pressure pressure = SheetState.pressure();
        if (pressure.present()) {
            chips.add(new SheetChips.Chip(I18n.get("screen.coi.sheet_pressure"),
                    pressure.stacks() + "/" + pressure.cap(), AMBER, GLYPH_POWER,
                    Component.translatable("screen.coi.sheet_pressure_desc")));
        }
        if (SheetState.anomaly()) {
            chips.add(new SheetChips.Chip(I18n.get("screen.coi.sheet_anomaly"), "", RED, GLYPH_WARD,
                    Component.translatable("screen.coi.sheet_anomaly_desc")));
        }
        return chips;
    }
}
