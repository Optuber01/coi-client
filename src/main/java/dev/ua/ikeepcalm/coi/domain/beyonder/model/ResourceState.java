package dev.ua.ikeepcalm.coi.domain.beyonder.model;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.ua.ikeepcalm.coi.util.CoiLog;
import dev.ua.ikeepcalm.coi.util.JsonRead;

import java.util.*;

/**
 * Ability resource meters, off {@code coi-client:resource}. Keyed by {@code id}, so a refresh
 * overwrites in place and a self-refreshing passive does not reshuffle the stack. A non-zero
 * {@code ttlMs} expires locally, which keeps the HUD clean when the server stops sending.
 */
public class ResourceState {

    /** Arrival order; expired bars are swept on the way out. */
    public static List<Entry> visible() {
        sweep();
        return new ArrayList<>(entries.values());
    }

    private static final int DEFAULT_RGB = 0xFFFFFF;

    private static final Map<String, Entry> entries = new LinkedHashMap<>();

    private ResourceState() {
    }

    public static void handle(String json) {
        if (json == null || json.isBlank()) return;
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            String id = JsonRead.string(root, "id");
            if (id.isEmpty()) return;
            if (JsonRead.bool(root, "remove")) {
                entries.remove(id);
                return;
            }
            long now = System.currentTimeMillis();
            long ttl = JsonRead.longOf(root, "ttlMs");
            double max = JsonRead.dbl(root, "max");
            if (!(max > 0)) max = 1;
            double current = Math.clamp(JsonRead.dbl(root, "current"), 0, max);
            // LinkedHashMap keeps the original slot when an existing key is re-put
            entries.put(id, new Entry(
                    id,
                    JsonRead.string(root, "label"),
                    current,
                    max,
                    parseColor(JsonRead.string(root, "color")),
                    ttl > 0 ? now + ttl : 0,
                    !"value".equalsIgnoreCase(JsonRead.string(root, "format")),
                    now
            ));
        } catch (Exception e) {
            CoiLog.LOG.warn("Malformed resource payload: {}", json);
        }
    }

    /** Cheap enough for the render gate — no copy, just the sweep. */
    public static boolean hasData() {
        sweep();
        return !entries.isEmpty();
    }

    /** Debug only: two fake bars, one of each format. */
    public static void debugInject() {
        handle("{\"id\":\"debug:rage\",\"label\":\"Rage Meter\",\"current\":62.5,\"max\":100,"
                + "\"color\":\"FF5555\",\"ttlMs\":8000,\"format\":\"percent\"}");
        handle("{\"id\":\"debug:seeds\",\"label\":\"Seeds\",\"current\":3,\"max\":5,"
                + "\"color\":\"55FF55\",\"ttlMs\":8000,\"format\":\"value\"}");
    }

    private static void sweep() {
        long now = System.currentTimeMillis();
        Iterator<Entry> it = entries.values().iterator();
        while (it.hasNext()) {
            Entry entry = it.next();
            if (entry.expiresAt() != 0 && now >= entry.expiresAt()) it.remove();
        }
    }

    public static void reset() {
        entries.clear();
    }

    /**
     * {@code expiresAt == 0} means "persistent until an explicit remove".
     */
    public record Entry(String id, String label, double current, double max, int rgb,
                        long expiresAt, boolean percent, long receivedAt) {
    }

    public static void debugClear() {
        reset();
    }

    private static int parseColor(String hex) {
        if (hex == null || hex.isBlank()) return DEFAULT_RGB;
        try {
            return Integer.parseInt(hex.startsWith("#") ? hex.substring(1) : hex, 16) & 0xFFFFFF;
        } catch (NumberFormatException e) {
            return DEFAULT_RGB;
        }
    }
}
