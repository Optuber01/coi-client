package dev.ua.ikeepcalm.coi.client.screen;

import dev.ua.ikeepcalm.coi.client.CircleOfImaginationClient;
import dev.ua.ikeepcalm.coi.client.config.ClientStateStore;
import dev.ua.ikeepcalm.coi.client.config.HudConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;

import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;

public class HudSettingsScreen extends Screen {

    private static final String[] PRESETS = {"Default", "Compact", "Large", "Minimal"};
    private static final String[] ANCHORS = {"TOP_LEFT", "TOP_CENTER", "BOTTOM_LEFT", "BOTTOM_CENTER"};
    private static final int ROW_SPACING = 35;
    private static final int FIELD_WIDTH = 52;
    private static final int SECTION_GAP = 24;
    private final Screen parent;
    private HudConfig.HudSettings settings;
    private Checkbox enabledCheckbox;
    private Checkbox showKeybindsCheckbox;
    private Checkbox showAbilityNamesCheckbox;
    private Checkbox showGlowEffectCheckbox;
    private Checkbox epilepsyModeCheckbox;
    private Checkbox showMadnessBarCheckbox;
    private Checkbox hallucinationsCheckbox;
    private EditBox hudXField;
    private EditBox hudYOffsetField;
    private EditBox slotSizeField;
    private EditBox slotSpacingField;
    private EditBox hudScaleField;
    private EditBox activeSlotsField;
    private EditBox wheelSlotsField;
    private EditBox madnessXOffsetField;
    private EditBox madnessYOffsetField;
    private Button resetButton;
    private Button presetButton;
    private Button madnessAnchorButton;
    private int currentPreset = 0;

    public HudSettingsScreen(Screen parent) {
        super(Component.translatable("screen.coi.hud_settings"));
        this.parent = parent;
        this.settings = new HudConfig.HudSettings();
        copySettings(HudConfig.getSettings(), this.settings);
    }

    private void copySettings(HudConfig.HudSettings from, HudConfig.HudSettings to) {
        to.enabled = from.enabled;
        to.hudX = from.hudX;
        to.hudYOffset = from.hudYOffset;
        to.slotSize = from.slotSize;
        to.slotSpacing = from.slotSpacing;
        to.showKeybinds = from.showKeybinds;
        to.showAbilityNames = from.showAbilityNames;
        to.showGlowEffect = from.showGlowEffect;
        to.hudScale = from.hudScale;
        to.wheelSlots = from.wheelSlots;
        to.activeAbilitySlots = from.activeAbilitySlots;
        to.epilepsyMode = from.epilepsyMode;
        to.showMadnessBar = from.showMadnessBar;
        to.madnessXOffset = from.madnessXOffset;
        to.madnessYOffset = from.madnessYOffset;
        to.madnessAnchor = from.madnessAnchor;
        to.effectSoundVolume = from.effectSoundVolume;
        to.enableHallucinations = from.enableHallucinations;
    }

    private SettingsLayout createLayout() {
        int buttonY = this.height - Math.max(40, this.height / 10);
        boolean twoColumns = this.width >= 560;
        int contentWidth = Math.min(this.width - 40, twoColumns ? 560 : 280);
        int gap = twoColumns ? SECTION_GAP : 0;
        int columnWidth = twoColumns ? (contentWidth - gap) / 2 : contentWidth;
        int leftX = (this.width - contentWidth) / 2;
        int rightX = twoColumns ? leftX + columnWidth + gap : leftX;
        int sliderWidth = Math.max(118, columnWidth - FIELD_WIDTH - 8);
        int topY = Math.max(58, this.height / 8);

        return new SettingsLayout(twoColumns, leftX, rightX, columnWidth, sliderWidth, buttonY, topY);
    }

    private EditBox addIntSliderField(int x, int y, SettingsLayout layout, String label, Component fieldLabel,
                                      int min, int max, int initialValue, IntConsumer setter) {
        final EditBox[] fieldRef = new EditBox[1];
        int clampedInitial = Math.clamp(initialValue, min, max);
        double sliderValue = (clampedInitial - min) / (double) (max - min);

        AbstractSliderButton slider = new AbstractSliderButton(x, y, layout.sliderWidth, 20, Component.literal(label + ": " + clampedInitial), sliderValue) {
            @Override
            protected void updateMessage() {
                int value = min + (int) Math.round(this.value * (max - min));
                setter.accept(value);
                this.setMessage(Component.literal(label + ": " + value));
                if (fieldRef[0] != null) {
                    fieldRef[0].setValue(String.valueOf(value));
                }
            }

            @Override
            protected void applyValue() {
                updateMessage();
            }
        };
        this.addRenderableWidget(slider);

        EditBox field = new EditBox(this.font, x + layout.sliderWidth + 8, y, FIELD_WIDTH, 20, fieldLabel);
        field.setValue(String.valueOf(clampedInitial));
        field.setResponder(text -> {
            try {
                setter.accept(Math.clamp(Integer.parseInt(text), min, max));
            } catch (NumberFormatException ignored) {
            }
        });
        fieldRef[0] = field;
        this.addRenderableWidget(field);
        return field;
    }

