package dev.ua.ikeepcalm.coi.ui;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Shared palette and card chrome, so every custom screen reads as one interface.
 */
public class CoiStyle {

    public static final int CARD_BG = 0xF0121216;
    public static final int TAB_BG_UNSELECTED = 0xC0121216;
    public static final int BORDER = 0xFF3A3A46;
    public static final int ACCENT = 0xFFFFD870;
    public static final int TEXT_BODY = 0xFFE0E0E0;
    public static final int TEXT_MUTED = 0xFF808088;
    public static final int ROW_HOVER = 0x28FFFFFF;
    /**
     * Named rather than typed as hex at the call site, because the choice is a judgement about
     * the screen behind: {@code BACKDROP} for a modal that owns the screen, {@code SCRIM} for
     * one the player glances past, {@code VEIL} where the world stays readable underneath.
     */
    public static final int BACKDROP = 0xA8000000;
    public static final int SCRIM = 0x90000000;
    public static final int VEIL = 0x50000000;
    public static final int INACTIVE = 0xFF55555C;
    public static final int SCROLL_TRACK = 0x30FFFFFF;

    /**
     * Shared by the sheet and the server-authored menus, which are the same object to a player.
     * The cap only binds at gui scale 2 and below; above that {@link #CARD_SIDE_MARGIN} decides
     * the card, which is why the gutter is thin rather than generous.
     */
    public static final int CARD_MAX_W = 720;
    public static final int CARD_MIN_W = 220;
    public static final int CARD_SIDE_MARGIN = 28;

    public static final int FORM_MAX_W = 440;
    public static final int FORM_MIN_W = 300;
    public static final int FORM_SIDE_MARGIN = 80;

    private CoiStyle() {
    }

    /**
     * @param compact a short window, where the gutter is halved to buy back rows
     */
    public static int cardWidth(int screenW, boolean compact) {
        int margin = compact ? 16 : CARD_SIDE_MARGIN;
        int w = Math.clamp(screenW - margin, CARD_MIN_W, CARD_MAX_W);
        return Math.clamp(screenW - 8, 120, w);
    }

    /**
     * A settings-style column of labelled rows. Deliberately <em>not</em> {@link #cardWidth}:
     * a form stops being readable long before a document does, so it takes a wider gutter and
     * caps far lower, and keeping the rules apart stops one widening with the other.
     */
    public static int formWidth(int screenW) {
        return Math.clamp(screenW - FORM_SIDE_MARGIN, FORM_MIN_W, FORM_MAX_W);
    }

    public static void drawCard(GuiGraphicsExtractor graphics, int x, int y, int w, int h) {
        graphics.fill(x, y, x + w, y + h, CARD_BG);
        graphics.outline(x, y, w, h, BORDER);
        graphics.fill(x, y, x + w, y + 1, ACCENT);
    }
}
