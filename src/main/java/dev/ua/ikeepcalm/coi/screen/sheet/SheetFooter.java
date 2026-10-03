package dev.ua.ikeepcalm.coi.screen.sheet;

import dev.ua.ikeepcalm.coi.domain.menu.model.MenuComponent;
import dev.ua.ikeepcalm.coi.domain.menu.model.MenuIcon;
import dev.ua.ikeepcalm.coi.network.ServerCapabilities;
import dev.ua.ikeepcalm.coi.screen.menu.MenuTheme;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.language.I18n;

import java.util.ArrayList;
import java.util.List;

import static dev.ua.ikeepcalm.coi.screen.sheet.SheetMetrics.GAP;

public class SheetFooter {

    private record Action(MenuComponent.Button model, Runnable run) {
    }

    private final List<Action> actions = new ArrayList<>();

    boolean isEmpty() {
        return actions.isEmpty();
    }

    void clear() {
        actions.clear();
    }

    void build(Runnable serverMenu, Runnable done) {
        actions.clear();
        actions.add(new Action(button("server_menu", I18n.get(
                        ServerCapabilities.has("menu_archive")
                                ? "screen.coi.sheet_btn_pathways" : "screen.coi.sheet_btn_server_menu"),
                MenuComponent.ButtonStyle.SECONDARY), serverMenu));
        actions.add(new Action(button("done", I18n.get("gui.done"),
                MenuComponent.ButtonStyle.PRIMARY), done));
    }

    private static MenuComponent.Button button(String id, String label, MenuComponent.ButtonStyle style) {
        return new MenuComponent.Button(id, label, "", style, true, "", null, MenuIcon.NONE);
    }

    void draw(Font font, GuiGraphicsExtractor graphics, int x, int y, int width, int h,
              int accent, int mouseX, int mouseY) {
        int bw = buttonW(width);
        for (int i = 0; i < actions.size(); i++) {
            int bx = x + i * (bw + GAP);
            boolean hovered = SheetMetrics.inBox(mouseX, mouseY, bx, y, bw, h);
            MenuTheme.button(graphics, font, bx, y, bw, h, actions.get(i).model(), accent, hovered, false);
        }
    }

    /**
     * @return what the button under the cursor does, or null when the press missed the row
     */
    Runnable hit(double mx, double my, int x, int y, int width, int h) {
        int bw = buttonW(width);
        for (int i = 0; i < actions.size(); i++) {
            int bx = x + i * (bw + GAP);
            if (SheetMetrics.inBox(mx, my, bx, y, bw, h)) return actions.get(i).run();
        }
        return null;
    }

    private int buttonW(int width) {
        return (width - (actions.size() - 1) * GAP) / actions.size();
    }
}
