package dev.ua.ikeepcalm.coi.screen.sheet;

import dev.ua.ikeepcalm.coi.domain.beyonder.model.SheetState;
import dev.ua.ikeepcalm.coi.network.payload.ServerboundPayloads.ActionPayload;
import dev.ua.ikeepcalm.coi.screen.menu.MenuTheme;
import dev.ua.ikeepcalm.coi.ui.CoiStyle;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;

import static dev.ua.ikeepcalm.coi.screen.sheet.SheetGlyphs.GLYPH_DEFENSE;
import static dev.ua.ikeepcalm.coi.screen.sheet.SheetGlyphs.GLYPH_DIVINATION;
import static dev.ua.ikeepcalm.coi.screen.sheet.SheetMetrics.*;
import static dev.ua.ikeepcalm.coi.screen.sheet.SheetPalette.accent;

final class SheetDestinations {

    private record Nav(String target, String[] glyph) {
    }

    private static final Nav[] NAV = {
            new Nav("church", SheetGlyphs.CHURCH),
            new Nav("abilities", SheetGlyphs.SPARK),
            new Nav("mythical", SheetGlyphs.BEAST),
            new Nav("uniqueness", SheetGlyphs.GEM),
            new Nav("honorific", SheetGlyphs.CROWN),
            new Nav("map", SheetGlyphs.PIN),
            new Nav("seat", SheetGlyphs.BLADES),
            new Nav("throne", SheetGlyphs.THRONE),
            new Nav("pantheon", SheetGlyphs.PILLARS)
    };

    private final SheetContext ctx;
    private final float[] navHover = new float[NAV.length];
    private float prefsHover;
    private int preferenceHeight = NAV_CARD_H;

    SheetDestinations(SheetContext ctx) {
        this.ctx = ctx;
    }

    int draw(GuiGraphicsExtractor graphics, int x, int y, int w, int mouseX, int mouseY, int bottom) {
        int ry = ctx.section(graphics, x, y, w, "screen.coi.sheet_sec_nav", GLYPH_DIVINATION) + 4;
        int cols = w >= THREE_COLUMN_MIN ? 3 : w >= TWO_COLUMN_MIN ? 2 : 1;
        int tileW = (w - GAP * (cols - 1)) / cols;
        boolean[] gates = gates();
        int rows = (NAV.length + cols - 1) / cols;
        int preferenceHeading = SheetMetrics.HEADING_H + 4;
        int naturalBottom = ry + rows * (NAV_CARD_H + GAP) - GAP
                + ctx.sectionGap() + preferenceHeading + NAV_CARD_H;
        int spare = Math.max(0, bottom - naturalBottom);
        int tileH = NAV_CARD_H + spare / (rows + 1);
        preferenceHeight = NAV_CARD_H + spare - (tileH - NAV_CARD_H) * rows;

        for (int i = 0; i < NAV.length; i++) {
            int cx = x + (i % cols) * (tileW + GAP);
            int cy = ry + (i / cols) * (tileH + GAP);
            drawNavCard(graphics, i, gates[i], cx, cy, tileW, tileH, mouseX, mouseY);
        }
        return ry + rows * (tileH + GAP) - GAP;
    }

    private static boolean[] gates() {
        SheetState.Actions actions = SheetState.actions();
        boolean[] gates = new boolean[NAV.length];
        for (int i = 0; i < NAV.length; i++) {
            gates[i] = actions.unlocked(NAV[i].target());
        }
        return gates;
    }

