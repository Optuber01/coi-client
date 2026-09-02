package dev.ua.ikeepcalm.coi.client.appearance.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.ua.ikeepcalm.coi.client.appearance.AppearanceTraitRenderer;
import dev.ua.ikeepcalm.coi.client.appearance.TraitGeometry;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;

/**
 * Curved talons growing from each hand: three tapered tubes that arc forward past the
 * fingertips. Corrosive claws glow with luminous magenta symbols near the base; werewolf
 * claws are venom-dark with a blood-red accent.
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
            float x = -1.3f + index * 1.3f;
            TraitGeometry.Point[] path = {
                    g.pointPixels(x, 10.5f, -1.65f),
                    g.pointPixels(x, 12.1f, -2.05f),
                    g.pointPixels(x, 14.2f, -2.85f)
            };
            g.drawTube(pose, consumer, path, new float[]{0.28f, 0.22f, 0.04f}, 5,
                    new TraitGeometry.Tint[]{symbol, claw}, light);
        }
    }
}
