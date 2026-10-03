package dev.ua.ikeepcalm.coi.hud.overlay;

import dev.ua.ikeepcalm.coi.config.HudConfig;
import dev.ua.ikeepcalm.coi.domain.ability.model.AbilityInfo;
import dev.ua.ikeepcalm.coi.domain.ability.service.AbilityBindings;
import dev.ua.ikeepcalm.coi.hud.HudAnchor;
import dev.ua.ikeepcalm.coi.hud.HudGaslight;
import dev.ua.ikeepcalm.coi.hud.HudGate;
import dev.ua.ikeepcalm.coi.hud.HudScale;
import dev.ua.ikeepcalm.coi.hud.render.AbilitySlotWidget;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * The ability slots. {@link #slotOrigin} is the single answer to "where does slot N go", shared
 * with the layout editor and the tour's spotlight; slots sit at their <em>index</em>, not at
 * their rank among the bound ones. {@code slotSize} is the only size knob: the widget always
 * draws at {@link HudConfig#BASE_SLOT_SIZE} under a pose scale, which is what makes the keybind
 * chip, the name line and the cooldown readout grow with the box.
 */
public class AbilityOverlay {

    private static final Identifier ABILITY_LAYER = Identifier.fromNamespaceAndPath("coi-client", "abilities");
    private static AbilitySlotWidget[] abilitySlots;
    /** Height of the ability-name line drawn under each slot. */
    public static final int NAME_LINE = 13;
    /** Gap between two slots at {@link HudConfig#BASE_SLOT_SIZE}; scales with the box. */
    private static final int SLOT_GAP = 10;
    /** The slot animations run off wall-clock millis; only the cooldown sweep reads this. */
    private static final float TICK_DELTA = 1.0f;
    /**
     * Never bound, so the editor's preview leaves the real slots' cooldowns alone.
     */
    private static AbilitySlotWidget[] previewSlots;

    private AbilityOverlay() {
    }

    public static void initialize() {
        int maxAbilities = AbilityBindings.MAX_ABILITIES;
        abilitySlots = new AbilitySlotWidget[maxAbilities];

        for (int i = 0; i < maxAbilities; i++) {
            abilitySlots[i] = new AbilitySlotWidget(i);
        }

        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, ABILITY_LAYER, AbilityOverlay::renderAbilities);
    }

    private static void renderAbilities(GuiGraphicsExtractor context, DeltaTracker tickCounter) {
        Minecraft client = Minecraft.getInstance();
        HudConfig.HudSettings settings = HudConfig.getSettings();

        // showAbilityHud hides the boxes and nothing else: the keymappings keep firing
        if (HudGate.blocked(client, settings) || !settings.showAbilityHud) {
            return;
        }

        int screenWidth = client.getWindow().getGuiScaledWidth();
        int screenHeight = client.getWindow().getGuiScaledHeight();

        List<AbilitySlotWidget> visible = visibleSlots();
        HudGaslight.update(visible.stream().map(AbilitySlotWidget::getSlotIndex).toList());

        for (AbilitySlotWidget abilitySlot : visible) {
            int[] origin = slotOrigin(drawnIndex(abilitySlot.getSlotIndex()), screenWidth, screenHeight, settings);
            HudScale.push(context, origin[0], origin[1], slotScale(settings));
            abilitySlot.render(context, origin[0], origin[1], HudConfig.BASE_SLOT_SIZE, TICK_DELTA);
            HudScale.pop(context);
        }
    }

    private static List<AbilitySlotWidget> visibleSlots() {
        int activeSlots = AbilityBindings.getActiveAbilitySlots();
        List<AbilitySlotWidget> visible = new ArrayList<>();
        for (AbilitySlotWidget abilitySlot : abilitySlots) {
            if (abilitySlot.getSlotIndex() < activeSlots && !abilitySlot.isEmpty()) {
                visible.add(abilitySlot);
            }
        }
        return visible;
    }

    /** At high madness two slots briefly trade origins; see {@code HudGaslight}. */
    private static int drawnIndex(int index) {
        int swapped = HudGaslight.swappedWith(index);
        if (swapped >= 0 && swapped < abilitySlots.length && !abilitySlots[swapped].isEmpty()) {
            return swapped;
        }
        return index;
    }

    /**
     * The one answer to "where does slot N go", read by the HUD, the editor and the tour.
     * Either way the box is clamped on screen, so a stale offset cannot hide a slot.
     */
    public static int[] slotOrigin(int index, int screenW, int screenH, HudConfig.HudSettings s) {
        int box = boxSize(s);
        HudConfig.SlotPlacement placement = placement(s, index);
        int x;
        int y;
        if (placement == null) {
            int[] row = rowOrigin(screenH, s);
            x = row[0] + index * rowStep(s);
            y = row[1];
        } else {
            int[] pos = HudAnchor.parse(placement.anchor())
                    .resolve(screenW, screenH, box, placement.x(), placement.y(), placement.y());
            x = pos[0];
            y = pos[1];
        }
        return new int[]{Mth.clamp(x, 0, Math.max(0, screenW - box)),
                Mth.clamp(y, 0, Math.max(0, screenH - box))};
    }

    /** {@code slotSize == BASE_SLOT_SIZE} is 1.0; the slider's range maps exactly onto the scale band. */
    public static float slotScale(HudConfig.HudSettings s) {
        return HudScale.clamp(s.slotSize / (float) HudConfig.BASE_SLOT_SIZE);
    }

    /** Equal to {@code slotSize} by definition; read it from here so the relationship stays in one place. */
    public static int boxSize(HudConfig.HudSettings s) {
        return HudScale.size(HudConfig.BASE_SLOT_SIZE, slotScale(s));
    }

    /** Derived from the box, so a bigger slot can never overlap its neighbour. */
    public static int rowStep(HudConfig.HudSettings s) {
        return boxSize(s) + HudScale.size(SLOT_GAP, slotScale(s));
    }

    /** @return null when the slot still follows the shared row */
    public static HudConfig.SlotPlacement placement(HudConfig.HudSettings s, int index) {
        if (s.slotPlacements == null || index < 0 || index >= s.slotPlacements.length) return null;
        return s.slotPlacements[index];
    }

    /** {@code {x, y, width, height}}, including the name line under the box when that is on. */
    public static int[] slotBounds(int index, int screenW, int screenH, HudConfig.HudSettings s) {
        int[] origin = slotOrigin(index, screenW, screenH, s);
        int box = boxSize(s);
        return new int[]{origin[0], origin[1], box,
                box + (s.showAbilityNames ? HudScale.size(NAME_LINE, slotScale(s)) : 0)};
    }

    /** Union of every active slot's {@link #slotBounds} — what the tour spotlights. */
    public static int[] rowBounds(int screenW, int screenH, HudConfig.HudSettings s) {
        int count = Math.clamp(s.activeAbilitySlots, 1, AbilityBindings.MAX_ABILITIES);
        int x0 = Integer.MAX_VALUE;
        int y0 = Integer.MAX_VALUE;
        int x1 = Integer.MIN_VALUE;
        int y1 = Integer.MIN_VALUE;
        for (int i = 0; i < count; i++) {
            int[] box = slotBounds(i, screenW, screenH, s);
            x0 = Math.min(x0, box[0]);
            y0 = Math.min(y0, box[1]);
            x1 = Math.max(x1, box[0] + box[2]);
            y1 = Math.max(y1, box[1] + box[3]);
        }
        return new int[]{x0, y0, x1 - x0, y1 - y0};
    }

    /** The offsets are plain screen pixels, so the delta goes in untouched. */
    public static void shiftRow(int dx, int dy, int screenH, HudConfig.HudSettings s) {
        s.hudX = Math.max(0, s.hudX + dx);
        s.hudYOffset = Mth.clamp(s.hudYOffset - dy, 0, screenH);
    }

    /**
     * Plain screen pixels: the slot scale is deliberately absent, since it grows the slots about
     * this very point rather than displacing the row.
     */
    public static int[] rowOrigin(int screenH, HudConfig.HudSettings s) {
        return new int[]{s.hudX, screenH - s.hudYOffset};
    }

    public static void renderSlotPreview(GuiGraphicsExtractor ctx, int index, int screenW, int screenH,
                                         HudConfig.HudSettings s) {
        if (previewSlots == null) {
            previewSlots = new AbilitySlotWidget[AbilityBindings.MAX_ABILITIES];
            for (int i = 0; i < previewSlots.length; i++) {
                previewSlots[i] = new AbilitySlotWidget(i);
            }
        }
        if (index < 0 || index >= previewSlots.length) return;
        int[] origin = slotOrigin(index, screenW, screenH, s);
        HudScale.push(ctx, origin[0], origin[1], slotScale(s));
        previewSlots[index].render(ctx, origin[0], origin[1], HudConfig.BASE_SLOT_SIZE, 1.0f);
        HudScale.pop(ctx);
    }

    /** Every slot, no early break: one ability can legitimately sit in several of them. */
    private static void forEachSlot(String abilityId, Consumer<AbilitySlotWidget> action) {
        if (abilitySlots == null || abilityId == null) return;
        for (AbilitySlotWidget abilitySlot : abilitySlots) {
            if (abilitySlot.hasAbility(abilityId)) {
                action.accept(abilitySlot);
            }
        }
    }

    /** Reactive: the server says it just went on cooldown, the slot times it out locally. */
    public static void setCooldown(String abilityId, int cooldownTicks) {
        forEachSlot(abilityId, slot -> {
            if (!slot.isOnCooldown()) {
                slot.setCooldown(cooldownTicks);
            }
        });
    }

    /** Authoritative, off the v2 ability list: a max known before the ability is ever cast. */
    public static void setCooldown(String abilityId, int remainingTicks, int maxTicks) {
        forEachSlot(abilityId, slot -> slot.setCooldown(remainingTicks, maxTicks));
    }

    public static void setActive(String abilityId, boolean active) {
        forEachSlot(abilityId, slot -> slot.setToggled(active));
    }

    public static void setCategoryLabel(String abilityId, String label) {
        forEachSlot(abilityId, slot -> slot.setCategoryLabel(label));
    }

    public static void applyCategories(String abilityId) {
        forEachSlot(abilityId, slot -> {
            String selected = slot.boundCategory().isEmpty()
                    ? dev.ua.ikeepcalm.coi.domain.ability.model.AbilityCategories.selected(abilityId) : slot.boundCategory();
            var category = dev.ua.ikeepcalm.coi.domain.ability.model.AbilityCategories.find(abilityId, selected);
            if (category == null) { slot.setCooldown(0, 0); slot.setCategoryLabel(""); return; }
            slot.setCategoryLabel(category.name());
            int remaining = Math.max(category.remainingTicks(), dev.ua.ikeepcalm.coi.domain.ability.model.AbilityCategories.lockTicks(abilityId));
            slot.setCooldown(remaining, Math.max(remaining, category.cooldownSeconds() * 20));
        });
    }

    public static void onAbilityCast(String binding) {
        forEachSlot(AbilityInfo.extractId(binding), slot -> {
            if (slot.boundCategory().equals(AbilityInfo.extractCategory(binding))) slot.triggerCastAnimation();
        });
    }

    public static void clearServerState() {
        if (abilitySlots == null) return;
        for (var slot : abilitySlots) { slot.setCooldown(0, 0); slot.setCategoryLabel(""); slot.setToggled(false); }
    }

    public static void updateAbilitySlot(int slot, String abilityId) {
        if (abilitySlots != null && slot >= 0 && slot < abilitySlots.length) {
            abilitySlots[slot].setAbility(abilityId);
            if (abilityId != null) applyCategories(AbilityInfo.extractId(abilityId));
        }
    }
}