    private void drawNavCard(GuiGraphicsExtractor graphics, int index, boolean unlocked,
                             int x, int y, int w, int h, int mouseX, int mouseY) {
        Nav nav = NAV[index];
        boolean inside = mouseY >= ctx.viewTop() && mouseY < ctx.viewBottom()
                && SheetMetrics.inBox(mouseX, mouseY, x, y, w, h);
        navHover[index] = ctx.approach(navHover[index], inside && unlocked ? 1f : 0f, HOVER_MS);
        float hoverT = navHover[index];

        int accent = unlocked ? accent() : CoiStyle.INACTIVE;
        graphics.fill(x, y, x + w, y + h, MenuTheme.withAlpha(accent, hoverT * 0.08f));
        graphics.fill(x, y + h - 1, x + w, y + h, 0xFF494C43);

        SheetGlyphs.draw(graphics, nav.glyph(), x + 7, y + (h - 16) / 2, 16, accent);

        int textX = x + 27;
        int textW = x + w - 8 - textX - (unlocked ? 0 : 12);
        graphics.text(ctx.font(), ctx.trim(I18n.get("screen.coi.sheet_btn_" + nav.target()), textW),
                textX, y + (h - 19) / 2, unlocked ? CoiStyle.TEXT_BODY : CoiStyle.INACTIVE, true);
        graphics.text(ctx.font(), ctx.trim(I18n.get("screen.coi.sheet_nav_" + nav.target() + "_desc"), textW),
                textX, y + (h - 19) / 2 + 11, unlocked ? CoiStyle.TEXT_MUTED : CoiStyle.INACTIVE, false);

        if (!unlocked) {
            SheetGlyphs.draw(graphics, SheetGlyphs.LOCK, x + w - 16, y + (h - 10) / 2, 10,
                    CoiStyle.INACTIVE);
        }
        Component tip = unlocked ? null : Component.translatable("screen.coi.sheet_lock_" + nav.target());
        ctx.addHit(new SheetContext.Hit(x, y, w, h,
                unlocked ? () -> ctx.open(nav.target()) : null, tip));
        if (inside && tip != null) ctx.hover(tip);
    }

    int preferences(GuiGraphicsExtractor graphics, int x, int y, int w, int mouseX, int mouseY) {
        int ry = ctx.section(graphics, x, y, w, "screen.coi.sheet_sec_prefs", GLYPH_DEFENSE) + 4;
        boolean on = SheetState.actions().terrainDamage();
        boolean hovered = mouseY >= ctx.viewTop() && mouseY < ctx.viewBottom()
                && SheetMetrics.inBox(mouseX, mouseY, x, ry, w, preferenceHeight);
        prefsHover = ctx.approach(prefsHover, hovered ? 1f : 0f, HOVER_MS);

        int accent = accent();
        int state = on ? MenuTheme.SUCCESS : CoiStyle.INACTIVE;
        MenuTheme.panel(graphics, x, ry, w, preferenceHeight, MenuTheme.surface(prefsHover),
                MenuTheme.lerpArgb(MenuTheme.withAlpha(accent, 0.30f), accent, prefsHover));

        SheetGlyphs.draw(graphics, SheetGlyphs.BLOCKS, x + 7, ry + (preferenceHeight - 16) / 2, 16, state);

        String label = I18n.get(on ? "screen.coi.sheet_on" : "screen.coi.sheet_off");
        int labelW = ctx.font().width(label);
        int switchX = x + w - 8 - 20;
        int textX = x + 27;
        int textW = switchX - 6 - labelW - 4 - textX;

        graphics.text(ctx.font(), ctx.trim(I18n.get("screen.coi.sheet_terrain_title"), textW), textX, ry + (preferenceHeight - 19) / 2,
                CoiStyle.TEXT_BODY, true);
        graphics.text(ctx.font(), ctx.trim(I18n.get("screen.coi.sheet_terrain_desc"), textW), textX, ry + (preferenceHeight - 19) / 2 + 11,
                CoiStyle.TEXT_MUTED, false);
        graphics.text(ctx.font(), label, switchX - 4 - labelW, ry + (preferenceHeight - 8) / 2, state, false);
        MenuTheme.toggle(graphics, switchX, ry + (preferenceHeight - 10) / 2, on, true, MenuTheme.SUCCESS);

        ctx.addHit(new SheetContext.Hit(x, ry, w, preferenceHeight,
                () -> CharacterSheetScreen.send(ActionPayload.of("toggle_terrain")), null));
        return ry + preferenceHeight;
    }
}
