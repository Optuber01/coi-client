package dev.ua.ikeepcalm.coi.domain.beyonder.model;

import dev.ua.ikeepcalm.coi.config.ClientStateStore;
import dev.ua.ikeepcalm.coi.domain.ability.model.Pathways;

/**
 * The local player's Beyonder condition, fed by {@code coi-client:conditions}.
 * <p>
 * Everything here is "unknown until a server says so": the pathway is empty, the sequence is -1
 * and both {@link #hasSpiritualityData()} and {@link #hasHealthData()} are false, which keeps
 * the spirituality and HP bars off a vanilla or pre-protocol-2 server instead of showing 0/0.
 */
public class BeyonderState {

    private static final long FLASH_MS = 500;

    private static double madness = 0.0;
    private static double permanentMadness = 0.0;
    private static int freezeStacks = 0;
    private static int mentalPressure = 0;
    private static double tiredness = 0.0;
    private static long lastMadnessIncreaseTime = 0;

    private static final SpiritualityTracker SPIRITUALITY = new SpiritualityTracker();

    // Only the *maximum* travels: the pool is a proportional mirror of vanilla health and the
    // server recomputes it from vanilla health after every hit, so the HUD derives the current
    // value itself rather than trusting a pushed number.
    private static double maxHealth = 0.0;
    private static boolean hasHealthData = false;

    // Lowercased, eternalaeon folded onto aeon; empty/-1 until a protocol-2 server says
    private static String pathway = "";
    private static int sequence = -1;

    public static double getMadness() {
        return madness;
    }

    public static double getPermanentMadness() {
        return permanentMadness;
    }

    public static int getFreezeStacks() {
        return freezeStacks;
    }

    public static int getMentalPressure() {
        return mentalPressure;
    }

    public static double getTiredness() {
        return tiredness;
    }

    /**
     * 1 → 0 over {@value #FLASH_MS} ms after madness goes up.
     */
    public static float getFlashIntensity() {
        long elapsed = System.currentTimeMillis() - lastMadnessIncreaseTime;
        if (elapsed >= FLASH_MS) return 0.0f;
        return 1.0f - (elapsed / (float) FLASH_MS);
    }

    public static String getPathway() {
        return pathway;
    }

    public static int getSequence() {
        return sequence;
    }

    public static boolean hasIdentity() {
        return !pathway.isEmpty();
    }

    public static void updateIdentity(String pathwayName, int sequenceLevel) {
        pathway = Pathways.normalizePathway(pathwayName);
        sequence = sequenceLevel;
    }

    /** In pool units. Meaningless while {@link #hasHealthData()} is false. */
    public static double maxHealth() {
        return maxHealth;
    }

    public static boolean hasHealthData() {
        return hasHealthData;
    }

    /** A non-positive maximum is "no pool here", not a pool of zero. */
    public static void updateMaxHealth(double value) {
        maxHealth = Math.max(0.0, value);
        hasHealthData = value > 0;
    }

    public static int getSpirituality() {
        return SPIRITUALITY.current();
    }

    public static int getMaxSpirituality() {
        return SPIRITUALITY.max();
    }

    public static boolean isSpiritualityRegen() {
        return SPIRITUALITY.regenerating();
    }

    public static boolean hasSpiritualityData() {
        return SPIRITUALITY.hasData();
    }

    public static float getSpiritualityFlashIntensity() {
        return SPIRITUALITY.flashIntensity();
    }

    /**
     * Milliseconds since spirituality last went <em>up</em>, or a large number if it never has.
     * Tied to gains, not to every {@code conditions} packet, so the HUD's flare is a pulse.
     */
    public static long spiritualityGainAgeMs() {
        return SPIRITUALITY.gainAgeMs();
    }

    /** @see SpiritualityTracker#predicted() */
    public static double predictedSpirituality() {
        return SPIRITUALITY.predicted();
    }

    public static void updateSpirituality(int current, int max, boolean regen) {
        SPIRITUALITY.update(current, max, regen);
    }

    public static void updateConditions(double madnessVal, double permMadnessVal, int freezeVal,
                                        int pressureVal, double tirednessVal) {
        if (madnessVal > madness) {
            lastMadnessIncreaseTime = System.currentTimeMillis();
        }
        madness = madnessVal;
        permanentMadness = permMadnessVal;
        ClientStateStore.setPermanentMadness(permMadnessVal);
        freezeStacks = freezeVal;
        mentalPressure = pressureVal;
        tiredness = tirednessVal;
    }

    public static void parseAndUpdate(String data) {
        ConditionsParser.apply(data);
    }

    public static void reset() {
        madness = 0.0;
        permanentMadness = 0.0;
        freezeStacks = 0;
        mentalPressure = 0;
        tiredness = 0.0;
        lastMadnessIncreaseTime = 0;
        SPIRITUALITY.reset();
        pathway = "";
        sequence = -1;
        maxHealth = 0.0;
        hasHealthData = false;
    }
}
