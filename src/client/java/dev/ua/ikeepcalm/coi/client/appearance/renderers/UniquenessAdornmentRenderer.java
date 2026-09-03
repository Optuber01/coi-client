package dev.ua.ikeepcalm.coi.client.appearance.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.ua.ikeepcalm.coi.client.appearance.TraitGeometry;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;

/** Small, full-bright model accents that make selected uniqueness pathways readable without particles. */
public final class UniquenessAdornmentRenderer {

    private static final TraitGeometry G = TraitGeometry.INSTANCE;
    private static final TraitGeometry.Tint GOLD = tint(0.95f, 0.67f, 0.16f, 0.92f);
    private static final TraitGeometry.Tint BRONZE = tint(0.58f, 0.30f, 0.10f, 0.95f);
    private static final TraitGeometry.Tint SUN = tint(1.0f, 0.83f, 0.27f, 0.72f);
    private static final TraitGeometry.Tint SHADOW = tint(0.08f, 0.03f, 0.15f, 0.76f);
    private static final TraitGeometry.Tint RED = tint(0.75f, 0.02f, 0.06f, 0.88f);
    private static final TraitGeometry.Tint SILVER = tint(0.78f, 0.88f, 1.0f, 0.82f);

    private UniquenessAdornmentRenderer() {
    }

    public static void submit(String pathway, PoseStack stack, SubmitNodeCollector collector,
                              AvatarRenderState state, PlayerModel model) {
        if (switch (pathway) {
            case "error", "tower", "sun", "darkness", "justiciar", "hanged", "moon", "fortune" -> true;
            default -> false;
        }) {
            stack.pushPose();
            model.head.translateAndRotate(stack);
            collector.order(4).submitCustomGeometry(stack, TraitRenderSupport.TRANSLUCENT,
                    (pose, consumer) -> drawHeadAdornment(pathway, pose, consumer));
            stack.popPose();
        }
        if ("emperor".equals(pathway)) {
            stack.pushPose();
            model.body.translateAndRotate(stack);
            collector.order(4).submitCustomGeometry(stack, TraitRenderSupport.TRANSLUCENT,
                    UniquenessAdornmentRenderer::drawEmperorBrooch);
            stack.popPose();
        }
    }

    private static void drawHeadAdornment(String pathway, PoseStack.Pose pose, VertexConsumer consumer) {
        switch (pathway) {
            case "error" -> drawMonocle(pose, consumer);
            case "tower" -> drawThirdEye(pose, consumer);
            case "sun" -> drawHalo(pose, consumer, SUN, 5.4f, -9.8f);
            case "darkness" -> drawHalo(pose, consumer, SHADOW, 5.8f, -9.4f);
            case "justiciar" -> drawHalo(pose, consumer, GOLD, 5.1f, -9.2f);
            case "hanged" -> drawCross(pose, consumer);
            case "moon" -> drawMoon(pose, consumer);
            case "fortune" -> drawInfinity(pose, consumer);
            default -> { }
        }
    }

    private static void drawMonocle(PoseStack.Pose pose, VertexConsumer consumer) {
        drawRing(pose, consumer, -2.0f, -4.0f, -4.35f, 1.55f, GOLD);
        G.boxPixels(pose, consumer, -0.62f, -3.92f, -4.45f, -0.42f, 1.8f, -4.18f,
                GOLD, TraitRenderSupport.FULL_BRIGHT);
    }

    private static void drawThirdEye(PoseStack.Pose pose, VertexConsumer consumer) {
        G.boxPixels(pose, consumer, -1.25f, -6.65f, -4.38f, 1.25f, -5.15f, -4.10f,
                BRONZE, TraitRenderSupport.FULL_BRIGHT);
        G.boxPixels(pose, consumer, -0.40f, -6.45f, -4.55f, 0.40f, -5.35f, -4.28f,
                SILVER, TraitRenderSupport.FULL_BRIGHT);
    }

    private static void drawHalo(PoseStack.Pose pose, VertexConsumer consumer,
                                 TraitGeometry.Tint color, float radius, float y) {
        for (int segment = 0; segment < 16; segment++) {
            double angle = segment * Math.PI * 2.0 / 16.0;
            float x = (float) Math.cos(angle) * radius;
            float z = (float) Math.sin(angle) * radius;
            G.boxPixels(pose, consumer, x - 0.48f, y - 0.18f, z - 0.48f,
                    x + 0.48f, y + 0.18f, z + 0.48f, color, TraitRenderSupport.FULL_BRIGHT);
        }
    }

    private static void drawCross(PoseStack.Pose pose, VertexConsumer consumer) {
        G.boxPixels(pose, consumer, -0.38f, -13.4f, -0.30f, 0.38f, -8.8f, 0.30f,
                RED, TraitRenderSupport.FULL_BRIGHT);
        G.boxPixels(pose, consumer, -2.0f, -12.0f, -0.30f, 2.0f, -11.25f, 0.30f,
                RED, TraitRenderSupport.FULL_BRIGHT);
    }

    private static void drawMoon(PoseStack.Pose pose, VertexConsumer consumer) {
        for (int y = -14; y <= -10; y++) {
            float half = 2.5f - Math.abs(y + 12) * 0.55f;
            G.boxPixels(pose, consumer, -half, y, 2.4f, half, y + 0.8f, 2.9f,
                    RED, TraitRenderSupport.FULL_BRIGHT);
        }
    }

    private static void drawInfinity(PoseStack.Pose pose, VertexConsumer consumer) {
        drawRing(pose, consumer, -1.65f, -11.1f, 1.8f, 1.65f, SILVER);
        drawRing(pose, consumer, 1.65f, -11.1f, 1.8f, 1.65f, SILVER);
    }

    private static void drawRing(PoseStack.Pose pose, VertexConsumer consumer, float cx, float cy,
                                 float z, float radius, TraitGeometry.Tint color) {
        for (int segment = 0; segment < 12; segment++) {
            double angle = segment * Math.PI * 2.0 / 12.0;
            float x = cx + (float) Math.cos(angle) * radius;
            float y = cy + (float) Math.sin(angle) * radius;
            G.boxPixels(pose, consumer, x - 0.32f, y - 0.32f, z - 0.16f,
                    x + 0.32f, y + 0.32f, z + 0.16f, color, TraitRenderSupport.FULL_BRIGHT);
        }
    }

    private static void drawEmperorBrooch(PoseStack.Pose pose, VertexConsumer consumer) {
        G.boxPixels(pose, consumer, 1.55f, 2.0f, -2.35f, 3.15f, 3.6f, -2.05f,
                GOLD, TraitRenderSupport.FULL_BRIGHT);
        G.boxPixels(pose, consumer, 2.18f, 3.45f, -2.28f, 2.52f, 7.4f, -2.08f,
                GOLD, TraitRenderSupport.FULL_BRIGHT);
        G.boxPixels(pose, consumer, 2.18f, 7.0f, -2.28f, 3.6f, 7.35f, -2.08f,
                GOLD, TraitRenderSupport.FULL_BRIGHT);
    }

    private static TraitGeometry.Tint tint(float r, float g, float b, float a) {
        return new TraitGeometry.Tint(r, g, b, a);
    }
}
