package dev.ua.ikeepcalm.coi.domain.menu.service;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.ua.ikeepcalm.coi.domain.menu.model.*;
import dev.ua.ikeepcalm.coi.util.CoiLog;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * {@code coi-client:menu} JSON to a {@link MenuDocument}.
 * <p>
 * <b>Nothing in here throws.</b> The document comes from a plugin that may be newer than this
 * client and is rendered as a whole screen, so the failure modes are asymmetric: dropping one
 * unreadable component costs a row, letting an exception out of a network receiver costs the
 * screen. Unknown {@code type}s, wrong JSON types and missing keys all skip and carry on; only
 * a body that is not a JSON object at all gives up entirely.
 */
public class MenuParser {

    private MenuParser() {
    }

    /**
     * @return the document, or null when the body is not usable at all
     */
    public static MenuDocument parse(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            JsonElement root = JsonParser.parseString(json);
            if (!root.isJsonObject()) return null;
            JsonObject node = root.getAsJsonObject();
            String session = MenuJson.string(node, "session", MenuLimits.MAX_ID);
            if (MenuJson.bool(node, "closed")) return MenuDocument.closed(session);

            return new MenuDocument(
                    session,
                    MenuJson.intOf(node, "version", 0),
                    MenuJson.string(node, "screen", MenuLimits.MAX_ID),
                    MenuJson.string(node, "title", MenuLimits.MAX_TITLE),
                    MenuJson.string(node, "subtitle", MenuLimits.MAX_TITLE),
                    MenuJson.color(node, "accent"),
                    MenuIcon.parse(node.get("icon")),
                    MenuJson.bool(node, "back"),
                    !node.has("closable") || MenuJson.bool(node, "closable"),
                    toast(node.get("toast")),
                    sections(node.get("sections")),
                    MenuParts.buttons(node.get("footer"), MenuLimits.MAX_BUTTONS),
                    false, presentation(node.get("presentation")));
        } catch (Exception e) {
            CoiLog.LOG.warn("Malformed menu payload", e);
            return null;
        }
    }

    private static MenuDocument.Presentation presentation(JsonElement element) {
        if (element == null || !element.isJsonObject()) return MenuDocument.Presentation.NONE;
        JsonObject node = element.getAsJsonObject();
        String template = MenuJson.string(node, "template", MenuLimits.MAX_ID);
        if (!Set.of("specimen", "ability_manual", "ledger", "relic", "inscription", "atlas", "challenge",
                        "throne", "pantheon", "ascension")
                .contains(template)) return MenuDocument.Presentation.NONE;
        return new MenuDocument.Presentation(template,
                MenuJson.string(node, "subject", MenuLimits.MAX_ID),
                MenuJson.string(node, "caption", MenuLimits.MAX_TITLE));
    }

    private static MenuDocument.Toast toast(JsonElement element) {
        if (element == null || !element.isJsonObject()) return null;
        JsonObject node = element.getAsJsonObject();
        String text = MenuJson.string(node, "text", MenuLimits.MAX_TEXT);
        if (text.isEmpty()) return null;
        return new MenuDocument.Toast(MenuStyles.word(node, "style"), text);
    }

    /**
     * A section whose heading carries v2's {@code icon} / {@code badge} / {@code collapsed}
     * becomes a {@link MenuComponent.Heading} at the head of its component list, with the
     * section's own title emptied, so the renderer draws exactly one heading.
     */
    private static List<MenuDocument.Section> sections(JsonElement element) {
        List<MenuDocument.Section> sections = new ArrayList<>();
        int index = 0;
        for (JsonObject node : MenuJson.objects(element, MenuLimits.MAX_SECTIONS)) {
            String title = MenuJson.string(node, "title", MenuLimits.MAX_TITLE);
            List<MenuComponent> components = components(node.get("components"));
            MenuComponent.Heading heading = heading(node, title, index);
            if (heading != null) {
                List<MenuComponent> decorated = new ArrayList<>(components.size() + 1);
                decorated.add(heading);
                decorated.addAll(components);
                sections.add(new MenuDocument.Section("", List.copyOf(decorated)));
            } else {
                sections.add(new MenuDocument.Section(title, components));
            }
            index++;
        }
        return List.copyOf(sections);
    }

    /** @return null when the section named none of v2's heading fields: leave it a caption */
    private static MenuComponent.Heading heading(JsonObject node, String title, int index) {
        boolean collapsible = node.has("collapsed");
        MenuIcon icon = MenuIcon.parse(node.get("icon"));
        String badge = MenuJson.string(node, "badge", MenuLimits.MAX_BADGE);
        if (!collapsible && !icon.present() && badge.isEmpty()) return null;
        String id = MenuJson.string(node, "id", MenuLimits.MAX_ID);
        return new MenuComponent.Heading(id.isEmpty() ? "#section" + index : id, title, icon, badge,
                MenuJson.color(node, "badgeColor"), collapsible, MenuJson.bool(node, "collapsed"));
    }

    private static List<MenuComponent> components(JsonElement element) {
        List<MenuComponent> components = new ArrayList<>();
        for (JsonObject node : MenuJson.objects(element, MenuLimits.MAX_COMPONENTS)) {
            MenuComponent component = component(node);
            if (component != null) components.add(component);
        }
        return List.copyOf(components);
    }

    /** @return null for a {@code type} this client has never heard of */
    private static MenuComponent component(JsonObject node) {
        return switch (MenuJson.string(node, "type", MenuLimits.MAX_TYPE).toLowerCase(Locale.ROOT)) {
            case "text" -> new MenuComponent.Text(MenuJson.string(node, "text", MenuLimits.MAX_TEXT),
                    MenuStyles.text(MenuStyles.word(node, "style")),
                    MenuStyles.align(MenuStyles.word(node, "align")));
            case "note" -> new MenuComponent.Note(MenuStyles.text(MenuStyles.word(node, "style")),
                    MenuJson.string(node, "title", MenuLimits.MAX_TITLE),
                    MenuJson.string(node, "text", MenuLimits.MAX_TEXT),
                    MenuIcon.parse(node.get("icon")));
            case "stat" -> stat(node);
            case "kv" -> new MenuComponent.Kv(MenuParts.kvRows(node.get("rows")));
            case "checklist" -> new MenuComponent.Checklist(MenuParts.checks(node.get("items")));
            case "button" -> MenuParts.button(node);
            case "buttons" -> new MenuComponent.Buttons(
                    MenuParts.buttons(node.get("buttons"), MenuLimits.MAX_BUTTONS),
                    Math.clamp(MenuJson.intOf(node, "columns", 1), 1, 4));
            case "toggle" -> toggle(node);
            case "list" -> new MenuComponent.ListView(MenuJson.string(node, "id", MenuLimits.MAX_ID),
                    MenuParts.rows(node.get("rows")), MenuJson.bool(node, "searchable"),
                    MenuJson.intOf(node, "maxVisible", 0),
                    MenuJson.string(node, "empty", MenuLimits.MAX_TEXT));
            case "grid" -> new MenuComponent.Grid(MenuParts.rows(node.get("cells")),
                    Math.clamp(MenuJson.intOf(node, "columns", 6), 1, 12),
                    MenuStyles.tile(MenuStyles.word(node, "size")));
            case "input" -> input(node);
            case "divider" -> new MenuComponent.Divider(MenuJson.string(node, "label", MenuLimits.MAX_LABEL));
            case "spacer" -> new MenuComponent.Spacer(Math.clamp(MenuJson.intOf(node, "size", 6), 1, 48));
            case "hero" -> hero(node);
            case "details" -> details(node);
            case "steps" -> new MenuComponent.Steps(MenuStyles.step(MenuStyles.word(node, "style")),
                    MenuParts.steps(node.get("items")));
            case "chips" -> new MenuComponent.Chips(MenuParts.chips(node.get("items"), MenuLimits.MAX_CHIPS));
            case "panels" -> new MenuComponent.Panels(Math.clamp(MenuJson.intOf(node, "columns", 2), 1, 3),
                    MenuParts.panelCells(node.get("cells")));
            default -> null;
        };
    }

    private static MenuComponent.Stat stat(JsonObject node) {
        return new MenuComponent.Stat(
                MenuJson.string(node, "label", MenuLimits.MAX_LABEL),
                MenuJson.string(node, "value", MenuLimits.MAX_LABEL),
                MenuJson.fraction(node, "fraction"), node.has("fraction"),
                MenuJson.color(node, "color"),
                MenuJson.string(node, "hint", MenuLimits.MAX_TEXT),
                MenuIcon.parse(node.get("icon")),
                MenuStyles.gauge(MenuStyles.word(node, "style")),
                MenuJson.fraction(node, "cap"), node.has("cap"),
                MenuJson.string(node, "delta", MenuLimits.MAX_BADGE));
    }

    private static MenuComponent.Toggle toggle(JsonObject node) {
        return new MenuComponent.Toggle(
                MenuJson.string(node, "id", MenuLimits.MAX_ID),
                MenuJson.string(node, "label", MenuLimits.MAX_LABEL),
                MenuJson.bool(node, "on"),
                MenuJson.enabled(node),
                MenuJson.string(node, "desc", MenuLimits.MAX_TEXT),
                MenuJson.string(node, "onText", MenuLimits.MAX_BADGE),
                MenuJson.string(node, "offText", MenuLimits.MAX_BADGE),
                MenuJson.string(node, "disabledReason", MenuLimits.MAX_TEXT),
                MenuIcon.parse(node.get("icon")));
    }

    private static MenuComponent.Input input(JsonObject node) {
        return new MenuComponent.Input(
                MenuJson.string(node, "id", MenuLimits.MAX_ID),
                MenuJson.string(node, "label", MenuLimits.MAX_LABEL),
                MenuJson.string(node, "placeholder", MenuLimits.MAX_LABEL),
                MenuJson.string(node, "value", MenuLimits.MAX_LABEL),
                Math.clamp(MenuJson.intOf(node, "maxLength", 64), 1, 256),
                MenuJson.string(node, "submit", MenuLimits.MAX_ID),
                MenuJson.string(node, "submitLabel", MenuLimits.MAX_LABEL),
                MenuJson.string(node, "hint", MenuLimits.MAX_TEXT));
    }

    private static MenuComponent.Hero hero(JsonObject node) {
        return new MenuComponent.Hero(
                MenuIcon.parse(node.get("icon")),
                MenuJson.string(node, "title", MenuLimits.MAX_TITLE),
                MenuJson.string(node, "subtitle", MenuLimits.MAX_TITLE),
                MenuJson.string(node, "badge", MenuLimits.MAX_BADGE),
                MenuJson.color(node, "badgeColor"),
                MenuStyles.hero(MenuStyles.word(node, "style")),
                MenuJson.fraction(node, "fraction"), node.has("fraction"),
                MenuJson.string(node, "fractionLabel", MenuLimits.MAX_BADGE),
                MenuJson.color(node, "color"),
                MenuParts.chips(node.get("chips"), MenuLimits.MAX_HERO_CHIPS));
    }

    /** An absent {@code style} is MUTED here, not the usual BODY: this is a menu's prose. */
    private static MenuComponent.Details details(JsonObject node) {
        return new MenuComponent.Details(
                MenuJson.string(node, "id", MenuLimits.MAX_ID),
                MenuJson.string(node, "summary", MenuLimits.MAX_LABEL),
                MenuJson.strings(node.get("text"), MenuLimits.MAX_DETAILS_BLOCKS),
                node.has("style") ? MenuStyles.text(MenuStyles.word(node, "style"))
                        : MenuComponent.TextStyle.MUTED,
                MenuIcon.parse(node.get("icon")),
                MenuJson.bool(node, "open"));
    }
}
