package dev.ua.ikeepcalm.coi.client.appearance.renderers;

import dev.ua.ikeepcalm.coi.client.appearance.LayeredHairModel;

/** Mother pathway hair — warm brown, falling to the shoulder blades. */
public class LongBrownHairRenderer extends AbstractHairRenderer {

    private static final LayeredHairModel.Palette PALETTE = new LayeredHairModel.Palette(
            new float[]{0.35f, 0.23f, 0.13f},
            new float[]{0.22f, 0.13f, 0.06f},
            new float[]{0.58f, 0.42f, 0.25f});

    public LongBrownHairRenderer() {
        super(LayeredHairModel.Style.LONG, PALETTE);
    }

    @Override
    public String traitId() {
        return "long_brown_hair";
    }
}
