package dev.ua.ikeepcalm.coi.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import dev.ua.ikeepcalm.coi.domain.ability.service.AbilityBindings;
import dev.ua.ikeepcalm.coi.hud.overlay.BeyonderHealthOverlay;
import dev.ua.ikeepcalm.coi.hud.overlay.CharacterPlateOverlay;
import dev.ua.ikeepcalm.coi.hud.render.HealthStyle;
import dev.ua.ikeepcalm.coi.util.CoiLog;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Every HUD preference, and the {@code config/coi_hud.json} behind it. <b>The
 * {@link HudSettings} field names are the on-disk keys</b>, so renaming one silently resets
 * that setting for every existing player.
 */
public class HudConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("coi_hud.json");

    /**
     * Schema version of the positional settings; each step's reason is in {@link HudConfigMigrations}.
     */
    public static final int LAYOUT_VERSION = 5;

    public static final int DEFAULT_MADNESS_Y = 20;

    /** Range of the per-element scale settings; see {@link dev.ua.ikeepcalm.coi.hud.HudScale}. */
    public static final float MIN_ELEMENT_SCALE = 0.5f;
    public static final float MAX_ELEMENT_SCALE = 2.0f;

    /**
     * The floor is 0.15 rather than 0 because the font renderer stops honouring the alpha
     * channel somewhere below {@code 4/255}: a slider reaching zero would fade an element's
     * chrome away while its numbers stayed solid. An element to be gone has a checkbox.
     */
    public static final float MIN_ELEMENT_OPACITY = 0.15f;
    public static final float MAX_ELEMENT_OPACITY = 1.0f;

    /** What every hand-tuned constant in {@code AbilitySlotWidget} was drawn against. */
    public static final int BASE_SLOT_SIZE = 40;
    public static final int DEFAULT_SLOT_SIZE = BASE_SLOT_SIZE;

    /**
     * Exactly {@link #MIN_ELEMENT_SCALE}..{@link #MAX_ELEMENT_SCALE} of {@link #BASE_SLOT_SIZE},
     * so the derived pose scale is never clamped away from what the slider promised.
     */
    public static final int MIN_SLOT_SIZE = Math.round(BASE_SLOT_SIZE * MIN_ELEMENT_SCALE);
    public static final int MAX_SLOT_SIZE = Math.round(BASE_SLOT_SIZE * MAX_ELEMENT_SCALE);

    private static HudSettings settings = new HudSettings();

    /**
     * <b>Known gap:</b> this still NPEs on a truncated or empty {@code coi_hud.json}.
     * {@code fromJson} returns null for a blank document, {@link HudConfigIo#read} calls
     * {@code json.has(...)} straight away, and only {@code IOException} is caught. Left alone
     * deliberately: the fix silently resets a corrupt config, which wants its own decision.
     */
    public static void load() {
        if (!Files.exists(CONFIG_PATH)) {
            save();
            return;
        }
        try {
            JsonObject json = GSON.fromJson(Files.readString(CONFIG_PATH), JsonObject.class);
            HudConfigIo.read(json, settings);
            HudConfigMigrations.migrate(settings);
        } catch (IOException e) {
            CoiLog.LOG.warn("Failed to read HUD settings", e);
        }
    }

    public static void save() {
        try {
            Files.writeString(CONFIG_PATH, GSON.toJson(HudConfigIo.write(settings)));
        } catch (IOException e) {
            CoiLog.LOG.warn("Failed to write HUD settings", e);
        }
    }

    public static void copySettings(HudSettings from, HudSettings to) {
        HudConfigIo.copy(from, to);
    }

    public static void clearSlotPlacements(HudSettings s) {
        s.slotPlacements = new SlotPlacement[AbilityBindings.MAX_ABILITIES];
    }

    public static HudSettings getSettings() {
        return settings;
    }

    public static void setSettings(HudSettings newSettings) {
        settings = newSettings;
        save();
    }

    public static void resetToDefaults() {
        settings = new HudSettings();
        save();
    }

    public record SlotPlacement(String anchor, int x, int y) {
    }

    public static class HudSettings {
        public boolean enabled = true;
        public int hudX = 10;
        public int hudYOffset = 60;
        /** The ability HUD's <b>only</b> size knob; the row's gap and every chip follow it. */
        public int slotSize = DEFAULT_SLOT_SIZE;
        /** Off hides the boxes and nothing else: the keys keep casting. */
        public boolean showAbilityHud = true;
        public boolean showKeybinds = true;
        public boolean showAbilityNames = true;
        public boolean showGlowEffect = true;
        public int wheelSlots = 8;
        public int activeAbilitySlots = 6;
        /** {@code null} leaves the slot in the shared row. */
        public SlotPlacement[] slotPlacements = new SlotPlacement[AbilityBindings.MAX_ABILITIES];
        public boolean epilepsyMode = false;
        /**
         * Three of the four {@link HealthStyle}s <em>replace</em> the vanilla hearts, so
         * switching this off is what hands them back — see {@link BeyonderHealthOverlay}.
         */
        public boolean showBeyonderHealth = true;
        public String beyonderHealthStyle = HealthStyle.DEFAULT.name();
        public String beyonderHealthAnchor = BeyonderHealthOverlay.DEFAULT_ANCHOR;
        public int beyonderHealthXOffset = BeyonderHealthOverlay.DEFAULT_X_OFFSET;
        public int beyonderHealthYOffset = HealthStyle.DEFAULT.defaultYOffset();
        public float beyonderHealthScale = 1.0f;
        /** Supersedes the madness, acting and resource bars while it is on; not spirituality. */
        public boolean showCharacterPlate = true;
        public String characterPlateAnchor = "TOP_LEFT";
        public int characterPlateXOffset = 0;
        public int characterPlateYOffset = CharacterPlateOverlay.DEFAULT_TOP_Y;
        public float characterPlateScale = 1.0f;
        /** 1.0 is the old behaviour exactly, which is why this needed no migration. */
        public float characterPlateOpacity = 1.0f;
        public boolean showMadnessBar = true;
        public int madnessXOffset = 0;
        public int madnessYOffset = DEFAULT_MADNESS_Y;
        public String madnessAnchor = "TOP_LEFT";
        public float madnessScale = 1.0f;
        public boolean showSpiritualityBar = true;
        public String spiritualityAnchor = "TOP_LEFT";
        public int spiritualityXOffset = 0;
        public int spiritualityYOffset = 50;
        public boolean spiritualityHideWhenFull = true;
        public float spiritualityScale = 1.0f;
        public boolean showActingBar = true;
        public String actingAnchor = "TOP_LEFT";
        public int actingXOffset = 0;
        public int actingYOffset = 80;
        public float actingScale = 1.0f;
        public boolean showResourceBars = true;
        public String resourceAnchor = "TOP_LEFT";
        public int resourceXOffset = 0;
        public int resourceYOffset = 100;
        public int resourceMaxBars = 4;
        public float resourceScale = 1.0f;
        public boolean showActionBar = true;
        public int actionBarXOffset = 0;
        public int actionBarYOffset = 72;
        public int actionBarLines = 0;
        public float actionBarScale = 1.0f;
        public boolean showTargetHealth = true;
        public int targetHealthXOffset = 0;
        public int targetHealthYOffset = 18;
        public float targetHealthScale = 1.0f;
        public boolean showCogitationOverlay = true;
        public int cogitationXOffset = 0;
        public int cogitationYOffset = -70;
        public float cogitationScale = 1.0f;
        public boolean showNotifications = true;
        public int notificationXOffset = 12;
        public int notificationYOffset = 12;
        public float notificationScale = 1.0f;
        public float effectSoundVolume = 1.0f;
        public boolean enableHallucinations = true;
        /**
         * Also suppressed by {@code epilepsyMode}; the rite still plays, the view holds still.
         */
        public boolean enableCameraShake = true;
        public boolean enableCinematicCamera = true;
        /**
         * 0..1. Fog is the one part of a sky tint with a gameplay cost, so it is what scales.
         */
        public float ceremonyFogDensity = 1.0f;
        public boolean enableDiscordPresence = true;
        public boolean presenceShowMadness = true;
        /** Opt out of the mod's own menus and ask the server for its InvUI chest GUIs instead. */
        public boolean useServerMenus = false;
        /** Off falls the whole takeover through to vanilla, leaving only the title haunting. */
        public boolean coiTitleScreen = true;
        public int layoutVersion = LAYOUT_VERSION;
    }
}
