package dev.ua.ikeepcalm.coi.client.screen;

import dev.ua.ikeepcalm.coi.client.CircleOfImaginationClient;
import dev.ua.ikeepcalm.coi.client.config.AbilityInfo;
import dev.ua.ikeepcalm.coi.client.effects.impl.EffectPaint;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;

public class AbilityWheelScreen extends Screen {

    private static final int WHEEL_RADIUS = 80;
    private static final int SLOT_RADIUS = 25;
    private static final int INNER_RADIUS = 30;

    private static final long OPEN_MS = 200;
    private static final long SLOT_STAGGER_MS = 22;
    private final long openTime = System.currentTimeMillis();
    private int selectedSlot = -1;
    private int ticksOpen = 0;

    public AbilityWheelScreen() {
        super(Component.literal("Ability Wheel"));
    }

    /**
     * Ease-out with a slight overshoot so slots land with a pop.
     */
    private static float easeOutBack(float t) {
        float c1 = 1.70158f;
        float c3 = c1 + 1f;
        float u = t - 1f;
        return 1f + c3 * u * u * u + c1 * u * u;
    }

    @Override
    protected void init() {
        super.init();
        if (this.minecraft != null) {
            // Force release mouse
            this.minecraft.mouseHandler.releaseMouse();
        }
    }

    @Override
    public void tick() {
        ticksOpen++;
        keepMovementKeysAlive();

        // Increase delay slightly and ensure robust key check
        if (ticksOpen > 2 && !CircleOfImaginationClient.isKeyDown(CircleOfImaginationClient.abilityWheel)) {
            if (selectedSlot != -1) {
                String ability = CircleOfImaginationClient.getWheelAbility(selectedSlot);
                if (ability != null) {
                    CircleOfImaginationClient.useAbilityById(ability);
                }
            }
            this.onClose();
        }
    }

    /**
     * Screens normally swallow keyboard input, freezing the player. Feed the
     * raw key state back into the movement bindings so the player can keep
     * moving while aiming the wheel.
     */
    private void keepMovementKeysAlive() {
        if (this.minecraft.player == null) return;
        var options = this.minecraft.options;
        KeyMapping[] movementKeys = {
                options.keyUp, options.keyDown, options.keyLeft, options.keyRight,
                options.keyJump, options.keyShift, options.keySprint
        };
        for (KeyMapping key : movementKeys) {
            key.setDown(CircleOfImaginationClient.isKeyDown(key));
        }
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);

        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int wheelSize = CircleOfImaginationClient.getWheelSize();

        long elapsed = System.currentTimeMillis() - openTime;
        float openP = EffectPaint.clamp(elapsed / (float) OPEN_MS, 0f, 1f);

        // Vanilla Screen already blurs its stratum; just fade in the dim layer
        graphics.fill(0, 0, this.width, this.height, (int) (0x80 * openP) << 24);

        double dx = mouseX - centerX;
        double dy = mouseY - centerY;
        double distance = Math.sqrt(dx * dx + dy * dy);

        if (distance > INNER_RADIUS) {
            double rawAngle = Math.toDegrees(Math.atan2(dy, dx)) + 90;
            if (rawAngle < 0) rawAngle += 360;
            selectedSlot = (int) ((rawAngle + (360.0 / wheelSize / 2)) % 360) / (360 / wheelSize);
            if (selectedSlot >= wheelSize) selectedSlot = wheelSize - 1;
        } else {
            selectedSlot = -1;
        }

        renderSlots(graphics, centerX, centerY, wheelSize, elapsed);
        renderSelectedAbilityName(graphics, centerX, centerY, openP);

