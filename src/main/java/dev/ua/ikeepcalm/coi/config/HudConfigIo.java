package dev.ua.ikeepcalm.coi.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import dev.ua.ikeepcalm.coi.config.HudConfig.HudSettings;
import dev.ua.ikeepcalm.coi.config.HudConfig.SlotPlacement;
import dev.ua.ikeepcalm.coi.domain.ability.service.AbilityBindings;
import dev.ua.ikeepcalm.coi.hud.overlay.BeyonderHealthOverlay;
import dev.ua.ikeepcalm.coi.hud.overlay.CharacterPlateOverlay;
import dev.ua.ikeepcalm.coi.hud.render.HealthStyle;

/**
 * {@link #read} from {@code config/coi_hud.json}, {@link #write} back to it, {@link #copy} into
 * a settings screen's working copy — the same list of fields three ways, together so they can
 * be checked against each other. The shared failure mode is <em>omission</em>: a key the writer
 * forgets silently resets on the next launch, one the copy forgets makes Cancel a no-op.
 * <p>
 * <b>The key names are the on-disk contract</b> and are spelled out literally rather than
 * derived from the field names. Reading does not migrate; {@link HudConfigMigrations} runs
 * afterwards, on the settings this produced.
 */
final class HudConfigIo {

    private HudConfigIo() {
    }


    /**
     * Any key the file does not name is left at whatever the fresh {@link HudSettings} had.
     */
    static void read(JsonObject json, HudSettings s) {
        readAbilityHud(json, s);
        readBeyonderHealth(json, s);
        readCharacterPlate(json, s);
        readBars(json, s);
        readOverlays(json, s);
        readGeneral(json, s);
        readSlotPlacements(json, s);
    }

    private static void readAbilityHud(JsonObject json, HudSettings s) {
        s.enabled = flag(json, "enabled", true);
        s.hudX = intOf(json, "hudX", 10);
        s.hudYOffset = intOf(json, "hudYOffset", 60);
        s.slotSize = Math.clamp(intOf(json, "slotSize", HudConfig.DEFAULT_SLOT_SIZE),
                HudConfig.MIN_SLOT_SIZE, HudConfig.MAX_SLOT_SIZE);
        s.showAbilityHud = flag(json, "showAbilityHud", true);
        s.showKeybinds = flag(json, "showKeybinds", true);
        s.showAbilityNames = flag(json, "showAbilityNames", true);
        s.showGlowEffect = flag(json, "showGlowEffect", true);
        s.wheelSlots = intOf(json, "wheelSlots", 8);
        s.activeAbilitySlots = Math.clamp(intOf(json, "activeAbilitySlots", 6), 1, AbilityBindings.MAX_ABILITIES);
    }

    /**
     * The style is read <em>first</em>: it is what the vertical placement defaults to.
     */
    private static void readBeyonderHealth(JsonObject json, HudSettings s) {
        s.showBeyonderHealth = flag(json, "showBeyonderHealth", true);
        s.beyonderHealthStyle = string(json, "beyonderHealthStyle", HealthStyle.DEFAULT.name());
        HealthStyle style = HealthStyle.parse(s.beyonderHealthStyle);
        s.beyonderHealthAnchor = string(json, "beyonderHealthAnchor", BeyonderHealthOverlay.DEFAULT_ANCHOR);
        s.beyonderHealthXOffset = intOf(json, "beyonderHealthXOffset", BeyonderHealthOverlay.DEFAULT_X_OFFSET);
        s.beyonderHealthYOffset = intOf(json, "beyonderHealthYOffset", style.defaultYOffset());
        s.beyonderHealthScale = scale(json, "beyonderHealthScale");
    }

    private static void readCharacterPlate(JsonObject json, HudSettings s) {
        s.showCharacterPlate = flag(json, "showCharacterPlate", true);
        s.characterPlateAnchor = string(json, "characterPlateAnchor", "TOP_LEFT");
        s.characterPlateXOffset = intOf(json, "characterPlateXOffset", 0);
        s.characterPlateYOffset = intOf(json, "characterPlateYOffset", CharacterPlateOverlay.DEFAULT_TOP_Y);
        s.characterPlateScale = scale(json, "characterPlateScale");
        s.characterPlateOpacity = opacity(json, "characterPlateOpacity");
    }

