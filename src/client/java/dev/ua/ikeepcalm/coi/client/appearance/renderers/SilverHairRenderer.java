package dev.ua.ikeepcalm.coi.client.appearance.renderers;

import dev.ua.ikeepcalm.coi.client.appearance.LayeredHairModel;

/** Fortune pathway hair — polished silver. */
public class SilverHairRenderer extends AbstractHairRenderer {

    private static final LayeredHairModel.Palette PALETTE = new LayeredHairModel.Palette(
            new float[]{0.76f, 0.78f, 0.84f},
            new float[]{0.55f, 0.57f, 0.64f},
            new float[]{0.97f, 0.98f, 1.00f});

    public SilverHairRenderer() {
        super(LayeredHairModel.Style.SHORT, PALETTE);
    }

    @Override
    public String traitId() {
        return "silver_hair";
    }
}
