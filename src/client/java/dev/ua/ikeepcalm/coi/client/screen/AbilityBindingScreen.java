package dev.ua.ikeepcalm.coi.client.screen;

import dev.ua.ikeepcalm.coi.client.CircleOfImaginationClient;
import dev.ua.ikeepcalm.coi.client.gesture.GestureType;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;

import java.util.List;

public class AbilityBindingScreen extends Screen {

    private static final int ITEMS_PER_PAGE = 6;
    private static final int MODE_KEYS = 0;
    private static final int MODE_WHEEL = 1;
    private static final int MODE_GESTURES = 2;

    private final Screen parent;
    private AbilityDropdownWidget[] abilityDropdowns;
    private AbilityDropdownWidget[] wheelDropdowns;
    private AbilityDropdownWidget[] gestureDropdowns;
    private Button clearAllButton;
    private Button settingsButton;
    private Button modeToggleButton;
    private int contentHeight;
    private int mode = MODE_KEYS;
    private int currentPage = 0;

    public AbilityBindingScreen(Screen parent) {
        super(Component.translatable("screen.coi.ability_binding"));
        this.parent = parent;
    }

    private int totalItems() {
        return switch (mode) {
            case MODE_WHEEL -> CircleOfImaginationClient.getWheelSize();
            case MODE_GESTURES -> GestureType.values().length;
            default -> CircleOfImaginationClient.getMaxAbilities();
        };
    }

