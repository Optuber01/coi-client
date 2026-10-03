package dev.ua.ikeepcalm.coi.domain.menu.model;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.ua.ikeepcalm.coi.domain.menu.service.MenuJson;

import java.util.ArrayList;
import java.util.List;

/**
 * The structures that appear inside more than one menu component. Same contract as everything
 * else on this channel: a malformed entry is skipped, an absent list is empty, nothing throws.
 */
public final class MenuParts {

    private MenuParts() {
    }

    public static MenuComponent.Button button(JsonObject node) {
        return new MenuComponent.Button(
                MenuJson.string(node, "id", MenuLimits.MAX_ID),
                MenuJson.string(node, "label", MenuLimits.MAX_LABEL),
                MenuJson.string(node, "desc", MenuLimits.MAX_TEXT),
                MenuStyles.button(MenuStyles.word(node, "style")),
                MenuJson.enabled(node),
                MenuJson.string(node, "disabledReason", MenuLimits.MAX_TEXT),
                confirm(node.get("confirm")),
                MenuIcon.parse(node.get("icon")));
    }

    public static List<MenuComponent.Button> buttons(JsonElement element, int cap) {
        List<MenuComponent.Button> buttons = new ArrayList<>();
        for (JsonObject node : MenuJson.objects(element, cap)) {
            buttons.add(button(node));
        }
        return List.copyOf(buttons);
    }

    /**
     * @return null when the button names no confirmation: send the click straight through
     */
    private static MenuComponent.Confirm confirm(JsonElement element) {
        if (element == null || !element.isJsonObject()) return null;
        JsonObject node = element.getAsJsonObject();
        return new MenuComponent.Confirm(MenuJson.string(node, "title", MenuLimits.MAX_TITLE),
                MenuJson.string(node, "body", MenuLimits.MAX_TEXT),
                MenuJson.string(node, "confirmLabel", MenuLimits.MAX_LABEL));
    }

    public static List<MenuComponent.KvRow> kvRows(JsonElement element) {
        List<MenuComponent.KvRow> rows = new ArrayList<>();
        for (JsonObject node : MenuJson.objects(element, MenuLimits.MAX_KV_ROWS)) {
            rows.add(new MenuComponent.KvRow(MenuJson.string(node, "label", MenuLimits.MAX_LABEL),
                    MenuJson.string(node, "value", MenuLimits.MAX_LABEL), MenuJson.color(node, "color"),
                    MenuJson.string(node, "hint", MenuLimits.MAX_TEXT), MenuIcon.parse(node.get("icon"))));
        }
        return List.copyOf(rows);
    }

    public static List<MenuComponent.Check> checks(JsonElement element) {
        List<MenuComponent.Check> checks = new ArrayList<>();
        for (JsonObject node : MenuJson.objects(element, MenuLimits.MAX_CHECKS)) {
            checks.add(new MenuComponent.Check(MenuStyles.check(node),
                    MenuJson.string(node, "label", MenuLimits.MAX_LABEL),
                    MenuJson.string(node, "detail", MenuLimits.MAX_TEXT),
                    MenuIcon.parse(node.get("icon")), MenuJson.color(node, "color")));
        }
        return List.copyOf(checks);
    }

    /**
     * The same record for a list row and a grid cell, hence fields a grid never draws.
     */
    public static List<MenuComponent.Row> rows(JsonElement element) {
        List<MenuComponent.Row> rows = new ArrayList<>();
        for (JsonObject node : MenuJson.objects(element, MenuLimits.MAX_ROWS)) {
            rows.add(new MenuComponent.Row(
                    MenuJson.string(node, "id", MenuLimits.MAX_ID),
                    MenuJson.string(node, "title", MenuLimits.MAX_LABEL),
                    MenuJson.string(node, "subtitle", MenuLimits.MAX_LABEL),
                    MenuIcon.parse(node.get("icon")),
                    MenuJson.string(node, "badge", MenuLimits.MAX_BADGE),
                    MenuJson.color(node, "badgeColor"),
                    tooltip(node.get("tooltip")),
                    MenuJson.string(node, "action", MenuLimits.MAX_ID),
                    MenuJson.enabled(node),
                    MenuJson.color(node, "color"),
                    MenuJson.fraction(node, "fraction"), node.has("fraction"),
                    MenuJson.string(node, "meta", MenuLimits.MAX_LABEL),
                    MenuJson.string(node, "disabledReason", MenuLimits.MAX_TEXT)));
        }
        return List.copyOf(rows);
    }

    public static List<MenuComponent.Chip> chips(JsonElement element, int cap) {
        List<MenuComponent.Chip> chips = new ArrayList<>();
        for (JsonObject node : MenuJson.objects(element, cap)) {
            chips.add(new MenuComponent.Chip(MenuJson.string(node, "label", MenuLimits.MAX_LABEL),
                    MenuJson.string(node, "value", MenuLimits.MAX_LABEL), MenuJson.color(node, "color"),
                    MenuIcon.parse(node.get("icon")), MenuJson.string(node, "hint", MenuLimits.MAX_TEXT)));
        }
        return List.copyOf(chips);
    }

    public static List<MenuComponent.Step> steps(JsonElement element) {
        List<MenuComponent.Step> steps = new ArrayList<>();
        for (JsonObject node : MenuJson.objects(element, MenuLimits.MAX_STEPS)) {
            // Boxed on purpose: an absent "done" is "not yet", neither of a boolean's answers
            Boolean done = node.has("done") && node.get("done").isJsonPrimitive()
                    ? MenuJson.bool(node, "done") : null;
            steps.add(new MenuComponent.Step(MenuJson.string(node, "title", MenuLimits.MAX_LABEL),
                    MenuJson.string(node, "text", MenuLimits.MAX_TEXT), done, MenuIcon.parse(node.get("icon"))));
        }
        return List.copyOf(steps);
    }

    public static List<MenuComponent.PanelCell> panelCells(JsonElement element) {
        List<MenuComponent.PanelCell> cells = new ArrayList<>();
        for (JsonObject node : MenuJson.objects(element, MenuLimits.MAX_PANEL_CELLS)) {
            cells.add(new MenuComponent.PanelCell(
                    MenuJson.string(node, "id", MenuLimits.MAX_ID),
                    MenuIcon.parse(node.get("icon")),
                    MenuJson.string(node, "title", MenuLimits.MAX_LABEL),
                    MenuJson.string(node, "value", MenuLimits.MAX_LABEL),
                    MenuJson.string(node, "subtitle", MenuLimits.MAX_TEXT),
                    MenuJson.color(node, "color"),
                    MenuJson.fraction(node, "fraction"), node.has("fraction"),
                    MenuJson.string(node, "badge", MenuLimits.MAX_BADGE),
                    MenuJson.color(node, "badgeColor"),
                    tooltip(node.get("tooltip")),
                    MenuJson.string(node, "action", MenuLimits.MAX_ID),
                    MenuJson.enabled(node),
                    MenuJson.string(node, "disabledReason", MenuLimits.MAX_TEXT)));
        }
        return List.copyOf(cells);
    }

    public static List<String> tooltip(JsonElement element) {
        return MenuJson.strings(element, MenuLimits.MAX_TOOLTIP_LINES);
    }
}
