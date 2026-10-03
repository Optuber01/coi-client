package dev.ua.ikeepcalm.coi.hud.layout;

import dev.ua.ikeepcalm.coi.config.HudConfig;
import dev.ua.ikeepcalm.coi.domain.ability.service.AbilityBindings;
import dev.ua.ikeepcalm.coi.hud.layout.BarElements.*;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Every positionable HUD element, in draw order (bottom-most first). The editor walks this list
 * forwards to render and backwards to hit-test, so the thing drawn on top is the thing you grab.
 */
public class HudElements {

    public static final String ABILITY_SLOTS = ElementIds.ABILITY_SLOTS;
    public static final String SLOT_PREFIX = ElementIds.SLOT_PREFIX;
    public static final String CHARACTER_PLATE = ElementIds.CHARACTER_PLATE;
    public static final String BEYONDER_HEALTH = ElementIds.BEYONDER_HEALTH;
    public static final String MADNESS = ElementIds.MADNESS;
    public static final String SPIRITUALITY = ElementIds.SPIRITUALITY;
    public static final String ACTING = ElementIds.ACTING;
    public static final String RESOURCES = ElementIds.RESOURCES;
    public static final String ACTION_BAR = ElementIds.ACTION_BAR;
    public static final String TARGET_HEALTH = ElementIds.TARGET_HEALTH;
    public static final String COGITATION = ElementIds.COGITATION;
    public static final String NOTIFICATIONS = ElementIds.NOTIFICATIONS;

    private static final List<HudElement> SLOTS = buildSlots();

    private static final List<HudElement> BARS = List.of(
            new CharacterPlateElement(),
            new BeyonderHealthElement(),
            new MadnessElement(),
            new SpiritualityElement(),
            new ActingElement(),
            new ResourceElement(),
            new ActionBarElement(),
            new TargetHealthElement(),
            new CogitationElement(),
            new NotificationElement());

    private HudElements() {
    }

    private static List<HudElement> buildSlots() {
        List<HudElement> slots = new ArrayList<>();
        for (int i = 0; i < AbilityBindings.MAX_ABILITIES; i++) {
            slots.add(new SlotElement(i));
        }
        return List.copyOf(slots);
    }

    /**
     * Depends on the settings, so the editor builds it once in its constructor.
     */
    public static List<HudElement> all(HudConfig.HudSettings s) {
        int count = Math.clamp(s.activeAbilitySlots, 1, SLOTS.size());
        List<HudElement> elements = new ArrayList<>(SLOTS.subList(0, count));
        for (HudElement element : BARS) {
            if (!supersededByPlate(element.id(), s)) elements.add(element);
        }
        return List.copyOf(elements);
    }

    /**
     * A superseded bar leaves the list outright rather than staying as a hidden ghost: that is
     * not the same thing as one the player switched off, which stays and is drawn under a scrim.
     */
    private static boolean supersededByPlate(String id, HudConfig.HudSettings s) {
        if (!s.showCharacterPlate) return false;
        return MADNESS.equals(id) || ACTING.equals(id) || RESOURCES.equals(id);
    }

    public static List<HudElement> soloSet(String id, HudConfig.HudSettings s) {
        return LayoutGroups.soloSet(id, all(s));
    }

    public static Component groupLabel(String id, List<HudElement> set) {
        return LayoutGroups.groupLabel(id, set);
    }

    public static void moveGroupBy(String groupId, List<HudElement> members, int dx, int dy,
                                   int w, int h, HudConfig.HudSettings s) {
        LayoutGroups.moveGroupBy(groupId, members, dx, dy, w, h, s);
    }

    /**
     * @return the element with this id, or {@code null} for an unknown one
     */
    public static HudElement byId(String id) {
        if (id == null) return null;
        for (HudElement element : SLOTS) {
            if (element.id().equals(id)) return element;
        }
        for (HudElement element : BARS) {
            if (element.id().equals(id)) return element;
        }
        return null;
    }
}
