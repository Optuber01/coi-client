package dev.ua.ikeepcalm.coi.domain.ceremony;

import dev.ua.ikeepcalm.coi.config.HudConfig;

import net.minecraft.util.Mth;

/**
 * The tremor laid over the view, and the arc that swings it around a point. Both are
 * <b>view only</b>: the player entity never moves, their gamemode never changes, and nothing
 * here is sent back to the server — a camera effect that forgot that would desync.
 * <p>
 * Both are pure functions of the wall clock, with no tick of their own, so the render thread
 * can read either several times in a frame and a second read in the same millisecond moves
 * nothing. {@code CameraMixin} applies both, after the camera has aligned with the player.
 */
public final class CameraCinematics {

    private CameraCinematics() {
    }

    public static final class ScreenShakeState {

        /**
         * Degrees of yaw at amplitude 1. Pitch takes 70% of it.
         */
        private static final float MAX_YAW_DEGREES = 3.2f;
        private static final float PITCH_RATIO = 0.7f;
        /**
         * Deliberately irrational against the yaw rate: equal rates trace a straight diagonal.
         */
        private static final float PITCH_FREQUENCY_RATIO = 1.37f;
        private static final float PITCH_PHASE = 1.1f;

        private static float amplitude;
        private static float hz = 3.0f;
        private static long duration;
        private static long startedAt;

        private ScreenShakeState() {
        }

        public static void set(float requestedAmplitude, float requestedHz, long durationMs) {
            amplitude = Math.clamp(requestedAmplitude, 0f, 1f);
            // Not clamped server-side; a negative or absurd rate is a buzz, not a shake
            hz = Math.clamp(requestedHz, 0.1f, 20f);
            duration = Math.max(1, durationMs);
            startedAt = System.currentTimeMillis();
        }

        public static void reset() {
            amplitude = 0f;
            duration = 0;
            startedAt = 0;
        }

        public static boolean active() {
            return amplitude > 0f && duration > 0 && System.currentTimeMillis() - startedAt < duration;
        }

        public static float yawDegrees() {
            return (float) Math.sin(phase()) * magnitude();
        }

        public static float pitchDegrees() {
            return (float) Math.sin(phase() * PITCH_FREQUENCY_RATIO + PITCH_PHASE) * magnitude() * PITCH_RATIO;
        }

        private static double phase() {
            return 2.0 * Math.PI * hz * (System.currentTimeMillis() - startedAt) / 1000.0;
        }

        /**
         * Decays to exactly zero: a shake that cuts mid-swing leaves the view off-axis.
         */
        private static float magnitude() {
            HudConfig.HudSettings settings = HudConfig.getSettings();
            if (!settings.enableCameraShake || settings.epilepsyMode || !active()) return 0f;
            float remaining = 1f - (System.currentTimeMillis() - startedAt) / (float) duration;
            return amplitude * MAX_YAW_DEGREES * Mth.square(Mth.clamp(remaining, 0f, 1f));
        }
    }

    /**
     * The arc is <b>offered, never imposed</b>, in three ways: it eases out of the player's own
     * eyes and back into them so there is no cut at either end, {@link #breakOut()} hands
     * control back the instant the player touches a movement key, and
     * {@code enableCinematicCamera} turns it off for good.
     */
    public static final class CameraOrbitState {

        /**
         * How much of the circle the camera walks over the whole shot.
         */
        private static final double SWEEP_DEGREES = 150.0;
        /**
         * Fraction of the shot spent easing out of, and back into, the eyes.
         */
        private static final float EASE = 0.2f;
        /**
         * How fast an interrupted shot hands the camera back.
         */
        private static final long RELEASE_MS = 300;
        /**
         * Below this the camera is close enough to the eyes not to be worth third person.
         */
        private static final float DETACH_BLEND = 0.02f;

