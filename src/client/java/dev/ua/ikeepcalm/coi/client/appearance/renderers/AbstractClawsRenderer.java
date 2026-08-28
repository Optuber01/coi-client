package dev.ua.ikeepcalm.coi.client.appearance.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.ua.ikeepcalm.coi.client.appearance.AppearanceTraitRenderer;
import dev.ua.ikeepcalm.coi.client.appearance.BodyBoxes;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;

import static dev.ua.ikeepcalm.coi.client.appearance.BodyBoxes.box;
import static dev.ua.ikeepcalm.coi.client.appearance.BodyBoxes.quad;

/**
 * Base for the claw traits: three talons per hand anchored to each arm so they track
 * the swing animation, tips extending past the fingertips.
 */
public abstract class AbstractClawsRenderer implements AppearanceTraitRenderer {

    /** Claw color; luminous traits add a glow aura around the talons. */
    protected abstract float[] clawColor();

    protected boolean luminous() {
        return false;
    }

    protected boolean drawsToeClaws() {
        return false;
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, AvatarRenderState state, PlayerModel model) {
        float[] color = clawColor();
        int light = luminous() ? 0xF000F0 : state.lightCoords;

        submitHand(poseStack, collector, state, model.rightArm, color, light);
        submitHand(poseStack, collector, state, model.leftArm, color, light);

        if (drawsToeClaws()) {
            submitFoot(poseStack, collector, state, model.rightLeg, color, light);
            submitFoot(poseStack, collector, state, model.leftLeg, color, light);
        }
    }

    private void submitHand(PoseStack poseStack, SubmitNodeCollector collector, AvatarRenderState state,
                            ModelPart arm, float[] color, int light) {
        poseStack.pushPose();
        arm.translateAndRotate(poseStack);
        collector.order(0).submitCustomGeometry(
                poseStack,
                BodyBoxes.tintType(),
                (pose, consumer) -> {
                    drawClawSet(pose, consumer, light, color, 12.0f);
                    if (luminous()) {
                        // Soft halo around the talons
                        box(pose, consumer, -3.0f, 9.6f, -2.1f, 1.2f, 13.2f, 2.1f,
                                color[0], color[1], color[2], 0.16f, light);
                    }
                }
        );
        poseStack.popPose();
    }

    private void submitFoot(PoseStack poseStack, SubmitNodeCollector collector, AvatarRenderState state,
                            ModelPart leg, float[] color, int light) {
        poseStack.pushPose();
        leg.translateAndRotate(poseStack);
        collector.order(0).submitCustomGeometry(
                poseStack,
                BodyBoxes.tintType(),
                (pose, consumer) -> drawClawSet(pose, consumer, light, color, 12.2f)
        );
        poseStack.popPose();
    }

    private void drawClawSet(PoseStack.Pose pose, com.mojang.blaze3d.vertex.VertexConsumer consumer,
                             int light, float[] color, float baseY) {
        drawClaw(pose, consumer, light, color, -2.15f, baseY);
        drawClaw(pose, consumer, light, color, -0.95f, baseY + 0.25f);
        drawClaw(pose, consumer, light, color, 0.15f, baseY);
    }

    private void drawClaw(PoseStack.Pose pose, com.mojang.blaze3d.vertex.VertexConsumer consumer,
                          int light, float[] color, float x, float baseY) {
        float halfWidth = 0.22f;
        float tipY = baseY + 2.6f;
        float z = -1.2f;
        // Tapering shard: front face narrowing to a point
        quad(pose, consumer,
                x - halfWidth, baseY, z, x + halfWidth, baseY, z, x, tipY, z, x - halfWidth, baseY, z,
                color[0], color[1], color[2], 1.0f, light);
        // Underside so the claw isn't invisible from below
        quad(pose, consumer,
                x - halfWidth, baseY, z + 0.3f, x + halfWidth, baseY, z + 0.3f, x, tipY, z + 0.3f, x - halfWidth, baseY, z + 0.3f,
                color[0] * 0.8f, color[1] * 0.8f, color[2] * 0.8f, 1.0f, light);
    }
}
