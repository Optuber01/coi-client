package dev.ua.ikeepcalm.coi.client.appearance.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.ua.ikeepcalm.coi.client.appearance.BodyBoxes;

import static dev.ua.ikeepcalm.coi.client.appearance.BodyBoxes.box;

/**
 * Masculine traits — a subtly broader frame: squared shoulder caps and a jaw shadow
 * that squares off the chin. The jaw samples the player's own skin so it reads as
 * bone structure rather than paint.
 */
public class MasculineTraitsRenderer extends AbstractBodyOverlayRenderer {

    private static final float[] FRAME = {0.14f, 0.13f, 0.12f};

    @Override
    public String traitId() {
        return "masculine_traits";
    }

    @Override
    protected float[] color() {
        return FRAME;
    }

    @Override
    protected float alpha() {
        return 0.0f;
    }

    @Override
    protected boolean tintsBody() {
        return false;
    }

    @Override
    protected void decorate(PoseStack.Pose pose, VertexConsumer consumer, int light, Part part) {
        switch (part) {
            case TORSO -> {
                box(pose, consumer, -4.5f, -1.2f, -2.5f, -2.2f, 1.6f, 2.5f,
                        FRAME[0], FRAME[1], FRAME[2], 0.85f, light);
                box(pose, consumer, 2.2f, -1.2f, -2.5f, 4.5f, 1.6f, 2.5f,
                        FRAME[0], FRAME[1], FRAME[2], 0.85f, light);
            }
            case HEAD -> {
                // Squared jaw: skin-sampled chin widened a touch beyond the face plane
                BodyBoxes.headFrontQuad(pose, consumer, -3.2f, 3.2f, -1.6f, 0.0f, -4.14f, false, light);
            }
            default -> {
            }
        }
    }
}
