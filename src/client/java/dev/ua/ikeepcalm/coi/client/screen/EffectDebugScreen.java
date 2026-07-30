package dev.ua.ikeepcalm.coi.client.screen;

import dev.ua.ikeepcalm.coi.client.ClientBeyonderState;
import dev.ua.ikeepcalm.coi.client.effects.EffectManager;
import dev.ua.ikeepcalm.coi.client.effects.VisualEffect;
import dev.ua.ikeepcalm.coi.client.effects.impl.ImpactFrameEffect;
import dev.ua.ikeepcalm.coi.client.mcf.MythicalFormManager;
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

/**
 * Developer-only screen for testing visual effects without server commands.
 * Only accessible via the debug keybinding registered in dev environments.
 */
public class EffectDebugScreen extends Screen {

    private static final int ROW_H = 26;
    private static final int BTN_W = 80;
    private static final int PANEL_W = 440;

    private final Screen parent;
    private final List<EffectRow> rows = new ArrayList<>();
    private EditBox paramsField;
    private int madnessRowY;

    public EffectDebugScreen(Screen parent) {
        super(Component.literal("Visual Effects — Debug"));
        this.parent = parent;
    }

    private static void addMadness(double delta) {
        setMadness(ClientBeyonderState.getMadness() + delta);
    }

    /**
     * Jumps to the next stage threshold: 0 → 25 → 50 → 75 → 100 → 0.
     */
    private static void cycleStage() {
        double m = ClientBeyonderState.getMadness();
        double next;
        if (m < 25) next = 25;
        else if (m < 50) next = 50;
        else if (m < 75) next = 75;
        else if (m < 100) next = 100;
        else next = 0;
        setMadness(next);
    }

    private static void addPermMadness(double delta) {
        double perm = Math.clamp(ClientBeyonderState.getPermanentMadness() + delta, 0.0, 100.0);
        ClientBeyonderState.updateConditions(
                ClientBeyonderState.getMadness(),
                perm,
                ClientBeyonderState.getFreezeStacks(),
                ClientBeyonderState.getMentalPressure(),
                ClientBeyonderState.getTiredness());
    }

    /**
     * Sets madness via updateConditions so increases also trigger the
     * bar's flash/shake animation, exactly like a server update would.
     */
    private static void setMadness(double value) {
        ClientBeyonderState.updateConditions(
                Math.clamp(value, 0.0, 100.0),
                ClientBeyonderState.getPermanentMadness(),
                ClientBeyonderState.getFreezeStacks(),
                ClientBeyonderState.getMentalPressure(),
                ClientBeyonderState.getTiredness());
    }

