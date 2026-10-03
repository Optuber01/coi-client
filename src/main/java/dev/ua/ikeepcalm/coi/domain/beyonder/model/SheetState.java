package dev.ua.ikeepcalm.coi.domain.beyonder.model;

import dev.ua.ikeepcalm.coi.domain.ability.model.Pathways;

import java.util.List;

/**
 * The character sheet snapshot from {@code coi-client:sheet}.
 * <p>
 * Every field is optional on the wire: an older server may omit whole sections, so each getter
 * has a sane default and {@link #acting()} is simply {@code null} for outer pathways. The whole
 * sheet is held as one immutable {@link Snapshot}, which is what makes a push atomic — the
 * screen never sees half of one document and half of the next.
 */
public class SheetState {

    /** Opaque ARGB: the server's colour when it sent one, else the mod's own pathway table. */
    public static int pathwayArgb() {
        Identity identity = snapshot.identity();
        int rgb = identity.pathwayColor() != 0 ? identity.pathwayColor() : Pathways.pathwayRgb(identity.pathway());
        return 0xFF000000 | rgb;
    }

    public record Overflow(boolean eligible, int banked, int ceiling, boolean uncapped, int foreignThrottlePercent) {
    }

    /** Seconds, counted down from the last push so it reads as a timer between 60-tick pushes. */
    public static int actingCooldownNow() {
        Acting acting = snapshot.acting();
        if (acting == null || acting.cooldownRemaining() <= 0) return 0;
        long elapsed = (System.currentTimeMillis() - snapshot.receivedAt()) / 1000L;
        return (int) Math.max(0, acting.cooldownRemaining() - elapsed);
    }

    /**
     * One acting ledger row; {@code unlimited} means {@code cap} does not apply.
     */
    public record Source(String source, String label, int contributed, int cap, boolean unlimited) {
        public boolean capped() {
            return !unlimited && cap > 0 && contributed >= cap;
        }
    }

    public record Pressure(boolean present, int stacks, int cap) {
        public static final Pressure ABSENT = new Pressure(false, 0, 0);
    }

    /** Absent for outer pathways. */
    public record Acting(int acting, int needed, double percent, boolean limited,
                         int cooldownRemaining, int cooldownTotal,
                         List<Source> sources, Overflow overflow) {
    }

    /** A present-or-not 0..1 meter, e.g. the Line of Life and Death. */
    public record Gauge(boolean present, double value) {
        public static final Gauge ABSENT = new Gauge(false, 0);
    }

    public record Vitals(double health, double maxHealth, int spirituality, int maxSpirituality) {
        public static final Vitals EMPTY = new Vitals(0, 0, 0, 0);
    }

    /** {@code terrainDamage} is the odd one out: the current toggle state, not a gate. */
    public record Actions(boolean church, boolean abilities, boolean mythical, boolean uniqueness,
                          boolean honorific, boolean map, boolean seat, boolean throne, boolean pantheon,
                          boolean terrainDamage) {
        public static final Actions NONE = new Actions(false, false, false, false, false, false, false, false,
                false, false);

        /** An unknown target is treated as open — it is the server's to refuse, not ours. */
        public boolean unlocked(String target) {
            return switch (target) {
                case "church" -> church;
                case "abilities" -> abilities;
                case "mythical" -> mythical;
                case "uniqueness" -> uniqueness;
                case "honorific" -> honorific;
                case "map" -> map;
                case "seat" -> seat;
                case "throne" -> throne;
                case "pantheon" -> pantheon;
                default -> true;
            };
        }
    }

    /** {@code pathwayColor} is the server's own accent, or 0 when it named none. */
    public record Identity(String pathway, String pathwayName, int pathwayColor,
                           int sequence, String sequenceName, boolean outer) {
        public static final Identity UNKNOWN = new Identity("", "", 0, -1, "", false);
    }

    private static Snapshot snapshot = Snapshot.EMPTY;

    private SheetState() {
    }

    public static void handle(String json) {
        Snapshot parsed = SheetParser.parse(json);
        if (parsed != null) snapshot = parsed;
    }

    public static void reset() {
        snapshot = Snapshot.EMPTY;
    }

    public static void debugInject() {
        handle(SheetParser.SAMPLE);
    }

    public static String pathway() {
        return snapshot.identity().pathway();
    }

    public static String pathwayName() {
        Identity identity = snapshot.identity();
        return identity.pathwayName().isEmpty() ? identity.pathway() : identity.pathwayName();
    }

    /** {@code permanentFloor} and {@code godhoodFloor} are the madness that cannot be worked off. */
    public record Mind(double madness, double permanentFloor, double godhoodFloor, int madnessStage,
                       double tiredness, int tirednessStage) {
        public static final Mind EMPTY = new Mind(0, 0, 0, 0, 0, 0);
    }

    public static int sequence() {
        return snapshot.identity().sequence();
    }

    public static String sequenceName() {
        return snapshot.identity().sequenceName();
    }

    public static boolean outer() {
        return snapshot.identity().outer();
    }

    public static double health() {
        return snapshot.vitals().health();
    }

    public static double maxHealth() {
        return snapshot.vitals().maxHealth();
    }

    public static int spirituality() {
        return snapshot.vitals().spirituality();
    }

    public static int maxSpirituality() {
        return snapshot.vitals().maxSpirituality();
    }

    public static double madness() {
        return snapshot.mind().madness();
    }

    public static double permanentFloor() {
        return snapshot.mind().permanentFloor();
    }

    public static double godhoodFloor() {
        return snapshot.mind().godhoodFloor();
    }

    public static int madnessStage() {
        return snapshot.mind().madnessStage();
    }

    public static double tiredness() {
        return snapshot.mind().tiredness();
    }

    public static int tirednessStage() {
        return snapshot.mind().tirednessStage();
    }

    public static Acting acting() {
        return snapshot.acting();
    }

    public static Gauge lifeAndDeath() {
        return snapshot.lifeAndDeath();
    }

    public static Pressure pressure() {
        return snapshot.pressure();
    }

    public static boolean anomaly() {
        return snapshot.anomaly();
    }

    public static Actions actions() {
        return snapshot.actions();
    }

    public static boolean hasData() {
        return snapshot.hasData();
    }

    public static long receivedAt() {
        return snapshot.receivedAt();
    }

    /** {@code receivedAt} is what the acting cooldown counts down from between pushes. */
    public record Snapshot(Identity identity, Vitals vitals, Mind mind, Acting acting,
                           Gauge lifeAndDeath, Pressure pressure, boolean anomaly, Actions actions,
                           boolean hasData, long receivedAt) {
        public static final Snapshot EMPTY = new Snapshot(Identity.UNKNOWN, Vitals.EMPTY, Mind.EMPTY,
                null, Gauge.ABSENT, Pressure.ABSENT, false, Actions.NONE, false, 0);
    }

    public static String actingCooldownClock() {
        int seconds = actingCooldownNow();
        return String.format("%d:%02d", seconds / 60, seconds % 60);
    }
}
