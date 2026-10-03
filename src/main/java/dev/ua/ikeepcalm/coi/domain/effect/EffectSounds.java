package dev.ua.ikeepcalm.coi.domain.effect;

import dev.ua.ikeepcalm.coi.config.HudConfig;
import dev.ua.ikeepcalm.coi.domain.effect.visual.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

import java.util.HashMap;
import java.util.Map;

/**
 * Audio companion for visual effects. A loop follows its effect's lifetime; a one-shot plays
 * out. Everything is scaled by {@code effectSoundVolume}, where 0 disables sound entirely.
 */
public class EffectSounds {

    private EffectSounds() {
    }

    private record Spec(Identifier soundId, boolean loop, float volume) {
        static Spec loop(String path, float volume) {
            return new Spec(id(path), true, volume);
        }

        static Spec once(String path, float volume) {
            return new Spec(id(path), false, volume);
        }
    }

    private static final Map<String, Spec> SPECS = Map.of(
            HeartbeatEffect.ID, Spec.loop("effect.heartbeat", 0.9f),
            WhispersEffect.ID, Spec.loop("effect.whispers", 0.8f),
            TunnelEffect.ID, Spec.loop("effect.tunnel", 0.7f),
            CracksEffect.ID, Spec.once("effect.cracks", 0.8f),
            FrostEffect.ID, Spec.once("effect.frost", 0.8f),
            GlitchEffect.ID, Spec.once("effect.glitch", 0.7f)
    );

    private static final Map<String, SoundInstance> activeLoops = new HashMap<>();

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("coi-client", path);
    }

    static void onEffectStart(String effectId) {
        Spec spec = SPECS.get(effectId);
        if (spec == null) return;
        float master = HudConfig.getSettings().effectSoundVolume;
        if (master <= 0f) return;

        if (spec.loop()) {
            if (activeLoops.containsKey(effectId)) return;
            SoundInstance instance = globalSound(spec, master, true);
            activeLoops.put(effectId, instance);
            Minecraft.getInstance().getSoundManager().play(instance);
        } else {
            Minecraft.getInstance().getSoundManager().play(globalSound(spec, master, false));
        }
    }

    static void onEffectStop(String effectId) {
        SoundInstance loop = activeLoops.remove(effectId);
        if (loop != null) {
            Minecraft.getInstance().getSoundManager().stop(loop);
        }
    }

    static void stopAll() {
        activeLoops.values().forEach(loop -> Minecraft.getInstance().getSoundManager().stop(loop));
        activeLoops.clear();
    }

    /**
     * Non-positional and non-attenuated, like the visual effect it accompanies.
     */
    private static SoundInstance globalSound(Spec spec, float master, boolean loop) {
        return new SimpleSoundInstance(spec.soundId(), SoundSource.AMBIENT,
                spec.volume() * master, 1.0f, RandomSource.create(), loop, 0,
                SoundInstance.Attenuation.NONE, 0, 0, 0, true);
    }
}
