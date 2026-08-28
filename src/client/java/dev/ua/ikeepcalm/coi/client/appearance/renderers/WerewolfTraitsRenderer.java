package dev.ua.ikeepcalm.coi.client.appearance.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.ua.ikeepcalm.coi.client.appearance.AppearanceTraitRenderer;
import dev.ua.ikeepcalm.coi.client.appearance.BakedAccessoryModel;
import dev.ua.ikeepcalm.coi.client.appearance.BodyBoxes;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

/**
 * Werewolf traits — the Moon pathway's chained curse: fur, a protruding muzzle with a
 * black nose, tall pointed ears, shaggy chest fur and bone claws on both hands.
 * Head/ear/fur geometry is baked; the claws reuse the claw trait anchors.
 */
public class WerewolfTraitsRenderer implements AppearanceTraitRenderer {

    private static final Identifier WHITE_TEXTURE =
            Identifier.fromNamespaceAndPath("coi-client", "textures/entity/white.png");
    private static final RenderType RENDER_TYPE = RenderTypes.entityTranslucent(WHITE_TEXTURE);

    private static final float[] FUR = {0.36f, 0.29f, 0.21f};
    private static final float[] FUR_DARK = {0.22f, 0.17f, 0.12f};
    private static final float[] FUR_LIGHT = {0.52f, 0.44f, 0.33f};
    private static final float[] NOSE = {0.05f, 0.04f, 0.04f};
    private static final float[] CLAW = {0.62f, 0.58f, 0.50f};

    private final BakedAccessoryModel headModel;
    private final BakedAccessoryModel chestModel;

    public WerewolfTraitsRenderer() {
        this.headModel = buildHead();
        this.chestModel = buildChest();
    }

    @Override
    public String traitId() {
        return "werewolf_traits";
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, AvatarRenderState state, PlayerModel model) {
        poseStack.pushPose();
        model.head.translateAndRotate(poseStack);
        collector.order(0).submitCustomGeometry(
                poseStack,
                RENDER_TYPE,
                (pose, consumer) -> headModel.render(pose, consumer, state.lightCoords)
        );
        poseStack.popPose();

        poseStack.pushPose();
        model.body.translateAndRotate(poseStack);
        collector.order(0).submitCustomGeometry(
                poseStack,
                RENDER_TYPE,
                (pose, consumer) -> chestModel.render(pose, consumer, state.lightCoords)
        );
        poseStack.popPose();

        poseStack.pushPose();
        model.rightArm.translateAndRotate(poseStack);
        collector.order(0).submitCustomGeometry(
                poseStack,
                RENDER_TYPE,
                (pose, consumer) -> drawHandClaws(pose, consumer, state.lightCoords)
        );
        poseStack.popPose();

        poseStack.pushPose();
        model.leftArm.translateAndRotate(poseStack);
        collector.order(0).submitCustomGeometry(
                poseStack,
                RENDER_TYPE,
                (pose, consumer) -> drawHandClaws(pose, consumer, state.lightCoords)
        );
        poseStack.popPose();
    }

    private void drawHandClaws(PoseStack.Pose pose, com.mojang.blaze3d.vertex.VertexConsumer consumer, int light) {
        for (float x : new float[]{-2.1f, -1.0f, 0.1f}) {
            BodyBoxes.quad(pose, consumer,
                    x - 0.22f, 9.6f, -1.2f, x + 0.22f, 9.6f, -1.2f, x, 12.1f, -1.2f, x - 0.22f, 9.6f, -1.2f,
                    CLAW[0], CLAW[1], CLAW[2], 1.0f, light);
        }
    }

    private BakedAccessoryModel buildHead() {
        BakedAccessoryModel.Builder builder = new BakedAccessoryModel.Builder();

        // Muzzle protruding from the lower face
        builder.box(-1.85f, -2.4f, -6.35f, 1.85f, 0.5f, -3.85f, FUR[0], FUR[1], FUR[2], 1.0f);
        // Nose at the muzzle tip
        builder.box(-0.75f, -2.4f, -6.55f, 0.75f, -1.5f, -6.15f, NOSE[0], NOSE[1], NOSE[2], 1.0f);
        // Muzzle underside shade
        builder.box(-1.5f, 0.2f, -6.1f, 1.5f, 0.55f, -3.9f, FUR_DARK[0], FUR_DARK[1], FUR_DARK[2], 1.0f);

        // Tall pointed ears
        builder.solidBox(-3.5f, -10.8f, -1.0f, -1.7f, -8.0f, 0.8f, FUR_DARK[0], FUR_DARK[1], FUR_DARK[2], 1.0f);
        builder.solidBox(1.7f, -10.8f, -1.0f, 3.5f, -8.0f, 0.8f, FUR_DARK[0], FUR_DARK[1], FUR_DARK[2], 1.0f);
        builder.box(-3.2f, -10.3f, -1.6f, -2.0f, -8.6f, -1.4f, FUR[0], FUR[1], FUR[2], 1.0f);
        builder.box(2.0f, -10.3f, -1.6f, 3.2f, -8.6f, -1.4f, FUR[0], FUR[1], FUR[2], 1.0f);

        // Fur ruff around the cheeks and crown
        builder.box(-4.45f, -7.6f, -4.4f, 4.45f, -5.6f, 4.4f, FUR[0], FUR[1], FUR[2], 0.95f);
        builder.box(-4.2f, -9.0f, -4.2f, 4.2f, -8.2f, 4.2f, FUR_DARK[0], FUR_DARK[1], FUR_DARK[2], 0.9f);

        return builder.bake();
    }

    private BakedAccessoryModel buildChest() {
        BakedAccessoryModel.Builder builder = new BakedAccessoryModel.Builder();

        // Shaggy chest fur slab
        builder.box(-4.35f, 0.0f, -2.65f, 4.35f, 6.8f, -2.1f, FUR[0], FUR[1], FUR[2], 0.96f);
        // Fur strands catching the light
        builder.box(-2.9f, 0.4f, -2.75f, -2.1f, 6.0f, -2.65f, FUR_LIGHT[0], FUR_LIGHT[1], FUR_LIGHT[2], 1.0f);
        builder.box(0.6f, 0.6f, -2.75f, 1.4f, 5.4f, -2.65f, FUR_LIGHT[0], FUR_LIGHT[1], FUR_LIGHT[2], 1.0f);
        builder.box(-0.9f, 1.2f, -2.75f, -0.2f, 6.4f, -2.65f, FUR_DARK[0], FUR_DARK[1], FUR_DARK[2], 1.0f);
        // Shoulder fur caps
        builder.box(-4.5f, -0.4f, -2.4f, -2.2f, 1.4f, 2.4f, FUR_DARK[0], FUR_DARK[1], FUR_DARK[2], 0.95f);
        builder.box(2.2f, -0.4f, -2.4f, 4.5f, 1.4f, 2.4f, FUR_DARK[0], FUR_DARK[1], FUR_DARK[2], 0.95f);

        return builder.bake();
    }
}
