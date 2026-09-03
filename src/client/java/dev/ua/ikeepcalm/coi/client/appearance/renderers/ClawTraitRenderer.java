package dev.ua.ikeepcalm.coi.client.appearance.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.ua.ikeepcalm.coi.client.appearance.AppearanceTraitRenderer;
import dev.ua.ikeepcalm.coi.client.appearance.TraitGeometry;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;

/**
 * Short, separate fingertip talons. The old tubes started inside the wrist and were too thin
 * to read reliably; these start at the hand edge, widen at the knuckle, and taper forward.
 */
public final class ClawTraitRenderer implements AppearanceTraitRenderer {

    public enum Style {CORROSIVE, WEREWOLF}

    private final String traitId;
    private final Style style;
    private final TraitGeometry.Tint claw;
    private final TraitGeometry.Tint symbol;

    public ClawTraitRenderer(String traitId, Style style) {
        this.traitId = traitId;
        this.style = style;
        this.claw = switch (style) {
            case CORROSIVE -> new TraitGeometry.Tint(0.19f, 0.015f, 0.23f, 0.98f);
            case WEREWOLF -> new TraitGeometry.Tint(0.08f, 0.07f, 0.065f, 0.99f);
        };
        this.symbol = switch (style) {
            case CORROSIVE -> new TraitGeometry.Tint(0.75f, 0.08f, 0.88f, 0.95f);
            case WEREWOLF -> new TraitGeometry.Tint(0.55f, 0.09f, 0.08f, 0.9f);
        };
    }

    @Override
    public String traitId() {
        return traitId;
    }

    @Override
    public void submit(PoseStack stack, SubmitNodeCollector collector, AvatarRenderState state, PlayerModel model) {
        submitHand(stack, collector, state, model.leftArm);
        submitHand(stack, collector, state, model.rightArm);
    }

    private void submitHand(PoseStack stack, SubmitNodeCollector collector, AvatarRenderState state, ModelPart arm) {
        stack.pushPose();
        arm.translateAndRotate(stack);
        int light = style == Style.CORROSIVE ? TraitRenderSupport.FULL_BRIGHT : state.lightCoords;
        collector.order(3).submitCustomGeometry(stack, TraitRenderSupport.TRANSLUCENT,
                (pose, consumer) -> drawClaws(pose, consumer, light));
        stack.popPose();
    }

    private void drawClaws(PoseStack.Pose pose, com.mojang.blaze3d.vertex.VertexConsumer consumer, int light) {
        TraitGeometry g = TraitGeometry.INSTANCE;
        for (int index = 0; index < 3; index++) {
            float x = -1.25f + index * 1.25f;
            TraitGeometry.Point[] path = {
                    g.pointPixels(x, 10.9f, -2.15f),
                    g.pointPixels(x, 12.0f, -2.65f),
                    g.pointPixels(x, 13.8f, -4.15f)
            };
            g.drawTube(pose, consumer, path, new float[]{0.44f, 0.30f, 0.08f}, 6,
                    new TraitGeometry.Tint[]{symbol, claw}, light);
        }
    }
}
