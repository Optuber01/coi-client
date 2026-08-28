package dev.ua.ikeepcalm.coi.client.appearance.renderers;

/** Pale skin — a cold, bloodless wash over the whole body. */
public class PaleSkinRenderer extends AbstractBodyOverlayRenderer {

    @Override
    public String traitId() {
        return "pale_skin";
    }

    @Override
    protected float[] color() {
        return new float[]{0.93f, 0.95f, 1.00f};
    }

    @Override
    protected float alpha() {
        return 0.32f;
    }
}