        private static double centreX;
        private static double centreY;
        private static double centreZ;
        private static float radius = 7f;
        private static float height = 3f;
        private static long duration;
        private static long startedAt;
        private static long releasedAt;
        /**
         * Captured on the first frame, so the arc opens where the player stands.
         */
        private static Double startAngle;

        private CameraOrbitState() {
        }

        public static void set(double x, double y, double z, float orbitRadius, float orbitHeight, long durationMs) {
            centreX = x;
            centreY = y;
            centreZ = z;
            radius = Math.clamp(orbitRadius, 1f, 64f);
            height = Math.clamp(orbitHeight, -32f, 32f);
            duration = Math.max(1, durationMs);
            startedAt = System.currentTimeMillis();
            releasedAt = 0;
            startAngle = null;
        }

        public static void clear() {
            if (releasedAt == 0 && running()) releasedAt = System.currentTimeMillis();
        }

        public static void breakOut() {
            clear();
        }

        public static void reset() {
            duration = 0;
            startedAt = 0;
            releasedAt = 0;
            startAngle = null;
        }

        public static boolean active() {
            return running() && HudConfig.getSettings().enableCinematicCamera;
        }

        /**
         * True once the shot has taken the camera off the player's eyes. Two renderer decisions
         * hang off this one answer and must not disagree: the camera detaches so the player's
         * body is in the shot, and the first-person hand stops drawing — vanilla gates the hand
         * on the camera <em>type</em>, not on {@code Camera#isDetached}, so an orbit that only
         * set {@code detached} left an arm floating over the cinematic.
         */
        public static boolean displacing() {
            return active() && blend() > DETACH_BLEND;
        }

        /**
         * @return where the camera goes this frame, or {@code null} to leave it
         * exactly where the player put it
         */
        public static Placement resolve(double eyeX, double eyeY, double eyeZ, float eyeYRot, float eyeXRot) {
            if (!active()) return null;
            float blend = blend();
            if (blend <= 0.001f) return null;

            if (startAngle == null) {
                startAngle = Math.atan2(eyeZ - centreZ, eyeX - centreX);
            }
            double angle = startAngle + Math.toRadians(SWEEP_DEGREES) * ease(progress());
            double camX = centreX + Math.cos(angle) * radius;
            double camY = centreY + height;
            double camZ = centreZ + Math.sin(angle) * radius;

            double dx = centreX - camX;
            double dy = centreY + height * 0.3 - camY;
            double dz = centreZ - camZ;
            float lookY = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
            float lookX = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));

            return new Placement(
                    Mth.lerp(blend, eyeX, camX),
                    Mth.lerp(blend, eyeY, camY),
                    Mth.lerp(blend, eyeZ, camZ),
                    eyeYRot + Mth.wrapDegrees(lookY - eyeYRot) * blend,
                    Mth.lerp(blend, eyeXRot, lookX),
                    blend);
        }

        private static boolean running() {
            if (duration <= 0) return false;
            long now = System.currentTimeMillis();
            if (releasedAt > 0 && now - releasedAt >= RELEASE_MS) return false;
            return now - startedAt < duration || releasedAt > 0;
        }

        private static float progress() {
            return Mth.clamp((System.currentTimeMillis() - startedAt) / (float) duration, 0f, 1f);
        }

        /**
         * 0 at both ends of the shot, so the camera never cuts; a release collapses it early.
         */
        private static float blend() {
            float t = progress();
            float in = Mth.clamp(t / EASE, 0f, 1f);
            float out = Mth.clamp((1f - t) / EASE, 0f, 1f);
            float blend = smoothstep(in) * smoothstep(out);
            if (releasedAt > 0) {
                blend *= 1f - Mth.clamp((System.currentTimeMillis() - releasedAt) / (float) RELEASE_MS, 0f, 1f);
            }
            return blend;
        }

        private static double ease(float t) {
            return smoothstep(t);
        }

        private static float smoothstep(float t) {
            return t * t * (3f - 2f * t);
        }

        public record Placement(double x, double y, double z, float yRot, float xRot, float blend) {
        }
    }
}
