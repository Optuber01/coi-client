package dev.ua.ikeepcalm.coi.client.appearance.renderers;

import dev.ua.ikeepcalm.coi.client.appearance.FeminineBodyGeometry;

/**
 * Chaos Primogenitor feminine profile — Demoness-width chest with a shallower projection.
 */
public class ChaosTraitsRenderer extends AbstractChestProfileRenderer {

    @Override
    public String traitId() {
        return "chaos_traits";
    }

    @Override
    protected FeminineBodyGeometry.ChestProfile profile() {
        return FeminineBodyGeometry.ChestProfile.CHAOS;
    }
}
