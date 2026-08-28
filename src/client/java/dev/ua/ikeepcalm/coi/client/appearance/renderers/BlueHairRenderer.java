package dev.ua.ikeepcalm.coi.client.appearance.renderers;

import dev.ua.ikeepcalm.coi.client.appearance.LayeredHairModel;

/** Tyrant pathway hair — deep sea blue. */
public class BlueHairRenderer extends AbstractHairRenderer {

    private static final LayeredHairModel.Palette PALETTE = new LayeredHairModel.Palette(
            new float[]{0.14f, 0.30f, 0.62f},
            new float[]{0.07f, 0.16f, 0.40f},
            new float[]{0.42f, 0.66f, 0.94f});

    public BlueHairRenderer() {
        super(LayeredHairModel.Style.SHORT, PALETTE);
    }

    @Override
    public String traitId() {
        return "blue_hair";
    }
}