    private static void readBars(JsonObject json, HudSettings s) {
        s.showMadnessBar = flag(json, "showMadnessBar", true);
        s.madnessXOffset = intOf(json, "madnessXOffset", 0);
        s.madnessYOffset = intOf(json, "madnessYOffset", HudConfig.DEFAULT_MADNESS_Y);
        s.madnessAnchor = string(json, "madnessAnchor", "TOP_LEFT");
        s.madnessScale = scale(json, "madnessScale");
        s.layoutVersion = intOf(json, "layoutVersion", 0);
        s.showSpiritualityBar = flag(json, "showSpiritualityBar", true);
        s.spiritualityAnchor = string(json, "spiritualityAnchor", "TOP_LEFT");
        s.spiritualityXOffset = intOf(json, "spiritualityXOffset", 0);
        s.spiritualityYOffset = intOf(json, "spiritualityYOffset", 50);
        s.spiritualityHideWhenFull = flag(json, "spiritualityHideWhenFull", true);
        s.spiritualityScale = scale(json, "spiritualityScale");
        s.showActingBar = flag(json, "showActingBar", true);
        s.actingAnchor = string(json, "actingAnchor", "TOP_LEFT");
        s.actingXOffset = intOf(json, "actingXOffset", 0);
        s.actingYOffset = intOf(json, "actingYOffset", 80);
        s.actingScale = scale(json, "actingScale");
        s.showResourceBars = flag(json, "showResourceBars", true);
        s.resourceAnchor = string(json, "resourceAnchor", "TOP_LEFT");
        s.resourceXOffset = intOf(json, "resourceXOffset", 0);
        s.resourceYOffset = intOf(json, "resourceYOffset", 100);
        s.resourceMaxBars = intOf(json, "resourceMaxBars", 4);
        s.resourceScale = scale(json, "resourceScale");
    }

    private static void readOverlays(JsonObject json, HudSettings s) {
        s.showActionBar = flag(json, "showActionBar", true);
        s.actionBarXOffset = intOf(json, "actionBarXOffset", 0);
        s.actionBarYOffset = intOf(json, "actionBarYOffset", 72);
        s.actionBarLines = intOf(json, "actionBarLines", 0);
        s.actionBarScale = scale(json, "actionBarScale");
        s.showTargetHealth = flag(json, "showTargetHealth", true);
        s.targetHealthXOffset = intOf(json, "targetHealthXOffset", 0);
        s.targetHealthYOffset = intOf(json, "targetHealthYOffset", 18);
        s.targetHealthScale = scale(json, "targetHealthScale");
        s.showCogitationOverlay = flag(json, "showCogitationOverlay", true);
        s.cogitationXOffset = intOf(json, "cogitationXOffset", 0);
        s.cogitationYOffset = intOf(json, "cogitationYOffset", -70);
        s.cogitationScale = scale(json, "cogitationScale");
        s.showNotifications = flag(json, "showNotifications", true);
        s.notificationXOffset = intOf(json, "notificationXOffset", 12);
        s.notificationYOffset = intOf(json, "notificationYOffset", 12);
        s.notificationScale = scale(json, "notificationScale");
    }

    private static void readGeneral(JsonObject json, HudSettings s) {
        s.epilepsyMode = flag(json, "epilepsyMode", false);
        s.effectSoundVolume = json.has("effectSoundVolume") ? json.get("effectSoundVolume").getAsFloat() : 1.0f;
        s.enableHallucinations = flag(json, "enableHallucinations", true);
        s.enableCameraShake = flag(json, "enableCameraShake", true);
        s.enableCinematicCamera = flag(json, "enableCinematicCamera", true);
        s.ceremonyFogDensity = json.has("ceremonyFogDensity")
                ? Math.clamp(json.get("ceremonyFogDensity").getAsFloat(), 0f, 1f) : 1.0f;
        s.enableDiscordPresence = flag(json, "enableDiscordPresence", true);
        s.presenceShowMadness = flag(json, "presenceShowMadness", true);
        s.useServerMenus = flag(json, "useServerMenus", false);
        s.coiTitleScreen = flag(json, "coiTitleScreen", true);
    }

    /**
     * A missing, short or malformed entry means "leave that slot in the row".
     */
    private static void readSlotPlacements(JsonObject json, HudSettings s) {
        HudConfig.clearSlotPlacements(s);
        if (!json.has("slotPlacements") || !json.get("slotPlacements").isJsonArray()) return;
        JsonArray array = json.getAsJsonArray("slotPlacements");
        int n = Math.min(array.size(), s.slotPlacements.length);
        for (int i = 0; i < n; i++) {
            s.slotPlacements[i] = placement(array.get(i));
        }
    }

