package dev.ua.ikeepcalm.coi.client.appearance.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import static dev.ua.ikeepcalm.coi.client.appearance.BodyBoxes.box;

/**
 * Chitin mutation — glossy dark plates: shoulder caps, forearm guards and a torso
 * carapace with pale edge highlights.
 */
public class ChitinMutationRenderer extends AbstractBodyOverlayRenderer {

    private static final float[] CHITIN = {0.12f, 0.08f, 0.16f};
    private static final float[] EDGE = {0.42f, 0.34f, 0.55f};

    @Override
    public String traitId() {
        return "chitin_mutation";
    }

    @Override
    protected float[] color() {
        return CHITIN;
    }

    @Override
    protected float alpha() {
        return 0.88f;
    }

    @Override
    protected void decorate(PoseStack.Pose pose, VertexConsumer consumer, int light, Part part) {
        switch (part) {
            case TORSO -> {
                box(pose, consumer, -4.4f, -0.1f, -2.4f, -2.4f, 2.6f, 2.4f,
                        EDGE[0], EDGE[1], EDGE[2], 0.55f, light);
                box(pose, consumer, 2.4f, -0.1f, -2.4f, 4.4f, 2.6f, 2.4f,
                        EDGE[0], EDGE[1], EDGE[2], 0.55f, light);
            }
            case RIGHT_ARM -> box(pose, consumer, -3.35f, 4.4f, -2.35f, 1.35f, 7.2f, 2.35f,
                    EDGE[0], EDGE[1], EDGE[2], 0.5f, light);
            case LEFT_ARM -> box(pose, consumer, -1.35f, 4.4f, -2.35f, 3.35f, 7.2f, 2.35f,
                    EDGE[0], EDGE[1], EDGE[2], 0.5f, light);
            default -> {
            }
        }
    }
}
