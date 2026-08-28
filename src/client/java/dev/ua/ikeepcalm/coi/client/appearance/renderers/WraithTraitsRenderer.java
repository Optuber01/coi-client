package dev.ua.ikeepcalm.coi.client.appearance.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import static dev.ua.ikeepcalm.coi.client.appearance.BodyBoxes.box;
import static dev.ua.ikeepcalm.coi.client.appearance.BodyBoxes.quad;

/**
 * Wraith traits — a spectral, half-there body: dim blue-gray translucency with frayed
 * trailing edges below the torso, as if the legs are dissolving into mist.
 */
public class WraithTraitsRenderer extends AbstractBodyOverlayRenderer {

    @Override
    public String traitId() {
        return "wraith_traits";
    }

    @Override
    protected float[] color() {
        return new float[]{0.42f, 0.48f, 0.60f};
    }

    @Override
    protected float alpha() {
        return 0.38f;
    }

    @Override
    protected void decorate(PoseStack.Pose pose, VertexConsumer consumer, int light, Part part) {
        if (part == Part.TORSO) {
            float[] mist = {0.62f, 0.68f, 0.82f};
            // Frayed trailing shreds below the waist
            quad(pose, consumer, -3.8f, 12.0f, 1.2f, -2.6f, 12.0f, 1.2f, -3.0f, 15.5f, 1.0f, -3.6f, 15.5f, 1.0f,
                    mist[0], mist[1], mist[2], 0.28f, light);
            quad(pose, consumer, 0.4f, 12.0f, -0.6f, 1.8f, 12.0f, -0.6f, 1.2f, 16.5f, -0.8f, -0.2f, 16.5f, -0.8f,
                    mist[0], mist[1], mist[2], 0.24f, light);
            quad(pose, consumer, 2.6f, 12.0f, 1.6f, 3.8f, 12.0f, 1.6f, 3.2f, 14.6f, 1.4f, 2.4f, 14.6f, 1.4f,
                    mist[0], mist[1], mist[2], 0.22f, light);
        } else if (part == Part.HEAD) {
            // Hollow gaze shadow
            box(pose, consumer, -2.9f, -4.6f, -4.4f, -1.1f, -3.4f, -4.3f,
                    0.05f, 0.05f, 0.10f, 0.9f, light);
            box(pose, consumer, 1.1f, -4.6f, -4.4f, 2.9f, -3.4f, -4.3f,
                    0.05f, 0.05f, 0.10f, 0.9f, light);
        }
    }
}
