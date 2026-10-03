package dev.ua.ikeepcalm.coi.domain.effect.visual;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import org.joml.Matrix3x2fStack;

import java.util.Random;

/**
 * One impact frame as a drawing. Seeded once, so the same impact is the same drawing on every
 * frame it is held for — the only thing that moves between two held frames is the line boil.
 * <p>
 * Everything is polar about a focus the caller passes in per frame, so a moving camera carries
 * the drawing with the world point without the geometry being rebuilt. Sizes are in multiples of
 * {@link #u}, one pen width derived from the screen's short side, which keeps the strokes the
 * same weight at 854×480 and at 4K on gui scale 1. Vanilla's GUI has only rectangles, so a wedge
 * is a run of rotated slices.
 */
final class ImpactFrameDrawing {

    /**
     * Angular slabs the splash and the ring are built from; few enough that the facets show.
     */
    private static final int SECTORS = 18;
    /**
     * Short-side pixels per pen width. 240 is a 480-high window on gui scale 2.
     */
    private static final float PEN_DIVISOR = 240f;
    /**
     * Slice length along a wedge, in pen widths: under the stroke weight, so the steps read as ink, not stairs.
     */
    private static final float SLICE_PEN = 2.5f;
    private static final int MAX_SLICES = 72;
    /**
     * One pen width: the stroke unit, floored at a pixel so a hairline survives a tiny gui.
     */
    final float u;
    /**
     * The silhouette unit, {@code radius / 180} with no floor. Shapes are sized in this rather
     * than in {@link #u} because the floor on {@code u} is right for a hairline and wrong for a
     * shape: on a 320x180 gui a pen width is still a pixel but the drawing is a third the size,
     * so shapes sized in pens come out three times as fat and turn the splash into a blot.
     */
    final float g;
    final float radius;
    private final long seed;
    private final Style style;
    private final float axis;
    private final Spike[] spikes;
    private final float[] splash;
    private final float[] ring;
    private final Line[] lines;
    private final Shard[] shards;
    /**
     * @param axis        the style's axis in screen radians (y down); ignored by {@link Style#BURST}
     * @param inward      true when the focus sits off-screen and the on-screen half of the
     *                    drawing has to carry all of it, so the parts facing the screen grow
     * @param inwardAngle the direction from the focus back toward the screen's centre
     */
    ImpactFrameDrawing(long seed, int w, int h, Style style, float axis, float intensity, float scale, boolean inward, float inwardAngle) {
        this.seed = seed;
        this.style = style;
        this.axis = axis;
        this.u = Math.max(1f, Math.min(w, h) / PEN_DIVISOR);
        this.radius = Math.min(w, h) * (0.40f + 0.34f * intensity) * scale;
        this.g = radius / 180f;
        Random rng = new Random(seed);

        int wedges = switch (style) {
            case BURST -> 9 + Math.round(4 * intensity);
            case SLASH -> 10;
            case PILLAR -> 11;
        };
        int teeth = 10 + Math.round(6 * intensity);
        spikes = new Spike[wedges + teeth];
        for (int i = 0; i < wedges; i++) {
            float angle = spikeAngle(rng, style, axis, i, wedges);
            float length = radius * (0.3f + 0.7f * (float) Math.pow(rng.nextFloat(), 1.6));
            if (rng.nextFloat() < 0.3f) length = radius * (1.05f + 0.6f * rng.nextFloat());
            if (inward) {
                float facing = (float) Math.cos(angle - inwardAngle);
                length *= 1f + 0.6f * Math.max(0f, facing);
            }
            float halfWidth = g * (5f + 13f * rng.nextFloat()) * (0.5f + 0.5f * Math.min(1f, length / radius));
            float concavity = 1.7f + 1.4f * rng.nextFloat();
            float bend = (rng.nextFloat() - 0.5f) * 0.16f;
            spikes[i] = new Spike(angle, length, halfWidth, concavity, bend);
        }
        for (int i = 0; i < teeth; i++) {
            float angle = rng.nextFloat() * (float) (Math.PI * 2);
            float length = radius * (0.07f + 0.13f * rng.nextFloat());
            float halfWidth = g * (5f + 7f * rng.nextFloat());
            spikes[wedges + i] = new Spike(angle, length, halfWidth, 1.0f + 0.5f * rng.nextFloat(), 0f);
        }

        splash = new float[SECTORS];
        ring = new float[SECTORS];
        float splashBase = radius * (style == Style.BURST ? 0.14f : 0.11f);
        for (int i = 0; i < SECTORS; i++) {
            float angle = sectorAngle(i);
            float ragged = 0.55f + 0.7f * rng.nextFloat();
            if (style == Style.PILLAR) {
                ragged *= 1f + 0.5f * Math.abs((float) Math.cos(angle - axis));
            }
            splash[i] = splashBase * ragged;
            ring[i] = splashBase * (0.85f + 0.3f * rng.nextFloat());
        }

        int lineCount = 50 + Math.round(70 * intensity);
        lines = new Line[lineCount];
        for (int i = 0; i < lineCount; i++) {
            float angle;
            if (style != Style.BURST && rng.nextFloat() < 0.6f) {
                angle = axis + (rng.nextBoolean() ? 0f : (float) Math.PI) + gaussian(rng) * 0.5f;
            } else {
                angle = rng.nextFloat() * (float) (Math.PI * 2);
            }
            float endDist = radius * (0.55f + 0.9f * rng.nextFloat());
            if (inward) endDist *= 1f + 0.4f * Math.max(0f, (float) Math.cos(angle - inwardAngle));
            float t = rng.nextFloat();
            lines[i] = new Line(angle, endDist, u * (0.6f + 2.6f * t * t), rng.nextFloat() < 0.22f);
        }

        int shardCount = 8 + Math.round(6 * intensity);
        shards = new Shard[shardCount];
        for (int i = 0; i < shardCount; i++) {
            float angle = style == Style.BURST || rng.nextFloat() < 0.5f
                    ? rng.nextFloat() * (float) (Math.PI * 2)
                    : axis + (rng.nextBoolean() ? 0f : (float) Math.PI) + gaussian(rng) * 0.4f;
            shards[i] = new Shard(angle, (rng.nextFloat() - 0.5f) * 0.6f, radius * (0.5f + 0.7f * rng.nextFloat()),
                    g * (7f + 14f * rng.nextFloat()), g * (2f + 4f * rng.nextFloat()));
        }
    }

