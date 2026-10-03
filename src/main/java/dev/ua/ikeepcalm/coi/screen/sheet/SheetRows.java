package dev.ua.ikeepcalm.coi.screen.sheet;

import dev.ua.ikeepcalm.coi.screen.menu.MenuGauges;
import dev.ua.ikeepcalm.coi.ui.CoiStyle;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import static dev.ua.ikeepcalm.coi.screen.sheet.SheetMetrics.*;

final class SheetRows {

    @FunctionalInterface
    interface BarPainter {
        void draw(GuiGraphicsExtractor graphics, int x, int y, int w);
    }

    @FunctionalInterface
    interface SymbolPainter {
        void draw(GuiGraphicsExtractor graphics, int x, int y);
    }

    private SheetRows() {
    }

    static int vital(SheetContext ctx, GuiGraphicsExtractor graphics, int x, int y, int w,
                            SymbolPainter symbol, String label, String value, int valueRgb,
                            BarPainter bar, Component sub, int subRgb, Component note) {
        // a null extractor measures the same wrapped row without submitting paint
        if (graphics != null) symbol.draw(graphics, x, y);
        int textX = x + SYMBOL + 8;
        int textW = x + w - textX;

        int valueW = ctx.font().width(value);
        boolean stacked = ctx.font().width(label) + valueW + 8 > textW;
        int valueY = y + (stacked ? 13 : 1);
        if (graphics != null) {
            graphics.text(ctx.font(), ctx.trim(label, textW), textX, y + 1, CoiStyle.TEXT_BODY, false);
            graphics.text(ctx.font(), ctx.trim(value, textW), stacked ? textX : textX + textW - valueW,
                    valueY, valueRgb, false);
            bar.draw(graphics, textX, valueY + 12, textW);
        }
        int next = valueY + 22;
        for (var line : ctx.font().split(sub, Math.max(1, textW))) {
            if (graphics != null) graphics.text(ctx.font(), line, textX, next, subRgb, false);
            next += 10;
        }
        if (note != null) {
            next += 3;
            for (var line : ctx.font().split(note, Math.max(1, textW))) {
                if (graphics != null) graphics.text(ctx.font(), line, textX, next, CoiStyle.TEXT_MUTED, false);
                next += 10;
            }
        }
        return Math.max(y + VITAL_ROW_H, next + 3);
    }

    static BarPainter bar(double fraction, int argb) {
        return (graphics, x, y, w) -> MenuGauges.gauge(graphics, x, y, w, BAR_H, fraction, argb);
    }
}
