package dev.ua.ikeepcalm.coi.network;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.ua.ikeepcalm.coi.util.CoiLog;
import dev.ua.ikeepcalm.coi.util.JsonRead;

import java.util.HashSet;
import java.util.Set;

/**
 * The {@code coi-client:server} reply to our hello. A server that never replies leaves every
 * {@link #has(String)} false, which is how the client hides UI an old server has no data for.
 */
public class ServerCapabilities {

    /**
     * The empty string, not this, means "no reply at all".
     */
    private static final String UNKNOWN_VERSION = "unknown";

    private static String pluginVersion = "";
    private static int protocol = 0;
    private static Set<String> features = Set.of();
    private static boolean known = false;

    private ServerCapabilities() {
    }

    public static void handle(String json) {
        if (json == null || json.isBlank()) return;
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            pluginVersion = JsonRead.string(root, "pluginVersion", UNKNOWN_VERSION);
            // A reply that names no protocol is a protocol-1 server
            protocol = JsonRead.intOf(root, "protocol", 1);
            features = readFeatures(root);
            known = true;
            CoiLog.LOG.info("Server capabilities — version {}, protocol {}, features {}", pluginVersion, protocol, features);
        } catch (Exception e) {
            CoiLog.LOG.warn("Malformed server capabilities payload: {}", json);
        }
    }

    private static Set<String> readFeatures(JsonObject root) {
        if (!root.has("features") || !root.get("features").isJsonArray()) return Set.of();
        Set<String> parsed = new HashSet<>();
        for (JsonElement element : root.getAsJsonArray("features")) {
            parsed.add(element.getAsString());
        }
        return Set.copyOf(parsed);
    }

    public static boolean has(String feature) {
        return features.contains(feature);
    }

    public static boolean known() {
        return known;
    }

    public static String pluginVersion() {
        return pluginVersion;
    }

    public static int protocol() {
        return protocol;
    }

    public static void reset() {
        pluginVersion = "";
        protocol = 0;
        features = Set.of();
        known = false;
    }
}