    @Override
    protected void init() {
        this.clearWidgets();
        // Request abilities from server when screen opens
        CircleOfImaginationClient.requestAbilitiesFromServer();

        List<String> abilities = CircleOfImaginationClient.getAvailableAbilities();

        // For testing purposes, add sample abilities if none are available
        if (abilities.isEmpty()) {
            System.out.println("COI Client: No abilities received from server, adding test abilities");
            CircleOfImaginationClient.addTestAbilities();
            abilities = CircleOfImaginationClient.getAvailableAbilities();
        }

        int maxAbilities = CircleOfImaginationClient.getMaxAbilities();
        int activeSlots = CircleOfImaginationClient.getActiveAbilitySlots();
        int wheelSize = CircleOfImaginationClient.getWheelSize();
        int gestureCount = GestureType.values().length;
        int totalItems = totalItems();
        int totalPages = (totalItems + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE;

        if (currentPage >= totalPages) currentPage = Math.max(0, totalPages - 1);

        abilityDropdowns = new AbilityDropdownWidget[maxAbilities];
        wheelDropdowns = new AbilityDropdownWidget[wheelSize];
        gestureDropdowns = new AbilityDropdownWidget[gestureCount];

        int centerX = this.width / 2;
        int topMargin = 60;
        int spacing = 40;
        int dropdownWidth = Math.clamp(this.width / 3, 200, this.width - 40);
        int dropdownHeight = 20;

        int startIdx = currentPage * ITEMS_PER_PAGE;
        int endIdx = Math.min(startIdx + ITEMS_PER_PAGE, totalItems);

        // Create dropdowns for current page (inactive key slots get no dropdown —
        // they render grayed out with a hint instead)
        for (int i = startIdx; i < endIdx; i++) {
            if (mode == MODE_KEYS && i >= activeSlots) continue;
            final int slot = i;
            int y = topMargin + ((i - startIdx) * spacing);

            String current = switch (mode) {
                case MODE_WHEEL -> CircleOfImaginationClient.getWheelAbility(slot);
                case MODE_GESTURES -> CircleOfImaginationClient.getGestureAbility(slot);
                default -> CircleOfImaginationClient.getBoundAbility(slot);
            };

            AbilityDropdownWidget dropdown = new AbilityDropdownWidget(
                    centerX - dropdownWidth / 2, y, dropdownWidth, dropdownHeight,
                    CircleOfImaginationClient::getAvailableAbilities,
                    current,
                    selected -> {
                        switch (mode) {
                            case MODE_WHEEL -> CircleOfImaginationClient.setWheelAbility(slot, selected);
                            case MODE_GESTURES -> CircleOfImaginationClient.setGestureAbility(slot, selected);
                            default -> CircleOfImaginationClient.setBoundAbility(slot, selected);
                        }
                    }
            );

            switch (mode) {
                case MODE_WHEEL -> wheelDropdowns[slot] = dropdown;
                case MODE_GESTURES -> gestureDropdowns[slot] = dropdown;
                default -> abilityDropdowns[slot] = dropdown;
            }

            this.addRenderableWidget(dropdown);
        }

        int buttonY = this.height - 35;

        // Pagination buttons
        if (totalPages > 1) {
            this.addRenderableWidget(Button.builder(Component.literal("<"), b -> {
                currentPage--;
                this.init();
            }).bounds(centerX - 180, buttonY - 25, 20, 20).build()).active = currentPage > 0;

            this.addRenderableWidget(Button.builder(Component.literal(">"), b -> {
                currentPage++;
                this.init();
            }).bounds(centerX + 160, buttonY - 25, 20, 20).build()).active = currentPage < totalPages - 1;

            // Page indicator text handled in render
        }

        // Button label names the mode it switches TO (keys → wheel → gestures → keys)
        String nextModeKey = switch (mode) {
            case MODE_KEYS -> "screen.coi.show_wheel_mode";
            case MODE_WHEEL -> "screen.coi.show_gestures_mode";
            default -> "screen.coi.show_keybinds_mode";
        };
        modeToggleButton = Button.builder(Component.translatable(nextModeKey),
                button -> {
                    mode = (mode + 1) % 3;
                    currentPage = 0;
                    this.init();
                }).bounds(centerX - 155, buttonY - 25, 310, 20).build();
        this.addRenderableWidget(modeToggleButton);

        clearAllButton = Button.builder(Component.translatable("screen.coi.clear_all"),
                button -> {
                    switch (mode) {
                        case MODE_WHEEL -> {
                            for (int i = 0; i < wheelSize; i++) {
                                CircleOfImaginationClient.setWheelAbility(i, null);
                            }
                        }
                        case MODE_GESTURES -> {
                            for (int i = 0; i < gestureCount; i++) {
                                CircleOfImaginationClient.setGestureAbility(i, null);
                            }
                        }
                        default -> {
                            for (int i = 0; i < activeSlots; i++) {
                                CircleOfImaginationClient.setBoundAbility(i, null);
                            }
                        }
                    }
                    this.init();
                }).bounds(centerX - 105, buttonY, 100, 20).build();
        this.addRenderableWidget(clearAllButton);

        settingsButton = Button.builder(Component.translatable("screen.coi.hud_settings"),
                button -> {
                    this.onClose();
                    Minecraft.getInstance().setScreen(new HudSettingsScreen(null));
                }).bounds(this.width - 130, 10, 120, 20).build();

        this.addRenderableWidget(settingsButton);

        this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> this.onClose()).bounds(centerX + 5, buttonY, 100, 20).build());
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)) {
            return true;
        }

        int totalPages = (totalItems() + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE;

        if (verticalAmount > 0 && currentPage > 0) {
            currentPage--;
            this.init();
            return true;
        } else if (verticalAmount < 0 && currentPage < totalPages - 1) {
            currentPage++;
            this.init();
            return true;
        }
        return false;
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        // Draw background and header info BEFORE super call (which renders widgets)
        graphics.fill(0, 0, this.width, this.height, 0x80000000);

        graphics.centeredText(this.font,
                this.title, this.width / 2, 10, 0xFFFFFFFF);

        String headerKey = switch (mode) {
            case MODE_WHEEL -> "screen.coi.wheel_bindings";
            case MODE_GESTURES -> "screen.coi.gesture_bindings";
            default -> "screen.coi.key_bindings";
        };
        graphics.centeredText(this.font, Component.translatable(headerKey), this.width / 2, 25, 0xFFAAAAAA);

        int totalItems = totalItems();
        int totalPages = (totalItems + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE;
        if (totalPages > 1) {
            graphics.centeredText(this.font, Component.literal((currentPage + 1) + " / " + totalPages), this.width / 2, this.height - 55, 0xFFFFFFFF);
        }

        List<String> abilities = CircleOfImaginationClient.getAvailableAbilities();
        if (abilities.isEmpty()) {
            graphics.centeredText(this.font, Component.translatable("screen.coi.no_abilities").withStyle(ChatFormatting.RED),
                    this.width / 2, 40, 0xFFFF5555);
        } else if (mode == MODE_GESTURES) {
            Component gestureKey = KeyMappingHelper.getBoundKeyOf(CircleOfImaginationClient.gestureCast).getDisplayName();
            graphics.centeredText(this.font, Component.translatable("screen.coi.gesture_hint", gestureKey),
                    this.width / 2, 40, 0xFF888888);
        }

        int centerX = this.width / 2;
        int topMargin = 60;
        int spacing = 40;
        int dropdownWidth = Math.clamp(this.width / 3, 200, this.width - 40);

        int startIdx = currentPage * ITEMS_PER_PAGE;
        int endIdx = Math.min(startIdx + ITEMS_PER_PAGE, totalItems);

        for (int i = startIdx; i < endIdx; i++) {
            int y = topMargin + ((i - startIdx) * spacing) - 15;
            int x = centerX - dropdownWidth / 2;
            switch (mode) {
                case MODE_WHEEL -> renderSlotInfo(graphics, i, x, y, Component.literal(String.valueOf(i + 1)), true);
                case MODE_GESTURES -> renderGestureSlotInfo(graphics, i, x, y);
                default -> {
                    Component key = KeyMappingHelper.getBoundKeyOf(CircleOfImaginationClient.abilityKeys[i]).getDisplayName();
                    renderSlotInfo(graphics, i, x, y, key, false);
                }
            }
        }

        // Render widgets (buttons and dropdowns)
        super.extractRenderState(graphics, mouseX, mouseY, a);

        renderTooltips(graphics, mouseX, mouseY);

        renderExpandedDropdowns(graphics, mouseX, mouseY, a);
    }

    private void renderExpandedDropdowns(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        AbilityDropdownWidget[] current = switch (mode) {
            case MODE_WHEEL -> wheelDropdowns;
            case MODE_GESTURES -> gestureDropdowns;
            default -> abilityDropdowns;
        };
        if (current != null) {
            for (AbilityDropdownWidget dropdown : current) {
                if (dropdown != null && dropdown.isExpanded()) {
                    dropdown.renderExpanded(graphics, mouseX, mouseY, delta);
                }
            }
        }
    }

    private void renderSlotInfo(GuiGraphicsExtractor graphics, int slot, int x, int y, Component key, boolean isWheel) {
        boolean inactive = !isWheel && slot >= CircleOfImaginationClient.getActiveAbilitySlots();
        Component label = Component.translatable(isWheel ? "screen.coi.wheel_slot" : "screen.coi.ability" + (slot + 1) + "_label");
        if (isWheel) label = label.copy().append(" " + (slot + 1));

        graphics.text(this.font, label, x, y, inactive ? 0xFF606060 : 0xFFA0A0A0);

        if (inactive) {
            graphics.text(this.font, Component.translatable("screen.coi.slot_inactive"), x + this.font.width(label) + 5, y, 0xFF606060);
            return;
        }

        if (!isWheel) {
            graphics.text(this.font, "Use [" + key.tryCollapseToString() + "]", x + this.font.width(label) + 5, y, 0xFFFFFF55);
        }

        String bound = isWheel ? CircleOfImaginationClient.getWheelAbility(slot) : CircleOfImaginationClient.getBoundAbility(slot);
        renderBoundAbility(graphics, bound, x, y);
    }

    private void renderGestureSlotInfo(GuiGraphicsExtractor graphics, int slot, int x, int y) {
        GestureType type = GestureType.values()[slot];
        type.drawPreview(graphics, x, y - 1, 11, 0xFFAAAAAA);
        graphics.text(this.font, type.displayName(), x + 17, y, 0xFFA0A0A0);
        renderBoundAbility(graphics, CircleOfImaginationClient.getGestureAbility(slot), x, y);
    }

    private void renderBoundAbility(GuiGraphicsExtractor graphics, String bound, int x, int y) {
        if (bound != null && bound.contains(" - ")) {
            String abilityName = bound.split(" - ")[1];
            Component boundText = Component.literal("→ " + abilityName).withStyle(ChatFormatting.GREEN);
            int textOffset = Math.clamp(this.width / 6, 100, 150);
            graphics.text(this.font, boundText, x + textOffset, y, 0xFF55FF55);
        }
    }

    private void renderTooltips(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (clearAllButton.isHovered()) {
            graphics.setTooltipForNextFrame(this.font, Component.translatable("screen.coi.clear_all.tooltip"), mouseX, mouseY);
        }
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }
}
