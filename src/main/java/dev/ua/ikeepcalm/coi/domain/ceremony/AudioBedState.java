package dev.ua.ikeepcalm.coi.domain.ceremony;

import dev.ua.ikeepcalm.coi.util.CoiLog;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * The scene's score, on the music layer rather than as a {@code RECORDS} sound, so the player's
 * music slider governs it and it does not fight vanilla's own music.
 * <p>
 * <b>Nothing sends {@code coi-client:bed} yet</b> — the twenty-two
 * {@code mysterria:ascension.*} tracks it names do not exist in the resource pack, and a
 * {@code track} that resolves to nothing plays silence.
 */
public final class AudioBedState {

    private static final long DEFAULT_FADE_MS = 1500;

    private static CeremonyBedSound current;

    private AudioBedState() {
    }

    public static void play(String track, float volume, long fadeMs) {
        Identifier id = Identifier.tryParse(track);
        if (id == null) {
            CoiLog.LOG.warn("Ceremony bed names an unparseable track '{}'", track);
            return;
        }
        stop(fadeMs > 0 ? fadeMs : DEFAULT_FADE_MS);
        current = new CeremonyBedSound(id, volume, fadeMs > 0 ? fadeMs : DEFAULT_FADE_MS);
        Minecraft.getInstance().getSoundManager().play(current);
    }

    public static void stop(long fadeMs) {
        if (current == null) return;
        current.fadeOut(fadeMs > 0 ? fadeMs : DEFAULT_FADE_MS);
        current = null;
    }

    /**
     * Disconnect and dimension change: no fade, the world it scored is gone.
     */
    public static void reset() {
        if (current != null) {
            Minecraft.getInstance().getSoundManager().stop(current);
            current = null;
        }
    }

    public static boolean active() {
        return current != null;
    }

    /**
     * The sound engine re-reads a tickable instance's volume every tick, so the fade is just a
     * volume recomputed in {@link #tick()}; a faded-out bed reports itself stopped.
     */
    public static class CeremonyBedSound extends AbstractTickableSoundInstance {

        private final float peak;
        private final long fadeInMs;
        private final long startedAt = System.currentTimeMillis();

        private long fadeOutAt;
        private long fadeOutMs = 1;

        public CeremonyBedSound(Identifier track, float peak, long fadeInMs) {
            super(SoundEvent.createVariableRangeEvent(track), SoundSource.MUSIC, RandomSource.create());
            this.peak = Math.clamp(peak, 0f, 1f);
            this.fadeInMs = Math.max(1, fadeInMs);
            this.looping = true;
            this.relative = true;
            this.attenuation = SoundInstance.Attenuation.NONE;
            this.volume = 0f;
            this.x = 0;
            this.y = 0;
            this.z = 0;
        }

        /**
         * Begins the fade out; the instance stops itself once it reaches zero.
         */
        public void fadeOut(long millis) {
            if (fadeOutAt > 0) return;
            fadeOutAt = System.currentTimeMillis();
            fadeOutMs = Math.max(1, millis);
        }

        public boolean fading() {
            return fadeOutAt > 0;
        }

        @Override
        public void tick() {
            long now = System.currentTimeMillis();
            float level = peak * Mth.clamp((now - startedAt) / (float) fadeInMs, 0f, 1f);
            if (fadeOutAt > 0) {
                level *= 1f - Mth.clamp((now - fadeOutAt) / (float) fadeOutMs, 0f, 1f);
                if (level <= 0f) {
                    this.volume = 0f;
                    stop();
                    return;
                }
            }
            this.volume = level;
        }
    }
}
