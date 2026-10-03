package dev.ua.ikeepcalm.coi.domain.effect.visual;

import java.util.function.BiConsumer;

/**
 * Splitter for the {@code key=value,key=value} param strings of {@code coi-client:effect}.
 */
public class EffectParams {

    private EffectParams() {
    }

    /**
     * Entries without an {@code =} are skipped; a null or blank string yields no calls at all,
     * leaving the effect's defaults in place.
     */
    public static void forEach(String params, BiConsumer<String, String> handler) {
        if (params == null || params.isBlank()) return;
        for (String part : params.split(",")) {
            String[] kv = part.split("=", 2);
            if (kv.length == 2) handler.accept(kv[0].trim(), kv[1].trim());
        }
    }
}
