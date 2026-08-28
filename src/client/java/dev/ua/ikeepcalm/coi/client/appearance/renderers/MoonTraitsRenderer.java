package dev.ua.ikeepcalm.coi.client.appearance.renderers;

import dev.ua.ikeepcalm.coi.client.appearance.FeminineBodyGeometry;

/**
 * Moon pathway feminine profile — a slightly slimmer chest shape than the Demoness one.
 */
public class MoonTraitsRenderer extends AbstractChestProfileRenderer {

    @Override
    public String traitId() {
        return "moon_traits";
    }

    @Override
    protected FeminineBodyGeometry.ChestProfile profile() {
        return FeminineBodyGeometry.ChestProfile.MOON;
    }
}
