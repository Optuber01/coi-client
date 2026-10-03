package dev.ua.ikeepcalm.coi.domain.ceremony;

import dev.ua.ikeepcalm.coi.config.HudConfig;

import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;

/**
 * The sky and the fog an aftermath leaves over the world. Three rules shape it:
 * <ul>
 * <li><b>Re-sends must be free.</b> Aftermaths repeat the same tint every five to ten seconds,
 *     so this holds a <em>target</em> and walks toward it; sending the tint already on screen
 *     moves nothing, where "start a fade-in" would restart the fade twice a minute.</li>
 * <li><b>{@code density=0} is the clear, not thin fog</b> — it ramps the whole tint out,
 *     colour included, and has to put the vanilla sky back exactly.</li>
 * <li><b>Only the distances answer to {@code ceremonyFogDensity}</b>; fog is the one parameter
 *     here with a gameplay cost, and the colour rides the full tint regardless.</li>
 * </ul>
 */
public final class SkyTintState {

    private static final long RAMP_MS = 1500;
    private static final long DEFAULT_CLEAR_MS = 800;
    private static final float MAX_SQUEEZE = 0.82f;
    private static final float[] sky = {1f, 1f, 1f};
    private static final float[] fog = {1f, 1f, 1f};
    private static final float[] targetSky = {1f, 1f, 1f};
    private static final float[] targetFog = {1f, 1f, 1f};
    private static final float[] fromSky = {1f, 1f, 1f};
    private static final float[] fromFog = {1f, 1f, 1f};
    private static float strength;
    private static float targetStrength;
    private static float fromStrength;
    private static long changedAt;
    private static long rampMs = RAMP_MS;
    private static long expiresAt;

    private SkyTintState() {
    }

    public static void set(int skyRgb, int fogRgb, float density, long durationMs) {
        float next = Math.clamp(density, 0f, 1f);
        if (next <= 0f) {
            clear(durationMs > 0 ? durationMs : DEFAULT_CLEAR_MS);
            return;
        }
        advance();
        // A tint arriving over nothing has no colour to travel from, so it starts at its own
        if (strength <= 0.001f) {
            unpack(skyRgb, sky);
            unpack(fogRgb, fog);
        }
        boolean unchanged = Math.abs(next - targetStrength) < 1e-4f
                && same(targetSky, skyRgb) && same(targetFog, fogRgb);
        expiresAt = durationMs > 0 ? System.currentTimeMillis() + durationMs : 0;
        if (unchanged) return;

        capture();
        unpack(skyRgb, targetSky);
        unpack(fogRgb, targetFog);
        targetStrength = next;
        rampMs = RAMP_MS;
        changedAt = System.currentTimeMillis();
    }

    public static void clear() {
        clear(DEFAULT_CLEAR_MS);
    }

    private static void clear(long fadeMs) {
        if (targetStrength <= 0f) return;
        advance();
        capture();
        targetStrength = 0f;
        rampMs = Math.max(1, fadeMs);
        changedAt = System.currentTimeMillis();
        expiresAt = 0;
    }

    public static void reset() {
        strength = 0f;
        targetStrength = 0f;
        fromStrength = 0f;
        changedAt = 0;
        expiresAt = 0;
        rampMs = RAMP_MS;
    }

    public static boolean active() {
        advance();
        return strength > 0.001f || targetStrength > 0f;
    }

    /**
     * Called from the renderer, so it does its own advance rather than relying on a tick.
     */
    public static int skyColor(int argb) {
        advance();
        if (strength <= 0.001f) return argb;
        return ARGB.color(ARGB.alpha(argb),
                blend(ARGB.red(argb), sky[0]),
                blend(ARGB.green(argb), sky[1]),
                blend(ARGB.blue(argb), sky[2]));
    }

    public static void applyFog(FogData data) {
        advance();
        if (strength <= 0.001f) return;

        data.color.set(Mth.lerp(strength, data.color.x, fog[0]),
                Mth.lerp(strength, data.color.y, fog[1]),
                Mth.lerp(strength, data.color.z, fog[2]),
                data.color.w);

        float thickness = strength * Math.clamp(HudConfig.getSettings().ceremonyFogDensity, 0f, 1f);
        if (thickness <= 0.001f) return;
        float squeeze = 1f - MAX_SQUEEZE * thickness;
        data.environmentalStart *= squeeze;
        data.environmentalEnd *= squeeze;
        data.renderDistanceStart *= squeeze;
        data.renderDistanceEnd *= squeeze;
        data.skyEnd *= squeeze;
        data.cloudEnd *= squeeze;
    }

    private static int blend(int channel, float target) {
        return Math.round(Mth.lerp(strength, channel, target * 255f));
    }

    private static void capture() {
        fromStrength = strength;
        System.arraycopy(sky, 0, fromSky, 0, 3);
        System.arraycopy(fog, 0, fromFog, 0, 3);
    }

    /**
     * Idempotent: every reader calls it, and a second call in the same frame moves nothing.
     */
    private static void advance() {
        long now = System.currentTimeMillis();
        if (expiresAt > 0 && now >= expiresAt) {
            // Zeroed before the call: clear() advances first, and would otherwise see the
            // same lapsed deadline and recurse
            expiresAt = 0;
            clear(DEFAULT_CLEAR_MS);
            return;
        }
        if (changedAt == 0) return;
        float t = Mth.clamp((now - changedAt) / (float) rampMs, 0f, 1f);
        strength = Mth.lerp(t, fromStrength, targetStrength);
        for (int i = 0; i < 3; i++) {
            sky[i] = Mth.lerp(t, fromSky[i], targetSky[i]);
            fog[i] = Mth.lerp(t, fromFog[i], targetFog[i]);
        }
    }

    private static void unpack(int rgb, float[] into) {
        into[0] = ((rgb >> 16) & 0xFF) / 255f;
        into[1] = ((rgb >> 8) & 0xFF) / 255f;
        into[2] = (rgb & 0xFF) / 255f;
    }

    private static boolean same(float[] stored, int rgb) {
        return Math.round(stored[0] * 255f) == ((rgb >> 16) & 0xFF)
                && Math.round(stored[1] * 255f) == ((rgb >> 8) & 0xFF)
                && Math.round(stored[2] * 255f) == (rgb & 0xFF);
    }
}
