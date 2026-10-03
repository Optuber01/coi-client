package dev.ua.ikeepcalm.coi.hud.render;

import java.util.Locale;

/**
 * Which shape the Beyonder HP pool takes. <b>All four occupy the same
 * {@code BAR_WIDTH x BAR_HEIGHT} box</b>, so switching style never moves the element and
 * {@code BeyonderHealthElement}'s {@code bounds}/{@code moveTo} stay exact inverses without
 * ever asking which style is on.
 */
public enum HealthStyle {

    /**
     * Vanilla keeps its hearts, absorption hearts included; the mod adds only the readout.
     */
    HEARTS(59),
    BAR(39),
    ORNATE(39),
    PIPS(39);

    public static final HealthStyle DEFAULT = HEARTS;

    private final int defaultYOffset;

    HealthStyle(int defaultYOffset) {
        this.defaultYOffset = defaultYOffset;
    }

    /**
     * Anything unrecognised reads as {@link #DEFAULT} — the least intrusive of the four.
     */
    public static HealthStyle parse(String value) {
        if (value == null) return DEFAULT;
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return DEFAULT;
        }
    }

    public HealthStyle next() {
        HealthStyle[] all = values();
        return all[(ordinal() + 1) % all.length];
    }

    public String labelKey() {
        return "screen.coi.health_style_" + name().toLowerCase(Locale.ROOT);
    }

    /**
     * The three replacing styles take the hearts' own row (39), free precisely because they took
     * the hearts off it. {@link #HEARTS} cannot: the hearts are still there and vanilla's armour
     * bar sits at {@code h - 49}, so the obvious "one line up" would print the readout over the
     * armour of every armoured player.
     */
    public int defaultYOffset() {
        return defaultYOffset;
    }

    /**
     * True only for {@link #HEARTS} — the reason the replacement hook needs three answers.
     */
    public boolean drawsOverVanillaHearts() {
        return this == HEARTS;
    }
}
