package dev.ua.ikeepcalm.coi.hud.render;

import dev.ua.ikeepcalm.coi.config.HudConfig;
import dev.ua.ikeepcalm.coi.domain.ability.model.AbilityInfo;
import dev.ua.ikeepcalm.coi.domain.ability.service.AbilityBindings;
import dev.ua.ikeepcalm.coi.domain.ability.service.AbilityRegistry;
import dev.ua.ikeepcalm.coi.domain.effect.visual.EffectPaint;
import dev.ua.ikeepcalm.coi.hud.HudGaslight;
import dev.ua.ikeepcalm.coi.input.CoiKeyBindings;
import dev.ua.ikeepcalm.coi.ui.AbilityIcons;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.Objects;

/**
 * One ability slot box. The widget draws from the box's top-left at whatever {@code size} it is
 * given; {@code AbilityOverlay} decides where that is and pushes the pose scale, so nothing here
 * knows about anchors or offsets. Cooldowns are timed off wall-clock millis, not ticks.
 */
public class AbilitySlotWidget {

    private static final int BORDER_COLOR = 0xFF90EE90;
    private static final int BACKGROUND_GRADIENT_TOP = 0xFF98FB98;
    private static final int BACKGROUND_GRADIENT_BOTTOM = 0xFF0A0A0A;
    private static final int COOLDOWN_COLOR = 0xC0000000;
    private static final int READY_BORDER_COLOR = 0xFF228B22;
    private static final int READY_OVERLAY_COLOR = 0x20228B22;
    private static final int GLOW_COLOR = 0x3C32CD32;
    private static final int KEYBIND_COLOR = 0xFFFFE135;
    private static final int KEYBIND_BACKGROUND = 0xC0000000;
    private static final int ABILITY_NAME_COLOR = 0xFFE0E0E0;
    private static final int SHADOW_COLOR = 0x60000000;
    private static final int TOGGLE_COLOR = 0xFF55FFFF;
    private static final int LOCKED_TINT = 0x80FF3030;
    private static final int LOCKED_NAME_COLOR = 0xFFFF5555;

    private static final int ICON_INSET = 3;
    private static final int CHIP_H = 10;
    private static final int CHIP_PAD = 2;
    private static final int CHIP_MARGIN = 2;
    private static final int CHIP_GAP = 2;
    private static final int NAME_MAX_CHARS = 12;
    private static final float COOLDOWN_WHOLE_SECONDS = 10f;
    private static final int TICKS_PER_SECOND = 20;
    private static final long MS_PER_TICK = 50L;

    private final int slotIndex;
    private final SlotAnimations animations = new SlotAnimations();
    private String abilityId;
    /**
     * Null on protocol-1 servers and for ids the server has never described.
     */
    private AbilityInfo info;
    private boolean toggled;
    private String categoryLabel;
    private int cooldownTicks;
    private int maxCooldownTicks;
    private long lastUseTime;

    public AbilitySlotWidget(int slotIndex) {
        this.slotIndex = slotIndex;
        this.cooldownTicks = 0;
        this.maxCooldownTicks = 0;
    }

    public void render(GuiGraphicsExtractor context, int x, int y, int size, float tickDelta) {
        Minecraft client = Minecraft.getInstance();
        Font textRenderer = client.font;
        HudConfig.HudSettings settings = HudConfig.getSettings();

        boolean hasAbility = abilityId != null;
        boolean onCooldown = cooldownTicks > 0;
        boolean isReady = hasAbility && !onCooldown;

        long now = System.currentTimeMillis();
        boolean popped = animations.pushCastPop(context, x, y, size, now);

        renderChrome(context, x, y, size, isReady, settings.showGlowEffect);

        if (hasAbility) {
            renderAbilityIcon(context, x, y, size);

            if (onCooldown) {
                renderCooldownOverlay(context, x, y, size, tickDelta);
                renderCooldownText(context, textRenderer, x, y, size);
            }

            if (settings.showAbilityNames) {
                renderAbilityName(context, textRenderer, x, y, size);
            }
        }

        if (settings.showKeybinds) {
            renderKeybind(context, textRenderer, x, y, size);
        }

        if (toggled) {
            renderToggleState(context, textRenderer, x, y, size, now, settings.epilepsyMode);
        }

        animations.renderReadyFlash(context, x, y, size, now);
        if (hasAbility) {
            animations.renderCastRing(context, x, y, size, now,
                    AbilityInfo.pathwayColor(AbilityInfo.extractId(abilityId)) & 0xFFFFFF);
        }

        if (popped) {
            context.pose().popMatrix();
        }

        if (onCooldown && cooldownTicks > 0) {
            updateCooldown();
        }
    }

