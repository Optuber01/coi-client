package dev.ua.ikeepcalm.coi.client.appearance;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.client.renderer.texture.OverlayTexture;

/**
 * Shared pixel-based box geometry for body overlays. All coordinates are part-local
 * pixels (16 px = 1 block; +Y points down, matching model-part space), so callers work
 * in the same units as vanilla skin dimensions: head y[-8, 0], torso y[0, 12] z[-2, 2],
 * arms/legs 4 px wide.
 *
 * <p>Overlay "layers" (jacket, sleeves, pants, hat) mirror the vanilla skin layout by
 * drawing a slightly inflated copy over a shifted UV region.</p>
 */
public final class BodyBoxes {

    public static final float PIXEL = 1.0f / 16.0f;
    private static final float TEXTURE_SIZE = 64.0f;

    private static final Identifier WHITE_TEXTURE =
            Identifier.fromNamespaceAndPath("coi-client", "textures/entity/white.png");

    private BodyBoxes() {
    }

    /** Vertex-colored translucent surface for tinted overlays (scales, skins, chitin...). */
    public static RenderType tintType() {
        return RenderTypes.entityTranslucent(WHITE_TEXTURE);
    }

    /** The player's own skin, cutout — for overlays that should sample skin pixels. */
    public static RenderType skinType(AvatarRenderState state) {
        return RenderTypes.entityCutout(state.skin.body().texturePath());
    }

    /**
     * Axis-aligned box in part-local pixel space, vertex colored. Back face is skipped
     * so front overlays don't fight the body they sit on.
     */
    public static void box(PoseStack.Pose pose, VertexConsumer consumer,
                           float minX, float minY, float minZ, float maxX, float maxY, float maxZ,
                           float r, float g, float b, float a, int light) {
        float x0 = minX * PIXEL, y0 = minY * PIXEL, z0 = minZ * PIXEL;
        float x1 = maxX * PIXEL, y1 = maxY * PIXEL, z1 = maxZ * PIXEL;
        // Front (-Z), back (+Z), top (-Y), bottom (+Y), right (+X) — left skipped
        quad(pose, consumer, x0, y0, z0, x1, y0, z0, x1, y1, z0, x0, y1, z0, r, g, b, a, light);
        quad(pose, consumer, x0, y0, z1, x0, y1, z1, x1, y1, z1, x1, y0, z1, r, g, b, a, light);
        quad(pose, consumer, x0, y0, z0, x0, y0, z1, x1, y0, z1, x1, y0, z0, r, g, b, a, light);
        quad(pose, consumer, x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1, r, g, b, a, light);
        quad(pose, consumer, x1, y0, z0, x1, y0, z1, x1, y1, z1, x1, y1, z0, r, g, b, a, light);
    }

    /** Full six-face box (for silhouettes seen from any angle, e.g. hair back strands). */
    public static void solidBox(PoseStack.Pose pose, VertexConsumer consumer,
                                float minX, float minY, float minZ, float maxX, float maxY, float maxZ,
                                float r, float g, float b, float a, int light) {
        box(pose, consumer, minX, minY, minZ, maxX, maxY, maxZ, r, g, b, a, light);
        float x0 = minX * PIXEL, y0 = minY * PIXEL, z0 = minZ * PIXEL;
        float x1 = maxX * PIXEL, y1 = maxY * PIXEL, z1 = maxZ * PIXEL;
        quad(pose, consumer, x0, y0, z0, x0, y1, z0, x0, y1, z1, x0, y0, z1, r, g, b, a, light);
    }

    /**
     * Flat quad in part-local pixel space with per-vertex colors. Translucent quads are
     * emitted double-sided so membranes read from both sides.
     */
    public static void quad(PoseStack.Pose pose, VertexConsumer consumer,
                            float x0, float y0, float z0,
                            float x1, float y1, float z1,
                            float x2, float y2, float z2,
                            float x3, float y3, float z3,
                            float r, float g, float b, float a, int light) {
        float[] normal = faceNormal(x0 * PIXEL, y0 * PIXEL, z0 * PIXEL,
                x1 * PIXEL, y1 * PIXEL, z1 * PIXEL, x2 * PIXEL, y2 * PIXEL, z2 * PIXEL);
        float[][] corners = {{x0, y0, z0}, {x1, y1, z1}, {x2, y2, z2}, {x3, y3, z3}};
        for (float[] v : corners) {
            vertex(pose, consumer, v[0] * PIXEL, v[1] * PIXEL, v[2] * PIXEL,
                    r, g, b, a, 0.5f, 0.5f, normal[0], normal[1], normal[2], light);
        }
        if (a < 1.0f) {
            for (int index = corners.length - 1; index >= 0; index--) {
                float[] v = corners[index];
                vertex(pose, consumer, v[0] * PIXEL, v[1] * PIXEL, v[2] * PIXEL,
                        r, g, b, a, 0.5f, 0.5f, -normal[0], -normal[1], -normal[2], light);
            }
        }
    }

