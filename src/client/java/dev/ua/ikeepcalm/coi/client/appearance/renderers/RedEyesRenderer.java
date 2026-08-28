package dev.ua.ikeepcalm.coi.client.appearance.renderers;

/** Moon pathway red eyes — luminous crimson, visible in the dark. */
public class RedEyesRenderer extends AbstractEyesRenderer {

    public RedEyesRenderer() {
        super(true);
    }

    @Override
    public String traitId() {
        return "red_eyes";
    }

    @Override
    protected float[] irisColor() {
        return solid(0.86f, 0.10f, 0.10f);
    }

    @Override
    protected float[] slitColor() {
        return solid(0.45f, 0.02f, 0.02f);
    }

    @Override
    protected float irisAlpha() {
        return 0.92f;
    }
}
