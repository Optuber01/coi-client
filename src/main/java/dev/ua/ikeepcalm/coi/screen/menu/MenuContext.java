package dev.ua.ikeepcalm.coi.screen.menu;

import dev.ua.ikeepcalm.coi.domain.menu.model.MenuComponent;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;

import java.util.List;
import java.util.function.Supplier;

/**
 * The whole of what a {@link MenuPart} may ask {@link MenuScreen} for. Nothing here exposes the
 * document or the scroll position — a part is told where to draw, it never asks.
 */
interface MenuContext {

    Font font();

    /** The document's own colour, or the mod's gold when it named none. */
    int accent();

    default boolean archival() { return false; }

    /** A short window has no room for the roomy paddings; see {@code pad}. */
    boolean compact();

    /** A button row's height, which the compact layout shortens. */
    int buttonH();

    /** The card's own padding, which the compact layout narrows. */
    int pad();

    /** The window, for the one thing centred on it rather than on the card. */
    int screenWidth();

    int screenHeight();

    /** The width inside the card's padding — every full-width band's width. */
    int contentW();

    float approach(float current, float target, float durationMs);

    /** Raises a muted one-line tooltip, unless the text is empty. */
    void hint(String text);

    /** Raises a red one-line tooltip, unless the text is empty. */
    void reason(String text);

    /** Raises a multi-line tooltip: {@code lead} in white, the body muted. */
    void lines(List<String> body, String lead);

    /** Names the chevron under the cursor, so a collapsed row is not a silent one. */
    void disclosureHint(boolean open);

    /** Plays the click and sends the action, guarded by {@code canSend}. */
    void fire(String action, String value);

    void activate(MenuComponent.Button button, String value);

    int rowHeightFor(List<MenuComponent.Button> buttons);

    void drawButtons(GuiGraphicsExtractor g, List<MenuComponent.Button> buttons,
                     int x, int y, int width, int cols, int rowH, int mouseX, int mouseY);

    /** @return the index of the button under the cursor, or -1 */
    int hitButton(double mx, double my, int count, int x, int y, int width, int cols, int rowH);

    /** A text field that outlives the layout it was built for, keyed by {@code key}. */
    EditBox field(String key, Supplier<EditBox> factory);

    void drawField(GuiGraphicsExtractor g, EditBox box, int x, int y, int w, int mouseX, int mouseY);

    void focusField(EditBox box);

    boolean disclosed(String id, boolean initial);

    void toggleDisclosure(String id, boolean initial);

    /** The eased rotation of one disclosure chevron, 0 closed to 1 open. */
    float chevron(String id, boolean open);

    /** Heights changed, so the whole card has to be laid out again. */
    void relayout();
}
