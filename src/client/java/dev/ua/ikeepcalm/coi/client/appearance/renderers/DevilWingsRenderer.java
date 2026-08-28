package dev.ua.ikeepcalm.coi.client.appearance.renderers;

/** Devil wings — large leathery black-red wings. Wins the wing family over Darkness and Bat. */
public class DevilWingsRenderer extends AbstractWingsRenderer {

    @Override
    public String traitId() {
        return "devil_wings";
    }

    @Override
    protected float[] membraneColor() {
        return new float[]{0.14f, 0.02f, 0.04f};
    }

    @Override
    protected float membraneAlpha() {
        return 0.85f;
    }

    @Override
    protected float[] boneColor() {
        return new float[]{0.05f, 0.02f, 0.03f};
    }

    @Override
    protected float spanScale() {
        return 1.15f;
    }
}
