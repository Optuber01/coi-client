package dev.ua.ikeepcalm.coi.domain.effect;

import dev.ua.ikeepcalm.coi.config.HudConfig;
import dev.ua.ikeepcalm.coi.domain.ceremony.CeremonyEffect;
import dev.ua.ikeepcalm.coi.domain.ceremony.CeremonyEffects;
import dev.ua.ikeepcalm.coi.domain.ceremony.CeremonyEffects.CeremonyParams;
import dev.ua.ikeepcalm.coi.domain.effect.visual.*;
import dev.ua.ikeepcalm.coi.util.CoiLog;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

import java.util.*;
import java.util.function.Supplier;

/**
 * Registry and active list for {@code coi-client:effect}. An effect is created fresh on each
 * trigger, rendered from one HUD element attached before chat, and dropped once finished.
 */
public class EffectManager {

    private static final Map<String, Supplier<VisualEffect>> REGISTRY = new LinkedHashMap<>();
    private static final List<VisualEffect> activeEffects = new ArrayList<>();
    private static final Set<String> PHOTOSENSITIVE_EFFECTS = Set.of(FlashEffect.ID, GlitchEffect.ID, HeartbeatEffect.ID);

    private EffectManager() {
    }

    public static void initialize() {
        register(CracksEffect.ID, CracksEffect::new);
        register(EyesEffect.ID, EyesEffect::new);
        register(VignetteEffect.ID, VignetteEffect::new);
        register(HeartbeatEffect.ID, HeartbeatEffect::new);
        register(GlitchEffect.ID, GlitchEffect::new);
        register(BloodRainEffect.ID, BloodRainEffect::new);
        register(FrostEffect.ID, FrostEffect::new);
        register(WhispersEffect.ID, WhispersEffect::new);
        register(TunnelEffect.ID, TunnelEffect::new);
        register(FlashEffect.ID, FlashEffect::new);
        register(SpellImpactEffect.ID, SpellImpactEffect::new);
        register(ImpactFrameEffect.ID, ImpactFrameEffect::new);
        register(HallucinationEffect.ID, HallucinationEffect::new);
        // The ceremony's set pieces ride this channel like any other effect, but keep their
        // state outside the instance - see dev.ua.ikeepcalm.coi.domain.ceremony
        for (CeremonyEffect.Kind kind : CeremonyEffect.Kind.values()) {
            register(kind.id(), () -> new CeremonyEffect(kind));
        }
        SpellImpactEffect.initializeWorldRenderer();

        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, Identifier.fromNamespaceAndPath("coi-client", "effects"), EffectManager::render);
    }

    private static void register(String id, Supplier<VisualEffect> factory) {
        REGISTRY.put(id, factory);
    }

    /**
     * {@code params = "stop"} removes this effect; {@code effectId = "all"} stops every active
     * one, params ignored.
     */
    public static void trigger(String effectId, String params) {
        trigger(effectId, params, false);
    }

    public static void triggerDebug(String effectId, String params) {
        trigger(effectId, params, true);
    }

    private static void trigger(String effectId, String params, boolean bypassPhotosensitiveGuard) {
        if (!bypassPhotosensitiveGuard && HudConfig.getSettings().epilepsyMode && PHOTOSENSITIVE_EFFECTS.contains(effectId)) {
            CoiLog.LOG.info("Skipped photosensitive effect '{}' because epilepsy mode is enabled", effectId);
            return;
        }

        if ("all".equals(effectId)) {
            stopAll();
            return;
        }
        if (CeremonyParams.isStop(params)) {
            stopEffect(effectId, params);
            return;
        }

        Supplier<VisualEffect> factory = REGISTRY.get(effectId);
        if (factory == null) {
            CoiLog.LOG.warn("Unknown effect '{}'", effectId);
            return;
        }

        activeEffects.removeIf(e -> e.getId().equals(effectId));

        VisualEffect effect = factory.get();
        effect.start(params);
        activeEffects.add(effect);
        EffectSounds.onEffectStart(effectId);
    }

    public static void stopEffect(String effectId) {
        stopEffect(effectId, "stop");
    }

    /**
     * {@code params} is passed through: an audio bed stops as {@code stop,fade=1500}.
     */
    public static void stopEffect(String effectId, String params) {
        if (SpellImpactEffect.ID.equals(effectId)) {
            SpellImpactEffect.clearWorldImpacts();
        }

        activeEffects.stream().filter(e -> e.getId().equals(effectId)).forEach(e -> e.stop(params));
        activeEffects.removeIf(e -> e.getId().equals(effectId));
        EffectSounds.onEffectStop(effectId);
    }

    public static void stopAll() {
        SpellImpactEffect.clearWorldImpacts();
        activeEffects.forEach(VisualEffect::stop);
        activeEffects.clear();
        EffectSounds.stopAll();
        // The ceremony effects are read by the renderer rather than drawn by their own
        // instance, so clearing the list is not enough to undo them
        CeremonyEffects.reset();
    }

    public static boolean isActive(String effectId) {
        return activeEffects.stream().anyMatch(e -> e.getId().equals(effectId));
    }

    public static Map<String, Supplier<VisualEffect>> getRegistry() {
        return Collections.unmodifiableMap(REGISTRY);
    }

    private static void render(GuiGraphicsExtractor ctx, DeltaTracker counter) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        int w = client.getWindow().getGuiScaledWidth();
        int h = client.getWindow().getGuiScaledHeight();
        float tickDelta = 1.0f;

        dropFinishedEffects();

        for (VisualEffect effect : new ArrayList<>(activeEffects)) {
            effect.render(ctx, w, h, tickDelta);
        }
    }

    /** Removal happens here, not at the trigger site, so a looping sound stops with the effect. */
    private static void dropFinishedEffects() {
        Iterator<VisualEffect> it = activeEffects.iterator();
        while (it.hasNext()) {
            VisualEffect effect = it.next();
            if (effect.isFinished()) {
                EffectSounds.onEffectStop(effect.getId());
                it.remove();
            }
        }
    }
}
