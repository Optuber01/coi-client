package dev.ua.ikeepcalm.coi.client.appearance.renderers;

/** Darkness pathway wings — translucent violet-black shadows given shape. */
public class DarknessWingsRenderer extends AbstractWingsRenderer {

    @Override
    public String traitId() {
        return "darkness_wings";
    }

    @Override
    protected float[] membraneColor() {
        return new float[]{0.10f, 0.05f, 0.20f};
    }

    @Override
    protected float membraneAlpha() {
        return 0.58f;
    }

    @Override
    protected float[] boneColor() {
        return new float[]{0.04f, 0.02f, 0.09f};
    }

    @Override
    protected float spanScale() {
        return 1.05f;
    }
}