    private static float spikeAngle(Random rng, Style style, float axis, int i, int n) {
        float spacing = (float) (Math.PI * 2 / n);
        switch (style) {
            case SLASH -> {
                if (i % 5 == 4) return rng.nextFloat() * (float) (Math.PI * 2);
                return axis + (i % 2 == 0 ? 0f : (float) Math.PI) + gaussian(rng) * 0.3f;
            }
            case PILLAR -> {
                if (i % 4 == 3) return axis + (float) Math.PI + gaussian(rng) * 0.35f;
                return axis + gaussian(rng) * 0.4f;
            }
            default -> {
                return i * spacing + (rng.nextFloat() - 0.5f) * spacing * 0.9f;
            }
        }
    }

    private static float gaussian(Random rng) {
        return (float) Math.clamp(rng.nextGaussian(), -2.2, 2.2);
    }

    private static float sectorAngle(int i) {
        return (float) (i * Math.PI * 2 / SECTORS);
    }

    private static void strokePolygon(GuiGraphicsExtractor ctx, float[] xs, float[] ys, int color, int thickness) {
        int half = Math.max(1, thickness / 2);
        for (int i = 0; i < xs.length; i++) {
            int j = (i + 1) % xs.length;
            EffectPaint.line(ctx, xs[i], ys[i], xs[j], ys[j], color, thickness);
            ctx.fill(Math.round(xs[i]) - half, Math.round(ys[i]) - half, Math.round(xs[i]) + half, Math.round(ys[i]) + half, color);
        }
    }

    static int lerpRgb(int a, int b, float t) {
        int r = Math.round(((a >> 16) & 0xFF) * (1 - t) + ((b >> 16) & 0xFF) * t);
        int g = Math.round(((a >> 8) & 0xFF) * (1 - t) + ((b >> 8) & 0xFF) * t);
        int bl = Math.round((a & 0xFF) * (1 - t) + (b & 0xFF) * t);
        return (r << 16) | (g << 8) | bl;
    }

