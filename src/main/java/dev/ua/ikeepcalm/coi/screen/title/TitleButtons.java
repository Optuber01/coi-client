package dev.ua.ikeepcalm.coi.screen.title;

import dev.ua.ikeepcalm.coi.screen.menu.MenuTheme;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * The plate drawn in place of a vanilla button's stone sprite. Only the background is ours — the
 * label comes from vanilla's {@code extractContents} afterwards, so nothing here touches text.
 * The 2px rail is the same mark {@code SheetDestinations} carries; a disabled control loses it.
 */
public class TitleButtons {

    private static final int PLATE = 0xC00A0910;

    private TitleButtons() {
    }

    public static void plate(GuiGraphicsExtractor graphics, int x, int y, int w, int h,
                             boolean hovered, boolean enabled) {
        if (w <= 0 || h <= 0) return;
        int accent = TitleTakeover.accent();
        boolean lifted = hovered && enabled;

        int border = !enabled ? MenuTheme.BORDER_OFF
                : lifted ? accent : MenuTheme.withAlpha(accent, 0.30f);
        MenuTheme.panel(graphics, x, y, w, h, PLATE, border);
        // its own layer, not a blend: SURFACE_HOVER is a translucent white wash, and lerping
        // toward it would take the plate's own opacity with it
        if (lifted) {
            MenuTheme.panel(graphics, x, y, w, h, MenuTheme.SURFACE_HOVER, 0);
        }
        if (enabled) {
            graphics.fill(x, y + 1, x + 2, y + h - 1, accent);
        }
    }
}
