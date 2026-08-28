package dev.ua.ikeepcalm.coi.client.appearance.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.ua.ikeepcalm.coi.client.appearance.AppearanceTraitRenderer;
import dev.ua.ikeepcalm.coi.client.appearance.BodyBoxes;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;

/**
 * Base for the eye-color traits: a translucent iris pair drawn just in front of the face
 * plane, covering the vanilla eye pixels. Fullbright variants ignore scene lighting so
 * they read as luminous in the dark.
 */
public abstract class AbstractEyesRenderer implements AppearanceTraitRenderer {

    private static final float FRONT_Z = -4.10f;
    private static final float IRIS_HALF_WIDTH = 0.80f;
    private static final float IRIS_CENTER_X = 2.00f;
    private static final float IRIS_TOP = -4.20f;
    private static final float IRIS_BOTTOM = -2.90f;

    private final boolean fullbright;

    protected AbstractEyesRenderer(boolean fullbright) {
        this.fullbright = fullbright;
    }

    /** RGB of the iris fill; the slit (when present) is drawn darker on top. */
    protected abstract float[] irisColor();

    protected abstract float[] slitColor();

    protected abstract float irisAlpha();

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, AvatarRenderState state, PlayerModel model) {
        float[] color = irisColor();
        float[] slit = slitColor();
        float alpha = irisAlpha();
        int light = fullbright ? 0xF000F0 : state.lightCoords;

        poseStack.pushPose();
        model.head.translateAndRotate(poseStack);
        collector.order(0).submitCustomGeometry(
                poseStack,
                BodyBoxes.tintType(),
                (pose, consumer) -> {
                    drawIris(pose, consumer, -IRIS_CENTER_X, IRIS_HALF_WIDTH, color, alpha, light);
                    drawIris(pose, consumer, IRIS_CENTER_X, IRIS_HALF_WIDTH, color, alpha, light);
                    if (slit != null) {
                        // Vertical slit pupils, dragon style
                        drawIris(pose, consumer, -IRIS_CENTER_X, 0.30f, slit, 1.0f, light);
                        drawIris(pose, consumer, IRIS_CENTER_X, 0.30f, slit, 1.0f, light);
                    }
                }
        );
        poseStack.popPose();
    }

    private void drawIris(PoseStack.Pose pose, com.mojang.blaze3d.vertex.VertexConsumer consumer,
                          float centerX, float halfWidth, float[] color, float alpha, int light) {
        BodyBoxes.quad(pose, consumer,
                centerX - IRIS_HALF_WIDTH, IRIS_TOP, FRONT_Z,
                centerX + IRIS_HALF_WIDTH, IRIS_TOP, FRONT_Z,
                centerX + IRIS_HALF_WIDTH, IRIS_BOTTOM, FRONT_Z,
                centerX - IRIS_HALF_WIDTH, IRIS_BOTTOM, FRONT_Z,
                color[0], color[1], color[2], alpha, light);
    }

    /** Convenience factory for the plain single-color eye traits. */
    protected static float[] solid(float r, float g, float b) {
        return new float[]{r, g, b};
    }
}
