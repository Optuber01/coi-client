package dev.ua.ikeepcalm.coi.client.appearance.renderers;

/** Predator mutation — long obsidian talons on hands and feet. */
public class PredatorMutationRenderer extends AbstractClawsRenderer {

    @Override
    public String traitId() {
        return "predator_mutation";
    }

    @Override
    protected float[] clawColor() {
        return new float[]{0.10f, 0.09f, 0.11f};
    }

    @Override
    protected boolean drawsToeClaws() {
        return true;
    }
}
