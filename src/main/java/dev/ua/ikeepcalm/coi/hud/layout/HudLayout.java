package dev.ua.ikeepcalm.coi.hud.layout;

/**
 * While the editor is open every overlay skips its own render and the screen draws sample-data
 * previews instead, so the player drags one picture rather than two copies of each element.
 */
public class HudLayout {

    private static boolean editing = false;

    private HudLayout() {
    }

    /**
     * Checked by every overlay's render gate, through {@code HudGate.blocked}.
     */
    public static boolean editing() {
        return editing;
    }

    public static void setEditing(boolean value) {
        editing = value;
    }
}
