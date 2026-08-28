package dev.ua.ikeepcalm.coi.client.appearance.renderers;

/** Wood skin — bark-toned hide for the Child of the Oak. */
public class WoodSkinRenderer extends AbstractBodyOverlayRenderer {

    @Override
    public String traitId() {
        return "wood_skin";
    }

    @Override
    protected float[] color() {
        return new float[]{0.42f, 0.28f, 0.15f};
    }

    @Override
    protected float alpha() {
        return 0.55f;
    }
}