    /**
     * The splash edge at {@code angle}, which is where every spike starts.
     */
    private float splashAt(float angle) {
        int i = Math.floorMod(Math.round(angle / (float) (Math.PI * 2 / SECTORS)), SECTORS);
        return splash[i];
    }

    /**
     * Deterministic jitter in [-1, 1]. Hashed rather than drawn from the RNG so a frame can be
     * re-rendered any number of times and come out the same.
     */
    private float boil(int i, int frame, int salt) {
        long x = seed ^ (i * 0x9E3779B97F4A7C15L) ^ ((long) frame * 0xC2B2AE3D27D4EB4FL) ^ (salt * 0x165667B19E3779F9L);
        x ^= x >>> 31;
        x *= 0x7FB5D329728EA185L;
        x ^= x >>> 27;
        x *= 0x81DADEF4BC2DD44DL;
        x ^= x >>> 33;
        return ((x >>> 40) / (float) (1 << 24)) * 2f - 1f;
    }

    /**
     * Inversion is the one treatment guaranteed to be high-contrast against <em>any</em> scene.
     */
    void drawNegative(GuiGraphicsExtractor ctx, int w, int h, float fx, float fy, int frame, int accent, int ink) {
        int tint = lerpRgb(0xFFFFFF, accent & 0xFFFFFF, 0.4f);
        ctx.fill(RenderPipelines.GUI_INVERT, 0, 0, w, h, 0xFF000000 | lerpRgb(tint, 0x000000, 0.1f));
        drawCut(ctx, fx, fy, w, h, 1f, 0, ink);
        drawSplash(ctx, fx, fy, frame, 1f, 0f, ink);
        drawSpikes(ctx, fx, fy, frame, 1f, 1f, 0f, 0f, ink);
    }

    void drawPlate(GuiGraphicsExtractor ctx, int w, int h, float fx, float fy, int frame, int ground, int burst, int accent, int groundAlpha) {
        ctx.fill(0, 0, w, h, EffectPaint.argb(ground, groundAlpha));
        drawLines(ctx, fx, fy, w, h, frame, 1f, 1f, 0.75f, burst, 0, accent);
        drawCut(ctx, fx, fy, w, h, 1f, accent, burst);
        drawSplash(ctx, fx, fy, frame, 1f, 1.6f * u, accent);
        drawSpikes(ctx, fx, fy, frame, 1f, 1f, 1.6f * u, 0f, accent);
        drawSplash(ctx, fx, fy, frame, 1f, 0f, burst);
        drawSpikes(ctx, fx, fy, frame, 1f, 1f, 0f, 0f, burst);
    }

    /**
     * Drawn over the live world, always in ink with a chalk rim so the strokes carry their own
     * contrast onto a bright sky or a dark cave.
     *
     * @param q tail progress, already quantised to the frame clock: 0 at the first live frame
     *          and 1 when the drawing is gone
     */
    void drawRelease(GuiGraphicsExtractor ctx, int w, int h, float fx, float fy, int frame, float q, int ink, int chalk, int accent, float intensity) {
        float spikeScale = (float) Math.pow(1f - q, 2.0);
        float widthScale = 0.6f + 0.4f * (1f - q);
        float lift = radius * 0.55f * q * q;
        float lineKeep = 1f - (float) Math.pow(q, 1.5);
        float lineRetreat = 1f + 1.3f * q;

        if (q < 0.6f) {
            EffectPaint.vignette(ctx, w, h, accent & 0xFFFFFF, Math.round(110 * intensity * (1f - q / 0.6f)), 0.32f, 0.34f);
        }

        drawLines(ctx, fx, fy, w, h, frame, lineKeep, lineRetreat, 1f, ink, chalk, accent);
        drawCut(ctx, fx, fy, w, h, 1f - q, chalk, ink);
        if (q < 0.9f) drawRing(ctx, fx, fy, frame, 1.1f + 5.5f * q, 1f - q, accent, chalk, ink);
        if (spikeScale > 0.04f) {
            drawSpikes(ctx, fx, fy, frame, spikeScale, widthScale, 2.6f * u, lift, accent);
            drawSpikes(ctx, fx, fy, frame, spikeScale, widthScale, 1.3f * u, lift, chalk);
            drawSpikes(ctx, fx, fy, frame, spikeScale, widthScale, 0f, lift, ink);
        }
        drawShards(ctx, fx, fy, frame, q, ink, chalk);
    }

