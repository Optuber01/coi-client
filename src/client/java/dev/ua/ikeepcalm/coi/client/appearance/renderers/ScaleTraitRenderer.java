package dev.ua.ikeepcalm.coi.client.appearance.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.ua.ikeepcalm.coi.client.appearance.AppearanceTraitRenderer;
import dev.ua.ikeepcalm.coi.client.appearance.TraitGeometry;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;

/**
 * Individual scale plates in an offset grid — dragon (muted purple with gold flecks)
 * or water (translucent aqua). Plates carry a small pointed tip so they catch light
 * instead of reading as flat paint.
 */
public final class ScaleTraitRenderer implements AppearanceTraitRenderer {

    public enum Style {DRAGON, WATER}

    private final String traitId;
    private final Style style;
    private final TraitGeometry.Tint primary;
    private final TraitGeometry.Tint secondary;

    public ScaleTraitRenderer(String traitId, Style style) {
        this.traitId = traitId;
        this.style = style;
        this.primary = style == Style.DRAGON
                ? new TraitGeometry.Tint(0.42f, 0.30f, 0.58f, 0.88f)
                : new TraitGeometry.Tint(0.05f, 0.52f, 0.82f, 0.46f);
        this.secondary = style == Style.DRAGON
                ? new TraitGeometry.Tint(0.70f, 0.56f, 0.22f, 0.90f)
                : new TraitGeometry.Tint(0.32f, 0.86f, 0.96f, 0.54f);
    }

    @Override
    public String traitId() {
        return traitId;
    }

    @Override
    public void submit(PoseStack stack, SubmitNodeCollector collector, AvatarRenderState state, PlayerModel model) {
        stack.pushPose();
        model.body.translateAndRotate(stack);
        collector.order(2).submitCustomGeometry(stack, TraitRenderSupport.TRANSLUCENT,
                (pose, consumer) -> drawBodyScales(pose, consumer, state.lightCoords));
        stack.popPose();

        stack.pushPose();
        model.head.translateAndRotate(stack);
        collector.order(2).submitCustomGeometry(stack, TraitRenderSupport.TRANSLUCENT,
                (pose, consumer) -> drawFaceScales(pose, consumer, state.lightCoords));
        stack.popPose();

        submitArm(stack, collector, state, model.leftArm, -1.0f);
        submitArm(stack, collector, state, model.rightArm, 1.0f);
    }

    private void submitArm(PoseStack stack, SubmitNodeCollector collector, AvatarRenderState state,
                           ModelPart arm, float side) {
        stack.pushPose();
        arm.translateAndRotate(stack);
        collector.order(2).submitCustomGeometry(stack, TraitRenderSupport.TRANSLUCENT,
                (pose, consumer) -> drawArmScales(pose, consumer, state.lightCoords, side));
        stack.popPose();
    }

    private void drawBodyScales(PoseStack.Pose pose, com.mojang.blaze3d.vertex.VertexConsumer consumer, int light) {
        TraitGeometry g = TraitGeometry.INSTANCE;
        for (int row = 0; row < 4; row++) {
            for (int column = 0; column < 4; column++) {
                float x = -3.25f + column * 2.15f + ((row & 1) == 0 ? 0.0f : 0.55f);
                float y = 1.15f + row * 2.35f;
                drawScale(g, pose, consumer, x, y, -2.18f, ((row + column) & 1) == 0 ? primary : secondary, light);
                drawScale(g, pose, consumer, x, y, 2.18f, ((row + column) & 1) == 0 ? secondary : primary, light);
            }
        }
    }

    private void drawFaceScales(PoseStack.Pose pose, com.mojang.blaze3d.vertex.VertexConsumer consumer, int light) {
        TraitGeometry g = TraitGeometry.INSTANCE;
        drawScale(g, pose, consumer, -3.15f, -1.35f, -4.20f, primary, light);
        drawScale(g, pose, consumer, -2.50f, 0.10f, -4.20f, secondary, light);
        drawScale(g, pose, consumer, 3.10f, -4.45f, -4.20f, primary, light);
    }

    private void drawArmScales(PoseStack.Pose pose, com.mojang.blaze3d.vertex.VertexConsumer consumer, int light, float side) {
        TraitGeometry g = TraitGeometry.INSTANCE;
        for (int row = 0; row < 4; row++) {
            drawScale(g, pose, consumer, side * 0.65f, 1.4f + row * 2.25f, -2.15f,
                    (row & 1) == 0 ? primary : secondary, light);
        }
    }

    private void drawScale(TraitGeometry g, PoseStack.Pose pose, com.mojang.blaze3d.vertex.VertexConsumer consumer,
                           float centerX, float centerY, float z, TraitGeometry.Tint tint, int light) {
        g.boxPixels(pose, consumer, centerX - 0.72f, centerY - 0.55f, z - 0.08f,
                centerX + 0.72f, centerY + 0.55f, z + 0.08f, tint, light);
        g.boxPixels(pose, consumer, centerX - 0.38f, centerY + 0.50f, z - 0.07f,
                centerX + 0.38f, centerY + 0.92f, z + 0.07f, tint, light);
    }
}
