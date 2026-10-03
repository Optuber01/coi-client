package dev.ua.ikeepcalm.coi.domain.effect;

import dev.ua.ikeepcalm.coi.config.HudConfig;
import dev.ua.ikeepcalm.coi.domain.beyonder.model.BeyonderState;
import dev.ua.ikeepcalm.coi.domain.effect.visual.EyesEffect;
import dev.ua.ikeepcalm.coi.domain.effect.visual.GlitchEffect;
import dev.ua.ikeepcalm.coi.hud.overlay.MadnessOverlay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Client-side madness hallucinations: phantom footsteps, whispers, sourceless block sounds and
 * brief visual flickers. Purely cosmetic; nothing is sent to the server. Frequency and event
 * pool scale with the madness stages {@link MadnessOverlay} uses (25 / 50 / 75).
 */
public class HallucinationManager {

    private static final Random RANDOM = new Random();

    private static final SoundEvent WHISPER_SOUND = SoundEvent.createVariableRangeEvent(
            Identifier.fromNamespaceAndPath("coi-client", "hallucination.whisper"));

    private static final double TIER_1 = 25.0; // phantom footsteps, cave ambience
    private static final double TIER_2 = 50.0; // + whispers, sourceless block sounds
    private static final double TIER_3 = 75.0; // + visual flickers, bolder events

    private record ScheduledSound(long playAtMs, SoundEvent event, double x, double y, double z,
                                  float volume, float pitch) {
    }

    private static final List<ScheduledSound> pending = new ArrayList<>();
    private static long nextEventAt = 0;

    private HallucinationManager() {
    }

    public static void tick(Minecraft client) {
        long now = System.currentTimeMillis();

        if (client.player == null || client.level == null) {
            pending.clear();
            nextEventAt = 0;
            return;
        }

        flushDueSounds(now);

        if (!HudConfig.getSettings().enableHallucinations) return;

        double madness = BeyonderState.getMadness();
        if (madness < TIER_1) {
            nextEventAt = 0;
            return;
        }

        if (nextEventAt == 0) {
            // Just crossed the threshold — schedule, don't fire instantly
            nextEventAt = now + nextDelayMs(madness);
            return;
        }
        if (now < nextEventAt) return;

        nextEventAt = now + nextDelayMs(madness);
        triggerRandom(madness);
    }

    private static void flushDueSounds(long now) {
        Iterator<ScheduledSound> it = pending.iterator();
        while (it.hasNext()) {
            ScheduledSound s = it.next();
            if (now >= s.playAtMs()) {
                playAt(s.event(), s.x(), s.y(), s.z(), s.volume(), s.pitch());
                it.remove();
            }
        }
    }

    /**
     * ~75s at stage 1 down to ~20s at 100, with ±40% jitter so events never feel scheduled.
     */
    private static long nextDelayMs(double madness) {
        double t = Math.min(1.0, (madness - TIER_1) / (100.0 - TIER_1));
        double base = 75_000 - t * 55_000;
        double jitter = 0.6 + RANDOM.nextDouble() * 0.8;
        return (long) (base * jitter * darknessFactor());
    }

