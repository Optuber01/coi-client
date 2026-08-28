package dev.ua.ikeepcalm.coi.client.appearance.renderers;

import dev.ua.ikeepcalm.coi.client.appearance.LayeredHairModel;

/** Darkness pathway hair — ink black, falling to the shoulder blades. */
public class LongBlackHairRenderer extends AbstractHairRenderer {

    private static final LayeredHairModel.Palette PALETTE = new LayeredHairModel.Palette(
            new float[]{0.05f, 0.05f, 0.07f},
            new float[]{0.02f, 0.02f, 0.03f},
            new float[]{0.19f, 0.18f, 0.24f});

    public LongBlackHairRenderer() {
        super(LayeredHairModel.Style.LONG, PALETTE);
    }

    @Override
    public String traitId() {
        return "long_black_hair";
    }
}