    private EditBox addDecimalSliderField(int x, int y, SettingsLayout layout, String label, Component fieldLabel,
                                          double min, double max, double initialValue, DoubleConsumer setter) {
        final EditBox[] fieldRef = new EditBox[1];
        double clampedInitial = Math.clamp(initialValue, min, max);
        double sliderValue = (clampedInitial - min) / (max - min);

        AbstractSliderButton slider = new AbstractSliderButton(x, y, layout.sliderWidth, 20, Component.literal(label + ": " + String.format("%.1f", clampedInitial)), sliderValue) {
            @Override
            protected void updateMessage() {
                double value = min + this.value * (max - min);
                value = Math.round(value * 10.0) / 10.0;
                setter.accept(value);
                this.setMessage(Component.literal(label + ": " + String.format("%.1f", value)));
                if (fieldRef[0] != null) {
                    fieldRef[0].setValue(String.format("%.1f", value));
                }
            }

            @Override
            protected void applyValue() {
                updateMessage();
            }
        };
        this.addRenderableWidget(slider);

        EditBox field = new EditBox(this.font, x + layout.sliderWidth + 8, y, FIELD_WIDTH, 20, fieldLabel);
        field.setValue(String.format("%.1f", clampedInitial));
        field.setResponder(text -> {
            try {
                setter.accept(Math.clamp(Double.parseDouble(text), min, max));
            } catch (NumberFormatException ignored) {
            }
        });
        fieldRef[0] = field;
        this.addRenderableWidget(field);
        return field;
    }

    private void drawSection(GuiGraphicsExtractor graphics, String title, int x, int y) {
        graphics.text(this.font, Component.literal(title).withStyle(ChatFormatting.GRAY), x, y, 0xFFA0A0A0);
    }

    private void drawSettingLabels(GuiGraphicsExtractor graphics, int x, int firstY, Component... labels) {
        for (int i = 0; i < labels.length; i++) {
            graphics.text(this.font, labels[i], x, firstY + i * ROW_SPACING - 12, 0xFFA0A0A0);
        }
    }

