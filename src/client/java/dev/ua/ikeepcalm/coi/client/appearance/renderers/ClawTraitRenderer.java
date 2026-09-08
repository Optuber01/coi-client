package dev.ua.ikeepcalm.coi.client.appearance.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.ua.ikeepcalm.coi.client.appearance.AppearanceTraitRenderer;
import dev.ua.ikeepcalm.coi.client.appearance.TraitGeometry;
import dev.ua.ikeepcalm.coi.client.config.AppearanceConfig;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.player.PlayerModelType;

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
            case CORROSIVE -> new TraitGeometry.Tint(0.14f, 0.18f, 0.16f, 0.98f);
            case WEREWOLF -> new TraitGeometry.Tint(0.08f, 0.07f, 0.065f, 0.99f);
        };
        this.symbol = switch (style) {
            case CORROSIVE -> new TraitGeometry.Tint(0.36f, 0.58f, 0.29f, 0.95f);
            case WEREWOLF -> new TraitGeometry.Tint(0.55f, 0.09f, 0.08f, 0.9f);
        };
    }

    @Override
    public String traitId() {
        return traitId;
    }

    @Override
    public void submit(PoseStack stack, SubmitNodeCollector collector, AvatarRenderState state, PlayerModel model) {
        submitHand(stack, collector, state, model.leftArm, true);
        submitHand(stack, collector, state, model.rightArm, false);
    }

    private void submitHand(PoseStack stack, SubmitNodeCollector collector, AvatarRenderState state, ModelPart arm, boolean left) {
        stack.pushPose();
        arm.translateAndRotate(stack);
        boolean slim = state.skin.model() == PlayerModelType.SLIM;
        float center = (left ? 1.0f : -1.0f) * (slim ? 0.5f : 1.0f);
        var settings = AppearanceConfig.get();
        stack.translate(center / 16.0f, settings.clawYOffsetPixels / 16.0f, settings.clawZOffsetPixels / 16.0f);
        int light = style == Style.CORROSIVE ? TraitRenderSupport.FULL_BRIGHT : state.lightCoords;
        collector.order(3).submitCustomGeometry(stack, TraitRenderSupport.TRANSLUCENT,
                (pose, consumer) -> drawClaws(pose, consumer, light, slim));
        stack.popPose();
    }

    private void drawClaws(PoseStack.Pose pose, com.mojang.blaze3d.vertex.VertexConsumer consumer, int light, boolean slim) {
        TraitGeometry g = TraitGeometry.INSTANCE;
        var settings = AppearanceConfig.get();
        float length = settings.clawLength;
        for (int index = 0; index < 3; index++) {
            float x = (index - 1) * (slim ? 0.8f : 1.1f) * settings.clawSpread;
            TraitGeometry.Point[] path = {
                    g.pointPixels(x, 9.6f, -2.05f),
                    g.pointPixels(x, 9.6f + 1.1f * length, -2.05f - 0.5f * length),
                    g.pointPixels(x, 9.6f + 2.8f * length, -2.05f - 1.55f * length)
            };
            g.drawTube(pose, consumer, path, new float[]{0.35f, 0.25f, 0.06f}, 6,
                    new TraitGeometry.Tint[]{symbol, claw}, light);
        }
    }
}
