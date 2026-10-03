package dev.ua.ikeepcalm.coi.domain.effect.visual;

import dev.ua.ikeepcalm.coi.config.HudConfig;
import dev.ua.ikeepcalm.coi.domain.ceremony.CameraCinematics.ScreenShakeState;
import dev.ua.ikeepcalm.coi.domain.effect.VisualEffect;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3fc;

/**
 * The {@code impact_frame} effect — not the {@code impact} spell VFX
 * ({@link SpellImpactEffect}), which plays <em>at</em> a position in the world.
 * <p>
 * Nothing tweens: it runs on a 24 fps clock and every stage is a whole number of frames, which
 * is what makes a viewer believe somebody drew it rather than faded it.
 * <p>
 * Params ({@code key=value}, all optional): {@code x,y,z} (world anchor), {@code intensity}
 * (0–1), {@code accent} (hex RGB), {@code tone} ({@code dark}/{@code light}), {@code style}
 * ({@code burst}, {@code slash}, {@code pillar}), {@code angle} (degrees, 0 = right, 90 = up),
 * {@code duration} (ms), {@code scale}, {@code shake} (0–1), {@code seed}.
 */
public class ImpactFrameEffect implements VisualEffect {

    public static final String ID = "impact_frame";

    /**
     * 24 frames a second.
     */
    private static final long FRAME_MS = 42;
    /**
     * How far past the edge an off-screen focus sits, as a fraction of the short side.
     */
    private static final float EDGE_OVERSHOOT = 0.12f;
    private static final float HIGH_INTENSITY = 0.7f;
    private static final float LOW_INTENSITY = 0.45f;
    private static final int INK = 0xFF0A0A0A;
    private static final int CHALK = 0xFFF6F3EC;

    private Vec3 anchor;
    private ImpactFrameDrawing.Style style = ImpactFrameDrawing.Style.BURST;
    private float intensity = 0.85f;
    private int accent = 0xFF7A22;
    private boolean lightTone = false;
    private float angleDeg = Float.NaN;
    private long duration = 600;
    private float scale = 1f;
    private float shake = -1f;
    private long seed = System.nanoTime();

    private boolean reduced;
    private int negativeFrames;
    private int plateFrames;
    private int tailFrames;
    private long startTime;

    private ImpactFrameDrawing drawing;
    private int drawnW, drawnH;
    private boolean drawnInward;

