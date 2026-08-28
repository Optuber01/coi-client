package dev.ua.ikeepcalm.coi.client.appearance.renderers;

/** Void-black eyes — the whites of the eyes are swallowed entirely. */
public class BlackEyesRenderer extends AbstractEyesRenderer {

    public BlackEyesRenderer() {
        super(false);
    }

    @Override
    public String traitId() {
        return "black_eyes";
    }

    @Override
    protected float[] irisColor() {
        return solid(0.02f, 0.02f, 0.03f);
    }

    @Override
    protected float[] slitColor() {
        return null;
    }

    @Override
    protected float irisAlpha() {
        return 1.0f;
    }
}
