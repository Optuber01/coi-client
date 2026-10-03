package dev.ua.ikeepcalm.coi.domain.ceremony;

import dev.ua.ikeepcalm.coi.domain.ceremony.CameraCinematics.CameraOrbitState;
import dev.ua.ikeepcalm.coi.domain.ceremony.CameraCinematics.ScreenShakeState;
import dev.ua.ikeepcalm.coi.domain.ceremony.CeremonyEffects.CeremonyParams;
import dev.ua.ikeepcalm.coi.domain.effect.VisualEffect;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.player.Player;

/**
 * The six ceremony effects. <b>Each one is a handle, not a holder:</b> the server re-sends an
 * effect to replace one already running and {@code EffectManager} discards the old instance, so
 * everything that must survive a re-send lives in the matching {@code *State} class.
 * {@link #isFinished()} asks the state rather than the clock, so an effect stays registered
 * until its fade-out has actually finished.
 */
public class CeremonyEffect implements VisualEffect {

    /**
     * Bars are drawn pure black: anything softer reads as a rendering fault.
     */
    private static final int BAR_COLOR = 0xFF000000;
    private final Kind kind;

    public CeremonyEffect(Kind kind) {
        this.kind = kind;
    }

    @Override
    public String getId() {
        return kind.id;
    }

    @Override
    public String getDisplayName() {
        return kind.displayName;
    }

    @Override
    public String getDefaultParams() {
        return kind.defaultParams;
    }

    @Override
    public void start(String params) {
        kind.apply(CeremonyParams.of(params));
    }

    @Override
    public void render(GuiGraphicsExtractor ctx, int screenWidth, int screenHeight, float tickDelta) {
        kind.render(ctx, screenWidth, screenHeight);
    }

    @Override
    public boolean isFinished() {
        return !kind.active();
    }

    @Override
    public void stop() {
        stop("stop");
    }

    @Override
    public void stop(String params) {
        kind.clear(CeremonyParams.of(params));
    }

    public enum Kind {
        POSTFX("postfx", "Rite: Post FX", "name=desaturate,intensity=0.65,duration=4000") {
            @Override
            void apply(CeremonyParams p) {
                PostFx.set(p.string("name", PostFx.NONE),
                        p.floatOf("intensity", 1f), p.longOf("duration", 0));
            }

            @Override
            void clear(CeremonyParams p) {
                PostFx.clear();
            }

            @Override
            boolean active() {
                return PostFx.active();
            }
        },
        SKY_TINT("sky_tint", "Rite: Sky Tint", "sky=8E8E8E,fog=A4A4A4,density=0.8,duration=15000") {
            @Override
            void apply(CeremonyParams p) {
                SkyTintState.set(p.rgb("sky", 0xFFFFFF), p.rgb("fog", 0xFFFFFF),
                        p.floatOf("density", 0f), p.longOf("duration", 0));
            }

            @Override
            void clear(CeremonyParams p) {
                SkyTintState.clear();
            }

            @Override
            boolean active() {
                return SkyTintState.active();
            }
        },
        LETTERBOX("letterbox", "Rite: Letterbox", "intensity=0.6,duration=15000") {
            @Override
            void apply(CeremonyParams p) {
                LetterboxState.set(p.floatOf("intensity", 0f), p.longOf("duration", 0));
            }

            @Override
            void clear(CeremonyParams p) {
                LetterboxState.clear();
            }

            @Override
            boolean active() {
                return LetterboxState.active();
            }

            @Override
            void render(GuiGraphicsExtractor ctx, int w, int h) {
                int bar = Math.round(h * LetterboxState.coverage());
                if (bar <= 0) return;
                ctx.fill(0, 0, w, bar, BAR_COLOR);
                ctx.fill(0, h - bar, w, h, BAR_COLOR);
            }
        },
        SHAKE("shake", "Rite: Shake", "amplitude=0.4,hz=3.0,duration=2000") {
            @Override
            void apply(CeremonyParams p) {
                ScreenShakeState.set(p.floatOf("amplitude", 0f), p.floatOf("hz", 3f), p.longOf("duration", 1000));
            }

            @Override
            void clear(CeremonyParams p) {
                ScreenShakeState.reset();
            }

            @Override
            boolean active() {
                return ScreenShakeState.active();
            }
        },
        ORBIT("orbit", "Rite: Orbit", "radius=7.00,height=3.00,duration=2500") {
            @Override
            void apply(CeremonyParams p) {
                // Defaulting the coordinates to the player is what lets F8 test the shot
                // with no server attached
                Player player = Minecraft.getInstance().player;
                double x = p.doubleOf("x", player == null ? 0 : player.getX());
                double y = p.doubleOf("y", player == null ? 0 : player.getY());
                double z = p.doubleOf("z", player == null ? 0 : player.getZ());
                CameraOrbitState.set(x, y, z, p.floatOf("radius", 7f), p.floatOf("height", 3f),
                        p.longOf("duration", 2500));
            }

            @Override
            void clear(CeremonyParams p) {
                CameraOrbitState.clear();
            }

            @Override
            boolean active() {
                return CameraOrbitState.active();
            }
        },
        BED("bed", "Rite: Audio Bed", "track=minecraft:music.creative,volume=0.8,fade=2000") {
            @Override
            void apply(CeremonyParams p) {
                AudioBedState.play(p.string("track", ""), p.floatOf("volume", 0.8f), p.longOf("fade", 0));
            }

            @Override
            void clear(CeremonyParams p) {
                AudioBedState.stop(p.longOf("fade", 0));
            }

            @Override
            boolean active() {
                return AudioBedState.active();
            }
        };

        private final String id;
        private final String displayName;
        private final String defaultParams;

        Kind(String id, String displayName, String defaultParams) {
            this.id = id;
            this.displayName = displayName;
            this.defaultParams = defaultParams;
        }

        public String id() {
            return id;
        }

        abstract void apply(CeremonyParams params);

        abstract void clear(CeremonyParams params);

        abstract boolean active();

        void render(GuiGraphicsExtractor ctx, int w, int h) {
        }
    }
}
