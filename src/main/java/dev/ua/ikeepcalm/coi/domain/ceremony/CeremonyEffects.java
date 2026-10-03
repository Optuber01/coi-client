package dev.ua.ikeepcalm.coi.domain.ceremony;

import dev.ua.ikeepcalm.coi.domain.ceremony.CameraCinematics.CameraOrbitState;
import dev.ua.ikeepcalm.coi.domain.ceremony.CameraCinematics.ScreenShakeState;
import dev.ua.ikeepcalm.coi.domain.effect.visual.EffectParams;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;

/**
 * The hard reset every ceremony exit path shares, plus the one tick batch 7 needs. These ride
 * the existing {@code coi-client:effect} channel and are registered in {@code EffectManager}.
 * <p>
 * <b>A clear on dimension change is cheap insurance.</b> Every clear the server sends travels
 * the ordinary suppressible path, so a player who becomes visually immune between an effect and
 * its clear would keep it with nothing able to reach them; walking through a portal can.
 */
public final class CeremonyEffects {

    /**
     * The world the last tick saw, so a change of world is a change of state.
     */
    private static Level lastLevel;

    private CeremonyEffects() {
    }

    /**
     * Drops every ceremony effect outright — no fades.
     */
    public static void reset() {
        PostFx.reset();
        SkyTintState.reset();
        LetterboxState.reset();
        ScreenShakeState.reset();
        CameraOrbitState.reset();
        AudioBedState.reset();
    }

    public static void tick(Minecraft client) {
        if (client.level != lastLevel) {
            lastLevel = client.level;
            reset();
            return;
        }
        if (CameraOrbitState.active() && wantsControl(client)) {
            CameraOrbitState.breakOut();
        }
    }

    /**
     * Deliberately broad: a player reaching for a key during a cutscene wants out of it.
     */
    private static boolean wantsControl(Minecraft client) {
        if (client.gui.screen() != null) return true;
        Options options = client.options;
        return options.keyUp.isDown() || options.keyDown.isDown()
                || options.keyLeft.isDown() || options.keyRight.isDown()
                || options.keyJump.isDown() || options.keyShift.isDown()
                || options.keyAttack.isDown() || options.keyUse.isDown();
    }

    /**
     * The {@code key=value,key=value} body of a ceremony effect, read defensively: unknown keys
     * are ignored and a malformed number reads as the caller's default, so a packet from a newer
     * plugin degrades to the part this client understands.
     * <p>
     * It also folds the two shapes a stop arrives in — the bare {@code stop} ({@code orbit}) and
     * {@code stop,fade=1500} ({@code bed}). Both answer {@link #stop()}.
     */
    public static final class CeremonyParams {

        private static final String STOP = "stop";

        private final Map<String, String> values = new HashMap<>();
        private final boolean stop;

        private CeremonyParams(String params) {
            this.stop = isStop(params);
            EffectParams.forEach(params, values::put);
        }

        public static CeremonyParams of(String params) {
            return new CeremonyParams(params);
        }

        /**
         * True for both {@code "stop"} and {@code "stop,key=value,…"}.
         */
        public static boolean isStop(String params) {
            return params != null && (params.equals(STOP) || params.startsWith(STOP + ","));
        }

        public boolean stop() {
            return stop;
        }

        public boolean has(String key) {
            return values.containsKey(key);
        }

        public String string(String key, String fallback) {
            String value = values.get(key);
            return value == null || value.isBlank() ? fallback : value;
        }

        public float floatOf(String key, float fallback) {
            try {
                return values.containsKey(key) ? Float.parseFloat(values.get(key)) : fallback;
            } catch (NumberFormatException e) {
                return fallback;
            }
        }

        public double doubleOf(String key, double fallback) {
            try {
                return values.containsKey(key) ? Double.parseDouble(values.get(key)) : fallback;
            } catch (NumberFormatException e) {
                return fallback;
            }
        }

        public long longOf(String key, long fallback) {
            try {
                return values.containsKey(key) ? Long.parseLong(values.get(key)) : fallback;
            } catch (NumberFormatException e) {
                return fallback;
            }
        }

        /**
         * Six hex digits, no leading {@code #} — the shape {@code sky_tint} sends.
         */
        public int rgb(String key, int fallback) {
            try {
                return values.containsKey(key) ? Integer.parseInt(values.get(key).replace("#", ""), 16) & 0xFFFFFF : fallback;
            } catch (NumberFormatException e) {
                return fallback;
            }
        }
    }
}