    /**
     * {@code {x, y, offScreen, inwardAngle}} in gui pixels. The world anchor is projected by
     * hand off the camera's basis vectors and FOV, <b>not</b> through
     * {@code Camera.getViewRotationProjectionMatrix}: that matrix is cached behind a dirty flag
     * and is stale by the time the HUD draws — the anchor snapped to the screen centre the
     * moment the camera jolt stopped dirtying it every frame.
     * <p>
     * Dividing by {@code |depth|} rather than {@code depth} keeps the direction honest for a
     * point behind the camera, which a signed divide mirrors onto the wrong side of the screen;
     * such a point is off-screen by definition, so only its direction is used.
     */
    static float[] focus(Vec3 anchor, int w, int h) {
        float cx = w / 2f, cy = h / 2f;
        if (anchor == null) return new float[]{cx, cy, 0f, 0f};

        Camera camera = Minecraft.getInstance().gameRenderer.mainCamera();
        if (camera == null || !camera.isInitialized() || camera.getFov() <= 0f) return new float[]{cx, cy, 0f, 0f};

        Vec3 rel = anchor.subtract(camera.position());
        Vector3fc forward = camera.forwardVector();
        Vector3fc up = camera.upVector();
        Vector3fc left = camera.leftVector();
        float depth = (float) (rel.x * forward.x() + rel.y * forward.y() + rel.z * forward.z());
        float side = -(float) (rel.x * left.x() + rel.y * left.y() + rel.z * left.z());
        float rise = (float) (rel.x * up.x() + rel.y * up.y() + rel.z * up.z());
        float absDepth = Math.abs(depth);
        if (absDepth < 1e-4f && Math.abs(side) + Math.abs(rise) < 1e-4f) return new float[]{cx, cy, 0f, 0f};

        float tanY = (float) Math.tan(Math.toRadians(camera.getFov()) / 2.0);
        float tanX = tanY * (w / (float) h);
        boolean behind = depth <= 0f;
        float nx = side / Math.max(absDepth, 1e-4f) / tanX;
        float ny = rise / Math.max(absDepth, 1e-4f) / tanY;
        float sx = (nx + 1f) * 0.5f * w;
        float sy = (1f - ny) * 0.5f * h;
        if (!behind && sx >= 0 && sx <= w && sy >= 0 && sy <= h) {
            return new float[]{sx, sy, 0f, 0f};
        }

        float dx = sx - cx, dy = sy - cy;
        float len = (float) Math.hypot(dx, dy);
        if (behind && len < Math.min(w, h) * 0.25f) {
            // Dead behind the camera the direction is noise; come up from the bottom instead
            dx = 0f;
            dy = 1f;
            len = 1f;
        } else if (len < 1e-3f) {
            return new float[]{cx, cy, 0f, 0f};
        }
        dx /= len;
        dy /= len;
        float margin = Math.min(w, h) * EDGE_OVERSHOOT;
        float tx = dx == 0f ? Float.MAX_VALUE : (cx + margin) / Math.abs(dx);
        float ty = dy == 0f ? Float.MAX_VALUE : (cy + margin) / Math.abs(dy);
        float t = Math.min(tx, ty);
        float inward = (float) Math.atan2(-dy, -dx);
        return new float[]{cx + dx * t, cy + dy * t, 1f, inward};
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getDisplayName() {
        return "Impact Frame";
    }

    @Override
    public String getDefaultParams() {
        return "style=burst,intensity=0.85,accent=FF7A22,tone=dark,duration=600";
    }

    @Override
    public void start(String params) {
        parse(params);
        startTime = System.currentTimeMillis();

        // Epilepsy mode drops the negative and the plate — exactly the parts that are a flash —
        // and holds the drawing over the live world instead. That is why this effect is not in
        // EffectManager.PHOTOSENSITIVE_EFFECTS.
        reduced = HudConfig.getSettings().epilepsyMode;
        negativeFrames = !reduced && intensity >= LOW_INTENSITY ? 1 : 0;
        plateFrames = reduced ? 3 : (intensity >= LOW_INTENSITY ? 2 : 1) + (intensity >= HIGH_INTENSITY ? 1 : 0);
        tailFrames = (int) Math.max(4, (duration - (negativeFrames + plateFrames) * FRAME_MS) / FRAME_MS);

        // ScreenShakeState is one static slot: a rite's long rumble must not be swapped for a
        // quarter-second one and then stop, so a shake already running is left alone.
        float jolt = shake < 0f ? 0.55f * intensity : shake;
        if (jolt > 0f && !ScreenShakeState.active()) {
            ScreenShakeState.set(jolt, 13f, 260 + Math.round(220 * intensity));
        }
    }

    @Override
    public void render(GuiGraphicsExtractor ctx, int w, int h, float tickDelta) {
        int frame = (int) ((System.currentTimeMillis() - startTime) / FRAME_MS);
        if (frame >= negativeFrames + plateFrames + tailFrames) return;

        float[] focus = focus(anchor, w, h);
        boolean inward = focus[2] != 0f;
        if (drawing == null || drawnW != w || drawnH != h || drawnInward != inward) {
            float axis = axisRadians();
            drawing = new ImpactFrameDrawing(seed, w, h, style, axis, intensity, scale, inward, focus[3]);
            drawnW = w;
            drawnH = h;
            drawnInward = inward;
        }

        int ground = lightTone ? CHALK : INK;
        int burst = lightTone ? INK : CHALK;
        float fx = focus[0], fy = focus[1];

        if (frame < negativeFrames) {
            drawing.drawNegative(ctx, w, h, fx, fy, frame, accent, INK);
            return;
        }
        int plateFrame = frame - negativeFrames;
        if (plateFrame < plateFrames) {
            if (reduced) {
                drawing.drawRelease(ctx, w, h, fx, fy, frame, 0f, INK, CHALK, accentArgb(), intensity);
                return;
            }
            boolean flip = intensity >= HIGH_INTENSITY && plateFrame == plateFrames - 1;
            int plateGround = flip ? burst : ground;
            int plateBurst = flip ? ground : burst;
            int alpha = Math.round(255 * Math.min(1f, 0.5f + intensity * 0.6f));
            drawing.drawPlate(ctx, w, h, fx, fy, frame, plateGround & 0xFFFFFF, plateBurst, accentArgb(), alpha);
            return;
        }
        int tailFrame = plateFrame - plateFrames;
        float q = (tailFrame + 1) / (float) (tailFrames + 1);
        drawing.drawRelease(ctx, w, h, fx, fy, frame, q, INK, CHALK, accentArgb(), intensity);
    }

    @Override
    public boolean isFinished() {
        return System.currentTimeMillis() - startTime > (negativeFrames + plateFrames + tailFrames) * FRAME_MS;
    }

    @Override
    public void stop() {
        drawing = null;
    }

    private int accentArgb() {
        return 0xFF000000 | accent;
    }

    /**
     * User-facing degrees (0 = right, 90 = up) to screen radians (y down).
     */
    private float axisRadians() {
        float deg = angleDeg;
        if (Float.isNaN(deg)) {
            deg = switch (style) {
                case SLASH -> 35f;
                case PILLAR -> 90f;
                case BURST -> 0f;
            };
        }
        return (float) -Math.toRadians(deg);
    }

    private void parse(String params) {
        double[] pos = new double[3];
        boolean[] has = new boolean[3];
        EffectParams.forEach(params, (key, value) -> {
            try {
                switch (key) {
                    case "x" -> {
                        pos[0] = Double.parseDouble(value);
                        has[0] = true;
                    }
                    case "y" -> {
                        pos[1] = Double.parseDouble(value);
                        has[1] = true;
                    }
                    case "z" -> {
                        pos[2] = Double.parseDouble(value);
                        has[2] = true;
                    }
                    case "intensity" -> intensity = EffectPaint.clamp(Float.parseFloat(value), 0f, 1f);
                    case "accent" -> accent = Integer.parseInt(value, 16) & 0xFFFFFF;
                    case "tone" -> lightTone = "light".equalsIgnoreCase(value);
                    case "style" -> style = switch (value.toLowerCase()) {
                        case "slash" -> ImpactFrameDrawing.Style.SLASH;
                        case "pillar" -> ImpactFrameDrawing.Style.PILLAR;
                        default -> ImpactFrameDrawing.Style.BURST;
                    };
                    case "angle" -> angleDeg = Float.parseFloat(value);
                    case "duration" -> duration = Math.clamp(Long.parseLong(value), 200, 4000);
                    case "scale" -> scale = EffectPaint.clamp(Float.parseFloat(value), 0.3f, 3f);
                    case "shake" -> shake = EffectPaint.clamp(Float.parseFloat(value), 0f, 1f);
                    case "seed" -> seed = Long.parseLong(value);
                }
            } catch (NumberFormatException ignored) {
            }
        });
        if (has[0] && has[1] && has[2]) anchor = new Vec3(pos[0], pos[1], pos[2]);
    }
}
