package dev.ua.ikeepcalm.coi.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import dev.ua.ikeepcalm.coi.util.CoiLog;
import dev.ua.ikeepcalm.coi.util.JsonRead;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Persistent state, not preference, in {@code coi_client_state.json}: it deliberately survives
 * a reset of {@code coi_hud.json}.
 */
public class ClientStateStore {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path STATE_PATH = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("coi_client_state.json");

    private static double lastPermanentMadness = 0.0;
    private static double lastMadness = 0.0;
    private static boolean tourCompleted = false;
    private static boolean inventoryHintDismissed = false;
    private static String lastPathway = "";

    public static void load() {
        if (!Files.exists(STATE_PATH)) return;
        try {
            JsonObject json = GSON.fromJson(Files.readString(STATE_PATH), JsonObject.class);
            if (json == null) return;
            lastPermanentMadness = JsonRead.dbl(json, "lastPermanentMadness", lastPermanentMadness);
            lastMadness = JsonRead.dbl(json, "lastMadness", lastMadness);
            tourCompleted = JsonRead.bool(json, "tourCompleted");
            inventoryHintDismissed = JsonRead.bool(json, "inventoryHintDismissed");
            lastPathway = JsonRead.string(json, "lastPathway", lastPathway);
        } catch (Exception e) {
            CoiLog.LOG.warn("Failed to read client state", e);
        }
    }

    public static void setPermanentMadness(double value) {
        if (Math.abs(value - lastPermanentMadness) < 0.001) return;
        lastPermanentMadness = value;
        save();
    }

    /**
     * Persisted once at disconnect rather than on every update: madness changes constantly.
     */
    public static void setLastMadness(double value) {
        if (Math.abs(value - lastMadness) < 0.001) return;
        lastMadness = value;
        save();
    }

    public static double getLastPermanentMadness() {
        return lastPermanentMadness;
    }

    public static double getLastMadness() {
        return lastMadness;
    }

    /** How corrupted the player was when last seen — drives the title screen haunting. */
    public static double getCorruption() {
        return Math.max(lastMadness, lastPermanentMadness);
    }

    public static boolean isTourNotCompleted() {
        return !tourCompleted;
    }

    public static void setTourCompleted(boolean value) {
        if (tourCompleted == value) return;
        tourCompleted = value;
        save();
    }

    /** Here rather than in coi_hud.json, where a reset would bring the hint back. */
    public static boolean isInventoryHintDismissed() {
        return inventoryHintDismissed;
    }

    public static void setInventoryHintDismissed(boolean value) {
        if (inventoryHintDismissed == value) return;
        inventoryHintDismissed = value;
        save();
    }

    /** Lets the title wheel light the player's own emblem before any server has spoken. */
    public static String getLastPathway() {
        return lastPathway;
    }

    public static void setLastPathway(String value) {
        String next = value == null ? "" : value;
        if (lastPathway.equals(next)) return;
        lastPathway = next;
        save();
    }

    /** Every setter calls this only when its value changed, so an idle session never writes. */
    private static void save() {
        JsonObject json = new JsonObject();
        json.addProperty("lastPermanentMadness", lastPermanentMadness);
        json.addProperty("lastMadness", lastMadness);
        json.addProperty("tourCompleted", tourCompleted);
        json.addProperty("inventoryHintDismissed", inventoryHintDismissed);
        json.addProperty("lastPathway", lastPathway);
        try {
            Files.writeString(STATE_PATH, GSON.toJson(json));
        } catch (IOException e) {
            CoiLog.LOG.warn("Failed to write client state", e);
        }
    }
}
