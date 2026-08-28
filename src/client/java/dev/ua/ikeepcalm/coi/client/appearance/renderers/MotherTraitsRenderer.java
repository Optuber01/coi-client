package dev.ua.ikeepcalm.coi.client.appearance.renderers;

import dev.ua.ikeepcalm.coi.client.appearance.FeminineBodyGeometry;

/**
 * Mother pathway feminine profile — the fullest of the body shapes; wins the body family
 * over Moon, Chaos and Demoness.
 */
public class MotherTraitsRenderer extends AbstractChestProfileRenderer {

    @Override
    public String traitId() {
        return "mother_traits";
    }

    @Override
    protected FeminineBodyGeometry.ChestProfile profile() {
        return FeminineBodyGeometry.ChestProfile.MOTHER;
    }
}
