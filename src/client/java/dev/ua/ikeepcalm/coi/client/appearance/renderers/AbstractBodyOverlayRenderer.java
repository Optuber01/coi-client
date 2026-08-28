package dev.ua.ikeepcalm.coi.client.appearance.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.ua.ikeepcalm.coi.client.appearance.AppearanceTraitRenderer;
import dev.ua.ikeepcalm.coi.client.appearance.BodyBoxes;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;

/**
 * Base for traits that tint or cloak the whole body (skins, scales, undead traits):
 * a translucent box over each animated part — head, torso, arms, legs — plus the
 * matching skin-customisation layer (jacket/sleeves/pants/hat) when the player has
 * that layer enabled, mirroring the vanilla outer-layer inflation.
 */
public abstract class AbstractBodyOverlayRenderer implements AppearanceTraitRenderer {

    protected enum Part {
        HEAD, TORSO, RIGHT_ARM, LEFT_ARM, RIGHT_LEG, LEFT_LEG
    }

    protected abstract float[] color();

    protected abstract float alpha();

    /**
     * Whether the base tint draws on a part. Return false for decorate-only traits
     * (e.g. vines that wrap the body without tinting it).
     */
    protected boolean tintsBody() {
        return true;
    }

    /** Extra detail on top of the base tint; default draws nothing. */
    protected void decorate(PoseStack.Pose pose, VertexConsumer consumer, int light, Part part) {
    }

    protected boolean draws(Part part) {
        return true;
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, AvatarRenderState state, PlayerModel model) {
        float[] color = color();
        float alpha = alpha();

        submitPart(poseStack, collector, state, model.head, state.showHat, Part.HEAD,
                -4.25f, -8.25f, -4.25f, 4.25f, 0.25f, 4.25f, color, alpha);
        submitPart(poseStack, collector, state, model.body, state.showJacket, Part.TORSO,
                -4.25f, 0.0f, -2.25f, 4.25f, 12.25f, 2.25f, color, alpha);
        submitPart(poseStack, collector, state, model.rightArm, state.showRightSleeve, Part.RIGHT_ARM,
                -3.25f, -2.25f, -2.25f, 1.25f, 10.25f, 2.25f, color, alpha);
        submitPart(poseStack, collector, state, model.leftArm, state.showLeftSleeve, Part.LEFT_ARM,
                -1.25f, -2.25f, -2.25f, 3.25f, 10.25f, 2.25f, color, alpha);
        submitPart(poseStack, collector, state, model.rightLeg, state.showRightPants, Part.RIGHT_LEG,
                -2.25f, 0.0f, -2.25f, 2.25f, 12.25f, 2.25f, color, alpha);
        submitPart(poseStack, collector, state, model.leftLeg, state.showLeftPants, Part.LEFT_LEG,
                -2.25f, 0.0f, -2.25f, 2.25f, 12.25f, 2.25f, color, alpha);
    }

    private void submitPart(PoseStack poseStack, SubmitNodeCollector collector, AvatarRenderState state,
                            ModelPart part, boolean overlayShown, Part partId,
                            float minX, float minY, float minZ, float maxX, float maxY, float maxZ,
                            float[] color, float alpha) {
        if (!draws(partId)) {
            return;
        }
        poseStack.pushPose();
        part.translateAndRotate(poseStack);
        collector.order(0).submitCustomGeometry(
                poseStack,
                BodyBoxes.tintType(),
                (pose, consumer) -> {
                    if (tintsBody()) {
                        BodyBoxes.box(pose, consumer, minX, minY, minZ, maxX, maxY, maxZ,
                                color[0], color[1], color[2], alpha, state.lightCoords);
                        if (overlayShown) {
                            BodyBoxes.box(pose, consumer, minX - 0.25f, minY - 0.25f, minZ - 0.25f,
                                    maxX + 0.25f, maxY + 0.25f, maxZ + 0.25f,
                                    color[0], color[1], color[2], alpha, state.lightCoords);
                        }
                    }
                    decorate(pose, consumer, state.lightCoords, partId);
                }
        );
        poseStack.popPose();
    }
}
