package dev.ua.ikeepcalm.coi.domain.menu.model;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.ua.ikeepcalm.coi.domain.menu.service.MenuJson;

import java.util.Locale;

/**
 * Anything the client cannot resolve degrades to {@link Kind#NONE} rather than a
 * missing-texture square. {@link Kind#GLYPH} is the only kind whose art ships in this jar, so
 * it is the only one a server can name without knowing the player's resource pack; its value
 * is a bare name, never a path — see {@code CoiIcons.glyph}.
 */
public record MenuIcon(Kind kind, String value) {

    public enum Kind {PATHWAY, ITEM, HEAD, GLYPH, ABILITY, NONE}

    public static final MenuIcon NONE = new MenuIcon(Kind.NONE, "");

    /**
     * Long enough for a namespaced item id, short enough not to smuggle a paragraph.
     */
    static final int MAX_VALUE = 128;

    public boolean present() {
        return kind != Kind.NONE && !value.isEmpty();
    }

    /** {@code {"kind":"pathway","value":"fool"}}, or nothing usable. */
    public static MenuIcon parse(JsonElement element) {
        if (element == null || !element.isJsonObject()) return NONE;
        JsonObject node = element.getAsJsonObject();
        String kind = MenuJson.string(node, "kind", MenuLimits.MAX_WORD).toLowerCase(Locale.ROOT);
        String value = MenuJson.string(node, "value", MenuIcon.MAX_VALUE);
        if (value.isEmpty()) return NONE;
        return switch (kind) {
            case "pathway" -> new MenuIcon(Kind.PATHWAY, value);
            case "item" -> new MenuIcon(Kind.ITEM, value);
            case "head" -> new MenuIcon(Kind.HEAD, value);
            case "glyph" -> new MenuIcon(Kind.GLYPH, value);
            case "ability" -> new MenuIcon(Kind.ABILITY, value);
            default -> NONE;
        };
    }
}
