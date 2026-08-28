package dev.ua.ikeepcalm.coi.client.appearance.renderers;

/**
 * Iron-black eyes — darker than the void variant with a cold metallic sheen rim,
 * the Eye-of-the-Blasphemer look.
 */
public class IronBlackEyesRenderer extends AbstractEyesRenderer {

    public IronBlackEyesRenderer() {
        super(false);
    }

    @Override
    public String traitId() {
        return "iron_black_eyes";
    }

    @Override
    protected float[] irisColor() {
        return solid(0.05f, 0.06f, 0.08f);
    }

    @Override
    protected float[] slitColor() {
        return solid(0.30f, 0.33f, 0.38f);
    }

    @Override
    protected float irisAlpha() {
        return 1.0f;
    }
}
