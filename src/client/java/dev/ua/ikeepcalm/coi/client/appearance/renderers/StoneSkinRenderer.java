package dev.ua.ikeepcalm.coi.client.appearance.renderers;

/** Stone skin — granite-gray petrified hide. */
public class StoneSkinRenderer extends AbstractBodyOverlayRenderer {

    @Override
    public String traitId() {
        return "stone_skin";
    }

    @Override
    protected float[] color() {
        return new float[]{0.55f, 0.55f, 0.57f};
    }

    @Override
    protected float alpha() {
        return 0.62f;
    }
}
