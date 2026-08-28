package dev.ua.ikeepcalm.coi.client.appearance.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import dev.ua.ikeepcalm.coi.client.appearance.AppearanceTraitRenderer;
import dev.ua.ikeepcalm.coi.client.appearance.BakedAccessoryModel;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Items;
import org.joml.Quaternionf;

/**
 * Shared anatomy for the wing traits: each wing is a baked mesh (arm bone, membrane
 * panels with a scalloped trailing edge, finger spurs) rooted at the shoulder blades in
 * torso space. An idle flap rotates each wing around its root — time-driven so it also
 * animates for other players. Wings fold away under an elytra, which owns the back.
 */
public abstract class AbstractWingsRenderer implements AppearanceTraitRenderer {

    private static final Identifier WHITE_TEXTURE =
            Identifier.fromNamespaceAndPath("coi-client", "textures/entity/white.png");
    private static final RenderType RENDER_TYPE = RenderTypes.entityTranslucent(WHITE_TEXTURE);

    private static final float PIXEL = 1.0f / 16.0f;
    private static final float ROOT_X = 1.7f;
    private static final float ROOT_Y = 1.1f;
    private static final float ROOT_Z = 2.0f;

    private final BakedAccessoryModel rightWing;
    private final BakedAccessoryModel leftWing;

    protected AbstractWingsRenderer() {
        this.rightWing = buildWing(1.0f);
        this.leftWing = buildWing(-1.0f);
    }

    /** Membrane color + alpha; bones are drawn darker and opaque. */
    protected abstract float[] membraneColor();

    protected abstract float membraneAlpha();

    protected abstract float[] boneColor();

    /** Wing span scale: 1.0 = ~11 px (0.7 block) of wing per side. */
    protected abstract float spanScale();

    /** Flap angular speed multiplier; bigger = buzzier. */
    protected float flapSpeed() {
        return 1.0f;
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, AvatarRenderState state, PlayerModel model) {
        if (!state.chestEquipment.isEmpty() && state.chestEquipment.is(Items.ELYTRA)) {
            return;
        }

        long now = System.currentTimeMillis();
        float flap = (float) Math.sin(now * 0.0021f * flapSpeed()) * 0.24f + 0.05f;

        poseStack.pushPose();
        model.body.translateAndRotate(poseStack);
        collector.order(0).submitCustomGeometry(
                poseStack,
                RENDER_TYPE,
                (pose, consumer) -> {
                    renderWing(pose, consumer, rightWing, flap, 1.0f, state.lightCoords);
                    renderWing(pose, consumer, leftWing, flap, -1.0f, state.lightCoords);
                }
        );
        poseStack.popPose();
    }

    private void renderWing(Pose basePose, com.mojang.blaze3d.vertex.VertexConsumer consumer,
                            BakedAccessoryModel wing, float flap, float side, int light) {
        // A throwaway pose chained off the submitted one gives each wing its own flap
        // pivot (the wing root) without disturbing the other wing or the body.
        PoseStack local = new PoseStack();
        local.mulPose(basePose.pose());
        local.translate(ROOT_X * side * PIXEL, ROOT_Y * PIXEL, ROOT_Z * PIXEL);
        local.mulPose(new Quaternionf().rotationZ(-flap * side));
        local.translate(-ROOT_X * side * PIXEL, -ROOT_Y * PIXEL, -ROOT_Z * PIXEL);
        wing.render(local.last(), consumer, light);
    }

    /**
     * Builds one wing; {@code side} of 1 is the right wing (extending +X), -1 mirrors it
     * into the left. All coordinates are torso-local pixels.
     */
    private BakedAccessoryModel buildWing(float side) {
        float scale = spanScale();
        float[] membrane = membraneColor();
        float[] bone = boneColor();
        float membraneAlpha = membraneAlpha();

        BakedAccessoryModel.Builder builder = new BakedAccessoryModel.Builder();
        float x = side;

        // Arm bone from the shoulder blade outward
        builder.box(Math.min(x * 1.2f, x * 11.6f * scale), 0.85f, 1.85f,
                Math.max(x * 1.2f, x * 11.6f * scale), 1.95f, 2.45f,
                bone[0], bone[1], bone[2], 1.0f);

        float z = 2.05f;
        float rootX = x * 1.5f;
        float tipX = x * 11.2f * scale;
        float bottom1 = 7.6f * scale;
        float bottom2 = 9.4f * scale;
        float bottom3 = 8.2f * scale;

        // Main membrane with a scalloped trailing edge (double-sided)
        builder.quad(rootX, 1.0f, z, tipX, 1.5f, z, x * 10.2f * scale, bottom1, z, rootX, 6.2f, z,
                membrane[0], membrane[1], membrane[2], membraneAlpha, true);
        builder.quad(rootX, 6.2f, z, x * 10.2f * scale, bottom1, z, x * 8.0f * scale, bottom2, z, x * 4.6f * scale, 6.8f, z,
                membrane[0], membrane[1], membrane[2], membraneAlpha * 0.92f, true);
        builder.quad(rootX, 6.2f, z, x * 4.6f * scale, 6.8f, z, x * 2.6f * scale, bottom3, z, x * 0.9f, 5.6f, z,
                membrane[0], membrane[1], membrane[2], membraneAlpha * 0.85f, true);

        // Finger spurs pressing the membrane taut
        float spurR = Math.min(1.0f, bone[0] * 1.4f + 0.15f);
        float spurG = Math.min(1.0f, bone[1] * 1.4f + 0.15f);
        float spurB = Math.min(1.0f, bone[2] * 1.4f + 0.15f);
        builder.quad(x * 5.6f * scale, 1.35f, z + 0.06f, x * 6.0f * scale, 1.35f, z + 0.06f,
                x * 10.3f * scale, bottom1 + 0.15f, z + 0.06f, x * 9.9f * scale, bottom1 + 0.15f, z + 0.06f,
                spurR, spurG, spurB, 1.0f, true);
        builder.quad(x * 4.0f * scale, 1.3f, z + 0.06f, x * 4.4f * scale, 1.3f, z + 0.06f,
                x * 8.1f * scale, bottom2 + 0.15f, z + 0.06f, x * 7.7f * scale, bottom2 + 0.15f, z + 0.06f,
                spurR, spurG, spurB, 1.0f, true);

        return builder.bake();
    }
}