    /**
     * @param extra grows every slice by this many pixels, for a rim pass
     * @param lift  moves the base of every spike outward, for the release
     */
    private void drawSpikes(GuiGraphicsExtractor ctx, float fx, float fy, int frame, float lengthScale, float widthScale, float extra, float lift, int color) {
        Matrix3x2fStack pose = ctx.pose();
        for (int i = 0; i < spikes.length; i++) {
            Spike s = spikes[i];
            float length = s.length() * lengthScale * (1f + 0.05f * boil(i, frame, 1)) + extra * 1.5f;
            float halfWidth = s.halfWidth() * widthScale * (1f + 0.08f * boil(i, frame, 2)) + extra;
            // Starts inside the splash so the join shows no seam; the lift is scattered per
            // spike, since one shared distance lines them up on a circle and reads as a crown
            float base = splashAt(s.angle()) * 0.7f + lift * (1f + 0.7f * boil(i, 0, 7)) - extra;
            if (length <= 1f) continue;

            pose.pushMatrix();
            pose.translate(fx, fy);
            pose.rotate(s.angle());
            int slices = Math.clamp(Math.round(length / (SLICE_PEN * u)), 4, MAX_SLICES);
            for (int k = 0; k < slices; k++) {
                float t0 = k / (float) slices;
                float t1 = (k + 1) / (float) slices;
                float tm = (t0 + t1) * 0.5f;
                float hw = halfWidth * (float) Math.pow(1f - tm, s.concavity());
                if (hw < 0.5f) break;
                float off = s.bend() * length * tm * tm;
                int x0 = Math.round(base + t0 * length);
                int x1 = Math.round(base + t1 * length) + 1;
                ctx.fill(x0, Math.round(off - hw), x1, Math.round(off + hw) + 1, color);
            }
            pose.popMatrix();
        }
    }

    private void drawSplash(GuiGraphicsExtractor ctx, float fx, float fy, int frame, float scale, float extra, int color) {
        Matrix3x2fStack pose = ctx.pose();
        float halfAngle = (float) Math.tan(Math.PI / SECTORS) * 1.2f;
        for (int i = 0; i < SECTORS; i++) {
            float r = splash[i] * scale * (1f + 0.07f * boil(i, frame, 3)) + extra;
            pose.pushMatrix();
            pose.translate(fx, fy);
            pose.rotate(sectorAngle(i));
            int hh = Math.round(r * halfAngle) + 1;
            ctx.fill(0, -hh, Math.round(r) + 1, hh, color);
            pose.popMatrix();
        }
    }

    /**
     * Stroked as a polyline rather than built from the splash's slabs: slabs at differing radii
     * leave gaps between sectors, and a gapped ring reads as a loading spinner, not a shockwave.
     */
    private void drawRing(GuiGraphicsExtractor ctx, float fx, float fy, int frame, float grow, float strength, int accent, int chalk, int ink) {
        int band = Math.max(1, Math.round(u * (1.2f + 3.5f * strength)));
        int rim = Math.max(1, Math.round(u * 1.4f));
        float[] xs = new float[SECTORS], ys = new float[SECTORS];
        for (int i = 0; i < SECTORS; i++) {
            float r = ring[i] * grow * (1f + 0.04f * boil(i, frame, 4));
            xs[i] = fx + (float) Math.cos(sectorAngle(i)) * r;
            ys[i] = fy + (float) Math.sin(sectorAngle(i)) * r;
        }
        strokePolygon(ctx, xs, ys, accent, band + 2 * rim);
        strokePolygon(ctx, xs, ys, chalk, band + rim);
        strokePolygon(ctx, xs, ys, ink, band);
    }

