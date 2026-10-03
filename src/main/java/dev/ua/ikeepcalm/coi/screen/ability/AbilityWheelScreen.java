package dev.ua.ikeepcalm.coi.screen.ability;

import dev.ua.ikeepcalm.coi.domain.ability.model.AbilityInfo;
import dev.ua.ikeepcalm.coi.domain.ability.service.AbilityBindings;
import dev.ua.ikeepcalm.coi.domain.effect.visual.EffectPaint;
import dev.ua.ikeepcalm.coi.input.CoiKeyBindings;
import dev.ua.ikeepcalm.coi.screen.ScreenInput;
import dev.ua.ikeepcalm.coi.ui.AbilityIcons;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;

/**
 * Held open while the wheel key is down: the mouse aims a slot, releasing casts it. Movement
 * stays enabled, so the wheel can be used mid-fight.
 */
public class AbilityWheelScreen extends Screen {

    private static final long OPEN_MS = 200;
    private static final long SLOT_STAGGER_MS = 22;
    private final long openTime = System.currentTimeMillis();
    private int selectedSlot = -1;
    private int ticksOpen = 0;

    public AbilityWheelScreen() {
        super(Component.literal("Ability Wheel"));
    }

    private int wheelRadius() {
        int base = Math.min(this.width, this.height);
        return Math.clamp(base * 15L / 100, 60, 140);
    }

    private int slotRadius() {
        return Math.max(16, wheelRadius() * 25 / 80);
    }

    private int innerRadius() {
        return wheelRadius() * 30 / 80;
    }

    private static float easeOutBack(float t) {
        float c1 = 1.70158f;
        float c3 = c1 + 1f;
        float u = t - 1f;
        return 1f + c3 * u * u * u + c1 * u * u;
    }

    @Override
    protected void init() {
        super.init();
        this.minecraft.mouseHandler.releaseMouse();
    }

    @Override
    public void tick() {
        ticksOpen++;
        ScreenInput.keepMovementKeysAlive(this.minecraft);

        if (ticksOpen > 2 && !CoiKeyBindings.isKeyDown(CoiKeyBindings.abilityWheel)) {
            if (selectedSlot != -1) {
                String ability = AbilityBindings.getWheelAbility(selectedSlot);
                if (ability != null) {
                    CoiKeyBindings.useAbility(ability);
                }
            }
            this.onClose();
        }
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
        int wheelSize = AbilityBindings.getWheelSize();

        long elapsed = System.currentTimeMillis() - openTime;
        float openP = EffectPaint.clamp(elapsed / (float) OPEN_MS, 0f, 1f);

        // vanilla Screen already blurs its stratum; just fade in the dim layer
        graphics.fill(0, 0, this.width, this.height, (int) (0x80 * openP) << 24);

        double dx = mouseX - centerX;
        double dy = mouseY - centerY;
        double distance = Math.sqrt(dx * dx + dy * dy);

        if (distance > innerRadius()) {
            double rawAngle = Math.toDegrees(Math.atan2(dy, dx)) + 90;
            if (rawAngle < 0) rawAngle += 360;
            selectedSlot = (int) ((rawAngle + (360.0 / wheelSize / 2)) % 360) / (360 / wheelSize);
            if (selectedSlot >= wheelSize) selectedSlot = wheelSize - 1;
        } else {
            selectedSlot = -1;
        }

        renderSlots(graphics, centerX, centerY, wheelSize, elapsed);
        renderSelectedAbilityName(graphics, centerX, centerY, openP);

        graphics.fill(mouseX - 2, mouseY - 2, mouseX + 2, mouseY + 2, 0xFFFFFFFF);
    }

    private void renderSlots(GuiGraphicsExtractor graphics, int centerX, int centerY, int wheelSize, long elapsed) {
        for (int i = 0; i < wheelSize; i++) {
            float slotP = EffectPaint.clamp((elapsed - i * SLOT_STAGGER_MS) / (float) OPEN_MS, 0f, 1f);
            if (slotP <= 0f) continue;
            float ease = easeOutBack(slotP);

            double angle = Math.toRadians(i * (360.0 / wheelSize) - 90);
            int x = centerX + (int) (Math.cos(angle) * wheelRadius() * ease);
            int y = centerY + (int) (Math.sin(angle) * wheelRadius() * ease);

            boolean isSelected = i == selectedSlot;
            String abilityIdWithName = AbilityBindings.getWheelAbility(i);
            int pathway = abilityIdWithName != null
                    ? AbilityInfo.pathwayColor(AbilityInfo.extractId(abilityIdWithName)) & 0xFFFFFF
                    : 0x777777;

            int r = Math.round(slotRadius() * (isSelected ? 1.15f : 1f) * (0.75f + 0.25f * ease));
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
                float pulse = 0.5f + 0.5f * (float) Math.sin(elapsed * 0.012);
                graphics.outline(x - r - 3, y - r - 3, r * 2 + 6, r * 2 + 6,
                        EffectPaint.argb(pathway, (int) (100 * pulse)));
                graphics.outline(x - r - 5, y - r - 5, r * 2 + 10, r * 2 + 10,
                        EffectPaint.argb(pathway, (int) (45 * pulse)));
            }

            if (abilityIdWithName != null) {
                int iconSize = r * 2 - 10;
                AbilityIcons.draw(graphics, abilityIdWithName, x - iconSize / 2, y - iconSize / 2, iconSize, alpha);
            } else if (alpha >= 8) {
                graphics.centeredText(this.font, Component.literal("+"), x, y - 4, EffectPaint.argb(0x555555, alpha));
            }
        }
    }

    private void renderSelectedAbilityName(GuiGraphicsExtractor graphics, int centerX, int centerY, float openP) {
        int alpha = (int) (255 * openP);
        if (alpha < 8) return;

        if (selectedSlot != -1) {
            String abilityIdWithName = AbilityBindings.getWheelAbility(selectedSlot);
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
}
