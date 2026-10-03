package dev.ua.ikeepcalm.coi.domain.ceremony;

import net.minecraft.util.Mth;

/**
 * Cinema bars for the length of an act. They <em>slide</em>, which is the reason this is a
 * state class and not two {@code fill} calls: {@code intensity=0} arrives on every teardown
 * path and a cut back to a full screen looks like a crash. Retargeting mid-slide starts from
 * where the bars actually are.
 */
public final class LetterboxState {

    /**
     * Fraction of the screen one bar covers at intensity 1.
     */
    public static final float MAX_COVERAGE = 0.12f;

    private static final long SLIDE_MS = 600;

    private static float current;
    private static float target;
    private static float from;
    private static long changedAt;
    private static long expiresAt;

    private LetterboxState() {
    }

    public static void set(float intensity, long durationMs) {
        retarget(Math.clamp(intensity, 0f, 1f));
        expiresAt = durationMs > 0 ? System.currentTimeMillis() + durationMs : 0;
    }

    public static void clear() {
        retarget(0f);
        expiresAt = 0;
    }

    public static void reset() {
        current = 0f;
        target = 0f;
        from = 0f;
        changedAt = 0;
        expiresAt = 0;
    }

    public static boolean active() {
        return coverage() > 0.001f || target > 0f;
    }

    /**
     * As a fraction of the screen height.
     */
    public static float coverage() {
        advance();
        return current * MAX_COVERAGE;
    }

    private static void retarget(float next) {
        if (Math.abs(next - target) < 1e-4f) return;
        advance();
        from = current;
        target = next;
        changedAt = System.currentTimeMillis();
    }

    /**
     * Idempotent: every reader calls it, and a second call in the same frame moves nothing.
     */
    private static void advance() {
        long now = System.currentTimeMillis();
        if (expiresAt > 0 && now >= expiresAt) {
            expiresAt = 0;
            from = current;
            target = 0f;
            changedAt = now;
        }
        if (changedAt == 0) {
            current = target;
            return;
        }
        current = Mth.lerp(Mth.clamp((now - changedAt) / (float) SLIDE_MS, 0f, 1f), from, target);
    }
}