    @Override
    protected void init() {
        SettingsLayout layout = createLayout();
        int centerX = this.width / 2;

        enabledCheckbox = Checkbox.builder(Component.translatable("screen.coi.hud_enabled"), Minecraft.getInstance().font)
                .pos(layout.leftX, Math.max(32, layout.topY - 34)).selected(settings.enabled)
                .maxWidth(200)
                .build();
        this.addRenderableWidget(enabledCheckbox);

        this.addRenderableWidget(Button.builder(Component.translatable("screen.coi.show_tour"),
                _ -> {
                    ClientStateStore.setTourCompleted(false);
                    if (this.minecraft.player != null) {
                        this.minecraft.setScreen(new TourScreen());
                    }
                }).bounds(this.width - 130, 10, 120, 20).build());
        int leftY = layout.topY + 24;
        int rightY = layout.twoColumns ? leftY : leftY + ROW_SPACING * 7 + SECTION_GAP;

        hudXField = addIntSliderField(layout.leftX, leftY, layout, "X", Component.translatable("screen.coi.hud_x_field"), 0, 500, settings.hudX, value -> settings.hudX = value);
        hudYOffsetField = addIntSliderField(layout.leftX, leftY + ROW_SPACING, layout, "Y Offset", Component.translatable("screen.coi.hud_y_offset_field"), 0, 200, settings.hudYOffset, value -> settings.hudYOffset = value);
        slotSizeField = addIntSliderField(layout.leftX, leftY + ROW_SPACING * 2, layout, "Slot Size", Component.translatable("screen.coi.slot_size_field"), 20, 100, settings.slotSize, value -> settings.slotSize = value);
        slotSpacingField = addIntSliderField(layout.leftX, leftY + ROW_SPACING * 3, layout, "Spacing", Component.translatable("screen.coi.slot_spacing_field"), 30, 100, settings.slotSpacing, value -> settings.slotSpacing = value);
        hudScaleField = addDecimalSliderField(layout.leftX, leftY + ROW_SPACING * 4, layout, "Scale", Component.translatable("screen.coi.hud_scale_field"), 0.5, 2.0, settings.hudScale, value -> settings.hudScale = (float) value);
        activeSlotsField = addIntSliderField(layout.leftX, leftY + ROW_SPACING * 5, layout, "Key Slots", Component.translatable("screen.coi.key_slots_field"), 1, CircleOfImaginationClient.MAX_ABILITIES, settings.activeAbilitySlots, value -> settings.activeAbilitySlots = value);
        wheelSlotsField = addIntSliderField(layout.leftX, leftY + ROW_SPACING * 6, layout, "Wheel Slots", Component.translatable("screen.coi.wheel_slots_field"), 2, 16, settings.wheelSlots, value -> settings.wheelSlots = value);

        madnessXOffsetField = addIntSliderField(layout.rightX, rightY, layout, "Madness X", Component.translatable("screen.coi.madness_x_offset_field"), -500, 500, settings.madnessXOffset, value -> settings.madnessXOffset = value);
        madnessYOffsetField = addIntSliderField(layout.rightX, rightY + ROW_SPACING, layout, "Madness Y", Component.translatable("screen.coi.madness_y_offset_field"), 0, 200, settings.madnessYOffset, value -> settings.madnessYOffset = value);

        madnessAnchorButton = Button.builder(Component.literal("Anchor: " + settings.madnessAnchor),
                button -> {
                    int idx = 0;
                    for (int i = 0; i < ANCHORS.length; i++) {
                        if (ANCHORS[i].equalsIgnoreCase(settings.madnessAnchor)) {
                            idx = i;
                            break;
                        }
                    }
                    int nextIdx = (idx + 1) % ANCHORS.length;
                    settings.madnessAnchor = ANCHORS[nextIdx];
                    button.setMessage(Component.literal("Anchor: " + settings.madnessAnchor));
                }).bounds(layout.rightX, rightY + ROW_SPACING * 2, layout.columnWidth, 20).build();
        this.addRenderableWidget(madnessAnchorButton);

        showMadnessBarCheckbox = Checkbox.builder(Component.translatable("screen.coi.show_madness_bar"), Minecraft.getInstance().font)
                .pos(layout.rightX, rightY + ROW_SPACING * 3)
                .maxWidth(layout.columnWidth).onValueChange((checkbox, checked) -> settings.showMadnessBar = checked).selected(settings.showMadnessBar)
                .build();
        this.addRenderableWidget(showMadnessBarCheckbox);

        int displayY = Math.max(leftY + ROW_SPACING * 7, rightY + ROW_SPACING * 4) + SECTION_GAP;
        int displayLeftX = layout.leftX;
        int displayRightX = layout.twoColumns ? layout.rightX : layout.leftX;
        int displayColumnYOffset = layout.twoColumns ? 0 : 24;
        int displaySecondRowY = layout.twoColumns ? displayY + 24 : displayY + 48;

        showKeybindsCheckbox = Checkbox.builder(Component.translatable("screen.coi.show_keybinds"), Minecraft.getInstance().font)
                .pos(displayLeftX, displayY)
                .maxWidth(layout.columnWidth).onValueChange((checkbox, checked) -> settings.showKeybinds = checked).selected(settings.showKeybinds)
                .build();
        this.addRenderableWidget(showKeybindsCheckbox);

        showAbilityNamesCheckbox = Checkbox.builder(Component.translatable("screen.coi.show_ability_names"), Minecraft.getInstance().font)
                .pos(displayRightX, displayY + displayColumnYOffset)
                .maxWidth(layout.columnWidth).onValueChange((checkbox, checked) -> settings.showAbilityNames = checked).selected(settings.showAbilityNames)
                .build();
        this.addRenderableWidget(showAbilityNamesCheckbox);

        showGlowEffectCheckbox = Checkbox.builder(Component.translatable("screen.coi.show_glow_effect"), Minecraft.getInstance().font)
                .pos(displayLeftX, displaySecondRowY)
                .maxWidth(layout.columnWidth).onValueChange((checkbox, checked) -> settings.showGlowEffect = checked).selected(settings.showGlowEffect)
                .build();
        this.addRenderableWidget(showGlowEffectCheckbox);

        epilepsyModeCheckbox = Checkbox.builder(Component.translatable("screen.coi.epilepsy_mode"), Minecraft.getInstance().font)
                .pos(displayRightX, displaySecondRowY + displayColumnYOffset)
                .maxWidth(layout.columnWidth).onValueChange((checkbox, checked) -> settings.epilepsyMode = checked).selected(settings.epilepsyMode)
                .build();
        this.addRenderableWidget(epilepsyModeCheckbox);

        int displayThirdRowY = layout.twoColumns ? displayY + 48 : displayY + 96;

        hallucinationsCheckbox = Checkbox.builder(Component.translatable("screen.coi.enable_hallucinations"), Minecraft.getInstance().font)
                .pos(displayLeftX, displayThirdRowY)
                .maxWidth(layout.columnWidth).onValueChange((checkbox, checked) -> settings.enableHallucinations = checked).selected(settings.enableHallucinations)
                .build();
        this.addRenderableWidget(hallucinationsCheckbox);

        int initialVolume = Math.round(Math.clamp(settings.effectSoundVolume, 0f, 1f) * 100);
        AbstractSliderButton soundVolumeSlider = new AbstractSliderButton(displayRightX, displayThirdRowY + displayColumnYOffset, layout.columnWidth, 20,
                Component.translatable("screen.coi.effect_sound_volume").append(": " + initialVolume + "%"), initialVolume / 100.0) {
            @Override
            protected void updateMessage() {
                int value = (int) Math.round(this.value * 100);
                settings.effectSoundVolume = value / 100f;
                this.setMessage(Component.translatable("screen.coi.effect_sound_volume").append(": " + value + "%"));
            }

            @Override
            protected void applyValue() {
                updateMessage();
            }
        };
        this.addRenderableWidget(soundVolumeSlider);

        int buttonY = layout.buttonY;
        int buttonWidth = Math.min(100, this.width / 8);
        int smallButtonWidth = Math.min(90, this.width / 9);

        presetButton = Button.builder(Component.translatable("screen.coi.preset").append(": " + PRESETS[currentPreset]),
                button -> {
                    currentPreset = (currentPreset + 1) % PRESETS.length;
                    button.setMessage(Component.translatable("screen.coi.preset").append(": " + PRESETS[currentPreset]));
                    applyPreset(currentPreset);
                }).bounds(centerX - Math.min(205, this.width / 3), buttonY, buttonWidth, 20).build();
        this.addRenderableWidget(presetButton);

        resetButton = Button.builder(Component.translatable("screen.coi.reset_defaults"),
                button -> {
                    resetToDefaults();
                    this.init();
                }).bounds(centerX - buttonWidth, buttonY, smallButtonWidth, 20).build();
        this.addRenderableWidget(resetButton);

        this.addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), button -> this.onClose()).bounds(centerX - 5, buttonY, smallButtonWidth, 20).build());

        this.addRenderableWidget(Button.builder(Component.translatable("gui.done"),
                button -> {
                    saveSettings();
                    this.onClose();
                }).bounds(centerX + Math.min(100, this.width / 10), buttonY, smallButtonWidth, 20).build());
    }

    private void applyPreset(int preset) {
        switch (preset) {
            case 0: // Default - Safe values that work on all GUI scales
                settings.hudX = 10;
                settings.hudYOffset = 60;
                settings.slotSize = 40;
                settings.slotSpacing = 50;
                settings.hudScale = 1.0f;
                settings.showKeybinds = true;
                settings.showAbilityNames = true;
                settings.showGlowEffect = true;
                settings.showMadnessBar = true;
                settings.madnessXOffset = 0;
                settings.madnessYOffset = 55;
                settings.madnessAnchor = "TOP_LEFT";
                break;
            case 1: // Compact - Small and minimal
                settings.hudX = 5;
                settings.hudYOffset = 40;
                settings.slotSize = 30;
                settings.slotSpacing = 35;
                settings.hudScale = 0.8f;
                settings.showKeybinds = true;
                settings.showAbilityNames = false;
                settings.showGlowEffect = false;
                settings.showMadnessBar = true;
                settings.madnessXOffset = 0;
                settings.madnessYOffset = 35;
                settings.madnessAnchor = "TOP_LEFT";
                break;
            case 2: // Large - Bigger but still safe
                settings.hudX = 15;
                settings.hudYOffset = 80;
                settings.slotSize = 55;
                settings.slotSpacing = 65;
                settings.hudScale = 1.2f;
                settings.showKeybinds = true;
                settings.showAbilityNames = true;
                settings.showGlowEffect = true;
                settings.showMadnessBar = true;
                settings.madnessXOffset = 0;
                settings.madnessYOffset = 70;
                settings.madnessAnchor = "TOP_LEFT";
                break;
            case 3: // Minimal - Very small and clean
                settings.hudX = 3;
                settings.hudYOffset = 30;
                settings.slotSize = 25;
                settings.slotSpacing = 30;
                settings.hudScale = 0.7f;
                settings.showKeybinds = false;
                settings.showAbilityNames = false;
                settings.showGlowEffect = false;
                settings.showMadnessBar = false;
                settings.madnessXOffset = 0;
                settings.madnessYOffset = 25;
                settings.madnessAnchor = "TOP_LEFT";
                break;
        }
        refreshWidgets();
    }

    private void resetToDefaults() {
        settings = new HudConfig.HudSettings();
        currentPreset = 0;
        refreshWidgets();
    }

    private void refreshWidgets() {
        hudXField.setValue(String.valueOf(settings.hudX));
        hudYOffsetField.setValue(String.valueOf(settings.hudYOffset));
        slotSizeField.setValue(String.valueOf(settings.slotSize));
        slotSpacingField.setValue(String.valueOf(settings.slotSpacing));
        hudScaleField.setValue(String.format("%.1f", settings.hudScale));
        activeSlotsField.setValue(String.valueOf(settings.activeAbilitySlots));
        wheelSlotsField.setValue(String.valueOf(settings.wheelSlots));
        madnessXOffsetField.setValue(String.valueOf(settings.madnessXOffset));
        madnessYOffsetField.setValue(String.valueOf(settings.madnessYOffset));
        if (madnessAnchorButton != null) {
            madnessAnchorButton.setMessage(Component.literal("Anchor: " + settings.madnessAnchor));
        }

        presetButton.setMessage(Component.translatable("screen.coi.preset").append(": " + PRESETS[currentPreset]));
    }

    private void saveSettings() {
        settings.enabled = enabledCheckbox.selected();
        settings.showKeybinds = showKeybindsCheckbox.selected();
        settings.showAbilityNames = showAbilityNamesCheckbox.selected();
        settings.showGlowEffect = showGlowEffectCheckbox.selected();
        settings.epilepsyMode = epilepsyModeCheckbox.selected();
        settings.showMadnessBar = showMadnessBarCheckbox.selected();
        settings.enableHallucinations = hallucinationsCheckbox.selected();

        HudConfig.setSettings(settings);
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);

        graphics.centeredText(this.font, this.title, this.width / 2, 20, 0xFFFFFFFF);

        SettingsLayout layout = createLayout();
        int leftY = layout.topY + 24;
        int rightY = layout.twoColumns ? leftY : leftY + ROW_SPACING * 7 + SECTION_GAP;
        int displayY = Math.max(leftY + ROW_SPACING * 7, rightY + ROW_SPACING * 4) + SECTION_GAP;

        drawSection(graphics, "Ability HUD", layout.leftX, leftY - 24);
        drawSettingLabels(graphics, layout.leftX, leftY,
                Component.translatable("screen.coi.hud_x"),
                Component.translatable("screen.coi.hud_y_offset"),
                Component.translatable("screen.coi.slot_size"),
                Component.translatable("screen.coi.slot_spacing"),
                Component.translatable("screen.coi.hud_scale"),
                Component.translatable("screen.coi.key_slots"),
                Component.translatable("screen.coi.wheel_slots"));

        drawSection(graphics, "Madness HUD", layout.rightX, rightY - 24);
        drawSettingLabels(graphics, layout.rightX, rightY,
                Component.translatable("screen.coi.madness_x_offset"),
                Component.translatable("screen.coi.madness_y_offset"),
                Component.literal("Madness Anchor"));

        drawSection(graphics, "Display", layout.leftX, displayY - 14);

        if (!settings.enabled) {
            graphics.centeredText(this.font, Component.translatable("screen.coi.hud_disabled_warning").withStyle(ChatFormatting.RED),
                    this.width / 2, this.height - 80, 0xFFFF5555);
        }

        renderTooltips(graphics, mouseX, mouseY);
    }

    private void renderTooltips(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        if (resetButton.isHovered()) {
            context.setTooltipForNextFrame(this.font, Component.translatable("screen.coi.reset_defaults.tooltip"), mouseX, mouseY);
        } else if (presetButton.isHovered()) {
            context.setTooltipForNextFrame(this.font, Component.translatable("screen.coi.preset.tooltip"), mouseX, mouseY);
        }
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }

    private record SettingsLayout(boolean twoColumns, int leftX, int rightX, int columnWidth, int sliderWidth,
                                  int buttonY, int topY) {
    }
}
