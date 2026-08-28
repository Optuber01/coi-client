package dev.ua.ikeepcalm.coi.client.appearance.renderers;

import dev.ua.ikeepcalm.coi.client.appearance.LayeredHairModel;

/** Moon pathway hair — near-black with cool highlights. */
public class BlackHairRenderer extends AbstractHairRenderer {

    private static final LayeredHairModel.Palette PALETTE = new LayeredHairModel.Palette(
            new float[]{0.07f, 0.06f, 0.09f},
            new float[]{0.03f, 0.03f, 0.04f},
            new float[]{0.24f, 0.22f, 0.30f});

    public BlackHairRenderer() {
        super(LayeredHairModel.Style.SHORT, PALETTE);
    }

    @Override
    public String traitId() {
        return "black_hair";
    }
}
