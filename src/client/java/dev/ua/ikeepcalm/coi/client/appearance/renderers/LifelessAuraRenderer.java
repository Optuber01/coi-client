package dev.ua.ikeepcalm.coi.client.appearance.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.ua.ikeepcalm.coi.client.appearance.AppearanceTraitRenderer;
import dev.ua.ikeepcalm.coi.client.appearance.BodyBoxes;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;

/**
 * Lifeless aura — gray spectral rings orbiting the body at hip, chest and crown height,
 * rotating slowly in alternating directions. Purely an emanation: it never touches the
 * body geometry itself.
 */
public class LifelessAuraRenderer implements AppearanceTraitRenderer {

    private static final float[] RING = {0.62f, 0.64f, 0.68f};

    private static final float[][] RING_HEIGHTS = {{-2.0f, 11.5f}, {4.0f, 10.0f}, {10.0f, 8.5f}};

    @Override
    public String traitId() {
        return "lifeless_aura";
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, AvatarRenderState state, PlayerModel model) {
        poseStack.pushPose();
        model.body.translateAndRotate(poseStack);
        collector.order(0).submitCustomGeometry(
                poseStack,
                BodyBoxes.tintType(),
                (pose, consumer) -> {
                    long now = System.currentTimeMillis();
                    for (int index = 0; index < RING_HEIGHTS.length; index++) {
                        float height = RING_HEIGHTS[index][0];
                        float radius = RING_HEIGHTS[index][1];
                        float direction = index % 2 == 0 ? 1.0f : -1.0f;
                        float speed = 0.00035f + index * 0.00012f;
                        drawRing(pose, consumer, state.lightCoords, height, radius,
                                now * speed * direction);
                    }
                }
        );
        poseStack.popPose();
    }

    private void drawRing(PoseStack.Pose pose, VertexConsumer consumer, int light,
                          float heightPixels, float radiusPixels, float angle) {
        int segments = 18;
        for (int segment = 0; segment < segments; segment++) {
            float a0 = angle + (float) (Math.PI * 2.0 * segment / segments);
            float a1 = angle + (float) (Math.PI * 2.0 * (segment + 0.55) / segments);
            float x0 = (float) Math.cos(a0) * radiusPixels;
            float z0 = (float) Math.sin(a0) * radiusPixels;
            float x1 = (float) Math.cos(a1) * radiusPixels;
            float z1 = (float) Math.sin(a1) * radiusPixels;

            // Short vertical dashes orbiting the body — reads as spectral motion
            float bob = (float) Math.sin(a0 * 3.0f + angle * 2.0f) * 0.8f;
            float dashTop = heightPixels - 0.6f + bob;
            float dashBottom = heightPixels + 0.6f + bob;

            BodyBoxes.quad(pose, consumer,
                    x0, dashTop, z0,
                    x1, dashTop, z1,
                    x1, dashBottom, z1,
                    x0, dashBottom, z0,
                    RING[0], RING[1], RING[2], 0.30f, light);
        }
    }
}
