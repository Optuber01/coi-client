package dev.ua.ikeepcalm.coi.hud.layout;

/**
 * The stable id of every positionable HUD element. <b>Never localized:</b> they are persisted
 * in {@code config/coi_hud.json}, key the {@code screen.coi.layout_el_<id>} lang strings and
 * are what an <em>Align</em> button passes in.
 */
public class ElementIds {

    public static final String ABILITY_SLOTS = "ability_slots";
    /**
     * Element ids are {@code slot_1} ... {@code slot_10}.
     */
    public static final String SLOT_PREFIX = "slot_";
    public static final String CHARACTER_PLATE = "character_plate";
    public static final String BEYONDER_HEALTH = "beyonder_health";
    public static final String MADNESS = "madness";
    public static final String SPIRITUALITY = "spirituality";
    public static final String ACTING = "acting";
    public static final String RESOURCES = "resources";
    public static final String ACTION_BAR = "action_bar";
    public static final String TARGET_HEALTH = "target_health";
    public static final String COGITATION = "cogitation";
    public static final String NOTIFICATIONS = "notifications";

    private ElementIds() {
    }
}