        // Render a small cursor dot at the mouse position
        graphics.fill(mouseX - 2, mouseY - 2, mouseX + 2, mouseY + 2, 0xFFFFFFFF);
    }

    private void renderSlots(GuiGraphicsExtractor graphics, int centerX, int centerY, int wheelSize, long elapsed) {
        for (int i = 0; i < wheelSize; i++) {
            // Slots bloom outward from the center, one after another
            float slotP = EffectPaint.clamp((elapsed - i * SLOT_STAGGER_MS) / (float) OPEN_MS, 0f, 1f);
            if (slotP <= 0f) continue;
            float ease = easeOutBack(slotP);

            double angle = Math.toRadians(i * (360.0 / wheelSize) - 90);
            int x = centerX + (int) (Math.cos(angle) * WHEEL_RADIUS * ease);
            int y = centerY + (int) (Math.sin(angle) * WHEEL_RADIUS * ease);

            boolean isSelected = i == selectedSlot;
            String abilityIdWithName = CircleOfImaginationClient.getWheelAbility(i);
            int pathway = abilityIdWithName != null
                    ? AbilityInfo.pathwayColor(AbilityInfo.extractId(abilityIdWithName)) & 0xFFFFFF
                    : 0x777777;

            // The hovered slot leans toward the cursor: slightly larger, pathway-colored
            int r = Math.round(SLOT_RADIUS * (isSelected ? 1.15f : 1f) * (0.75f + 0.25f * ease));
            int alpha = (int) (255 * slotP);

            int bgColor = isSelected
                    ? EffectPaint.argb(pathway, (int) (alpha * 0.45f))
                    : EffectPaint.argb(0x000000, alpha / 2);
            int borderColor = isSelected
                    ? EffectPaint.argb(pathway, alpha)
                    : EffectPaint.argb(0xFFFFFF, alpha * 2 / 3);

            graphics.fill(x - r, y - r, x + r, y + r, bgColor);
            graphics.outline(x - r - 1, y - r - 1, r * 2 + 2, r * 2 + 2, borderColor);

            if (isSelected) {
                // Pulsing pathway glow around the hovered slot
                float pulse = 0.5f + 0.5f * (float) Math.sin(elapsed * 0.012);
                graphics.outline(x - r - 3, y - r - 3, r * 2 + 6, r * 2 + 6,
                        EffectPaint.argb(pathway, (int) (100 * pulse)));
                graphics.outline(x - r - 5, y - r - 5, r * 2 + 10, r * 2 + 10,
                        EffectPaint.argb(pathway, (int) (45 * pulse)));
            }

            if (abilityIdWithName != null) {
                renderAbilityIcon(graphics, abilityIdWithName, x, y, r * 2 - 10, alpha);
            } else if (alpha >= 8) {
                graphics.centeredText(this.font, Component.literal("+"), x, y - 4, EffectPaint.argb(0x555555, alpha));
            }
        }
    }

    private void renderSelectedAbilityName(GuiGraphicsExtractor graphics, int centerX, int centerY, float openP) {
        int alpha = (int) (255 * openP);
        if (alpha < 8) return;

        if (selectedSlot != -1) {
            String abilityIdWithName = CircleOfImaginationClient.getWheelAbility(selectedSlot);
            if (abilityIdWithName != null) {
                String name = AbilityInfo.extractDisplayName(abilityIdWithName);
                int pathway = AbilityInfo.pathwayColor(AbilityInfo.extractId(abilityIdWithName)) & 0xFFFFFF;
                graphics.centeredText(this.font, Component.literal(name), centerX, centerY - 10, EffectPaint.argb(pathway, alpha));
            } else {
                graphics.centeredText(this.font, Component.translatable("screen.coi.empty_slot"), centerX, centerY - 10, EffectPaint.argb(0xAAAAAA, alpha));
            }
        } else {
            graphics.centeredText(this.font, Component.translatable("screen.coi.select_ability"), centerX, centerY - 10, EffectPaint.argb(0xFFFFFF, alpha));
        }
    }

    private void renderAbilityIcon(GuiGraphicsExtractor context, String abilityIdWithName, int x, int y, int size, int alpha) {
        String id = AbilityInfo.extractId(abilityIdWithName);
        AbilityInfo info = CircleOfImaginationClient.getAbilityInfo(id);

        int iconX = x - size / 2;
        int iconY = y - size / 2;

        int pathwayColor = AbilityInfo.pathwayColor(id);
        context.fill(iconX, iconY, iconX + size, iconY + size, EffectPaint.argb(pathwayColor, alpha / 2));

        String category = info != null ? info.category() : "uncategorized";
        String tier = AbilityInfo.tierOf(id);
        Identifier texture = Identifier.fromNamespaceAndPath("coi-client", "textures/icons/" + category.toLowerCase() + "/" + tier + ".png");
        context.blit(RenderPipelines.GUI_TEXTURED, texture, iconX, iconY, 0, 0, size, size, size, size, EffectPaint.argb(0xFFFFFF, alpha));
    }

    @Override
    public void onClose() {
        super.onClose();
    }
}
