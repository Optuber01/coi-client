package dev.ua.ikeepcalm.coi.client.appearance.renderers;

/** Corrosive claws — luminous acid-green talons that glow in the dark. Wins the claw family. */
public class CorrosiveClawsRenderer extends AbstractClawsRenderer {

    @Override
    public String traitId() {
        return "corrosive_claws";
    }

    @Override
    protected float[] clawColor() {
        return new float[]{0.55f, 0.95f, 0.25f};
    }

    @Override
    protected boolean luminous() {
        return true;
    }
}
