package dev.ua.ikeepcalm.coi.client.appearance.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import static dev.ua.ikeepcalm.coi.client.appearance.BodyBoxes.box;

/**
 * Giant physique — a silhouette-bulking mass over the whole frame: slab shoulders,
 * thickened limbs and a heavier torso. Pure visual mass; no hitbox or scale changes.
 * Wins the physique family over the masculine frame.
 */
public class GiantPhysiqueRenderer extends AbstractBodyOverlayRenderer {

    private static final float[] MASS = {0.16f, 0.14f, 0.13f};

    @Override
    public String traitId() {
        return "giant_physique";
    }

    @Override
    protected float[] color() {
        return MASS;
    }

    @Override
    protected float alpha() {
        return 0.88f;
    }

    @Override
    protected void decorate(PoseStack.Pose pose, VertexConsumer consumer, int light, Part part) {
        switch (part) {
            case TORSO -> {
                // Massive trapezius ridge across the top of the torso
                box(pose, consumer, -4.6f, -1.6f, -2.6f, 4.6f, 1.2f, 2.6f,
                        MASS[0], MASS[1], MASS[2], 0.95f, light);
            }
            case RIGHT_ARM -> box(pose, consumer, -3.9f, -2.9f, -2.9f, 1.9f, 3.2f, 2.9f,
                    MASS[0], MASS[1], MASS[2], 0.92f, light);
            case LEFT_ARM -> box(pose, consumer, -1.9f, -2.9f, -2.9f, 3.9f, 3.2f, 2.9f,
                    MASS[0], MASS[1], MASS[2], 0.92f, light);
            case RIGHT_LEG -> box(pose, consumer, -2.8f, 0.0f, -2.8f, 2.8f, 4.6f, 2.8f,
                    MASS[0], MASS[1], MASS[2], 0.92f, light);
            case LEFT_LEG -> box(pose, consumer, -2.8f, 0.0f, -2.8f, 2.8f, 4.6f, 2.8f,
                    MASS[0], MASS[1], MASS[2], 0.92f, light);
            default -> {
            }
        }
    }
}
