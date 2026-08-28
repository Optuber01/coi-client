package dev.ua.ikeepcalm.coi.client.appearance;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.ua.ikeepcalm.coi.client.mcf.Coi3dPrimitives;

/**
 * A static mesh of vertex-colored quads, baked once and re-rendered every frame — the
 * accessoire equivalent of a baked Blockbench model. Geometry is authored in part-local
 * <em>pixels</em> (16 px = 1 block, +Y down, matching model-part space) via the builder,
 * so hair, wings and similar fixed 3D cosmetics are just data, not per-frame math.
 *
 * <p>Rendered against the blank {@code white.png} through a translucent render type;
 * all shading comes from the vertex colors, which lets one mesh carry layered tints
 * (base/shadow/highlight) without any texture.</p>
 */
public final class BakedAccessoryModel implements Coi3dPrimitives {

    /**
     * Stride per vertex: x, y, z, r, g, b, a, nx, ny, nz. UVs are unused (blank texture).
     */
    private static final int STRIDE = 10;

    private final float[] vertices;

    private BakedAccessoryModel(float[] vertices) {
        this.vertices = vertices;
    }

    public void render(PoseStack.Pose pose, VertexConsumer consumer, int light) {
        for (int offset = 0; offset + STRIDE <= vertices.length; offset += STRIDE) {
            addVertex(pose, consumer,
                    vertices[offset], vertices[offset + 1], vertices[offset + 2],
                    vertices[offset + 3], vertices[offset + 4], vertices[offset + 5], vertices[offset + 6],
                    0.5f, 0.5f,
                    vertices[offset + 7], vertices[offset + 8], vertices[offset + 9],
                    light);
        }
    }

    public static final class Builder {

        private float[] vertices = new float[64];
        private int size;

        private void vertex(float x, float y, float z, float r, float g, float b, float a, float nx, float ny, float nz) {
            if (size + STRIDE > vertices.length) {
                float[] grown = new float[Math.max(vertices.length * 2, size + STRIDE)];
                System.arraycopy(vertices, 0, grown, 0, size);
                vertices = grown;
            }
            vertices[size] = x;
            vertices[size + 1] = y;
            vertices[size + 2] = z;
            vertices[size + 3] = r;
            vertices[size + 4] = g;
            vertices[size + 5] = b;
            vertices[size + 6] = a;
            vertices[size + 7] = nx;
            vertices[size + 8] = ny;
            vertices[size + 9] = nz;
            size += STRIDE;
        }

        private void quad(float[] p, float r, float g, float b, float a, boolean doubleSided) {
            float[] normal = faceNormal(p[0], p[1], p[2], p[3], p[4], p[5], p[6], p[7], p[8]);
            for (int index : new int[]{0, 3, 6, 9}) {
                vertex(p[index], p[index + 1], p[index + 2], r, g, b, a, normal[0], normal[1], normal[2]);
            }
            if (doubleSided) {
                for (int index : new int[]{9, 6, 3, 0}) {
                    vertex(p[index], p[index + 1], p[index + 2], r, g, b, a, -normal[0], -normal[1], -normal[2]);
                }
            }
        }

        /** Axis-aligned box; the -X face is skipped (mirrors sit on the body). */
        public Builder box(float minX, float minY, float minZ, float maxX, float maxY, float maxZ,
                           float r, float g, float b, float a) {
            // Front (-Z), back (+Z), top (-Y), bottom (+Y), right (+X) — left skipped
            quad(new float[]{minX, minY, minZ, maxX, minY, minZ, maxX, maxY, minZ, minX, maxY, minZ}, r, g, b, a, false);
            quad(new float[]{minX, minY, maxZ, minX, maxY, maxZ, maxX, maxY, maxZ, maxX, minY, maxZ}, r, g, b, a, false);
            quad(new float[]{minX, minY, minZ, minX, minY, maxZ, maxX, minY, maxZ, maxX, minY, minZ}, r, g, b, a, false);
            quad(new float[]{minX, maxY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, minX, maxY, maxZ}, r, g, b, a, false);
            quad(new float[]{maxX, minY, minZ, maxX, minY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ}, r, g, b, a, false);
            return this;
        }

        /** Six-face box for standalone shapes that must read from every angle. */
        public Builder solidBox(float minX, float minY, float minZ, float maxX, float maxY, float maxZ,
                                float r, float g, float b, float a) {
            box(minX, minY, minZ, maxX, maxY, maxZ, r, g, b, a);
            quad(new float[]{minX, minY, minZ, minX, maxY, minZ, minX, maxY, maxZ, minX, minY, maxZ}, r, g, b, a, false);
            return this;
        }

        /** Flat quad, 12 floats = 4 corner xyz. Winding defines the front. */
        public Builder quad(float x0, float y0, float z0, float x1, float y1, float z1,
                            float x2, float y2, float z2, float x3, float y3, float z3,
                            float r, float g, float b, float a, boolean doubleSided) {
            quad(new float[]{x0, y0, z0, x1, y1, z1, x2, y2, z2, x3, y3, z3}, r, g, b, a, doubleSided);
            return this;
        }

        /** Triangle, the fourth vertex duplicating the third. */
        public Builder triangle(float x0, float y0, float z0, float x1, float y1, float z1, float x2, float y2, float z2,
                                float r, float g, float b, float a, boolean doubleSided) {
            quad(new float[]{x0, y0, z0, x1, y1, z1, x2, y2, z2, x2, y2, z2}, r, g, b, a, doubleSided);
            return this;
        }

        public BakedAccessoryModel bake() {
            float[] baked = new float[size];
            System.arraycopy(vertices, 0, baked, 0, size);
            return new BakedAccessoryModel(baked);
        }

        private static float[] faceNormal(float x0, float y0, float z0, float x1, float y1, float z1, float x2, float y2, float z2) {
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
    }
}
