package dev.ua.ikeepcalm.coi.client.appearance.renderers;

/** Bat mutation — compact natural bat wings, brown and fur-veined. */
public class BatMutationRenderer extends AbstractWingsRenderer {

    @Override
    public String traitId() {
        return "bat_mutation";
    }

    @Override
    protected float[] membraneColor() {
        return new float[]{0.36f, 0.24f, 0.14f};
    }

    @Override
    protected float membraneAlpha() {
        return 0.78f;
    }

    @Override
    protected float[] boneColor() {
        return new float[]{0.22f, 0.14f, 0.08f};
    }

    @Override
    protected float spanScale() {
        return 0.78f;
    }

    @Override
    protected float flapSpeed() {
        return 1.6f;
    }
}
