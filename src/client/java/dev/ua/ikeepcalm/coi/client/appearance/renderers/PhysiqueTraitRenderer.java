package dev.ua.ikeepcalm.coi.client.appearance.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.ua.ikeepcalm.coi.client.appearance.AppearanceTraitRenderer;
import dev.ua.ikeepcalm.coi.client.appearance.TraitGeometry;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;

/**
 * Silhouette bulk for the giant (widest) and masculine (subtle) physiques — translucent
 * shadow masses over the torso and upper arms with a warm highlight edge, so the frame
 * reads broader without touching the hitbox.
 */
public final class PhysiqueTraitRenderer implements AppearanceTraitRenderer {

    public enum Style {GIANT, MASCULINE}

    private final String traitId;
    private final Style style;
    private final TraitGeometry.Tint shadow;
    private final TraitGeometry.Tint highlight;

    public PhysiqueTraitRenderer(String traitId, Style style) {
        this.traitId = traitId;
        this.style = style;
        this.shadow = style == Style.GIANT
                ? new TraitGeometry.Tint(0.20f, 0.16f, 0.13f, 0.32f)
                : new TraitGeometry.Tint(0.25f, 0.10f, 0.07f, 0.27f);
        this.highlight = style == Style.GIANT
                ? new TraitGeometry.Tint(0.66f, 0.48f, 0.25f, 0.22f)
                : new TraitGeometry.Tint(0.72f, 0.26f, 0.16f, 0.20f);
    }

    @Override
    public String traitId() {
        return traitId;
    }

    @Override
    public void submit(PoseStack stack, SubmitNodeCollector collector, AvatarRenderState state, PlayerModel model) {
        stack.pushPose();
        model.body.translateAndRotate(stack);
        collector.order(1).submitCustomGeometry(stack, TraitRenderSupport.TRANSLUCENT,
                (pose, consumer) -> drawTorso(pose, consumer, state.lightCoords));
        stack.popPose();
        submitArm(stack, collector, state, model.leftArm);
        submitArm(stack, collector, state, model.rightArm);
    }

    private void submitArm(PoseStack stack, SubmitNodeCollector collector, AvatarRenderState state, ModelPart arm) {
        stack.pushPose();
        arm.translateAndRotate(stack);
        collector.order(1).submitCustomGeometry(stack, TraitRenderSupport.TRANSLUCENT,
                (pose, consumer) -> drawArm(pose, consumer, state.lightCoords));
        stack.popPose();
    }

    private void drawTorso(PoseStack.Pose pose, com.mojang.blaze3d.vertex.VertexConsumer consumer, int light) {
        TraitGeometry g = TraitGeometry.INSTANCE;
        float width = style == Style.GIANT ? 4.75f : 4.48f;
        float depth = style == Style.GIANT ? 2.55f : 2.38f;
        g.boxPixels(pose, consumer, -width, -0.25f, -depth, width, 4.7f, depth, shadow, light);
        g.boxPixels(pose, consumer, -4.15f, 0.45f, -depth - 0.15f, 4.15f, 1.05f, -depth + 0.08f, highlight, light);
    }

    private void drawArm(PoseStack.Pose pose, com.mojang.blaze3d.vertex.VertexConsumer consumer, int light) {
        TraitGeometry g = TraitGeometry.INSTANCE;
        float inflate = style == Style.GIANT ? 0.56f : 0.38f;
        g.boxPixels(pose, consumer, -2 - inflate, -2 - inflate, -2 - inflate,
                2 + inflate, 5.8f, 2 + inflate, shadow, light);
    }
}
