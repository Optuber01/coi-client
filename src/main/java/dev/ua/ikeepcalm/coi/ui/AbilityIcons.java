package dev.ua.ikeepcalm.coi.ui;

import dev.ua.ikeepcalm.coi.domain.ability.model.AbilityInfo;
import dev.ua.ikeepcalm.coi.domain.ability.service.AbilityRegistry;
import dev.ua.ikeepcalm.coi.domain.effect.visual.EffectPaint;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Locale;
import java.util.Set;

/**
 * Ability icons, from the resource pack's per-ability item model where there is one — the same
 * route the plugin skins its own item with — else the bundled
 * {@code textures/icons/<category>/<tier>.png}.
 */
public class AbilityIcons {

    /**
     * The folders that actually ship: a server-invented category would blit a missing texture.
     */
    private static final Set<String> CATEGORIES = Set.of(
            "analytical", "attack", "buffs", "control", "craft", "debuffs", "defence",
            "exploratory", "mobility", "multitype", "summon", "support", "theft", "uncategorized"
    );
    private static final String FALLBACK_CATEGORY = "uncategorized";

    private AbilityIcons() {
    }

    public static void draw(GuiGraphicsExtractor graphics, String abilityIdWithName, int x, int y, int size, int alpha) {
        String id = AbilityInfo.extractId(abilityIdWithName);
        AbilityInfo info = AbilityRegistry.getAbilityInfo(id);

        graphics.fill(x, y, x + size, y + size, EffectPaint.argb(AbilityInfo.pathwayColor(id), alpha / 2));

        if (info != null && drawItemModel(graphics, info.icon(), x, y, size)) return;
        drawCategoryIcon(graphics, info, id, x, y, size, alpha);
    }

    /**
     * Public because a server-authored menu names item models too, and must resolve them
     * exactly the way an ability icon does.
     *
     * @return false when the pack defining the model is not loaded
     */
    public static boolean drawItemModel(GuiGraphicsExtractor graphics, String icon, int x, int y, int size) {
        if (icon == null || icon.isEmpty() || !IconModels.exists(icon)) return false;
        Identifier model = Identifier.tryParse(icon);
        if (model == null) return false;

        ItemStack stack = new ItemStack(Items.GLOWSTONE_DUST);
        stack.set(DataComponents.ITEM_MODEL, model);

        // Item rendering is fixed at 16x16, so the box size comes from the pose
        var pose = graphics.pose();
        pose.pushMatrix();
        pose.translate(x, y);
        pose.scale(size / 16f, size / 16f);
        graphics.fakeItem(stack, 0, 0);
        pose.popMatrix();
        return true;
    }

    private static void drawCategoryIcon(GuiGraphicsExtractor graphics, AbilityInfo info, String id,
                                         int x, int y, int size, int alpha) {
        Identifier texture = Identifier.fromNamespaceAndPath("coi-client",
                "textures/icons/" + category(info) + "/" + AbilityInfo.tierOf(id) + ".png");
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 0, 0, size, size, size, size,
                EffectPaint.argb(0xFFFFFF, alpha));
    }

    public static String category(AbilityInfo info) {
        if (info == null || info.category() == null) return FALLBACK_CATEGORY;
        String category = info.category().toLowerCase(Locale.ROOT);
        return CATEGORIES.contains(category) ? category : FALLBACK_CATEGORY;
    }
}
