package dev.ua.ikeepcalm.coi.client.appearance.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.ua.ikeepcalm.coi.client.appearance.BodyBoxes;

import static dev.ua.ikeepcalm.coi.client.appearance.BodyBoxes.box;

/**
 * Regrowth mutation — living vines spiraling the torso and limbs, with small leaf quads.
 * Mother pathway flavor for bodies that refuse to stay dead.
 */
public class RegrowthMutationRenderer extends AbstractBodyOverlayRenderer {

    private static final float[] VINE = {0.20f, 0.52f, 0.18f};
    private static final float[] LEAF = {0.35f, 0.75f, 0.28f};

    @Override
    public String traitId() {
        return "regrowth_mutation";
    }

    @Override
    protected float[] color() {
        return new float[]{0.0f, 0.0f, 0.0f};
    }

    @Override
    protected float alpha() {
        return 0.0f;
    }

    @Override
    protected boolean tintsBody() {
        // No base tint — only the vines themselves
        return false;
    }

    @Override
    protected void decorate(PoseStack.Pose pose, VertexConsumer consumer, int light, Part part) {
        switch (part) {
            case TORSO -> {
                vine(pose, consumer, light, -4.4f, 1.0f, -2.4f, 3.4f, 10.0f, 2.4f);
                vine(pose, consumer, light, 2.0f, 2.5f, -2.45f, 4.5f, 11.0f, -2.45f);
                leaf(pose, consumer, light, -2.6f, 3.4f, -2.5f);
                leaf(pose, consumer, light, 3.1f, 6.8f, -2.5f);
            }
            case RIGHT_ARM -> {
                vine(pose, consumer, light, -3.4f, -1.5f, -2.4f, 1.4f, 8.5f, -2.4f);
                leaf(pose, consumer, light, -2.2f, 1.8f, -2.55f);
            }
            case LEFT_ARM -> {
                vine(pose, consumer, light, -1.4f, 0.5f, -2.4f, 3.4f, 10.0f, -2.4f);
                leaf(pose, consumer, light, 1.9f, 5.2f, -2.55f);
            }
            case RIGHT_LEG -> {
                vine(pose, consumer, light, -2.4f, 0.5f, -2.4f, 2.4f, 11.0f, -2.4f);
                leaf(pose, consumer, light, -1.2f, 4.0f, -2.55f);
            }
            case LEFT_LEG -> {
                vine(pose, consumer, light, -2.4f, 1.5f, 2.4f, 2.4f, 11.5f, 2.4f);
                leaf(pose, consumer, light, 0.8f, 6.4f, 2.55f);
            }
            default -> {
            }
        }
    }

    /** A thin three-sided vine strand wrapping diagonally between two points. */
    private void vine(PoseStack.Pose pose, VertexConsumer consumer, int light,
                      float x0, float y0, float z0, float x1, float y1, float z1) {
        float thickness = 0.28f;
        BodyBoxes.box(pose, consumer,
                Math.min(x0, x1) - thickness, Math.min(y0, y1), Math.min(z0, z1) - thickness,
                Math.max(x0, x1) + thickness, Math.max(y0, y1), Math.max(z0, z1) + thickness,
                VINE[0], VINE[1], VINE[2], 0.95f, light);
    }

    private void leaf(PoseStack.Pose pose, VertexConsumer consumer, int light, float x, float y, float z) {
        BodyBoxes.quad(pose, consumer,
                x, y, z, x + 0.9f, y + 0.4f, z, x + 1.1f, y + 1.2f, z, x + 0.2f, y + 0.9f, z,
                LEAF[0], LEAF[1], LEAF[2], 1.0f, light);
    }
}
