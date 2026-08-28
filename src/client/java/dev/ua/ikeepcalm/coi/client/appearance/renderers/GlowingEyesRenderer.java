package dev.ua.ikeepcalm.coi.client.appearance.renderers;

/** Visionary pathway glowing eyes — white-hot core with a violet halo. */
public class GlowingEyesRenderer extends AbstractEyesRenderer {

    public GlowingEyesRenderer() {
        super(true);
    }

    @Override
    public String traitId() {
        return "glowing_eyes";
    }

    @Override
    protected float[] irisColor() {
        return solid(0.95f, 0.92f, 1.00f);
    }

    @Override
    protected float[] slitColor() {
        return solid(0.62f, 0.42f, 0.95f);
    }

    @Override
    protected float irisAlpha() {
        return 0.85f;
    }
}
