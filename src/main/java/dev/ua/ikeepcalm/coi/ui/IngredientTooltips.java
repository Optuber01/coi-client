package dev.ua.ikeepcalm.coi.ui;

import dev.ua.ikeepcalm.coi.config.ClientDataLoader;
import dev.ua.ikeepcalm.coi.config.ClientDataLoader.IngredientInfo;
import dev.ua.ikeepcalm.coi.domain.ability.model.Pathways;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.List;
import java.util.Optional;

/**
 * Names the pathway an item belongs to in its tooltip. Two servers write that fact differently
 * into the persistent data container — the plugin's {@code circleofimagination:ingredient} key
 * and Venture to the Subspace's loot-shard id — so both are read here.
 */
public class IngredientTooltips {

    /** Bukkit stores an item's persistent data under this tag. */
    private static final String BUKKIT_VALUES = "PublicBukkitValues";
    private static final String INGREDIENT_KEY = "circleofimagination:ingredient";
    private static final String LOOT_SHARD_KEY = "venturetothesubspace:loot_shard_id";

    private IngredientTooltips() {
    }

    public static void register(ClientDataLoader data) {
        ItemTooltipCallback.EVENT.register((stack, _, _, lines) -> {
            persistentData(stack).ifPresent(pdc -> {
                if (pdc.contains(INGREDIENT_KEY)) {
                    pdc.getString(INGREDIENT_KEY).ifPresent(id -> describeIngredient(data, id, lines));
                } else if (pdc.contains(LOOT_SHARD_KEY)) {
                    pdc.getString(LOOT_SHARD_KEY).ifPresent(id -> describeLootShard(data, id, lines));
                }
            });
        });
    }

    private static Optional<CompoundTag> persistentData(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return Optional.empty();
        CompoundTag tag = customData.copyTag();
        if (!tag.contains(BUKKIT_VALUES)) return Optional.empty();
        return tag.getCompound(BUKKIT_VALUES);
    }

    private static void describeIngredient(ClientDataLoader data, String ingredientId, List<Component> lines) {
        IngredientInfo info = data.getIngredient(ingredientId);
        if (info == null) return;

        prefixWithEmblem(lines, info.pathway());
        lines.add(Component.literal(info.isMain() ? "Main ingredient" : "Supplementary ingredient").withStyle(info.color()));
        lines.add(sequenceLine(info.sequence(), info.pathway(), info.color()));
    }

    /**
     * {@code coi:ingredients-<pathway>-<sequence>}; anything else is left alone.
     */
    private static void describeLootShard(ClientDataLoader data, String shardId, List<Component> lines) {
        String path = shardId.contains(":") ? shardId.split(":", 2)[1] : shardId;
        String[] parts = path.split("-", 3);
        if (parts.length < 3) return;

        String pathway = parts[1];
        int sequence;
        try {
            sequence = Integer.parseInt(parts[2]);
        } catch (NumberFormatException e) {
            return;
        }

        prefixWithEmblem(lines, pathway);
        lines.add(sequenceLine(sequence, pathway, data.getPathwayColor(pathway)));
    }

    private static Component sequenceLine(int sequence, String pathway, ChatFormatting color) {
        return Component.literal("Sequence " + sequence + " of the " + Pathways.formatPathwayName(pathway) + " pathway")
                .withStyle(color);
    }

    /** Puts the pathway's emblem in front of the item's name, when it has one. */
    private static void prefixWithEmblem(List<Component> lines, String pathway) {
        Component emblem = Pathways.pathwayEmblem(pathway);
        if (emblem == null || lines.isEmpty()) return;
        lines.set(0, Component.empty()
                .append(emblem)
                .append(Component.literal(" "))
                .append(lines.getFirst()));
    }
}
