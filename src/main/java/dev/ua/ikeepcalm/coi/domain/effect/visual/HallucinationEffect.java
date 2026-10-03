package dev.ua.ikeepcalm.coi.domain.effect.visual;

import dev.ua.ikeepcalm.coi.domain.effect.HallucinationManager;
import dev.ua.ikeepcalm.coi.domain.effect.VisualEffect;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Pseudo-effect: fires one hallucination event and finishes immediately, so the server and the
 * debug screen can both reach {@link HallucinationManager} through the effect payload.
 * <p>
 * Params: {@code event} ({@code footsteps}, {@code whisper}, {@code cave}, {@code block},
 * {@code flicker} or {@code random}).
 */
public class HallucinationEffect implements VisualEffect {

    public static final String ID = "hallucination";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getDisplayName() {
        return "Hallucination (one-shot)";
    }

    @Override
    public String getDefaultParams() {
        return "event=random";
    }

    @Override
    public void start(String params) {
        HallucinationManager.triggerNamed(params);
    }

    @Override
    public void render(GuiGraphicsExtractor ctx, int screenWidth, int screenHeight, float tickDelta) {
    }

    @Override
    public boolean isFinished() {
        return true;
    }
}