    /** Textured triangle for skin-sampled overlays (fourth vertex duplicated). */
    public static void texturedTriangle(PoseStack.Pose pose, VertexConsumer consumer,
                                        float x0, float y0, float z0, float u0, float v0,
                                        float x1, float y1, float z1, float u1, float v1,
                                        float x2, float y2, float z2, float u2, float v2,
                                        int light) {
        float[] normal = faceNormal(x0, y0, z0, x1, y1, z1, x2, y2, z2);
        float[][] data = {
                {x0, y0, z0, u0, v0},
                {x1, y1, z1, u1, v1},
                {x2, y2, z2, u2, v2},
                {x2, y2, z2, u2, v2}};
        for (float[] v : data) {
            vertex(pose, consumer, v[0], v[1], v[2], 1, 1, 1, 1, v[3], v[4],
                    normal[0], normal[1], normal[2], light);
        }
    }

    /** Textured quad for skin-sampled overlays. */
    public static void texturedQuad(PoseStack.Pose pose, VertexConsumer consumer,
                                    float x0, float y0, float z0, float u0, float v0,
                                    float x1, float y1, float z1, float u1, float v1,
                                    float x2, float y2, float z2, float u2, float v2,
                                    float x3, float y3, float z3, float u3, float v3,
                                    int light) {
        float[] normal = faceNormal(x0, y0, z0, x1, y1, z1, x2, y2, z2);
        float[][] data = {
                {x0, y0, z0, u0, v0},
                {x1, y1, z1, u1, v1},
                {x2, y2, z2, u2, v2},
                {x3, y3, z3, u3, v3}};
        for (float[] v : data) {
            vertex(pose, consumer, v[0], v[1], v[2], 1, 1, 1, 1, v[3], v[4],
                    normal[0], normal[1], normal[2], light);
        }
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer consumer,
                               float x, float y, float z,
                               float r, float g, float b, float a,
                               float u, float v,
                               float nx, float ny, float nz, int light) {
        consumer.addVertex(pose, x, y, z)
                .setColor(r, g, b, a)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, nx, ny, nz);
    }

    private static float[] faceNormal(float x0, float y0, float z0,
                                      float x1, float y1, float z1,
                                      float x2, float y2, float z2) {
        float ux = x1 - x0, uy = y1 - y0, uz = z1 - z0;
        float vx = x2 - x0, vy = y2 - y0, vz = z2 - z0;
        float nx = uy * vz - uz * vy;
        float ny = uz * vx - ux * vz;
        float nz = ux * vy - uy * vx;
        float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (len > 1.0e-5f) {
            nx /= len;
            ny /= len;
            nz /= len;
        }
        return new float[]{nx, ny, nz};
    }

    /**
     * Flat rectangle on the front plane of a body part, sampling the player's own skin
     * so overlays read as body rather than paint. {@code uOrigin}/{@code vOrigin} are the
     * texture coordinates of the face region's top-left corner (see the part helpers).
     */
    public static void skinFrontQuad(PoseStack.Pose pose, VertexConsumer consumer,
                                     float minX, float maxX, float minY, float maxY, float z,
                                     float uOrigin, float vOrigin, int light) {
        float u0 = (uOrigin + minX) / TEXTURE_SIZE;
        float u1 = (uOrigin + maxX) / TEXTURE_SIZE;
        float v0 = (vOrigin + minY) / TEXTURE_SIZE;
        float v1 = (vOrigin + maxY) / TEXTURE_SIZE;
        texturedQuad(
                pose, consumer,
                minX * PIXEL, minY * PIXEL, z * PIXEL, u0, v0,
                maxX * PIXEL, minY * PIXEL, z * PIXEL, u1, v0,
                maxX * PIXEL, maxY * PIXEL, z * PIXEL, u1, v1,
                minX * PIXEL, maxY * PIXEL, z * PIXEL, u0, v1,
                light
        );
    }

    /**
     * Torso-front rectangle sampling the skin torso or jacket region. Torso-local pixels:
     * x[-4, 4], y[0 = neck, 12 = waist]. Face region is u 20-28, v 20-32 (jacket v 36-48).
     */
    public static void torsoFrontQuad(PoseStack.Pose pose, VertexConsumer consumer,
                                      float minX, float maxX, float minY, float maxY, float z,
                                      boolean jacketLayer, int light) {
        skinFrontQuad(pose, consumer, minX, maxX, minY, maxY, z,
                20.0f, jacketLayer ? 36.0f : 20.0f, light);
    }

    /**
     * Head-front rectangle sampling the skin face or hat region. Head-local pixels:
     * x[-4, 4], y[-8 = crown, 0 = chin]. Face region is u 8-16, v 8-16 (hat u 40-48).
     */
    public static void headFrontQuad(PoseStack.Pose pose, VertexConsumer consumer,
                                     float minX, float maxX, float minY, float maxY, float z,
                                     boolean hatLayer, int light) {
        skinFrontQuad(pose, consumer, minX, maxX, minY, maxY, z,
                hatLayer ? 40.0f : 8.0f, 16.0f, light);
    }
}
