package dev.ua.ikeepcalm.coi.domain.ceremony;

import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Which post-processing pass the ceremony wants over the world, and the chain each name
 * resolves to.
 * <p>
 * The state lives here rather than on {@code CeremonyEffect} because the server re-sends a pass
 * to replace one already running and {@code EffectManager.trigger} throws the old instance away;
 * anything that must survive that sits outside the instance. Everything here is a pure function
 * of the wall clock, so the render thread can read it from a mixin without a tick ever running.
 * <p>
 * <b>Why the intensity is quantised:</b> a post chain bakes its uniform values at load time —
 * nothing binds a clock or a settable float to a post pass — so an intensity cannot be dialled
 * into one chain at runtime. Every name therefore ships at four graded strengths under
 * {@code assets/coi-client/post_effect/ceremony/} and {@link #resolve} picks the nearest.
 * <p>
 * {@link #NONE} is the clear, not a pass, and an unknown name folds onto it: leaving the
 * previous pass stuck is the one failure a witness cannot get out of.
 */
public final class PostFx {

    public static final String NONE = "none";

    public static final List<String> NAMES = List.of("desaturate", "invert", "bits", "sobel", "wobble");

    /**
     * How many graded variants each name ships at.
     */
    private static final int STEPS = 4;

    /**
     * Built once: {@link #resolve} runs inside the frame loop.
     */
    private static final Map<String, Identifier[]> CHAINS = NAMES.stream().collect(Collectors.toMap(
            name -> name,
            name -> IntStream.rangeClosed(1, STEPS)
                    .mapToObj(step -> Identifier.fromNamespaceAndPath("coi-client",
                            "ceremony/" + name + "_" + step * (100 / STEPS)))
                    .toArray(Identifier[]::new)));

    private static final long FADE_IN_MS = 400;
    private static final long FADE_OUT_MS = 700;

    private static String pass = NONE;
    private static float intensity;
    private static long startedAt;
    /**
     * 0 means "until replaced or stopped"; the server sends that a lot.
     */
    private static long duration;
    /**
     * When a clear began, so the pass can step down instead of vanishing.
     */
    private static long clearedAt;

    private PostFx() {
    }

    public static void set(String name, float requested, long durationMs) {
        String resolved = sanitise(name);
        if (NONE.equals(resolved)) {
            clear();
            return;
        }
        pass = resolved;
        intensity = Math.clamp(requested, 0f, 1f);
        duration = Math.max(0, durationMs);
        startedAt = System.currentTimeMillis();
        clearedAt = 0;
    }

    public static void clear() {
        if (NONE.equals(pass) || clearedAt > 0) return;
        clearedAt = System.currentTimeMillis();
    }

    /**
     * Drops the pass outright — disconnect, dimension change, debug stop.
     */
    public static void reset() {
        pass = NONE;
        intensity = 0f;
        startedAt = 0;
        duration = 0;
        clearedAt = 0;
    }

    public static Identifier chain() {
        return resolve(pass, strength());
    }

    public static boolean active() {
        return !NONE.equals(pass) && strength() > 0f;
    }

    /**
     * The intensity after both ramps; {@link #resolve} then quantises it to three visible steps.
     */
    private static float strength() {
        if (NONE.equals(pass)) return 0f;
        long now = System.currentTimeMillis();

        float level = intensity * Mth.clamp((now - startedAt) / (float) FADE_IN_MS, 0f, 1f);
        if (clearedAt > 0) {
            level *= 1f - Mth.clamp((now - clearedAt) / (float) FADE_OUT_MS, 0f, 1f);
        } else if (duration > 0) {
            long remaining = startedAt + duration - now;
            if (remaining <= 0) return 0f;
            if (remaining < FADE_OUT_MS) level *= remaining / (float) FADE_OUT_MS;
        }
        return level;
    }

    /**
     * @return the chain at the nearest graded intensity, or {@code null} for {@link #NONE}, an
     * unknown name, or an intensity that has faded to nothing
     */
    public static Identifier resolve(String name, float level) {
        Identifier[] graded = name == null ? null : CHAINS.get(name);
        if (graded == null) return null;
        int step = Math.round(Math.clamp(level, 0f, 1f) * STEPS);
        return step <= 0 ? null : graded[step - 1];
    }

    public static String sanitise(String name) {
        return name != null && NAMES.contains(name) ? name : NONE;
    }
}
