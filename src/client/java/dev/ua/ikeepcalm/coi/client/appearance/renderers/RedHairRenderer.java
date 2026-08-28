package dev.ua.ikeepcalm.coi.client.appearance.renderers;

import dev.ua.ikeepcalm.coi.client.appearance.LayeredHairModel;

/** Red Priest pathway hair — burning crimson. */
public class RedHairRenderer extends AbstractHairRenderer {

    private static final LayeredHairModel.Palette PALETTE = new LayeredHairModel.Palette(
            new float[]{0.62f, 0.13f, 0.09f},
            new float[]{0.42f, 0.06f, 0.04f},
            new float[]{0.90f, 0.38f, 0.16f});

    public RedHairRenderer() {
        super(LayeredHairModel.Style.SHORT, PALETTE);
    }

    @Override
    public String traitId() {
        return "red_hair";
    }
}
