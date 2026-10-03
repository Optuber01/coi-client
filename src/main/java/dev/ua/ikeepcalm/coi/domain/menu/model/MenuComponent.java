package dev.ua.ikeepcalm.coi.domain.menu.model;

import dev.ua.ikeepcalm.coi.domain.menu.service.MenuParser;

import java.util.List;

/**
 * One item inside a menu section. Sealed here, but the <em>wire</em> is not:
 * {@link MenuParser} skips a {@code type} it has never heard of, because a newer
 * plugin talking to an older client has to degrade to a screen missing one row
 * rather than no screen at all.
 * <p>
 * Every string arrives already localized. Colours are 0xRRGGBB with {@code 0}
 * meaning "the server did not name one".
 */
public sealed interface MenuComponent {

    enum TextStyle {BODY, MUTED, HEADING, WARN, DANGER, SUCCESS}

    enum ButtonStyle {PRIMARY, SECONDARY, DANGER, SUCCESS, GHOST}

    enum Align {LEFT, CENTER, RIGHT}

    enum GaugeStyle {BAR, RING, SEGMENTS}

    enum HeroStyle {PLAIN, RING}

    enum StepStyle {NUMBERED, TIMELINE}

    /**
     * A {@link Grid}'s tile edge — 18 / 28 / 40 px. {@code MEDIUM} is what v1 always drew.
     */
    enum TileSize {SMALL, MEDIUM, LARGE}

    enum CheckState {OK, NO, PENDING}

    /**
     * The modal is drawn locally and nothing leaves the client until the player agrees.
     */
    record Confirm(String title, String body, String confirmLabel) {
    }

    record Chip(String label, String value, int rgb, MenuIcon icon, String hint) {
    }

    record Text(String text, TextStyle style, Align align) implements MenuComponent {
    }

    record Note(TextStyle style, String title, String text, MenuIcon icon) implements MenuComponent {
    }

    /**
     * {@code hasBar} is separate from {@code fraction} because a genuine 0% is not the same
     * thing as a stat that has no bar at all — and {@code hasCap} from {@code cap} likewise.
     * {@code cap} is a ceiling the fill cannot reach; {@code delta} a trend token such as
     * {@code "+12"}, coloured by its sign.
     */
    record Stat(String label, String value, double fraction, boolean hasBar, int rgb, String hint,
                MenuIcon icon, GaugeStyle style, double cap, boolean hasCap,
                String delta) implements MenuComponent {
    }

    record KvRow(String label, String value, int rgb, String hint, MenuIcon icon) {
    }

    record Kv(List<KvRow> rows) implements MenuComponent {
    }

    /**
     * {@code state} is already resolved: the parser folds the wire's older {@code ok} boolean into it.
     */
    record Check(CheckState state, String label, String detail, MenuIcon icon, int rgb) {
    }

    record Checklist(List<Check> items) implements MenuComponent {
    }

    /**
     * {@code id} is an opaque token the server minted for this document version; the client
     * never invents one. A disabled button keeps its {@code disabledReason} as a tooltip.
     */
    record Button(String id, String label, String desc, ButtonStyle style, boolean enabled,
                  String disabledReason, Confirm confirm, MenuIcon icon) implements MenuComponent {
    }

    record Buttons(List<Button> buttons, int columns) implements MenuComponent {
    }

    record Toggle(String id, String label, boolean on, boolean enabled, String desc,
                  String onText, String offText, String disabledReason,
                  MenuIcon icon) implements MenuComponent {
    }

    /**
     * A row without an {@code action} is inert but still drawn — that is a read-only ledger.
     */
    record Row(String id, String title, String subtitle, MenuIcon icon, String badge, int badgeRgb,
               List<String> tooltip, String action, boolean enabled, int rgb,
               double fraction, boolean hasFraction, String meta, String disabledReason) {
    }

    record ListView(String id, List<Row> rows, boolean searchable, int maxVisible,
                    String empty) implements MenuComponent {
    }

    record Grid(List<Row> cells, int columns, TileSize size) implements MenuComponent {
    }

    record Input(String id, String label, String placeholder, String value, int maxLength,
                 String submit, String submitLabel, String hint) implements MenuComponent {
    }

    record Divider(String label) implements MenuComponent {
    }

    record Spacer(int size) implements MenuComponent {
    }

    // --- Vocabulary v2 ---

    record Hero(MenuIcon icon, String title, String subtitle, String badge, int badgeRgb,
                HeroStyle style, double fraction, boolean hasFraction, String fractionLabel,
                int rgb, List<Chip> chips) implements MenuComponent {
    }

    /**
     * A disclosure row. The open/closed state is the client's once the player touches it, keyed
     * on {@code id}; {@code open} is only the server's opening suggestion.
     */
    record Details(String id, String summary, List<String> text, TextStyle style, MenuIcon icon,
                   boolean open) implements MenuComponent {
    }

    /**
     * {@code done} is deliberately boxed: {@code null} is "not yet", neither done nor failed.
     */
    record Step(String title, String text, Boolean done, MenuIcon icon) {
    }

    record Steps(StepStyle style, List<Step> items) implements MenuComponent {
    }

    record Chips(List<Chip> items) implements MenuComponent {
    }

    record PanelCell(String id, MenuIcon icon, String title, String value, String subtitle,
                     int rgb, double fraction, boolean hasFraction, String badge, int badgeRgb,
                     List<String> tooltip, String action, boolean enabled, String disabledReason) {
    }

    record Panels(int columns, List<PanelCell> cells) implements MenuComponent {
    }

    /**
     * The wire puts {@code icon} / {@code badge} / {@code collapsed} on the <em>section</em>, but
     * {@code MenuDocument.Section} has only a title, so {@link MenuParser} lifts a decorated
     * heading into the section's component list as this and leaves the section's own title empty.
     * {@code key} is what the client's disclosure map is keyed on — the section's {@code id}, or
     * {@code "#section<n>"} when it named none.
     */
    record Heading(String key, String title, MenuIcon icon, String badge, int badgeRgb,
                   boolean collapsible, boolean collapsed) implements MenuComponent {
    }
}