    private void renderChrome(GuiGraphicsExtractor context, int x, int y, int size,
                              boolean isReady, boolean showGlow) {
        context.fill(x + 2, y + 2, x + size + 3, y + size + 3, SHADOW_COLOR);

        if (isReady && showGlow) {
            context.fill(x - 2, y - 2, x + size + 2, y + size + 2, GLOW_COLOR);
        }

        int borderColor = isReady ? READY_BORDER_COLOR : BORDER_COLOR;
        context.fill(x - 1, y - 1, x + size + 1, y + size + 1, borderColor);

        context.fill(x, y, x + size, y + size / 2, BACKGROUND_GRADIENT_TOP);
        context.fill(x, y + size / 2, x + size, y + size, BACKGROUND_GRADIENT_BOTTOM);

        if (isReady) {
            context.fill(x, y, x + size, y + size, READY_OVERLAY_COLOR);
        }
    }

    public void triggerCastAnimation() {
        animations.triggerCast();
    }

    private void renderAbilityIcon(GuiGraphicsExtractor context, int x, int y, int size) {
        if (abilityId == null) return;

        int iconX = x + ICON_INSET;
        int iconY = y + ICON_INSET;
        int iconSize = size - ICON_INSET * 2;

        AbilityIcons.draw(context, abilityId, iconX, iconY, iconSize, 255);

        if (isUnavailable()) {
            context.fill(iconX, iconY, iconX + iconSize, iconY + iconSize, LOCKED_TINT);
        }
    }

    private void renderToggleState(GuiGraphicsExtractor context, Font font, int x, int y, int size,
                                   long now, boolean epilepsyMode) {
        float pulse = epilepsyMode ? 1f : 0.72f + 0.28f * (float) Math.sin(now * 0.003);
        int color = EffectPaint.argb(TOGGLE_COLOR, (int) (255 * pulse));
        context.outline(x - 1, y - 1, size + 2, size + 2, color);
        context.outline(x - 2, y - 2, size + 4, size + 4, EffectPaint.argb(TOGGLE_COLOR, (int) (90 * pulse)));

        int tagW = toggleTagWidth(font);
        context.fill(x + 1, y + 1, x + 1 + tagW, y + 1 + CHIP_H, KEYBIND_BACKGROUND);
        context.text(font, I18n.get("hud.coi.toggle_on"), x + 3, y + 2, color, true);
    }

    /** Measured, not assumed: the keybind chip has to stay clear of it and the word is translated. */
    private static int toggleTagWidth(Font font) {
        return font.width(I18n.get("hud.coi.toggle_on")) + 4;
    }

    private boolean isUnavailable() {
        return info != null && info.isUnavailable();
    }

    private void renderCooldownOverlay(GuiGraphicsExtractor context, int x, int y, int size, float tickDelta) {
        if (maxCooldownTicks <= 0) return;

        float progress = (cooldownTicks - tickDelta) / maxCooldownTicks;
        progress = Mth.clamp(progress, 0.0f, 1.0f);

        int overlayHeight = (int) (size * progress);
        if (overlayHeight > 0) {
            context.fill(x, y, x + size, y + overlayHeight, COOLDOWN_COLOR);
        }
    }

    private void renderCooldownText(GuiGraphicsExtractor context, Font textRenderer, int x, int y, int size) {
        if (cooldownTicks <= 0) return;

        float seconds = cooldownTicks / (float) TICKS_PER_SECOND;
        String cooldownText = seconds >= COOLDOWN_WHOLE_SECONDS
                ? String.valueOf((int) seconds)
                : String.format("%.1f", seconds);

        String fake = HudGaslight.cooldownOverride(slotIndex);
        if (fake != null) {
            cooldownText = fake;
        }

        int textWidth = textRenderer.width(cooldownText);
        int textX = x + (size - textWidth) / 2;
        int textY = y + size / 2 - 4;

        context.text(textRenderer, cooldownText, textX, textY, 0xFFFFFF, true);
    }

    private void updateCooldown() {
        if (cooldownTicks > 0 && lastUseTime > 0) {
            long currentTime = System.currentTimeMillis();
            long elapsedTime = currentTime - lastUseTime;
            int elapsedTicks = (int) (elapsedTime / MS_PER_TICK);

            cooldownTicks = Math.max(0, maxCooldownTicks - elapsedTicks);

            if (cooldownTicks <= 0) {
                lastUseTime = 0;
                animations.triggerReadyFlash();
            }
        }
    }

