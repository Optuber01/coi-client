package dev.ua.ikeepcalm.coi.hud.layout;

import dev.ua.ikeepcalm.coi.config.HudConfig;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/**
 * One draggable thing on the HUD. Implementations translate between the editor's single notion
 * of position — a top-left corner in gui-scaled pixels — and whatever the overlay actually
 * stores. <b>{@link #bounds} and {@link #moveTo} must stay exact inverses:</b> after
 * {@code moveTo(x, y, ...)}, {@code bounds(...)} reports {@code (x, y)} again, give or take the
 * on-screen clamp. A pad scaled in one and not the other makes the element jump when dragged.
 */
public interface HudElement {

    /**
     * Never localized: it keys the lang strings, {@code coi_hud.json} and the Align buttons.
     */
    String id();

    default Component label() {
        return Component.translatable("screen.coi.layout_el_" + id());
    }

    /**
     * {@code null} when the element stands alone. A group moves together under Ctrl-drag.
     */
    default String group() {
        return null;
    }

    /**
     * Called once per group by Reset, on top of each member's own {@link #resetPosition}.
     */
    default void resetGroup(HudConfig.HudSettings s) {
    }

    /**
     * A hidden element is still positionable; the editor ghosts it rather than dropping it.
     */
    boolean visible(HudConfig.HudSettings s);

    /**
     * {@code {x, y, width, height}} in gui-scaled pixels, labels included.
     */
    int[] bounds(int w, int h, HudConfig.HudSettings s);

    void moveTo(int newX, int newY, int w, int h, HudConfig.HudSettings s);

    /**
     * Sample data only: the editor runs over a live session, so no {@code domain} state here.
     */
    void renderPreview(GuiGraphicsExtractor ctx, int w, int h, HudConfig.HudSettings s, long timeMs);

    void resetPosition(HudConfig.HudSettings s);
}