    @Override
    protected void init() {
        rows.clear();

        int panelX = (this.width - PANEL_W) / 2;
        int y = 50;

        // Params input shared by all "Test" buttons
        this.paramsField = new EditBox(this.font,
                panelX, y, PANEL_W - 4, 20, Component.literal("params")
        );
        paramsField.setMaxLength(200);
        paramsField.setHint(Component.literal("params (leave blank for defaults)").withStyle(ChatFormatting.DARK_GRAY));
        addRenderableWidget(paramsField);
        y += 28;

        // One row per registered effect
        Map<String, Supplier<VisualEffect>> registry = EffectManager.getRegistry();
        for (Map.Entry<String, Supplier<VisualEffect>> entry : registry.entrySet()) {
            String id = entry.getKey();
            VisualEffect probe = entry.getValue().get(); // just for metadata
            String defaultParams = probe.getDefaultParams();

            final int rowY = y;

            // [Test] button
            Button testBtn = Button.builder(Component.literal("Test"), btn -> {
                String raw = paramsField.getValue().trim();
                String p = raw.isEmpty() ? defaultParams : raw;
                if (ImpactFrameEffect.ID.equals(id)) {
                    if (raw.isEmpty()) {
                        p = "style=burst,scope=world,color=FFFFFF,accent=FF7A22,intensity=0.95,radius=2.5,duration=1200";
                    }
                    EffectManager.triggerDebug(id, p);
                    onClose();
                } else {
                    EffectManager.trigger(id, p);
                }
            }).bounds(panelX, rowY, BTN_W, 20).build();
            addRenderableWidget(testBtn);

            // [Stop] button
            Button stopBtn = Button.builder(Component.literal("Stop"), btn ->
                    EffectManager.stopEffect(id)).bounds(panelX + BTN_W + 4, rowY, 50, 20).build();
            addRenderableWidget(stopBtn);

            // [Defaults] button — fills the params field with this effect's defaults
            Button defsBtn = Button.builder(Component.literal("↩ defaults"), btn -> paramsField.setValue(defaultParams)).bounds(panelX + BTN_W + 58, rowY, 90, 20).build();
            addRenderableWidget(defsBtn);

            rows.add(new EffectRow(id, probe.getDisplayName(), panelX + BTN_W + 154, rowY));
            y += ROW_H;
        }

        y += 6;

        // Madness debug controls — exercise the madness bar stages without a server
        madnessRowY = y;
        addRenderableWidget(Button.builder(Component.literal("-10"), btn -> addMadness(-10))
                .bounds(panelX, madnessRowY, 40, 20).build());
        addRenderableWidget(Button.builder(Component.literal("+10"), btn -> addMadness(10))
                .bounds(panelX + 44, madnessRowY, 40, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Cycle Stage"), btn -> cycleStage())
                .bounds(panelX + 88, madnessRowY, 80, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Perm +10"), btn -> addPermMadness(10))
                .bounds(panelX + 172, madnessRowY, 70, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Reset"), btn -> ClientBeyonderState.reset())
                .bounds(panelX + 246, madnessRowY, 50, 20).build());
        y += 26;

        int formY = y;
        java.util.List<String> forms = MythicalFormManager.getRegisteredPathwayNames();
        String currentForm = minecraft.player != null ? MythicalFormManager.getForm(minecraft.player.getName().getString()) : null;
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
        }).bounds(panelX, formY, PANEL_W / 2 - 2, 20).build();
        addRenderableWidget(formCycleBtn);

        addRenderableWidget(Button.builder(Component.literal("Clear Form").withStyle(ChatFormatting.YELLOW), btn -> {
            if (minecraft.player != null) {
                MythicalFormManager.handlePacket(minecraft.player.getUUID().toString(), ":true:stop");
                activeIndex[0] = -1;
                formCycleBtn.setMessage(Component.literal("Form: None (Click to cycle)"));
            }
        }).bounds(panelX + PANEL_W / 2 + 2, formY, PANEL_W / 2 - 2, 20).build());

        y += 26;

        addRenderableWidget(Button.builder(
                Component.literal("Appearance Traits — Local Preview").withStyle(ChatFormatting.AQUA),
                btn -> minecraft.setScreen(new AppearanceDebugScreen(this))
        ).bounds(panelX, y, PANEL_W, 20).build());
        y += 26;

        // Stop All
        addRenderableWidget(Button.builder(Component.literal("Stop All Effects").withStyle(ChatFormatting.RED),
                btn -> EffectManager.stopAll()).bounds(panelX, y, PANEL_W / 2 - 2, 20).build());

        // Done
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), btn -> onClose()).bounds(panelX + PANEL_W / 2 + 2, y, PANEL_W / 2 - 2, 20).build());
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        // Semi-transparent panel behind controls (no blur — world is still rendering)
        int panelX = (this.width - PANEL_W) / 2;
        int panelH = 50 + EffectManager.getRegistry().size() * ROW_H + 34 + 26 + 26 + 26;
        graphics.fill(panelX - 8, 8, panelX + PANEL_W + 8, 8 + panelH, 0xCC000000);

        super.extractRenderState(graphics, mouseX, mouseY, a);

        // Title
        graphics.centeredText(font, Component.literal("Visual Effects — Debug").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                this.width / 2, 18, 0xFFFFFFFF);

        // Column header
        graphics.text(font, Component.literal("Params:").withStyle(ChatFormatting.GRAY),
                panelX, 38, 0xFFFFFFFF);

        // Effect name labels + active indicator
        for (EffectRow row : rows) {
            boolean active = EffectManager.isActive(row.id);
            int nameColor = active ? 0xFF55FF55 : 0xFFAAAAAA;
            String indicator = active ? "● " : "○ ";
            graphics.text(font, Component.literal(indicator + row.displayName).withStyle(active ? ChatFormatting.GREEN : ChatFormatting.GRAY),
                    row.labelX, row.y + 6, nameColor);
        }

        // Live madness readout next to the debug controls
        double m = ClientBeyonderState.getMadness();
        int stage = m >= 100 ? 4 : m >= 75 ? 3 : m >= 50 ? 2 : m >= 25 ? 1 : 0;
        String madnessLabel = String.format("Madness %.0f%% · S%d (Min %.0f%%)",
                m, stage, ClientBeyonderState.getPermanentMadness());
        graphics.text(font, Component.literal(madnessLabel).withStyle(ChatFormatting.LIGHT_PURPLE),
                panelX + 300, madnessRowY + 6, 0xFFFFFFFF);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private record EffectRow(String id, String displayName, int labelX, int y) {
    }
}
