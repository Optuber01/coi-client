package dev.ua.ikeepcalm.coi.client.appearance.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import static dev.ua.ikeepcalm.coi.client.appearance.BodyBoxes.box;

/** Zombie traits — a sickly green putrefied tint with darker decay patches. */
public class ZombieTraitsRenderer extends AbstractBodyOverlayRenderer {

    @Override
    public String traitId() {
        return "zombie_traits";
    }

    @Override
    protected float[] color() {
        return new float[]{0.30f, 0.48f, 0.24f};
    }

    @Override
    protected float alpha() {
        return 0.42f;
    }

    @Override
    protected void decorate(PoseStack.Pose pose, VertexConsumer consumer, int light, Part part) {
        if (part == Part.TORSO) {
            box(pose, consumer, -2.8f, 6.4f, -2.35f, -0.6f, 9.2f, -2.25f,
                    0.16f, 0.26f, 0.12f, 0.85f, light);
            box(pose, consumer, 1.0f, 2.2f, 2.25f, 3.4f, 5.0f, 2.35f,
                    0.16f, 0.26f, 0.12f, 0.85f, light);
        } else if (part == Part.HEAD) {
            box(pose, consumer, -2.2f, -3.6f, -4.35f, 0.4f, -1.4f, -4.25f,
                    0.16f, 0.26f, 0.12f, 0.85f, light);
        }
    }
}
