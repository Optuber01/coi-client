package dev.ua.ikeepcalm.coi.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Persists appearance preferences → {@code config/coi_appearance.json}: master toggles
 * for the trait layer and the uniqueness particle effects (self vs. other players).
 */
public class AppearanceConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("coi_appearance.json");

    private static AppearanceSettings settings = new AppearanceSettings();

    public static void load() {
        if (Files.exists(CONFIG_PATH)) {
            try {
                String content = Files.readString(CONFIG_PATH);
                JsonObject json = GSON.fromJson(content, JsonObject.class);

                settings.enableAppearanceTraits = !json.has("enableAppearanceTraits")
                        || json.get("enableAppearanceTraits").getAsBoolean();
                settings.enableUniquenessEffects = !json.has("enableUniquenessEffects")
                        || json.get("enableUniquenessEffects").getAsBoolean();
                settings.uniquenessShowSelf = !json.has("uniquenessShowSelf")
                        || json.get("uniquenessShowSelf").getAsBoolean();
                settings.uniquenessShowOthers = !json.has("uniquenessShowOthers")
                        || json.get("uniquenessShowOthers").getAsBoolean();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            save();
        }
    }

    public static void save() {
        JsonObject json = new JsonObject();
        json.addProperty("enableAppearanceTraits", settings.enableAppearanceTraits);
        json.addProperty("enableUniquenessEffects", settings.enableUniquenessEffects);
        json.addProperty("uniquenessShowSelf", settings.uniquenessShowSelf);
        json.addProperty("uniquenessShowOthers", settings.uniquenessShowOthers);

        try {
            Files.writeString(CONFIG_PATH, GSON.toJson(json));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static AppearanceSettings getSettings() {
        return settings;
    }

    public static void resetToDefaults() {
        settings = new AppearanceSettings();
        save();
    }

    public static class AppearanceSettings {
        /** Master switch for the whole cosmetic trait layer. */
        public boolean enableAppearanceTraits = true;
        /** Master switch for the uniqueness particle effects. */
        public boolean enableUniquenessEffects = true;
        /** Uniqueness effects on the local player (visible in third person). */
        public boolean uniquenessShowSelf = true;
        /** Uniqueness effects on other players. */
        public boolean uniquenessShowOthers = true;
    }
}
