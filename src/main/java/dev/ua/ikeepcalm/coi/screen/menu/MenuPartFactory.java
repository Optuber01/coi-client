package dev.ua.ikeepcalm.coi.screen.menu;

import dev.ua.ikeepcalm.coi.domain.menu.model.MenuComponent;
import dev.ua.ikeepcalm.coi.domain.menu.model.MenuDocument;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Locale;

/**
 * Heights are decided here and nowhere else: a part is given the running content height as its
 * {@code y} and hands back its own, so the scrollbar and the hit test walk exactly what was drawn.
 * The {@code switch} is exhaustive because {@link MenuComponent} is sealed.
 */
public class MenuPartFactory {

    private final MenuContext ctx;
    private final List<MenuPart> parts;
    private int contentH;
    private List<MenuArchiveLeaf.Anchor> anchors;

    private MenuPartFactory(MenuContext ctx, List<MenuPart> parts) {
        this.ctx = ctx;
        this.parts = parts;
    }

    static int build(MenuContext ctx, MenuDocument doc, List<MenuPart> parts) {
        return build(ctx, doc, parts, null);
    }

    static int build(MenuContext ctx, MenuDocument doc, List<MenuPart> parts, List<MenuArchiveLeaf.Anchor> anchors) {
        MenuPartFactory factory = new MenuPartFactory(ctx, parts);
        factory.anchors = anchors;
        if (anchors != null) anchors.clear();
        factory.buildParts(doc);
        return factory.contentH;
    }

    private void add(MenuPart part) {
        part.y = contentH;
        parts.add(part);
        contentH += part.height;
    }

    private void buildParts(MenuDocument doc) {
        parts.clear();
        contentH = 0;

        for (MenuDocument.Section section : doc.sections()) {
            String title = section.title();
            if (title.isEmpty()) {
                for (MenuComponent component : section.components()) {
                    if (component instanceof MenuComponent.Heading heading) {
                        title = heading.title();
                        break;
                    }
                }
            }
            if (anchors != null && !title.isEmpty()) anchors.add(new MenuArchiveLeaf.Anchor(title, contentH));
            if (!section.title().isEmpty()) add(new MenuTextParts.HeadingPart(ctx, section.title()));
            for (MenuComponent component : section.components()) {
                if (component instanceof MenuComponent.Heading heading) {
                    add(new MenuTextParts.HeadingPart(ctx, heading));
                    // a collapsed section keeps its heading and drops its body
                    if (heading.collapsible() && !ctx.disclosed(heading.key(), !heading.collapsed())) break;
                    continue;
                }
                addComponent(component);
            }
            add(new MenuTextParts.SpacerPart(ctx, ctx.compact() ? 4 : 8));
        }
        if (parts.isEmpty()) {
            add(new MenuTextParts.MessagePart(ctx, Component.translatable("screen.coi.menu_empty").getString()));
        }
    }

    private void addComponent(MenuComponent component) {
        switch (component) {
            case MenuComponent.Text text -> add(new MenuTextParts.TextPart(ctx, text));
            case MenuComponent.Note note -> add(new MenuTextParts.NotePart(ctx, note));
            case MenuComponent.Stat stat -> add(new MenuValueParts.StatPart(ctx, stat));
            case MenuComponent.Kv kv -> add(new MenuValueParts.KvPart(ctx, kv));
            case MenuComponent.Checklist checklist -> add(new MenuValueParts.ChecklistPart(ctx, checklist));
            case MenuComponent.Button button -> add(new MenuControlParts.ButtonsPart(ctx, List.of(button), 1));
            case MenuComponent.Buttons buttons -> {
                if (!buttons.buttons().isEmpty()) {
                    add(new MenuControlParts.ButtonsPart(ctx, buttons.buttons(), buttons.columns()));
                }
            }
            case MenuComponent.Toggle toggle -> add(new MenuControlParts.TogglePart(ctx, toggle));
            case MenuComponent.ListView list -> addList(list);
            case MenuComponent.Grid grid -> {
                if (!grid.cells().isEmpty()) add(new MenuCollectionParts.GridPart(ctx, grid));
            }
            case MenuComponent.Input input -> add(new MenuControlParts.InputPart(ctx, input));
            case MenuComponent.Divider divider -> add(new MenuTextParts.DividerPart(ctx, divider.label()));
            case MenuComponent.Spacer spacer -> add(new MenuTextParts.SpacerPart(ctx, spacer.size()));
            case MenuComponent.Hero hero -> add(new MenuValueParts.HeroPart(ctx, hero));
            case MenuComponent.Details details -> add(new MenuTextParts.DetailsPart(ctx, details));
            case MenuComponent.Steps steps -> {
                if (!steps.items().isEmpty()) add(new MenuTextParts.StepsPart(ctx, steps));
            }
            case MenuComponent.Chips chips -> {
                if (!chips.items().isEmpty()) add(new MenuValueParts.ChipsPart(ctx, chips));
            }
            case MenuComponent.Panels panels -> {
                if (!panels.cells().isEmpty()) add(new MenuCollectionParts.PanelsPart(ctx, panels));
            }
            // only the section-level collapse is handled in buildParts
            case MenuComponent.Heading heading -> add(new MenuTextParts.HeadingPart(ctx, heading));
        }
    }

    /**
     * A list flattens into the outer scroll: one card, one scrollbar — which is why
     * {@code maxVisible} is advisory.
     */
    private void addList(MenuComponent.ListView list) {
        String query = "";
        if (list.searchable()) {
            MenuControlParts.SearchPart search = new MenuControlParts.SearchPart(ctx, list);
            add(search);
            query = search.query();
        }
        int shown = 0;
        for (MenuComponent.Row row : list.rows()) {
            if (!matches(row, query)) continue;
            add(new MenuCollectionParts.RowPart(ctx, row));
            shown++;
        }
        if (shown == 0) {
            add(new MenuTextParts.MessagePart(ctx, emptyText(list, query)));
        }
    }

    private static String emptyText(MenuComponent.ListView list, String query) {
        if (!query.isEmpty()) {
            return Component.translatable("screen.coi.menu_no_results", query).getString();
        }
        return list.empty().isEmpty()
                ? Component.translatable("screen.coi.menu_list_empty").getString()
                : list.empty();
    }

    private static boolean matches(MenuComponent.Row row, String query) {
        if (query.isEmpty()) return true;
        return row.title().toLowerCase(Locale.ROOT).contains(query)
                || row.subtitle().toLowerCase(Locale.ROOT).contains(query)
                || row.badge().toLowerCase(Locale.ROOT).contains(query);
    }
}
