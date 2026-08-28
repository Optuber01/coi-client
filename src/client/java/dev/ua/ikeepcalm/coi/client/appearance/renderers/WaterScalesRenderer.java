package dev.ua.ikeepcalm.coi.client.appearance.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import static dev.ua.ikeepcalm.coi.client.appearance.BodyBoxes.quad;

/**
 * Water scales — a translucent aqua film over the body with pale shimmer streaks,
 * Tyrant/sea flavor.
 */
public class WaterScalesRenderer extends AbstractBodyOverlayRenderer {

    @Override
    public String traitId() {
        return "water_scales";
    }

    @Override
    protected float[] color() {
        return new float[]{0.28f, 0.72f, 0.85f};
    }

    @Override
    protected float alpha() {
        return 0.42f;
    }

    @Override
    protected void decorate(PoseStack.Pose pose, VertexConsumer consumer, int light, Part part) {
        if (part != Part.TORSO && part != Part.HEAD) {
            return;
        }
        float[] shimmer = {0.85f, 0.97f, 1.00f};
        if (part == Part.TORSO) {
            quad(pose, consumer,
                    -3.6f, 1.2f, 2.28f, -3.2f, 1.2f, 2.28f, -3.2f, 7.8f, 2.28f, -3.6f, 7.8f, 2.28f,
                    shimmer[0], shimmer[1], shimmer[2], 0.35f, light);
            quad(pose, consumer,
                    2.6f, 2.4f, 2.28f, 3.0f, 2.4f, 2.28f, 3.0f, 9.0f, 2.28f, 2.6f, 9.0f, 2.28f,
                    shimmer[0], shimmer[1], shimmer[2], 0.30f, light);
        } else {
            quad(pose, consumer,
                    -3.4f, -7.4f, -4.28f, -3.0f, -7.4f, -4.28f, -3.0f, -1.2f, -4.28f, -3.4f, -1.2f, -4.28f,
                    shimmer[0], shimmer[1], shimmer[2], 0.30f, light);
        }
    }
}
