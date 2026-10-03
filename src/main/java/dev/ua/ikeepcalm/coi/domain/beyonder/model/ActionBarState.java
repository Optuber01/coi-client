package dev.ua.ikeepcalm.coi.domain.beyonder.model;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import dev.ua.ikeepcalm.coi.util.CoiLog;
import dev.ua.ikeepcalm.coi.util.JsonRead;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;

import java.util.ArrayList;
import java.util.List;

/**
 * Each {@code coi-client:actionbar} payload replaces the whole list; entries then expire
 * locally at {@code receivedAt + ttlMs}, so the HUD keeps draining between pushes.
 */
public class ActionBarState {

    /** What a payload that names neither is given. */
    private static final int DEFAULT_MAX_VISIBLE = 2;

    /**
     * Through the registry-free {@link ComponentSerialization#CODEC}; Adventure's Gson
     * serializer, which the plugin uses, writes the same shape. Anything the codec rejects
     * degrades to its plain string form rather than dropping the line.
     */
    private static Component deserialize(JsonElement element) {
        if (element == null || element.isJsonNull()) return null;
        return ComponentSerialization.CODEC.parse(JsonOps.INSTANCE, element)
                .result()
                .orElseGet(() -> element.isJsonPrimitive() ? Component.literal(element.getAsString()) : null);
    }
    private static final long DEFAULT_TTL_MS = 2000L;

    private static final List<Entry> entries = new ArrayList<>();
    private static int maxVisible = DEFAULT_MAX_VISIBLE;

    private ActionBarState() {
    }

    public static void handle(String json) {
        if (json == null || json.isBlank()) return;
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            long now = System.currentTimeMillis();
            List<Entry> parsed = new ArrayList<>();
            if (root.has("entries") && root.get("entries").isJsonArray()) {
                for (JsonElement element : root.getAsJsonArray("entries")) {
                    Entry entry = parseEntry(element.getAsJsonObject(), now);
                    if (entry != null) parsed.add(entry);
                }
            }
            synchronized (entries) {
                entries.clear();
                entries.addAll(parsed);
            }
            maxVisible = Math.max(1, JsonRead.intOf(root, "maxVisible", DEFAULT_MAX_VISIBLE));
        } catch (Exception e) {
            CoiLog.LOG.warn("Malformed action bar payload: {}", json);
        }
    }

    private static Entry parseEntry(JsonObject entry, long now) {
        Component text = deserialize(entry.get("text"));
        if (text == null) return null;
        long ttl = JsonRead.longOf(entry, "ttlMs", DEFAULT_TTL_MS);
        return new Entry(
                JsonRead.string(entry, "channel"),
                JsonRead.string(entry, "key"),
                JsonRead.intOf(entry, "priority"),
                now + Math.max(0L, ttl),
                text);
    }

    /** Unexpired entries in server order, truncated to {@code limit}. */
    public static List<Entry> visibleEntries(long now, int limit) {
        List<Entry> visible = new ArrayList<>();
        synchronized (entries) {
            for (Entry entry : entries) {
                if (entry.expiresAtMs() > now) visible.add(entry);
                if (visible.size() >= limit) break;
            }
        }
        return visible;
    }

    /** Debug only. */
    public static void debugInject(long ttlMs) {
        long expiry = System.currentTimeMillis() + ttlMs;
        synchronized (entries) {
            entries.clear();
            entries.add(new Entry("COOLDOWN", "artifact:sword", 60, expiry,
                    Component.literal("Sword of Judgement ready in 3s")));
            entries.add(new Entry("RESERVE", "sun:light", 40, expiry,
                    Component.literal("Holy Light reserve 42/60")));
        }
        maxVisible = DEFAULT_MAX_VISIBLE;
    }

    public static int getMaxVisible() {
        return maxVisible;
    }

    /**
     * {@code expiresAtMs} is already absolute.
     */
    public record Entry(String channel, String key, int priority, long expiresAtMs, Component text) {
    }

    public static void reset() {
        synchronized (entries) {
            entries.clear();
        }
        maxVisible = DEFAULT_MAX_VISIBLE;
    }
}
