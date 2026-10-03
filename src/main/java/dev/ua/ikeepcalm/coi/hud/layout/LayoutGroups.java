package dev.ua.ikeepcalm.coi.hud.layout;

import dev.ua.ikeepcalm.coi.config.HudConfig;
import dev.ua.ikeepcalm.coi.hud.overlay.AbilityOverlay;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

/**
 * Everything the editor does to a {@link HudElement#group() group} rather than to one element.
 * The ability slots are the only group, and the only reason this is not three lines: a slot
 * still in the shared row has to move <em>with the row</em> rather than be pinned where it is.
 */
public class LayoutGroups {

    private LayoutGroups() {
    }

    /**
     * A whole group when the id names one, otherwise the single element; empty for an unknown id.
     */
    public static List<HudElement> soloSet(String id, List<HudElement> candidates) {
        if (id == null) return List.of();
        List<HudElement> found = new ArrayList<>();
        for (HudElement element : candidates) {
            if (id.equals(element.group()) || id.equals(element.id())) found.add(element);
        }
        return List.copyOf(found);
    }

    public static Component groupLabel(String id, List<HudElement> set) {
        if (!set.isEmpty() && id.equals(set.getFirst().group())) {
            return Component.translatable("screen.coi.layout_el_" + id);
        }
        return set.isEmpty() ? Component.empty() : set.getFirst().label();
    }

    /**
     * Slots still in the shared row move with the row itself, once, via
     * {@code hudX}/{@code hudYOffset}; the ones pulled out take the delta through their own anchor.
     */
    public static void moveGroupBy(String groupId, List<HudElement> members, int dx, int dy,
                                   int w, int h, HudConfig.HudSettings s) {
        if (groupId == null) return;
        if (rowFollows(groupId, members, s)) {
            AbilityOverlay.shiftRow(dx, dy, h, s);
        }
        for (HudElement element : members) {
            if (!groupId.equals(element.group())) continue;
            if (element instanceof SlotElement slot && slot.inRow(s)) continue;
            int[] box = element.bounds(w, h, s);
            element.moveTo(Mth.clamp(box[0] + dx, 0, Math.max(0, w - box[2])),
                    Mth.clamp(box[1] + dy, 0, Math.max(0, h - box[3])), w, h, s);
        }
    }

    private static boolean rowFollows(String groupId, List<HudElement> members, HudConfig.HudSettings s) {
        for (HudElement element : members) {
            if (groupId.equals(element.group()) && element instanceof SlotElement slot && slot.inRow(s)) {
                return true;
            }
        }
        return false;
    }
}
