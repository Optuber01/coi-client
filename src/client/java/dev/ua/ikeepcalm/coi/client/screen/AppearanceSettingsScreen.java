package dev.ua.ikeepcalm.coi.client.screen;

import dev.ua.ikeepcalm.coi.client.config.AppearanceConfig;
import dev.ua.ikeepcalm.coi.util.CoiStyle;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Appearance preferences: the trait layer master switch and the uniqueness particle
 * toggles (self vs. other players). Persists immediately through {@link AppearanceConfig}.
 */
public final class AppearanceSettingsScreen extends Screen {

    private static final int PANEL_W = 320;
    private static final int ROW_H = 26;

    private record Toggle(String labelKey, String tooltipKey) {
    }

    private final List<Toggle> toggles = List.of(
            new Toggle("screen.coi.appearance.traits_enabled", "screen.coi.appearance.traits_enabled.tooltip"),
            new Toggle("screen.coi.appearance.uniqueness_enabled", "screen.coi.appearance.uniqueness_enabled.tooltip"),
            new Toggle("screen.coi.appearance.uniqueness_self", "screen.coi.appearance.uniqueness_self.tooltip"),
            new Toggle("screen.coi.appearance.uniqueness_others", "screen.coi.appearance.uniqueness_others.tooltip")
    );

    private int panelX;
    private int panelY;
    private int panelH;

    public AppearanceSettingsScreen() {
        super(Component.translatable("screen.coi.appearance_settings"));
    }

    @Override
    protected void init() {
        this.clearWidgets();
        panelX = (this.width - PANEL_W) / 2;
        panelH = 58 + toggles.size() * ROW_H + 30;
        panelY = Math.max(20, (this.height - panelH) / 2);

        int rowY = panelY + 46;
        for (Toggle toggle : toggles) {
            addRenderableWidget(Button.builder(toggleLabel(toggle.labelKey), button -> {
                flipSetting(toggle.labelKey());
                button.setMessage(toggleLabel(toggle.labelKey()));
            }).bounds(panelX + 16, rowY, PANEL_W - 32, 20).build());
            rowY += ROW_H;
        }

        addRenderableWidget(Button.builder(Component.translatable("screen.coi.reset_defaults"), button -> {
            AppearanceConfig.resetToDefaults();
            rebuildWidgets();
        }).bounds(panelX + 16, rowY + 6, (PANEL_W - 40) / 2, 20).build());

        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
                .bounds(panelX + 24 + (PANEL_W - 40) / 2, rowY + 6, (PANEL_W - 40) / 2, 20).build());
    }

    private void flipSetting(String labelKey) {
        AppearanceConfig.AppearanceSettings settings = AppearanceConfig.getSettings();
        switch (labelKey) {
            case "screen.coi.appearance.traits_enabled" -> settings.enableAppearanceTraits ^= true;
            case "screen.coi.appearance.uniqueness_enabled" -> settings.enableUniquenessEffects ^= true;
            case "screen.coi.appearance.uniqueness_self" -> settings.uniquenessShowSelf ^= true;
            case "screen.coi.appearance.uniqueness_others" -> settings.uniquenessShowOthers ^= true;
        }
        AppearanceConfig.save();
    }

    private Component toggleLabel(String labelKey) {
        boolean enabled = switch (labelKey) {
            case "screen.coi.appearance.traits_enabled" -> AppearanceConfig.getSettings().enableAppearanceTraits;
            case "screen.coi.appearance.uniqueness_enabled" -> AppearanceConfig.getSettings().enableUniquenessEffects;
            case "screen.coi.appearance.uniqueness_self" -> AppearanceConfig.getSettings().uniquenessShowSelf;
            case "screen.coi.appearance.uniqueness_others" -> AppearanceConfig.getSettings().uniquenessShowOthers;
            default -> false;
        };
        return Component.empty()
                .append(Component.literal(enabled ? "● " : "○ ").withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.GRAY))
                .append(Component.translatable(labelKey));
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, this.width, this.height, 0x90000000);
        CoiStyle.drawCard(graphics, panelX, panelY, PANEL_W, panelH);

        graphics.centeredText(font, title, panelX + PANEL_W / 2, panelY + 12, CoiStyle.ACCENT);
        graphics.centeredText(font,
                Component.translatable("screen.coi.appearance_settings.hint").withStyle(ChatFormatting.GRAY),
                panelX + PANEL_W / 2, panelY + 27, 0xFFFFFFFF);

        int rowY = panelY + 46;
        for (Toggle toggle : toggles) {
            if (mouseY >= rowY && mouseY < rowY + 20 && mouseX >= panelX + 16 && mouseX < panelX + PANEL_W - 16) {
                graphics.fill(panelX + 16, rowY, panelX + PANEL_W - 16, rowY + 20, CoiStyle.ROW_HOVER);
                graphics.text(font,
                        Component.translatable(toggle.tooltipKey).withStyle(ChatFormatting.GRAY),
                        panelX + PANEL_W + 10, rowY + 6, 0xFFFFFFFF);
            }
            rowY += ROW_H;
        }

        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
