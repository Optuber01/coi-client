package dev.ua.ikeepcalm.coi.client.appearance;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;

/**
 * The feminine chest profiles shared by the body-shape traits (demoness / mother / moon /
 * chaos primogenitor). The bump is a grid of lifted quads sampling the player's own skin,
 * drawn on the torso front plane and repeated on the jacket layer; see
 * {@code FemaleTraitsRenderer}'s original geometry, which this parameterizes.
 */
public final class FeminineBodyGeometry {

    /**
     * Chest scale knobs: width spread, vertical extent, and projection depth.
     * Identity (1, 1, 1) matches the original Demoness shape.
     */
    public record ChestProfile(float xScale, float yScale, float depthScale) {

        public static final ChestProfile DEMONESS = new ChestProfile(1.00f, 1.00f, 1.00f);
        public static final ChestProfile MOTHER = new ChestProfile(1.08f, 1.06f, 1.20f);
        public static final ChestProfile MOON = new ChestProfile(0.96f, 0.96f, 0.90f);
        public static final ChestProfile CHAOS = new ChestProfile(1.00f, 0.95f, 0.95f);
    }

    private static final float TEXTURE_SIZE = 64.0f;

    private static final float[] CHEST_X = {-4.0f, -2.75f, -1.25f, 0.0f, 1.25f, 2.75f, 4.0f};
    private static final float[] CHEST_X_LIFT = {0.0f, 0.68f, 0.96f, 1.0f, 0.96f, 0.68f, 0.0f};
    private static final float[] CHEST_Y = {1.25f, 2.75f, 4.5f, 6.25f, 8.0f};
    private static final float[] CHEST_Y_LIFT = {0.0f, 0.62f, 1.0f, 0.65f, 0.0f};
    private static final float CHEST_DEPTH = 1.35f;

    private FeminineBodyGeometry() {
    }

    /**
     * Draws the chest bump for one layer. Callers wrap this in
     * {@code model.body.translateAndRotate(poseStack)} and skip entirely when chest
     * armor or an elytra covers the torso.
     */
    public static void drawChest(PoseStack.Pose pose, VertexConsumer consumer,
                                 AvatarRenderState state, ChestProfile profile, boolean jacketLayer) {
        float front = jacketLayer ? -2.27f : -2.01f;
        float vOffset = jacketLayer ? 36.0f : 20.0f;
        float depth = CHEST_DEPTH * profile.depthScale();

        for (int yIndex = 0; yIndex < CHEST_Y.length - 1; yIndex++) {
            for (int xIndex = 0; xIndex < CHEST_X.length - 1; xIndex++) {
                float centerX = (CHEST_X[xIndex] + CHEST_X[xIndex + 1]) * 0.5f;
                float scaledHalfWidth = (CHEST_X[xIndex + 1] - CHEST_X[xIndex]) * 0.5f * profile.xScale();
                float centerY = (CHEST_Y[yIndex] + CHEST_Y[yIndex + 1]) * 0.5f;
                float scaledHalfHeight = (CHEST_Y[yIndex + 1] - CHEST_Y[yIndex]) * 0.5f * profile.yScale();

                SkinVertex topLeft = chestVertex(centerX - scaledHalfWidth, centerY - scaledHalfHeight,
                        front, vOffset, depth, profile);
                SkinVertex topRight = chestVertex(centerX + scaledHalfWidth, centerY - scaledHalfHeight,
                        front, vOffset, depth, profile);
                SkinVertex bottomLeft = chestVertex(centerX - scaledHalfWidth, centerY + scaledHalfHeight,
                        front, vOffset, depth, profile);
                SkinVertex bottomRight = chestVertex(centerX + scaledHalfWidth, centerY + scaledHalfHeight,
                        front, vOffset, depth, profile);

                BodyBoxes.texturedTriangle(pose, consumer,
                        topRight.x(), topRight.y(), topRight.z(), topRight.u(), topRight.v(),
                        topLeft.x(), topLeft.y(), topLeft.z(), topLeft.u(), topLeft.v(),
                        bottomLeft.x(), bottomLeft.y(), bottomLeft.z(), bottomLeft.u(), bottomLeft.v(),
                        state.lightCoords);
                BodyBoxes.texturedTriangle(pose, consumer,
                        topRight.x(), topRight.y(), topRight.z(), topRight.u(), topRight.v(),
                        bottomLeft.x(), bottomLeft.y(), bottomLeft.z(), bottomLeft.u(), bottomLeft.v(),
                        bottomRight.x(), bottomRight.y(), bottomRight.z(), bottomRight.u(), bottomRight.v(),
                        state.lightCoords);
            }
        }
    }

    private static SkinVertex chestVertex(float x, float y, float front, float vOffset, float depth, ChestProfile profile) {
        float xIndex = Math.clamp((x + 4.0f) / 8.0f * (CHEST_X.length - 1), 0.0f, CHEST_X.length - 1);
        float yIndex = Math.clamp(y / 8.0f * (CHEST_Y.length - 1), 0.0f, CHEST_Y.length - 1);
        float lift = sampleLift(CHEST_X_LIFT, xIndex) * sampleLift(CHEST_Y_LIFT, yIndex);
        float z = front - depth * lift;
        return new SkinVertex(
                x / 16.0f,
                y / 16.0f,
                z / 16.0f,
                (24.0f + x) / TEXTURE_SIZE,
                (vOffset + y) / TEXTURE_SIZE
        );
    }

    private static float sampleLift(float[] table, float index) {
        int low = (int) index;
        if (low >= table.length - 1) {
            return table[table.length - 1];
        }
        float fraction = index - low;
        return table[low] + (table[low + 1] - table[low]) * fraction;
    }

    private record SkinVertex(float x, float y, float z, float u, float v) {
    }
}