    /**
     * Up to ~2.5x as often in pitch black as in full light. Reads the time-of-day-aware local
     * brightness, so night in the open counts as dark.
     */
    private static double darknessFactor() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) return 1.0;
        int light = client.level.getMaxLocalRawBrightness(client.player.blockPosition());
        return 0.4 + 0.6 * (Math.clamp(light, 0, 15) / 15.0);
    }

    private static void triggerRandom(double madness) {
        List<Runnable> pool = new ArrayList<>();
        pool.add(() -> footsteps(madness));
        pool.add(HallucinationManager::caveAmbience);
        if (madness >= TIER_2) {
            pool.add(HallucinationManager::whisper);
            pool.add(HallucinationManager::blockSound);
            pool.add(() -> footsteps(madness)); // footsteps get more likely, too
        }
        if (madness >= TIER_3) {
            pool.add(HallucinationManager::visualFlicker);
            pool.add(HallucinationManager::whisper);
        }
        pool.get(RANDOM.nextInt(pool.size())).run();
    }

    /**
     * The {@code hallucination} pseudo-effect.
     * Params: {@code event=footsteps|whisper|cave|block|flicker|random}.
     */
    public static void triggerNamed(String params) {
        if (Minecraft.getInstance().player == null) return;

        String event = "random";
        if (params != null) {
            for (String part : params.split(",")) {
                String[] kv = part.split("=", 2);
                if (kv.length == 2 && kv[0].trim().equals("event")) {
                    event = kv[1].trim();
                }
            }
        }

        // Explicit triggers count as high madness, so the full pool is available
        double madness = Math.max(BeyonderState.getMadness(), 80.0);
        switch (event) {
            case "footsteps" -> footsteps(madness);
            case "whisper" -> whisper();
            case "cave" -> caveAmbience();
            case "block" -> blockSound();
            case "flicker" -> visualFlicker();
            default -> triggerRandom(madness);
        }
    }

    private static void footsteps(double madness) {
        SoundEvent step = switch (RANDOM.nextInt(3)) {
            case 0 -> SoundEvents.STONE_STEP;
            case 1 -> SoundEvents.GRAVEL_STEP;
            default -> SoundEvents.WOOD_STEP;
        };
        int steps = 3 + RANDOM.nextInt(3);
        double angle = RANDOM.nextDouble() * 70 - 35;
        double startDist = 6.0;
        double endDist = madness >= TIER_3 ? 2.0 : 3.5; // they come closer when you're far gone
        long now = System.currentTimeMillis();
        long stepInterval = 300 + RANDOM.nextInt(80);

        for (int i = 0; i < steps; i++) {
            double t = i / (double) (steps - 1);
            double dist = startDist + (endDist - startDist) * t;
            Vec3 pos = offsetFromPlayer(dist, angle + RANDOM.nextDouble() * 10 - 5);
            pending.add(new ScheduledSound(now + i * stepInterval, step,
                    pos.x, pos.y, pos.z, 0.55f, 0.85f + RANDOM.nextFloat() * 0.2f));
        }
    }

    private static void whisper() {
        Vec3 pos = offsetFromPlayer(2.5 + RANDOM.nextDouble() * 2.0, RANDOM.nextDouble() * 120 - 60);
        playAt(WHISPER_SOUND, pos.x, pos.y, pos.z, 0.9f, 0.9f + RANDOM.nextFloat() * 0.2f);
    }

    private static void caveAmbience() {
        Vec3 pos = offsetFromPlayer(8.0 + RANDOM.nextDouble() * 6.0, RANDOM.nextDouble() * 360);
        playAt(SoundEvents.AMBIENT_CAVE.value(), pos.x, pos.y, pos.z, 0.8f, 1.0f);
    }

    private static void blockSound() {
        SoundEvent sound = switch (RANDOM.nextInt(4)) {
            case 0 -> SoundEvents.WOODEN_DOOR_OPEN;
            case 1 -> SoundEvents.WOODEN_DOOR_CLOSE;
            case 2 -> SoundEvents.CHEST_OPEN;
            default -> SoundEvents.WOODEN_TRAPDOOR_OPEN;
        };
        Vec3 pos = offsetFromPlayer(4.0 + RANDOM.nextDouble() * 4.0, RANDOM.nextDouble() * 360);
        playAt(sound, pos.x, pos.y, pos.z, 0.6f, 0.9f + RANDOM.nextFloat() * 0.2f);
    }

    private static void visualFlicker() {
        if (RANDOM.nextBoolean()) {
            EffectManager.trigger(EyesEffect.ID, "count=1,stare=1200");
        } else {
            EffectManager.trigger(GlitchEffect.ID, "intensity=0.45,duration=700");
        }
    }

    /** An angle offset of 0 is directly behind the player. */
    private static Vec3 offsetFromPlayer(double distance, double angleOffsetDeg) {
        var player = Minecraft.getInstance().player;
        double yaw = Math.toRadians(player.getYRot() + 180.0 + angleOffsetDeg);
        double dx = -Math.sin(yaw) * distance;
        double dz = Math.cos(yaw) * distance;
        return player.position().add(dx, 0, dz);
    }

    private static void playAt(SoundEvent event, double x, double y, double z, float volume, float pitch) {
        float master = HudConfig.getSettings().effectSoundVolume;
        if (master <= 0f) return;
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;
        client.getSoundManager().play(new SimpleSoundInstance(event, SoundSource.AMBIENT,
                volume * master, pitch, RandomSource.create(), x, y, z));
    }
}
