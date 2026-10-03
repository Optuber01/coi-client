package dev.ua.ikeepcalm.coi.screen.menu;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import static dev.ua.ikeepcalm.coi.screen.menu.MenuMetrics.GAUGE_MS;
import static dev.ua.ikeepcalm.coi.screen.menu.MenuMetrics.HOVER_MS;

/**
 * One laid-out piece, positioned in <em>content space</em>: the render loop adds
 * {@code viewTop - scrollY}. Heights are decided once, here, so the scrollbar and the hit test
 * cannot disagree with what was drawn.
 */
public abstract class MenuPart {

    protected final MenuContext ctx;
    protected final Font font;

    int y;
    int height;

    float hoverT;
    float shownT;

    MenuPart(MenuContext ctx) {
        this.ctx = ctx;
        this.font = ctx.font();
    }

    abstract void render(GuiGraphicsExtractor g, int x, int top, int mouseX, int mouseY);

    int growthCapacity() {
        return 0;
    }

    boolean click(double mx, double my, int x, int top) {
        return false;
    }

    float hover(boolean hovered) {
        hoverT = ctx.approach(hoverT, hovered ? 1f : 0f, HOVER_MS);
        return hoverT;
    }

    double shown(double target) {
        shownT = ctx.approach(shownT, (float) target, GAUGE_MS);
        return shownT;
    }
}