    private static SlotPlacement placement(JsonElement element) {
        if (element == null || !element.isJsonObject()) return null;
        JsonObject entry = element.getAsJsonObject();
        if (!entry.has("anchor")) return null;
        try {
            return new SlotPlacement(entry.get("anchor").getAsString(),
                    entry.has("x") ? entry.get("x").getAsInt() : 0,
                    entry.has("y") ? entry.get("y").getAsInt() : 0);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    /**
     * Clamped into the scale band; a missing or malformed entry reads as 1.0.
     */
    private static float scale(JsonObject json, String key) {
        if (!json.has(key)) return 1.0f;
        try {
            return Math.clamp(json.get(key).getAsFloat(), HudConfig.MIN_ELEMENT_SCALE, HudConfig.MAX_ELEMENT_SCALE);
        } catch (RuntimeException e) {
            return 1.0f;
        }
    }

    /**
     * Deliberately not {@link #scale}: same shape, different bounds, and folding them together
     * would let a later change to the scale band move the opacity floor that keeps text legible.
     */
    private static float opacity(JsonObject json, String key) {
        if (!json.has(key)) return 1.0f;
        try {
            return Math.clamp(json.get(key).getAsFloat(),
                    HudConfig.MIN_ELEMENT_OPACITY, HudConfig.MAX_ELEMENT_OPACITY);
        } catch (RuntimeException e) {
            return 1.0f;
        }
    }

    private static boolean flag(JsonObject json, String key, boolean fallback) {
        return json.has(key) ? json.get(key).getAsBoolean() : fallback;
    }

    private static int intOf(JsonObject json, String key, int fallback) {
        return json.has(key) ? json.get(key).getAsInt() : fallback;
    }

    private static String string(JsonObject json, String key, String fallback) {
        return json.has(key) ? json.get(key).getAsString() : fallback;
    }

    // The mirror of the reader, key for key. layoutVersion is the one field not copied out of
    // the settings: a file this build writes is current by definition.


    static JsonObject write(HudSettings s) {
        JsonObject json = new JsonObject();
        writeAbilityHud(json, s);
        writeBeyonderHealth(json, s);
        writeCharacterPlate(json, s);
        writeBars(json, s);
        writeOverlays(json, s);
        writeGeneral(json, s);
        json.add("slotPlacements", slotPlacements(s));
        return json;
    }

    private static void writeAbilityHud(JsonObject json, HudSettings s) {
        json.addProperty("enabled", s.enabled);
        json.addProperty("hudX", s.hudX);
        json.addProperty("hudYOffset", s.hudYOffset);
        json.addProperty("slotSize", s.slotSize);
        json.addProperty("showAbilityHud", s.showAbilityHud);
        json.addProperty("showKeybinds", s.showKeybinds);
        json.addProperty("showAbilityNames", s.showAbilityNames);
        json.addProperty("showGlowEffect", s.showGlowEffect);
        json.addProperty("wheelSlots", s.wheelSlots);
        json.addProperty("activeAbilitySlots", s.activeAbilitySlots);
    }

    private static void writeBeyonderHealth(JsonObject json, HudSettings s) {
        json.addProperty("showBeyonderHealth", s.showBeyonderHealth);
        json.addProperty("beyonderHealthStyle", s.beyonderHealthStyle);
        json.addProperty("beyonderHealthAnchor", s.beyonderHealthAnchor);
        json.addProperty("beyonderHealthXOffset", s.beyonderHealthXOffset);
        json.addProperty("beyonderHealthYOffset", s.beyonderHealthYOffset);
        json.addProperty("beyonderHealthScale", s.beyonderHealthScale);
    }

    private static void writeCharacterPlate(JsonObject json, HudSettings s) {
        json.addProperty("showCharacterPlate", s.showCharacterPlate);
        json.addProperty("characterPlateAnchor", s.characterPlateAnchor);
        json.addProperty("characterPlateXOffset", s.characterPlateXOffset);
        json.addProperty("characterPlateYOffset", s.characterPlateYOffset);
        json.addProperty("characterPlateScale", s.characterPlateScale);
        json.addProperty("characterPlateOpacity", s.characterPlateOpacity);
    }

    private static void writeBars(JsonObject json, HudSettings s) {
        json.addProperty("showMadnessBar", s.showMadnessBar);
        json.addProperty("madnessXOffset", s.madnessXOffset);
        json.addProperty("madnessYOffset", s.madnessYOffset);
        json.addProperty("madnessAnchor", s.madnessAnchor);
        json.addProperty("madnessScale", s.madnessScale);
        json.addProperty("layoutVersion", HudConfig.LAYOUT_VERSION);
        json.addProperty("showSpiritualityBar", s.showSpiritualityBar);
        json.addProperty("spiritualityAnchor", s.spiritualityAnchor);
        json.addProperty("spiritualityXOffset", s.spiritualityXOffset);
        json.addProperty("spiritualityYOffset", s.spiritualityYOffset);
        json.addProperty("spiritualityHideWhenFull", s.spiritualityHideWhenFull);
        json.addProperty("spiritualityScale", s.spiritualityScale);
        json.addProperty("showActingBar", s.showActingBar);
        json.addProperty("actingAnchor", s.actingAnchor);
        json.addProperty("actingXOffset", s.actingXOffset);
        json.addProperty("actingYOffset", s.actingYOffset);
        json.addProperty("actingScale", s.actingScale);
        json.addProperty("showResourceBars", s.showResourceBars);
        json.addProperty("resourceAnchor", s.resourceAnchor);
        json.addProperty("resourceXOffset", s.resourceXOffset);
        json.addProperty("resourceYOffset", s.resourceYOffset);
        json.addProperty("resourceMaxBars", s.resourceMaxBars);
        json.addProperty("resourceScale", s.resourceScale);
    }

    private static void writeOverlays(JsonObject json, HudSettings s) {
        json.addProperty("showActionBar", s.showActionBar);
        json.addProperty("actionBarXOffset", s.actionBarXOffset);
        json.addProperty("actionBarYOffset", s.actionBarYOffset);
        json.addProperty("actionBarLines", s.actionBarLines);
        json.addProperty("actionBarScale", s.actionBarScale);
        json.addProperty("showTargetHealth", s.showTargetHealth);
        json.addProperty("targetHealthXOffset", s.targetHealthXOffset);
        json.addProperty("targetHealthYOffset", s.targetHealthYOffset);
        json.addProperty("targetHealthScale", s.targetHealthScale);
        json.addProperty("showCogitationOverlay", s.showCogitationOverlay);
        json.addProperty("cogitationXOffset", s.cogitationXOffset);
        json.addProperty("cogitationYOffset", s.cogitationYOffset);
        json.addProperty("cogitationScale", s.cogitationScale);
        json.addProperty("showNotifications", s.showNotifications);
        json.addProperty("notificationXOffset", s.notificationXOffset);
        json.addProperty("notificationYOffset", s.notificationYOffset);
        json.addProperty("notificationScale", s.notificationScale);
    }

    private static void writeGeneral(JsonObject json, HudSettings s) {
        json.addProperty("epilepsyMode", s.epilepsyMode);
        json.addProperty("effectSoundVolume", s.effectSoundVolume);
        json.addProperty("enableHallucinations", s.enableHallucinations);
        json.addProperty("enableCameraShake", s.enableCameraShake);
        json.addProperty("enableCinematicCamera", s.enableCinematicCamera);
        json.addProperty("ceremonyFogDensity", s.ceremonyFogDensity);
        json.addProperty("enableDiscordPresence", s.enableDiscordPresence);
        json.addProperty("presenceShowMadness", s.presenceShowMadness);
        json.addProperty("useServerMenus", s.useServerMenus);
        json.addProperty("coiTitleScreen", s.coiTitleScreen);
    }

    /**
     * Always {@link AbilityBindings#MAX_ABILITIES} entries, with an explicit null per row slot.
     */
    private static JsonArray slotPlacements(HudSettings s) {
        JsonArray array = new JsonArray();
        for (int i = 0; i < AbilityBindings.MAX_ABILITIES; i++) {
            SlotPlacement placement = s.slotPlacements == null || i >= s.slotPlacements.length
                    ? null : s.slotPlacements[i];
            if (placement == null) {
                array.add(JsonNull.INSTANCE);
                continue;
            }
            JsonObject entry = new JsonObject();
            entry.addProperty("anchor", placement.anchor());
            entry.addProperty("x", placement.x());
            entry.addProperty("y", placement.y());
            array.add(entry);
        }
        return array;
    }

    // Explicit rather than reflective: a working copy that silently shared an array with the
    // live settings would make Cancel a no-op.


    static void copy(HudSettings from, HudSettings to) {
        copyAbilityHud(from, to);
        copyBeyonderHealth(from, to);
        copyCharacterPlate(from, to);
        copyBars(from, to);
        copyOverlays(from, to);
        copyGeneral(from, to);
        copySlotPlacements(from, to);
    }

    private static void copyAbilityHud(HudSettings from, HudSettings to) {
        to.enabled = from.enabled;
        to.hudX = from.hudX;
        to.hudYOffset = from.hudYOffset;
        to.slotSize = from.slotSize;
        to.showAbilityHud = from.showAbilityHud;
        to.showKeybinds = from.showKeybinds;
        to.showAbilityNames = from.showAbilityNames;
        to.showGlowEffect = from.showGlowEffect;
        to.wheelSlots = from.wheelSlots;
        to.activeAbilitySlots = from.activeAbilitySlots;
    }

    private static void copyBeyonderHealth(HudSettings from, HudSettings to) {
        to.showBeyonderHealth = from.showBeyonderHealth;
        to.beyonderHealthStyle = from.beyonderHealthStyle;
        to.beyonderHealthAnchor = from.beyonderHealthAnchor;
        to.beyonderHealthXOffset = from.beyonderHealthXOffset;
        to.beyonderHealthYOffset = from.beyonderHealthYOffset;
        to.beyonderHealthScale = from.beyonderHealthScale;
    }

    private static void copyCharacterPlate(HudSettings from, HudSettings to) {
        to.showCharacterPlate = from.showCharacterPlate;
        to.characterPlateAnchor = from.characterPlateAnchor;
        to.characterPlateXOffset = from.characterPlateXOffset;
        to.characterPlateYOffset = from.characterPlateYOffset;
        to.characterPlateScale = from.characterPlateScale;
        to.characterPlateOpacity = from.characterPlateOpacity;
    }

    private static void copyBars(HudSettings from, HudSettings to) {
        to.showMadnessBar = from.showMadnessBar;
        to.madnessXOffset = from.madnessXOffset;
        to.madnessYOffset = from.madnessYOffset;
        to.madnessAnchor = from.madnessAnchor;
        to.madnessScale = from.madnessScale;
        to.showSpiritualityBar = from.showSpiritualityBar;
        to.spiritualityAnchor = from.spiritualityAnchor;
        to.spiritualityXOffset = from.spiritualityXOffset;
        to.spiritualityYOffset = from.spiritualityYOffset;
        to.spiritualityHideWhenFull = from.spiritualityHideWhenFull;
        to.spiritualityScale = from.spiritualityScale;
        to.showActingBar = from.showActingBar;
        to.actingAnchor = from.actingAnchor;
        to.actingXOffset = from.actingXOffset;
        to.actingYOffset = from.actingYOffset;
        to.actingScale = from.actingScale;
        to.showResourceBars = from.showResourceBars;
        to.resourceAnchor = from.resourceAnchor;
        to.resourceXOffset = from.resourceXOffset;
        to.resourceYOffset = from.resourceYOffset;
        to.resourceMaxBars = from.resourceMaxBars;
        to.resourceScale = from.resourceScale;
    }

    private static void copyOverlays(HudSettings from, HudSettings to) {
        to.showActionBar = from.showActionBar;
        to.actionBarXOffset = from.actionBarXOffset;
        to.actionBarYOffset = from.actionBarYOffset;
        to.actionBarLines = from.actionBarLines;
        to.actionBarScale = from.actionBarScale;
        to.showTargetHealth = from.showTargetHealth;
        to.targetHealthXOffset = from.targetHealthXOffset;
        to.targetHealthYOffset = from.targetHealthYOffset;
        to.targetHealthScale = from.targetHealthScale;
        to.showCogitationOverlay = from.showCogitationOverlay;
        to.cogitationXOffset = from.cogitationXOffset;
        to.cogitationYOffset = from.cogitationYOffset;
        to.cogitationScale = from.cogitationScale;
        to.showNotifications = from.showNotifications;
        to.notificationXOffset = from.notificationXOffset;
        to.notificationYOffset = from.notificationYOffset;
        to.notificationScale = from.notificationScale;
    }

    private static void copyGeneral(HudSettings from, HudSettings to) {
        to.epilepsyMode = from.epilepsyMode;
        to.effectSoundVolume = from.effectSoundVolume;
        to.enableHallucinations = from.enableHallucinations;
        to.enableCameraShake = from.enableCameraShake;
        to.enableCinematicCamera = from.enableCinematicCamera;
        to.ceremonyFogDensity = from.ceremonyFogDensity;
        to.enableDiscordPresence = from.enableDiscordPresence;
        to.presenceShowMadness = from.presenceShowMadness;
        to.useServerMenus = from.useServerMenus;
        to.coiTitleScreen = from.coiTitleScreen;
        to.layoutVersion = from.layoutVersion;
    }

    /**
     * The records are immutable, so only the array itself has to be fresh.
     */
    private static void copySlotPlacements(HudSettings from, HudSettings to) {
        to.slotPlacements = new HudConfig.SlotPlacement[AbilityBindings.MAX_ABILITIES];
        if (from.slotPlacements == null) return;
        int n = Math.min(to.slotPlacements.length, from.slotPlacements.length);
        System.arraycopy(from.slotPlacements, 0, to.slotPlacements, 0, n);
    }
}
