package dev.ua.ikeepcalm.coi.domain.effect;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * One screen-space effect the server can trigger over {@code coi-client:effect}. Registered by
 * id in {@link EffectManager} and instantiated fresh per trigger.
 */
public interface VisualEffect {

    String getId();

    String getDisplayName();

    String getDefaultParams();

    void start(String params);

    void render(GuiGraphicsExtractor ctx, int screenWidth, int screenHeight, float tickDelta);

    boolean isFinished();

    default void stop() {
    }

    /**
     * The same stop, with the params it arrived with — the ceremony's audio bed takes its
     * crossfade out of {@code stop,fade=1500}, which is why the string gets this far.
     */
    default void stop(String params) {
        stop();
    }
}
