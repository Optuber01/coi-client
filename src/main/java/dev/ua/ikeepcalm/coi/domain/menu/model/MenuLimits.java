package dev.ua.ikeepcalm.coi.domain.menu.model;

/**
 * How much of a menu document this client will read. Not a security boundary — the payload is
 * already capped at 1 MiB by {@code MenuPayload} — but a runaway server-side loop must not lay
 * out a card a hundred thousand pixels tall. Exceeding a cap truncates; it never fails.
 */
public final class MenuLimits {

    public static final int MAX_SECTIONS = 32;
    public static final int MAX_COMPONENTS = 96;
    public static final int MAX_ROWS = 400;
    public static final int MAX_KV_ROWS = 64;
    public static final int MAX_CHECKS = 64;
    public static final int MAX_BUTTONS = 24;
    public static final int MAX_TOOLTIP_LINES = 10;
    /**
     * Low on purpose: past a dozen chips or steps the server should have sent a list.
     */
    public static final int MAX_CHIPS = 12;
    public static final int MAX_STEPS = 12;
    public static final int MAX_PANEL_CELLS = 12;
    public static final int MAX_DETAILS_BLOCKS = 8;
    public static final int MAX_HERO_CHIPS = 4;
    public static final int MAX_TITLE = 128;
    public static final int MAX_LABEL = 160;
    public static final int MAX_TEXT = 2000;
    public static final int MAX_ID = 96;
    /**
     * Short free-text fields: a badge, a delta, an on/off word.
     */
    public static final int MAX_BADGE = 32;
    /**
     * An enum word on the wire ({@code "primary"}, {@code "timeline"}, …).
     */
    public static final int MAX_WORD = 16;
    /**
     * A component {@code type} name.
     */
    public static final int MAX_TYPE = 24;
    /**
     * {@code "#RRGGBB"} at its longest.
     */
    public static final int MAX_COLOR = 8;

    private MenuLimits() {
    }
}
