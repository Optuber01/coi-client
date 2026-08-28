package dev.ua.ikeepcalm.coi.client.appearance.renderers;

/**
 * Dragon irises — molten gold discs with vertical slit pupils, fullbright so they smolder
 * at night. Wins the eye family over every other eye trait.
 */
public class DragonIrisesRenderer extends AbstractEyesRenderer {

    public DragonIrisesRenderer() {
        super(true);
    }

    @Override
    public String traitId() {
        return "dragon_irises";
    }

    @Override
    protected float[] irisColor() {
        return solid(0.95f, 0.68f, 0.14f);
    }

    @Override
    protected float[] slitColor() {
        return solid(0.10f, 0.04f, 0.01f);
    }

    @Override
    protected float irisAlpha() {
        return 1.0f;
    }
}