    /**
     * @param keep    fraction of the lines still drawn, from the front of the list
     * @param retreat multiplier on where each line ends — above 1 pulls them back out
     * @param rim     0 for no twin; otherwise a hairline of this colour alongside each
     */
    private void drawLines(GuiGraphicsExtractor ctx, float fx, float fy, int w, int h, int frame, float keep, float retreat, float weight, int ink, int rim, int accent) {
        float far = (float) Math.hypot(w, h) + radius;
        int count = Math.round(lines.length * keep);
        int hair = Math.max(1, Math.round(u));
        for (int i = 0; i < count; i++) {
            Line l = lines[i];
            float cos = (float) Math.cos(l.angle());
            float sin = (float) Math.sin(l.angle());
            float end = l.endDist() * retreat * (1f + 0.05f * boil(i, frame, 5));
            float mid = end + (far - end) * 0.45f;
            int thick = Math.max(1, Math.round(l.thickness() * weight));
            int color = l.accent() && accent != 0 ? accent : ink;

            float sx = fx + cos * far, sy = fy + sin * far;
            float mx = fx + cos * mid, my = fy + sin * mid;
            float ex = fx + cos * end, ey = fy + sin * end;
            if (rim != 0) {
                float ox = -sin * (thick + 1), oy = cos * (thick + 1);
                EffectPaint.line(ctx, sx + ox, sy + oy, ex + ox, ey + oy, rim, hair);
            }
            EffectPaint.line(ctx, sx, sy, mx, my, color, thick);
            EffectPaint.line(ctx, mx, my, ex, ey, color, Math.max(1, thick / 2));
        }
    }

    private void drawCut(GuiGraphicsExtractor ctx, float fx, float fy, int w, int h, float strength, int rim, int ink) {
        if (style != Style.SLASH || strength <= 0.05f) return;
        float far = (float) Math.hypot(w, h) + radius;
        float cos = (float) Math.cos(axis), sin = (float) Math.sin(axis);
        int thick = Math.max(1, Math.round(u * (1f + 2.5f * strength)));
        if (rim != 0) {
            EffectPaint.line(ctx, fx - cos * far, fy - sin * far, fx + cos * far, fy + sin * far, rim, thick + Math.round(2 * u));
        }
        EffectPaint.line(ctx, fx - cos * far, fy - sin * far, fx + cos * far, fy + sin * far, ink, thick);
    }

    private void drawShards(GuiGraphicsExtractor ctx, float fx, float fy, int frame, float q, int ink, int chalk) {
        Matrix3x2fStack pose = ctx.pose();
        float travel = 1f - (1f - q) * (1f - q);
        float shrink = (float) Math.pow(1f - q, 0.6);
        for (int i = 0; i < shards.length; i++) {
            Shard s = shards[i];
            float d = splashAt(s.angle()) * 1.4f + s.speed() * travel;
            float length = s.length() * shrink;
            if (length < 2f) continue;
            pose.pushMatrix();
            pose.translate(fx + (float) Math.cos(s.angle()) * d, fy + (float) Math.sin(s.angle()) * d);
            pose.rotate(s.angle() + s.skew());
            lozenge(ctx, length + 1.2f * u, s.halfWidth() * shrink + 1.2f * u, chalk);
            lozenge(ctx, length, s.halfWidth() * shrink, ink);
            pose.popMatrix();
        }
    }

    private void lozenge(GuiGraphicsExtractor ctx, float length, float halfWidth, int color) {
        int slices = Math.clamp(Math.round(length / (SLICE_PEN * u)), 3, 24);
        for (int k = 0; k < slices; k++) {
            float t0 = k / (float) slices, t1 = (k + 1) / (float) slices;
            float hw = halfWidth * (1f - (t0 + t1) * 0.5f);
            if (hw < 0.5f) break;
            ctx.fill(Math.round(t0 * length), Math.round(-hw), Math.round(t1 * length) + 1, Math.round(hw) + 1, color);
            float back = length / 3f;
            ctx.fill(-Math.round(t1 * back) - 1, Math.round(-hw), -Math.round(t0 * back), Math.round(hw) + 1, color);
        }
    }

    enum Style {BURST, SLASH, PILLAR}

    private record Spike(float angle, float length, float halfWidth, float concavity, float bend) {
    }

    /**
     * A concentration line: starts off-screen along {@code angle}, ends {@code endDist} from the focus.
     */
    private record Line(float angle, float endDist, float thickness, boolean accent) {
    }

    /**
     * A shard: a lozenge pointing along its flight, {@code length} long and {@code halfWidth} wide.
     */
    private record Shard(float angle, float skew, float speed, float length, float halfWidth) {
    }
}
