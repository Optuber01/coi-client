package dev.ua.ikeepcalm.coi.client.appearance.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import static dev.ua.ikeepcalm.coi.client.appearance.BodyBoxes.quad;

/** Dragon scales — a purple hide with molten gold accents along the spine and brow. */
public class DragonScalesRenderer extends AbstractBodyOverlayRenderer {

    private static final float[] GOLD = {0.95f, 0.78f, 0.25f};

    @Override
    public String traitId() {
        return "dragon_scales";
    }

    @Override
    protected float[] color() {
        return new float[]{0.42f, 0.18f, 0.62f};
    }

    @Override
    protected float alpha() {
        return 0.80f;
    }

    @Override
    protected void decorate(PoseStack.Pose pose, VertexConsumer consumer, int light, Part part) {
        switch (part) {
            case TORSO -> quad(pose, consumer,
                    -0.5f, 1.0f, 2.3f, 0.5f, 1.0f, 2.3f, 0.5f, 11.2f, 2.3f, -0.5f, 11.2f, 2.3f,
                    GOLD[0], GOLD[1], GOLD[2], 0.9f, light);
            case RIGHT_ARM -> quad(pose, consumer,
                    -0.4f, -1.6f, -2.3f, 0.4f, -1.6f, -2.3f, 0.4f, 3.4f, -2.3f, -0.4f, 3.4f, -2.3f,
                    GOLD[0], GOLD[1], GOLD[2], 0.85f, light);
            case LEFT_ARM -> quad(pose, consumer,
                    0.6f, -1.6f, -2.3f, 1.4f, -1.6f, -2.3f, 1.4f, 3.4f, -2.3f, 0.6f, 3.4f, -2.3f,
                    GOLD[0], GOLD[1], GOLD[2], 0.85f, light);
            case HEAD -> quad(pose, consumer,
                    -0.6f, -8.3f, -4.3f, 0.6f, -8.3f, -4.3f, 0.6f, -2.0f, -4.3f, -0.6f, -2.0f, -4.3f,
                    GOLD[0], GOLD[1], GOLD[2], 0.85f, light);
            default -> {
            }
        }
    }
}
