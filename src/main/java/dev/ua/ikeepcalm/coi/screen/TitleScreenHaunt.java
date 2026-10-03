package dev.ua.ikeepcalm.coi.screen;

import dev.ua.ikeepcalm.coi.config.ClientStateStore;
import dev.ua.ikeepcalm.coi.config.HudConfig;
import dev.ua.ikeepcalm.coi.domain.effect.visual.EffectPaint;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.SplashRenderer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.Random;

/**
 * Corruption is the higher of madness / permanent madness at last disconnect, persisted via
 * {@link ClientStateStore}. The haunting respects the hallucinations toggle; the flavor splashes
 * do not, since they are branding rather than horror. Slow fades only — nothing flashes.
 */
public class TitleScreenHaunt {

    private static final Random RANDOM = new Random();
    private static final Identifier[] PUPIL_FRAMES = new Identifier[4];

    static {
        for (int i = 0; i < PUPIL_FRAMES.length; i++) {
            PUPIL_FRAMES[i] = Identifier.fromNamespaceAndPath("coi-client", "textures/eyes/eye4." + (i + 1) + ".png");
        }
    }

    private static final int HAUNT_SPLASH_LINES = 5;
    private static final int LOTM_SPLASH_LINES = 7;
    private static final long EYE_DURATION_MS = 3200;
    private static final long EYE_FADE_MS = 900;

    private static long nextEyeAt = 0;
    private static long eyeStartedAt = 0;
    private static int eyeX;
    private static int eyeY;
    private static int eyeHalfW;
    private static float eyeRotation;
    private static boolean splashDecided = false;
    private static Component splashText = null;
    private static SplashRenderer hauntedSplash = null;

    private TitleScreenHaunt() {
    }

    public static float intensity() {
        if (!HudConfig.getSettings().enableHallucinations) return 0f;
        double corruption = ClientStateStore.getCorruption();
        if (corruption < 10.0) return 0f;
        return (float) Math.min(1.0, (corruption - 10.0) / 90.0);
    }

    public static SplashRenderer hauntedSplash() {
        decideSplash();
        return hauntedSplash;
    }

    /**
     * The same line as {@link #hauntedSplash}, unwrapped, for the takeover to draw itself.
     * Reading the component rather than re-rolling keeps the two in step.
     */
    public static Component splashText() {
        decideSplash();
        return splashText;
    }

    private static void decideSplash() {
        if (splashDecided) return;
        splashDecided = true;
        float level = intensity();
        if (level > 0 && ClientStateStore.getCorruption() >= 25.0
                && RANDOM.nextDouble() < 0.25 + 0.5 * level) {
            splashText = Component.translatable("title.coi.haunt_splash." + RANDOM.nextInt(HAUNT_SPLASH_LINES));
        } else {
            splashText = Component.translatable("title.coi.splash." + RANDOM.nextInt(LOTM_SPLASH_LINES));
        }
        hauntedSplash = new SplashRenderer(splashText);
    }

    public static void render(GuiGraphicsExtractor ctx, int width, int height) {
        float level = intensity();
        if (level <= 0f) return;

        long now = System.currentTimeMillis();

        EffectPaint.vignette(ctx, width, height, 0x14000A, (int) (40 + 85 * level), 0.18f, 0.22f);

        if (nextEyeAt == 0) {
            nextEyeAt = now + 6_000 + RANDOM.nextInt(10_000);
        }

        if (eyeStartedAt == 0 && now >= nextEyeAt) {
            eyeStartedAt = now;
            eyeHalfW = 40 + RANDOM.nextInt(30);
            int eyeH = eyeHalfW; // eye textures are 2:1
            eyeX = eyeHalfW + 20 + RANDOM.nextInt(Math.max(1, width - (eyeHalfW + 20) * 2));
            eyeY = eyeH / 2 + 20 + RANDOM.nextInt(Math.max(1, height - eyeH - 40));
            eyeRotation = (RANDOM.nextFloat() - 0.5f) * 0.5f;
        }

        if (eyeStartedAt > 0) {
            long elapsed = now - eyeStartedAt;
            if (elapsed >= EYE_DURATION_MS) {
                eyeStartedAt = 0;
                nextEyeAt = now + 10_000 + (long) (RANDOM.nextInt(28_000) * (1.2f - level));
            } else {
                float alpha;
                if (elapsed < EYE_FADE_MS) {
                    alpha = elapsed / (float) EYE_FADE_MS;
                } else if (elapsed > EYE_DURATION_MS - EYE_FADE_MS) {
                    alpha = (EYE_DURATION_MS - elapsed) / (float) EYE_FADE_MS;
                } else {
                    alpha = 1f;
                }
                alpha *= 0.22f + 0.30f * level; // ghostly, never solid
                drawEye(ctx, elapsed, alpha);
            }
        }
    }

    private static void drawEye(GuiGraphicsExtractor ctx, long elapsed, float alpha) {
        if (alpha <= 0f) return;
        Identifier frame = PUPIL_FRAMES[(int) ((elapsed / 400) % PUPIL_FRAMES.length)];
        int w = eyeHalfW * 2;
        int h = eyeHalfW;

        var pose = ctx.pose();
        pose.pushMatrix();
        pose.translate(eyeX, eyeY);
        pose.rotate(eyeRotation);
        int argb = ((int) (alpha * 255) << 24) | 0xFFFFFF;
        ctx.blit(RenderPipelines.GUI_TEXTURED, frame, -w / 2, -h / 2, 0f, 0f, w, h, w, h, argb);
        pose.popMatrix();
    }
}
