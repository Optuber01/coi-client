package dev.ua.ikeepcalm.coi.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import dev.ua.ikeepcalm.coi.domain.ability.service.AbilityBindings;
import dev.ua.ikeepcalm.coi.domain.gesture.GestureType;
import dev.ua.ikeepcalm.coi.util.CoiLog;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.IntFunction;

/**
 * {@code config/coi_abilities.json}. All three binding sets share one flat key space —
 * {@code ability1..N}, {@code wheel1..N}, {@code gesture_<id>} — so a save always writes all
 * three. An absent key is an unbound slot, never an error.
 */
public class AbilityConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("coi_abilities.json");

    private AbilityConfig() {
    }

    /**
     * The file has no notion of a partial save, so every caller hands over all three sets.
     */
    public static void saveBindings(String[] abilities, String[] wheelAbilities, String[] gestureAbilities) {
        JsonObject json = new JsonObject();
        for (int i = 0; i < abilities.length; i++) {
            json.addProperty(abilityKey(i), abilities[i]);
        }
        for (int i = 0; i < wheelAbilities.length; i++) {
            json.addProperty(wheelKey(i), wheelAbilities[i]);
        }
        for (int i = 0; i < gestureAbilities.length; i++) {
            json.addProperty(gestureKey(i), gestureAbilities[i]);
        }

        try {
            Files.writeString(CONFIG_PATH, GSON.toJson(json));
        } catch (IOException e) {
            CoiLog.LOG.warn("Failed to write ability bindings", e);
        }
    }

    public static String[] loadBindings() {
        return load(AbilityBindings.MAX_ABILITIES, AbilityConfig::abilityKey, "key-slot");
    }

    public static String[] loadWheelBindings() {
        return load(AbilityBindings.MAX_WHEEL_SIZE, AbilityConfig::wheelKey, "wheel");
    }

    public static String[] loadGestureBindings() {
        return load(GestureType.values().length, AbilityConfig::gestureKey, "gesture");
    }

    /**
     * A missing file, an unreadable one and a missing key all mean "unbound", so the array
     * always comes back {@code size} long rather than short.
     */
    private static String[] load(int size, IntFunction<String> keyFor, String what) {
        String[] bound = new String[size];
        if (!Files.exists(CONFIG_PATH)) return bound;
        try {
            JsonObject json = GSON.fromJson(Files.readString(CONFIG_PATH), JsonObject.class);
            for (int i = 0; i < size; i++) {
                String key = keyFor.apply(i);
                bound[i] = json.has(key) && !json.get(key).isJsonNull() ? json.get(key).getAsString() : null;
            }
        } catch (IOException e) {
            CoiLog.LOG.warn("Failed to read {} bindings", what, e);
        }
        return bound;
    }

    private static String abilityKey(int slot) {
        return "ability" + (slot + 1);
    }

    private static String wheelKey(int slot) {
        return "wheel" + (slot + 1);
    }

    /** Keyed by name, not index: inserting a shape would otherwise re-point every later binding. */
    private static String gestureKey(int slot) {
        return "gesture_" + GestureType.values()[slot].id();
    }
}
