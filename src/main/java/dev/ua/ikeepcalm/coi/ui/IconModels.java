package dev.ua.ikeepcalm.coi.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * "Does the loaded pack define this item model?", so an unknown icon falls back instead of
 * blitting a missing texture every frame. Cached per icon id; cleared on resource reload and on
 * disconnect, since the next server may key icons differently.
 */
public class IconModels {

    private static final Map<String, Boolean> CACHE = new ConcurrentHashMap<>();

    private IconModels() {
    }

    /**
     * @param icon a full item-model id such as {@code circleofimagination:sun/holylight}
     */
    public static boolean exists(String icon) {
        if (icon == null || icon.isEmpty()) return false;
        Boolean cached = CACHE.get(icon);
        if (cached != null) return cached;
        boolean present = lookup(icon);
        CACHE.put(icon, present);
        return present;
    }

    private static boolean lookup(String icon) {
        Identifier model = Identifier.tryParse(icon);
        if (model == null) return false;
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.getResourceManager() == null) return false;
        Identifier path = Identifier.fromNamespaceAndPath(model.getNamespace(), "items/" + model.getPath() + ".json");
        try {
            return client.getResourceManager().getResource(path).isPresent();
        } catch (Exception e) {
            return false;
        }
    }

    public static void clearCache() {
        CACHE.clear();
    }
}
