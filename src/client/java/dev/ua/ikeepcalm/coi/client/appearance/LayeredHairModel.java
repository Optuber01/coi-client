package dev.ua.ikeepcalm.coi.client.appearance;

/**
 * Builds the fixed 3D hair models as baked meshes, in head-local pixel space
 * (crown at y=-8, chin at y=0, face at z=-4). Short styles draw cap/sides/back/fringe;
 * long styles add hanging back strands. Layered tints (shadow under the cap edge,
 * highlight streaks) give the mesh depth without a texture.
 *
 * <p>The hat-layer variant is inflated slightly and renders in the same pass as the
 * base so players with the hat layer enabled don't see hair clip through it.</p>
 */
public final class LayeredHairModel {

    public enum Style {
        SHORT,
        LONG
    }

    public record Palette(float[] base, float[] shadow, float[] highlight) {
    }

    private static final float HAT_INFLATE = 0.32f;

    private LayeredHairModel() {
    }

    public static BakedAccessoryModel build(Style style, Palette palette, boolean hatLayer) {
        float inflation = hatLayer ? HAT_INFLATE : 0.0f;
        BakedAccessoryModel.Builder builder = new BakedAccessoryModel.Builder();
        float[] base = palette.base();
        float[] shadow = palette.shadow();
        float[] highlight = palette.highlight();

        float capTop = -9.35f - inflation;
        float capLow = -7.05f + inflation * 0.5f;
        float capSide = 4.25f + inflation;

        // Cap over the crown
        builder.box(-capSide, capTop, -capSide, capSide, capLow, capSide, base[0], base[1], base[2], 1.0f);
        // Shadow rim under the cap edge
        builder.box(-capSide - 0.06f, capLow - 0.45f, -capSide - 0.06f,
                capSide + 0.06f, capLow, capSide + 0.06f, shadow[0], shadow[1], shadow[2], 1.0f);
        // Highlight streak across the crown
        builder.box(-1.15f - inflation, capTop - 0.14f, -capSide + 0.2f,
                1.15f + inflation, capTop + 0.34f, 0.4f, highlight[0], highlight[1], highlight[2], 1.0f);

        // Sides sweeping past the temples
        float sideOuter = capSide + 0.45f;
        builder.solidBox(-sideOuter, capTop + 0.7f, -4.15f, -3.8f - inflation, -1.7f, 4.15f,
                base[0], base[1], base[2], 1.0f);
        builder.solidBox(3.8f + inflation, capTop + 0.7f, -4.15f, sideOuter, -1.7f, 4.15f,
                base[0], base[1], base[2], 1.0f);
        builder.box(-sideOuter + 0.25f, capTop + 1.1f, -4.05f, -sideOuter + 0.65f, -2.2f, 0.2f,
                highlight[0], highlight[1], highlight[2], 1.0f);
        builder.box(sideOuter - 0.65f, capTop + 1.1f, -4.05f, sideOuter - 0.25f, -2.2f, 0.2f,
                highlight[0], highlight[1], highlight[2], 1.0f);

        // Back panel between the side sweeps
        builder.box(-3.85f, capTop + 0.6f, 3.85f + inflation, 3.85f, -2.6f, capSide + 0.5f,
                base[0], base[1], base[2], 1.0f);

        // Fringe across the forehead, parted above the right eye
        float fringeTop = -8.85f - inflation * 0.5f;
        float fringeLow = -6.6f + inflation * 0.5f;
        builder.box(-capSide + 0.05f, fringeTop, -4.55f - inflation, 0.7f, fringeLow, -3.8f,
                base[0], base[1], base[2], 1.0f);
        builder.box(1.15f, fringeTop + 0.25f, -4.55f - inflation, capSide - 0.05f, fringeLow - 0.2f, -3.8f,
                base[0], base[1], base[2], 1.0f);
        builder.box(-capSide + 0.05f, fringeTop - 0.2f, -4.6f - inflation, -1.2f, fringeTop + 0.5f, -3.82f,
                highlight[0], highlight[1], highlight[2], 1.0f);
        builder.box(-0.15f, fringeTop - 0.1f, -4.6f - inflation, 1.9f, fringeTop + 0.4f, -3.82f,
                shadow[0], shadow[1], shadow[2], 1.0f);

        if (style == Style.LONG) {
            // Back strands hanging to the shoulder blades; layered taper via narrower tails
            builder.solidBox(-2.1f, capLow - 0.3f, 3.9f + inflation, 2.1f, 5.6f, 4.85f + inflation,
                    base[0], base[1], base[2], 1.0f);
            builder.solidBox(-1.5f, 5.4f, 4.0f + inflation, 1.5f, 6.6f, 4.7f + inflation,
                    shadow[0], shadow[1], shadow[2], 1.0f);
            builder.solidBox(-3.75f, capLow - 0.3f, 3.6f + inflation, -2.4f, 3.2f, 4.6f + inflation,
                    base[0], base[1], base[2], 1.0f);
            builder.solidBox(2.4f, capLow - 0.3f, 3.6f + inflation, 3.75f, 3.2f, 4.6f + inflation,
                    base[0], base[1], base[2], 1.0f);
            builder.box(-2.0f, capLow, 4.86f + inflation, -1.1f, 4.4f, 5.0f + inflation,
                    highlight[0], highlight[1], highlight[2], 1.0f);
            builder.box(0.4f, capLow, 4.86f + inflation, 1.4f, 4.4f, 5.0f + inflation,
                    highlight[0], highlight[1], highlight[2], 1.0f);
        }

        return builder.bake();
    }
}
