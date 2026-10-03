package dev.ua.ikeepcalm.coi.domain.menu.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.ua.ikeepcalm.coi.domain.menu.model.MenuLimits;

import java.util.ArrayList;
import java.util.List;

/**
 * The defensive reads a menu document is parsed with. Nothing here throws: a missing key, a
 * value of the wrong JSON type and a number that will not parse all read as "absent". Lengths
 * are trimmed and arrays capped at the call site, so a runaway server-side loop costs a
 * truncated row rather than a card a hundred thousand pixels tall.
 */
public final class MenuJson {

    private MenuJson() {
    }

    /**
     * Up to {@code cap}; a non-object entry is skipped rather than counted.
     */
    public static List<JsonObject> objects(JsonElement element, int cap) {
        if (element == null || !element.isJsonArray()) return List.of();
        JsonArray array = element.getAsJsonArray();
        List<JsonObject> objects = new ArrayList<>();
        for (JsonElement item : array) {
            if (objects.size() >= cap) break;
            if (item.isJsonObject()) objects.add(item.getAsJsonObject());
        }
        return objects;
    }

    /**
     * Each trimmed to {@link MenuLimits#MAX_TEXT}.
     */
    public static List<String> strings(JsonElement element, int cap) {
        if (element == null || !element.isJsonArray()) return List.of();
        List<String> lines = new ArrayList<>();
        for (JsonElement line : element.getAsJsonArray()) {
            if (lines.size() >= cap) break;
            if (line.isJsonPrimitive()) lines.add(trim(line.getAsString(), MenuLimits.MAX_TEXT));
        }
        return List.copyOf(lines);
    }

    /**
     * Absent means enabled: a server made to spell out {@code true} will eventually forget one.
     */
    public static boolean enabled(JsonObject node) {
        return !node.has("enabled") || bool(node, "enabled");
    }

    public static String string(JsonObject node, String key, int max) {
        if (node == null || !node.has(key)) return "";
        JsonElement element = node.get(key);
        if (!element.isJsonPrimitive()) return "";
        return trim(element.getAsString(), max);
    }

    private static String trim(String value, int max) {
        if (value == null) return "";
        return value.length() <= max ? value : value.substring(0, max);
    }

    public static int intOf(JsonObject node, String key, int fallback) {
        try {
            return node.has(key) && node.get(key).isJsonPrimitive() ? node.get(key).getAsInt() : fallback;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public static double dbl(JsonObject node, String key) {
        try {
            return node.has(key) && node.get(key).isJsonPrimitive() ? node.get(key).getAsDouble() : 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public static boolean bool(JsonObject node, String key) {
        try {
            return node.has(key) && node.get(key).isJsonPrimitive() && node.get(key).getAsBoolean();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Clamped to 0..1, so a server sending 1.4 gets a full bar, not one past its own frame.
     */
    public static double fraction(JsonObject node, String key) {
        return Math.clamp(dbl(node, key), 0.0, 1.0);
    }

    /** Six hex digits, no {@code #}, to 0xRRGGBB; 0 means "unspecified". */
    public static int hex(String value) {
        String clean = value.startsWith("#") ? value.substring(1) : value;
        if (clean.length() != 6) return 0;
        try {
            return Integer.parseInt(clean, 16);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public static int color(JsonObject node, String key) {
        return hex(string(node, key, MenuLimits.MAX_COLOR));
    }
}
