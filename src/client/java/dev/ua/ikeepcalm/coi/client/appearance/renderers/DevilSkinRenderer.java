package dev.ua.ikeepcalm.coi.client.appearance.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import static dev.ua.ikeepcalm.coi.client.appearance.BodyBoxes.box;

/**
 * Devil armor skin — a near-black hide reading as worn armor plating with crimson
 * detail trim on the chest and shoulders.
 */
public class DevilSkinRenderer extends AbstractBodyOverlayRenderer {

    private static final float[] PLATE = {0.07f, 0.05f, 0.08f};
    private static final float[] TRIM = {0.70f, 0.08f, 0.10f};

    @Override
    public String traitId() {
        return "devil_skin";
    }

    @Override
    protected float[] color() {
        return PLATE;
    }

    @Override
    protected float alpha() {
        return 0.94f;
    }

    @Override
    protected void decorate(PoseStack.Pose pose, VertexConsumer consumer, int light, Part part) {
        if (part == Part.TORSO) {
            // Chest sigil stripe
            box(pose, consumer, -1.1f, 2.2f, -2.35f, 1.1f, 8.6f, -2.25f,
                    TRIM[0], TRIM[1], TRIM[2], 0.95f, light);
            // Shoulder trim
            box(pose, consumer, -4.3f, 0.1f, -2.3f, -2.6f, 0.7f, 2.3f,
                    TRIM[0], TRIM[1], TRIM[2], 0.9f, light);
            box(pose, consumer, 2.6f, 0.1f, -2.3f, 4.3f, 0.7f, 2.3f,
                    TRIM[0], TRIM[1], TRIM[2], 0.9f, light);
        } else if (part == Part.HEAD) {
            // Brow bar
            box(pose, consumer, -4.3f, -6.9f, -4.3f, 4.3f, -6.1f, 4.3f,
                    TRIM[0], TRIM[1], TRIM[2], 0.85f, light);
        }
    }
}