    private void renderAbilityName(GuiGraphicsExtractor context, Font textRenderer, int x, int y, int size) {
        if (abilityId == null) return;

        String displayName = AbilityInfo.extractDisplayName(abilityId);
        if (displayName == null) return;

        if (categoryLabel != null && !categoryLabel.isEmpty()) {
            displayName = displayName + " · " + categoryLabel;
        }
        if (displayName.length() > NAME_MAX_CHARS) {
            displayName = displayName.substring(0, NAME_MAX_CHARS) + "...";
        }

        int textWidth = textRenderer.width(displayName);
        int textX = x + (size - textWidth) / 2;
        int textY = y + size + 3;

        context.text(textRenderer, displayName, textX + 1, textY + 1, 0x80000000, false);
        if (isUnavailable()) {
            Component struck = Component.literal(displayName).withStyle(ChatFormatting.STRIKETHROUGH);
            context.text(textRenderer, struck, textX, textY, LOCKED_NAME_COLOR, true);
        } else {
            context.text(textRenderer, displayName, textX, textY, ABILITY_NAME_COLOR, true);
        }
    }

    /** Unbound slots get no chip at all: "Not bound" is noise on a box this small. */
    private void renderKeybind(GuiGraphicsExtractor context, Font textRenderer, int x, int y, int size) {
        KeyMapping keyBinding = getKeyBinding();
        if (keyBinding == null || keyBinding.isUnbound()) return;

        String keyText = HudGaslight.corruptKeybind(slotIndex, KeyMappingHelper.getBoundKeyOf(keyBinding).getDisplayName().getString());

        int budget = keyLabelBudget(textRenderer, size);
        if (budget <= 0) return;
        if (textRenderer.width(keyText) > budget) {
            keyText = textRenderer.plainSubstrByWidth(keyText, budget);
            if (keyText.isEmpty()) return;
        }

        int textWidth = textRenderer.width(keyText);
        int bgWidth = textWidth + CHIP_PAD * 2;

        int bgX = x + size - bgWidth - CHIP_MARGIN;
        int bgY = y + 2;
        int textX = bgX + CHIP_PAD;
        int textY = bgY + 1;

        context.fill(bgX, bgY, bgX + bgWidth, bgY + CHIP_H, KEYBIND_BACKGROUND);
        context.fill(bgX, bgY, bgX + bgWidth, bgY + 1, 0xFF555555);
        context.text(textRenderer, keyText, textX, textY, KEYBIND_COLOR, true);
    }

    /**
     * Measured in pixels, not characters: {@code LALT} is four characters and fits,
     * {@code BUTTON 4} is eight and never could.
     */
    private int keyLabelBudget(Font font, int size) {
        int budget = size - CHIP_MARGIN * 2 - CHIP_PAD * 2;
        return toggled ? budget - toggleTagWidth(font) - CHIP_GAP : budget;
    }

    private KeyMapping getKeyBinding() {
        if (slotIndex >= 0 && slotIndex < AbilityBindings.MAX_ABILITIES) {
            return CoiKeyBindings.abilityKey(slotIndex);
        }
        return null;
    }

    public void setAbility(String abilityId) {
        if (!Objects.equals(this.abilityId, abilityId)) setCooldown(0, 0);
        this.abilityId = abilityId;
        this.categoryLabel = null;
        if (abilityId != null && abilityId.contains(" - ")) {
            this.info = AbilityRegistry.getAbilityInfo(AbilityInfo.extractId(abilityId));
            this.toggled = info != null && info.active();
        } else {
            this.info = null;
            this.toggled = false;
        }
    }

    public void setCooldown(int cooldownTicks) {
        this.cooldownTicks = cooldownTicks;
        this.maxCooldownTicks = cooldownTicks;
        this.lastUseTime = System.currentTimeMillis();
    }

    /** Back-dates {@code lastUseTime}, so the countdown picks up mid-cooldown instead of restarting. */
    public void setCooldown(int remainingTicks, int maxTicks) {
        this.cooldownTicks = Math.max(0, remainingTicks);
        this.maxCooldownTicks = Math.max(this.cooldownTicks, maxTicks);
        this.lastUseTime = this.cooldownTicks > 0
                ? System.currentTimeMillis() - (long) (this.maxCooldownTicks - this.cooldownTicks) * MS_PER_TICK
                : 0;
    }

    public void setToggled(boolean toggled) {
        this.toggled = toggled;
    }

    public void setCategoryLabel(String categoryLabel) {
        this.categoryLabel = AbilityInfo.extractCategory(abilityId).isEmpty() ? categoryLabel : null;
    }

    public String boundCategory() { return AbilityInfo.extractCategory(abilityId);
    }

    /** Exact match: {@code contains} used to let {@code sun-9-0} light up {@code sun-9-01} too. */
    public boolean hasAbility(String abilityId) {
        return Objects.equals(AbilityInfo.extractId(this.abilityId), abilityId);
    }

    public boolean isEmpty() {
        return this.abilityId == null;
    }

    public boolean isOnCooldown() {
        return cooldownTicks > 0;
    }

    public int getSlotIndex() {
        return slotIndex;
    }
}
