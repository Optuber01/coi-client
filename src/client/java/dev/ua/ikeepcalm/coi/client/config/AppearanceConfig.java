package dev.ua.ikeepcalm.coi.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Local controls for the appearance system, persisted to {@code config/coi_appearance.json}:
 * visibility switches, and per-element fit knobs (chest shape, hair length/offset, eye
 * size/spacing/height, wing scale) so cosmetics can be tuned to the player's own skin.
 */
public final class AppearanceConfig {

    private static final int CONFIG_VERSION = 4;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir()
            .resolve("coi_appearance.json");
    private static Settings settings = new Settings();

    private AppearanceConfig() {
    }

    public static void load() {
        if (!Files.exists(CONFIG_PATH)) {
            save();
            return;
        }
        try {
            Settings loaded = GSON.fromJson(Files.readString(CONFIG_PATH), Settings.class);
            settings = loaded == null ? new Settings() : loaded;
            sanitize();
        } catch (Exception exception) {
            System.err.println("COI Client: Failed to read appearance settings; using defaults");
            exception.printStackTrace();
            settings = new Settings();
        }
    }

    public static void save() {
        sanitize();
        try {
            Files.writeString(CONFIG_PATH, GSON.toJson(settings));
        } catch (IOException exception) {
            System.err.println("COI Client: Failed to save appearance settings");
            exception.printStackTrace();
        }
    }

    public static Settings get() {
        return settings;
    }

    public static void reset() {
        settings = new Settings();
        save();
    }

    /**
     * Master visibility gate for trait rendering on a player: the global switch plus
     * the self/other split (the local player sees their own traits only when the camera
     * is not first-person — handled by the uniqueness manager for particles and by the
     * player being effectively unseen for their own body in first person).
     */
    public static boolean shouldRender(String playerUuid) {
        if (!settings.enabled || playerUuid == null) return false;
        Minecraft minecraft = Minecraft.getInstance();
        boolean self = minecraft.player != null
                && playerUuid.equals(minecraft.player.getUUID().toString());
        return self ? settings.showSelf : settings.showOthers;
    }

    private static void sanitize() {
        if (settings.configVersion < CONFIG_VERSION) {
            settings.configVersion = CONFIG_VERSION;
        }
        settings.chestScale = Math.clamp(settings.chestScale, 0.80f, 1.50f);
        settings.chestSeparationPixels = Math.clamp(settings.chestSeparationPixels, -0.40f, 1.0f);
        settings.chestYOffsetPixels = Math.clamp(settings.chestYOffsetPixels, -1.5f, 1.5f);
        settings.chestFullness = Math.clamp(settings.chestFullness, 0.75f, 1.35f);
        settings.hairLength = Math.clamp(settings.hairLength, 0.50f, 1.60f);
        settings.hairYOffsetPixels = Math.clamp(settings.hairYOffsetPixels, -1.5f, 1.5f);
        settings.eyeScale = Math.clamp(settings.eyeScale, 0.60f, 1.60f);
        settings.eyeSpacing = Math.clamp(settings.eyeSpacing, 0.70f, 1.40f);
        settings.eyeYOffsetPixels = Math.clamp(settings.eyeYOffsetPixels, -1.5f, 1.5f);
        settings.wingScale = Math.clamp(settings.wingScale, 0.60f, 1.50f);
    }

    public static final class Settings {
        public int configVersion = CONFIG_VERSION;
        // Visibility
        public boolean enabled = true;
        public boolean showSelf = true;
        public boolean showOthers = true;
        public boolean showBodyChanges = true;
        public boolean projectJacket = true;
        // Chest shape
        public float chestScale = 1.0f;
        public float chestSeparationPixels = 0.0f;
        public float chestYOffsetPixels = 0.0f;
        public float chestFullness = 1.0f;
        // Hair fit
        public float hairLength = 1.0f;
        public float hairYOffsetPixels = 0.0f;
        // Eye fit
        public float eyeScale = 1.0f;
        public float eyeSpacing = 1.0f;
        public float eyeYOffsetPixels = 0.0f;
        // Wings
        public float wingScale = 1.0f;
        // Uniqueness particles
        public boolean enableUniquenessEffects = true;
        public boolean uniquenessShowSelf = true;
        public boolean uniquenessShowOthers = true;
    }
}
