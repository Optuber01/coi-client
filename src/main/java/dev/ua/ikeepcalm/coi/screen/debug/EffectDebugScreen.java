package dev.ua.ikeepcalm.coi.screen.debug;

import dev.ua.ikeepcalm.coi.domain.beyonder.model.*;
import dev.ua.ikeepcalm.coi.domain.effect.EffectManager;
import dev.ua.ikeepcalm.coi.domain.effect.VisualEffect;
import dev.ua.ikeepcalm.coi.domain.effect.visual.ImpactFrameEffect;
import dev.ua.ikeepcalm.coi.domain.effect.visual.SpellImpactEffect;
import dev.ua.ikeepcalm.coi.domain.form.MythicalFormManager;
import dev.ua.ikeepcalm.coi.domain.menu.service.MenuState;
import dev.ua.ikeepcalm.coi.screen.menu.MenuScreen;
import dev.ua.ikeepcalm.coi.screen.sheet.CharacterSheetScreen;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class EffectDebugScreen extends Screen {

    private static final int ROW_H = 26;
    private static final int BTN_W = 80;
    private static final int PANEL_W = 440;
    /**
     * Paged, not listed: every protocol batch adds effects, and the ones past the window's
     * bottom edge could not be clicked at all.
     */
    private static final int EFFECTS_PER_PAGE = 6;
    private static final int PAGER_H = 24;
    private static final int CONTROL_ROW_H = 26;
    private static final int CONTROL_ROWS = 7;

    private final Screen parent;
    private final List<EffectRow> rows = new ArrayList<>();
    private EditBox paramsField;
    private int madnessRowY;
    private int spiritRowY;
    private int actingRowY;
    private int overlayRowY;
    private int resourceRowY;
    private int pagerY;
    private int page;

    public EffectDebugScreen(Screen parent) {
        super(Component.literal("Visual Effects — Debug"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        rows.clear();

        int panelX = (this.width - PANEL_W) / 2;
        int y = 50;

        y = addParamsField(panelX, y);
        y = addEffectRows(panelX, y) + 6;
        y = addMadnessRow(panelX, y);
        y = addSpiritRow(panelX, y);
        y = addActingRow(panelX, y);
        y = addOverlayRow(panelX, y);
        y = addResourceRow(panelX, y);
        y = addFormRow(panelX, y);
        addFooterRows(panelX, y);
    }

    private int addParamsField(int panelX, int y) {
        this.paramsField = new EditBox(this.font,
                panelX, y, PANEL_W - 4, 20, Component.literal("params")
        );
        paramsField.setMaxLength(200);
        paramsField.setHint(Component.literal("params (leave blank for defaults)").withStyle(ChatFormatting.DARK_GRAY));
        addRenderableWidget(paramsField);
        return y + 28;
    }

    private static int pageCount() {
        return Math.max(1, (EffectManager.getRegistry().size() + EFFECTS_PER_PAGE - 1) / EFFECTS_PER_PAGE);
    }

    private int addEffectRows(int panelX, int y) {
        List<Map.Entry<String, Supplier<VisualEffect>>> all =
                new ArrayList<>(EffectManager.getRegistry().entrySet());
        page = Math.clamp(page, 0, pageCount() - 1);
        int first = page * EFFECTS_PER_PAGE;
        int last = Math.min(all.size(), first + EFFECTS_PER_PAGE);

        for (Map.Entry<String, Supplier<VisualEffect>> entry : all.subList(first, last)) {
            String id = entry.getKey();
            VisualEffect probe = entry.getValue().get(); // just for metadata
            String defaultParams = probe.getDefaultParams();

            final int rowY = y;
            final int colX = panelX;

            Button testBtn = Button.builder(Component.literal("Test"), btn -> {
                String raw = paramsField.getValue().trim();
                String p = raw.isEmpty() ? defaultParams : raw;
                if (SpellImpactEffect.ID.equals(id)) {
                    if (raw.isEmpty()) {
                        p = "style=burst,scope=world,color=FFFFFF,accent=FF7A22,intensity=0.95,radius=2.5,duration=1200";
                    }
                    EffectManager.triggerDebug(id, p);
                    onClose();
                } else if (ImpactFrameEffect.ID.equals(id)) {
                    // a frame drawn under this screen's dimming is not the frame; close first
                    EffectManager.triggerDebug(id, p);
                    onClose();
                } else {
                    EffectManager.trigger(id, p);
                }
            }).bounds(colX, rowY, BTN_W, 20).build();
            addRenderableWidget(testBtn);

            Button stopBtn = Button.builder(Component.literal("Stop"), btn ->
                    EffectManager.stopEffect(id)).bounds(colX + BTN_W + 4, rowY, 50, 20).build();
            addRenderableWidget(stopBtn);

            Button defsBtn = Button.builder(Component.literal("↩ defaults"), btn -> paramsField.setValue(defaultParams)).bounds(colX + BTN_W + 58, rowY, 90, 20).build();
            addRenderableWidget(defsBtn);

            rows.add(new EffectRow(id, probe.getDisplayName(), colX + BTN_W + 154, rowY));
            y += ROW_H;
        }
        // a short last page still reserves its rows, so nothing below moves
        return addPager(panelX, y + (EFFECTS_PER_PAGE - (last - first)) * ROW_H);
    }

    private int addPager(int panelX, int y) {
        pagerY = y;
        addRenderableWidget(Button.builder(Component.literal("<"), btn -> turnPage(-1))
                .bounds(panelX, y, 24, 20).build());
        addRenderableWidget(Button.builder(Component.literal(">"), btn -> turnPage(1))
                .bounds(panelX + 28, y, 24, 20).build());
        return y + PAGER_H;
    }

    private void turnPage(int delta) {
        page = Math.floorMod(page + delta, pageCount());
        // the params field is shared by every page, so a page turn carries what is typed
        String typed = paramsField.getValue();
        rebuildWidgets();
        paramsField.setValue(typed);
    }

    private int addMadnessRow(int panelX, int y) {
        madnessRowY = y;
        addRenderableWidget(Button.builder(Component.literal("-10"), btn -> DebugStates.addMadness(-10))
                .bounds(panelX, madnessRowY, 40, 20).build());
        addRenderableWidget(Button.builder(Component.literal("+10"), btn -> DebugStates.addMadness(10))
                .bounds(panelX + 44, madnessRowY, 40, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Cycle Stage"), btn -> DebugStates.cycleStage())
                .bounds(panelX + 88, madnessRowY, 80, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Perm +10"), btn -> DebugStates.addPermMadness(10))
                .bounds(panelX + 172, madnessRowY, 70, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Reset"), btn -> BeyonderState.reset())
                .bounds(panelX + 246, madnessRowY, 50, 20).build());
        return y + CONTROL_ROW_H;
    }

    private int addSpiritRow(int panelX, int y) {
        spiritRowY = y;
        addRenderableWidget(Button.builder(Component.literal("Spirit -50"), btn -> DebugStates.addSpirit(-50))
                .bounds(panelX, spiritRowY, 70, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Spirit +50"), btn -> DebugStates.addSpirit(50))
                .bounds(panelX + 74, spiritRowY, 70, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Spirit Full"), btn -> DebugStates.addSpirit(DebugStates.debugMaxSpirit()))
                .bounds(panelX + 148, spiritRowY, 70, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Max 1000"), btn -> DebugStates.setSpiritMax(DebugStates.DEBUG_MAX_SPIRIT))
                .bounds(panelX + 222, spiritRowY, 70, 20).build());
        return y + CONTROL_ROW_H;
    }

    private int addActingRow(int panelX, int y) {
        actingRowY = y;
        addRenderableWidget(Button.builder(Component.literal("Acting +10%"), btn -> ActingState.debugGrant(10))
                .bounds(panelX, actingRowY, 80, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Acting CD 5m"), btn -> ActingState.debugCooldown(300))
                .bounds(panelX + 84, actingRowY, 80, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Acting reset"), btn -> ActingState.reset())
                .bounds(panelX + 168, actingRowY, 80, 20).build());
        return y + CONTROL_ROW_H;
    }

    private int addOverlayRow(int panelX, int y) {
        overlayRowY = y;
        addRenderableWidget(Button.builder(Component.literal("ActionBar"), btn -> ActionBarState.debugInject(3000))
                .bounds(panelX, overlayRowY, 80, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Target"), btn -> TargetState.debugHit())
                .bounds(panelX + 84, overlayRowY, 70, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Cogitate"), btn -> CogitationState.debugPrompt())
                .bounds(panelX + 158, overlayRowY, 70, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Toast"), btn -> NotificationState.debugToast())
                .bounds(panelX + 232, overlayRowY, 60, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Sheet"), btn -> {
            // fake payload first, so the sheet has something to draw offline
            SheetState.debugInject();
            minecraft.gui.setScreen(new CharacterSheetScreen(this));
        }).bounds(panelX + 296, overlayRowY, 60, 20).build());
        // the sample document covers every component type, so the renderer can be judged
        // with no server attached
        addRenderableWidget(Button.builder(Component.literal("Menu"), btn -> {
            MenuState.debugInject();
            minecraft.gui.setScreen(new MenuScreen(this));
        }).bounds(panelX + 360, overlayRowY, 76, 20).build());
        return y + CONTROL_ROW_H;
    }

    private int addResourceRow(int panelX, int y) {
        resourceRowY = y;
        addRenderableWidget(Button.builder(Component.literal("Resource"), btn -> ResourceState.debugInject())
                .bounds(panelX, resourceRowY, 80, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Res clear"), btn -> ResourceState.debugClear())
                .bounds(panelX + 84, resourceRowY, 70, 20).build());
        // the pool bar only replaces the hearts once a server has named a maximum; the current
        // value needs no button, being derived from vanilla health
        addRenderableWidget(Button.builder(Component.literal("HP pool"), btn -> BeyonderState.updateMaxHealth(DebugStates.DEBUG_MAX_HEALTH))
                .bounds(panelX + 158, resourceRowY, 65, 20).build());
        addRenderableWidget(Button.builder(Component.literal("HP clear"), btn -> BeyonderState.updateMaxHealth(0))
                .bounds(panelX + 227, resourceRowY, 65, 20).build());
        // the plate needs all four before it draws more than a header, so one button seeds them
        addRenderableWidget(Button.builder(Component.literal("Plate sample"), btn -> DebugStates.seedPlate())
                .bounds(panelX + 296, resourceRowY, 80, 20).build());
        return y + CONTROL_ROW_H;
    }

    private int addFormRow(int panelX, int y) {
        List<String> forms = MythicalFormManager.getRegisteredPathwayNames();
        String currentForm = minecraft.player != null ? MythicalFormManager.getForm(minecraft.player.getUUID().toString()) : null;
        final int[] activeIndex = {-1};
        if (currentForm != null) {
            for (int i = 0; i < forms.size(); i++) {
                if (forms.get(i).equalsIgnoreCase(currentForm)) {
                    activeIndex[0] = i;
                    break;
                }
            }
        }

        String label = activeIndex[0] == -1 ? "Form: None (Click to cycle)" : "Form: " + forms.get(activeIndex[0]);
        Button formCycleBtn = Button.builder(Component.literal(label), btn -> {
            if (minecraft.player == null || forms.isEmpty()) return;
            String uuid = minecraft.player.getUUID().toString();
            activeIndex[0] = (activeIndex[0] + 1) % forms.size();
            String selected = forms.get(activeIndex[0]);
            MythicalFormManager.handlePacket(uuid, selected + ":true:start");
            btn.setMessage(Component.literal("Form: " + selected));
        }).bounds(panelX, y, PANEL_W / 2 - 2, 20).build();
        addRenderableWidget(formCycleBtn);

        addRenderableWidget(Button.builder(Component.literal("Clear Form").withStyle(ChatFormatting.YELLOW), btn -> {
            if (minecraft.player != null) {
                MythicalFormManager.handlePacket(minecraft.player.getUUID().toString(), ":true:stop");
                activeIndex[0] = -1;
                formCycleBtn.setMessage(Component.literal("Form: None (Click to cycle)"));
            }
        }).bounds(panelX + PANEL_W / 2 + 2, y, PANEL_W / 2 - 2, 20).build());

        return y + CONTROL_ROW_H;
    }

    private void addFooterRows(int panelX, int y) {
        addRenderableWidget(Button.builder(
                Component.literal("Appearance Traits — Local Preview").withStyle(ChatFormatting.AQUA),
                btn -> minecraft.gui.setScreen(new AppearanceDebugScreen(this))
        ).bounds(panelX, y, PANEL_W / 2 - 2, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Specimen preview"), btn -> {
            MenuState.debugSpecimen();
            minecraft.gui.setScreen(new MenuScreen(this));
        }).bounds(panelX + PANEL_W / 2 + 2, y, PANEL_W / 2 - 2, 20).build());
        y += CONTROL_ROW_H;

        addRenderableWidget(Button.builder(Component.literal("Stop All Effects").withStyle(ChatFormatting.RED),
                btn -> EffectManager.stopAll()).bounds(panelX, y, PANEL_W / 2 - 2, 20).build());

        addRenderableWidget(Button.builder(Component.translatable("gui.done"), btn -> onClose()).bounds(panelX + PANEL_W / 2 + 2, y, PANEL_W / 2 - 2, 20).build());
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        // no blur: the world is still rendering
        int panelX = (this.width - PANEL_W) / 2;
        int panelH = 50 + EFFECTS_PER_PAGE * ROW_H + PAGER_H + 34 + CONTROL_ROWS * CONTROL_ROW_H;
        graphics.fill(panelX - 8, 8, panelX + PANEL_W + 8, 8 + panelH, 0xCC000000);

        super.extractRenderState(graphics, mouseX, mouseY, a);

        graphics.centeredText(font, Component.literal("Visual Effects — Debug").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                this.width / 2, 18, 0xFFFFFFFF);

        graphics.text(font, Component.literal("Params:").withStyle(ChatFormatting.GRAY),
                panelX, 38, 0xFFFFFFFF);

        renderEffectLabels(graphics);
        graphics.text(font, Component.literal("page " + (page + 1) + " / " + pageCount()).withStyle(ChatFormatting.GRAY),
                panelX + 58, pagerY + 6, 0xFFFFFFFF);
        renderReadouts(graphics, panelX);
    }

    private void renderEffectLabels(GuiGraphicsExtractor graphics) {
        for (EffectRow row : rows) {
            boolean active = EffectManager.isActive(row.id);
            int nameColor = active ? 0xFF55FF55 : 0xFFAAAAAA;
            String indicator = active ? "● " : "○ ";
            graphics.text(font, Component.literal(indicator + row.displayName).withStyle(active ? ChatFormatting.GREEN : ChatFormatting.GRAY),
                    row.labelX, row.y + 6, nameColor);
        }
    }

    private void renderReadouts(GuiGraphicsExtractor graphics, int panelX) {
        double m = BeyonderState.getMadness();
        int stage = m >= 100 ? 4 : m >= 75 ? 3 : m >= 50 ? 2 : m >= 25 ? 1 : 0;
        String madnessLabel = String.format("Madness %.0f%% · S%d (Min %.0f%%)",
                m, stage, BeyonderState.getPermanentMadness());
        graphics.text(font, Component.literal(madnessLabel).withStyle(ChatFormatting.LIGHT_PURPLE),
                panelX + 300, madnessRowY + 6, 0xFFFFFFFF);

        String spiritLabel = BeyonderState.hasSpiritualityData()
                ? String.format("Spirit %d / %d", BeyonderState.getSpirituality(), BeyonderState.getMaxSpirituality())
                : "Spirit — no data";
        graphics.text(font, Component.literal(spiritLabel).withStyle(ChatFormatting.AQUA),
                panelX + 300, spiritRowY + 6, 0xFFFFFFFF);

        graphics.text(font, Component.literal("Overlays").withStyle(ChatFormatting.GRAY),
                panelX + 300, overlayRowY + 6, 0xFFFFFFFF);

        String actingLabel = ActingState.hasData()
                ? String.format("Acting %.1f%% · CD %s", ActingState.getPercent(), ActingState.cooldownClock())
                : "Acting — no data";
        graphics.text(font, Component.literal(actingLabel).withStyle(ChatFormatting.GOLD),
                panelX + 260, actingRowY + 6, 0xFFFFFFFF);

        graphics.text(font, Component.literal("Resource bars: " + ResourceState.visible().size()).withStyle(ChatFormatting.GRAY),
                panelX + 160, resourceRowY + 6, 0xFFFFFFFF);
    }

    @Override
    public void onClose() {
        minecraft.gui.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private record EffectRow(String id, String displayName, int labelX, int y) {
    }
}
