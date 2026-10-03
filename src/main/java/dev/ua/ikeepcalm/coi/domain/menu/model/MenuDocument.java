package dev.ua.ikeepcalm.coi.domain.menu.model;

import java.util.List;

/**
 * A whole server-authored screen. A document is rendered, never interpreted: the client never
 * branches on {@link #screen()}, which exists only to recognise "the same screen refreshed"
 * (scroll and search text survive) from a new one.
 */
public record MenuDocument(String session, int version, String screen,
                           String title, String subtitle, int accentRgb, MenuIcon icon,
                           boolean back, boolean closable, Toast toast,
                           List<Section> sections, List<MenuComponent.Button> footer,
                           boolean closed, Presentation presentation) {

    /** Optional decoration only. Every fact and action is still present in sections/footer. */
    public record Presentation(String template, String subject, String caption) {
        public static final Presentation NONE = new Presentation("", "", "");
        public boolean specimen() { return template.equals("specimen") && !subject.isEmpty(); }
        public boolean abilityManual() { return template.equals("ability_manual"); }

        public boolean archive() {
            return switch (template) {
                case "ledger", "relic", "inscription", "atlas", "challenge",
                     "throne", "pantheon", "ascension", "ability_manual" -> true;
                default -> false;
            };
        }
    }

    /**
     * For a document that names no accent.
     */
    public static final int DEFAULT_ACCENT = 0xFFD870;

    public record Section(String title, List<MenuComponent> components) {
    }

    /** {@code {"session":…,"closed":true}} — the server taking its own screen away. */
    public static MenuDocument closed(String session) {
        return new MenuDocument(session, 0, "", "", "", 0, MenuIcon.NONE,
                false, true, null, List.of(), List.of(), true, Presentation.NONE);
    }

    /** Styles are the {@link MenuComponent.TextStyle} words {@code info|success|warn|error}. */
    public record Toast(String style, String text) {
    }

    public int accentArgb() {
        return 0xFF000000 | (accentRgb != 0 ? accentRgb : DEFAULT_ACCENT);
    }
}
