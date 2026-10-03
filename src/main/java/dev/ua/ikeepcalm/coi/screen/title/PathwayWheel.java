package dev.ua.ikeepcalm.coi.screen.title;

import dev.ua.ikeepcalm.coi.domain.ability.model.Pathways;
import dev.ua.ikeepcalm.coi.domain.effect.visual.EffectPaint;
import dev.ua.ikeepcalm.coi.screen.menu.MenuTheme;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Random;

/**
 * Three rules a change here must keep:
 * <ul>
 * <li>The <em>ring</em> turns; the emblems do not. Rotating each emblem with its own angle puts
 * half the wheel upside down, and these are symbols the player is meant to recognise.
 * <li>Emblems are blitted from {@link Pathways#emblemTexture}, not drawn through
 * {@code CoiIcons.drawPathwayEmblem} — that renders a 9px bitmap glyph and is mush at 32px.
 * <li>Their position lives in the <em>pose matrix</em>, not the blit's {@code int} coordinates.
 * At one revolution per four minutes an emblem crosses a tenth of a pixel per frame, so rounding
 * to the blit grid means sitting still for ten frames and then jumping a whole pixel.
 * </ul>
 */
public class PathwayWheel {

    private static final long PERIOD_MS = 240_000L;

    /**
     * Emblem art is 64x64; 32 is the whole-pixel 2:1 reduction the mod draws icons at.
     */
    private static final int EMBLEM_SRC = 64;
    private static final int EMBLEM = 32;
    private static final int EMBLEM_LIT = 40;

    private static final int EMBLEM_GROWTH = 4;

    private static final int REST_ALPHA = 77;

    private static final float INNER_RING = 0.86f;
    private static final int INNER_DASHES = 42;
    private static final float TAU = (float) (Math.PI * 2);

    /** Thirteen laps to the wheel's one, travelling against it — the same sense the dashed inner
     * ring counter-rotates in, so the light reads as driven by the mechanism. */
    private static final long CREST_PERIOD_MS = 18_000L;

    /** Crest reach, in emblem slots: just over one, so one emblem peaks and two are warmed. */
    private static final float CREST_SPREAD = 1.2f;

    private static final float HALO_THRESHOLD = 0.35f;

    /** How near the cursor has to be to move an emblem at all, and how far it moves it. */
    private static final float REPEL_RADIUS = 64f;
    private static final float REPEL_MAX = 16f;

    private static final float REPEL_EASE_MS = 120f;

    private static final float CRACK_THRESHOLD = 0.6f;

    private static final long CORRUPTION_SEED = 0x5E9_1CE_7L;

    private static final long JUDDER_CYCLE_MS = 2600L;
    private static final long JUDDER_MS = 200L;

    /** Eased displacement per ring slot. <em>Render thread only.</em> */
    private static final float[] PUSH_X = new float[Math.max(1, Pathways.RING.size())];
    private static final float[] PUSH_Y = new float[Math.max(1, Pathways.RING.size())];
    /** Used first-N, so raising corruption adds a failing emblem rather than reshuffling them. */
    private static final Flicker[] FLICKERS = flickers();

    static void draw(GuiGraphicsExtractor graphics, TitleTakeover.Geometry geo,
                     int accent, float corruption, int mouseX, int mouseY) {
        List<String> ring = Pathways.RING;
        if (ring.isEmpty()) return;

        float spin = spin(corruption);
        int inner = Math.round(geo.radius() * INNER_RING);

        strokeCircle(graphics, geo.cx(), geo.cy(), geo.radius(), MenuTheme.withAlpha(accent, 0.22f));
        dashedCircle(graphics, geo.cx(), geo.cy(), inner, -spin, MenuTheme.withAlpha(accent, 0.16f));
        if (corruption > CRACK_THRESHOLD) {
            cracks(graphics, geo, accent, corruption);
        }

        String lit = Pathways.normalizePathway(TitleTakeover.litPathway());
        float crest = crest(ring.size());
        for (int i = 0; i < ring.size(); i++) {
            String pathway = ring.get(i);
            // -PI/2 puts the first emblem at the top of the circle
            double angle = spin + i * TAU / ring.size() - Math.PI / 2;
            float x = (float) (geo.cx() + Math.cos(angle) * geo.radius());
            float y = (float) (geo.cy() + Math.sin(angle) * geo.radius());

            float near = repel(i, x, y, mouseX, mouseY);
            float px = x + PUSH_X[i];
            float py = y + PUSH_Y[i];
            int grown = Math.round(near * EMBLEM_GROWTH);

            if (!lit.isEmpty() && lit.equals(pathway)) {
                // the crest skips the lit emblem: already at full strength, so a travelling
                // light could only make a permanent mark blink
                drawLit(graphics, geo, pathway, px, py, angle, EMBLEM_LIT + grown, inner, accent);
                continue;
            }

            float shine = shine(i, ring.size(), crest);
            int rgb = flickerRgb(i, pathway, corruption);
            crestHalo(graphics, px, py, EMBLEM + grown, rgb, shine);
            // One proximity value, two effects: the emblem the cursor pushes is
            // the emblem it brightens. Deciding the brightness by hit-testing
            // where the emblem is *now* would dim it as it slides out from under
            // the cursor it is fleeing, which reads as a bug rather than a rule.
            float glow = Math.max(near, shine);
            drawEmblem(graphics, pathway, px, py, EMBLEM + grown,
                    EffectPaint.argb(rgb, Math.round(REST_ALPHA + (255 - REST_ALPHA) * glow)));
        }
    }

    private PathwayWheel() {
    }

    private static Flicker[] flickers() {
        Random random = new Random(CORRUPTION_SEED);
        int size = Math.max(1, Pathways.RING.size());
        Flicker[] out = new Flicker[3];
        for (int i = 0; i < out.length; i++) {
            out[i] = new Flicker(random.nextInt(size), 3400L + random.nextInt(5200), random.nextInt(4000));
        }
        return out;
    }

    /** A pure function of the clock and the ring's size — no state, nothing to pop when it
     * changes target, nothing to decide if the ring changes size. */
    private static float crest(int size) {
        float phase = (TitleTakeover.now() % CREST_PERIOD_MS) / (float) CREST_PERIOD_MS;
        return (1f - phase) * size;
    }

    /** How lit the crest has this slot, 0..1, measured the shorter way round the ring. */
    private static float shine(int index, int size, float crest) {
        float gap = Math.abs(index - crest);
        gap = Math.min(gap, size - gap);
        return smoothstep(1f - gap / CREST_SPREAD);
    }

    /**
     * {@code (x, y)} is the emblem's <em>undisturbed</em> centre, never where it has already been
     * pushed to: a displacement fed back into its own input oscillates — pushed further away, so
     * pushed less, so it returns. The easing is the only memory involved. Returns nearness, 0..1.
     */
    private static float repel(int index, float x, float y, int mouseX, int mouseY) {
        float dx = x - mouseX;
        float dy = y - mouseY;
        float distance = (float) Math.sqrt(dx * dx + dy * dy);
        float near = smoothstep(1f - distance / REPEL_RADIUS);
        // max(distance, 1) also covers the cursor sitting exactly on a centre, where "away"
        // names no direction
        float reach = near * REPEL_MAX / Math.max(distance, 1f);
        PUSH_X[index] = TitleTakeover.approach(PUSH_X[index], dx * reach, REPEL_EASE_MS);
        PUSH_Y[index] = TitleTakeover.approach(PUSH_Y[index], dy * reach, REPEL_EASE_MS);
        return near;
    }

    /** Hermite ease, clamped. A linear ramp leaves a visible crease where it reaches zero. */
    private static float smoothstep(float t) {
        if (t <= 0f) return 0f;
        if (t >= 1f) return 1f;
        return t * t * (3f - 2f * t);
    }

    /** Corruption judders intermittently rather than wobbling constantly — a constant tremor
     * reads as a bad easing curve, not as something wrong. */
    private static float spin(float corruption) {
        long now = TitleTakeover.now();
        float base = (now % PERIOD_MS) / (float) PERIOD_MS * TAU;
        if (corruption <= 0f || TitleTakeover.steady()) return base;

        long window = now % JUDDER_CYCLE_MS;
        if (window > JUDDER_MS) return base;
        float through = window / (float) JUDDER_MS;
        return base + (float) Math.sin(through * Math.PI * 3) * 0.05f * corruption;
    }

    /** Blitted at the origin, with the real, fractional position in the pose. */
    private static void drawEmblem(GuiGraphicsExtractor graphics, String pathway,
                                   float cx, float cy, int size, int argb) {
        Identifier texture = Pathways.emblemTexture(pathway);
        if (texture == null) return;
        var pose = graphics.pose();
        pose.pushMatrix();
        pose.translate(cx - size / 2f, cy - size / 2f);
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, 0, 0,
                0f, 0f, size, size, EMBLEM_SRC, EMBLEM_SRC, EMBLEM_SRC, EMBLEM_SRC, argb);
        pose.popMatrix();
    }

    private static int flickerRgb(int index, String pathway, float corruption) {
        int own = Pathways.pathwayRgb(pathway);
        if (corruption <= 0f || TitleTakeover.steady()) return own;

        int count = 1 + Math.round(corruption * 2f);
        for (int i = 0; i < Math.min(count, FLICKERS.length); i++) {
            Flicker flicker = FLICKERS[i];
            if (flicker.index() != index) continue;
            float phase = ((TitleTakeover.now() + flicker.offset()) % flicker.period())
                    / (float) flicker.period();
            float bite = Math.max(0f, (float) Math.sin(phase * TAU)) * corruption;
            return MenuTheme.lerpArgb(0xFF000000 | own,
                    0xFF000000 | Pathways.pathwayRgb("error"), bite) & 0xFFFFFF;
        }
        return own;
    }

    private static void crestHalo(GuiGraphicsExtractor graphics, float x, float y,
                                  int size, int rgb, float shine) {
        if (shine <= HALO_THRESHOLD) return;
        float bite = (shine - HALO_THRESHOLD) / (1f - HALO_THRESHOLD);
        strokeCircle(graphics, x, y, size / 2 + 4, EffectPaint.argb(rgb, Math.round(34 * bite)));
        strokeCircle(graphics, x, y, size / 2 + 9, EffectPaint.argb(rgb, Math.round(15 * bite)));
    }

    private static void drawLit(GuiGraphicsExtractor graphics, TitleTakeover.Geometry geo,
                                String pathway, float x, float y, double angle,
                                int size, int inner, int accent) {
        int rgb = Pathways.pathwayRgb(pathway);
        for (int step = 4; step >= 1; step--) {
            strokeCircle(graphics, x, y, size / 2 + step * 4,
                    EffectPaint.argb(rgb, 10 + (5 - step) * 6));
        }
        EffectPaint.line(graphics,
                (float) (x - Math.cos(angle) * (size / 2f + 3)),
                (float) (y - Math.sin(angle) * (size / 2f + 3)),
                (float) (geo.cx() + Math.cos(angle) * (inner + 2)),
                (float) (geo.cy() + Math.sin(angle) * (inner + 2)),
                MenuTheme.withAlpha(accent, 0.55f), 1);
        drawEmblem(graphics, pathway, x, y, size, EffectPaint.argb(rgb, 255));
    }

    /** Centre is {@code float} so a halo sits on the same fractional point its emblem does;
     * rounding leaves the rings crawling against the symbol they belong to. */
    private static void strokeCircle(GuiGraphicsExtractor graphics, float cx, float cy, int radius, int color) {
        if (radius < 2) return;
        int segments = Math.clamp(radius, 24, 96);
        float previousX = cx + radius;
        float previousY = cy;
        for (int i = 1; i <= segments; i++) {
            double angle = i * TAU / segments;
            float x = (float) (cx + Math.cos(angle) * radius);
            float y = (float) (cy + Math.sin(angle) * radius);
            EffectPaint.line(graphics, previousX, previousY, x, y, color, 1);
            previousX = x;
            previousY = y;
        }
    }

    /** Chords, not radii — a crack goes through a thing rather than radiating from its middle. */
    private static void cracks(GuiGraphicsExtractor graphics, TitleTakeover.Geometry geo,
                               int accent, float corruption) {
        float bite = (corruption - CRACK_THRESHOLD) / (1f - CRACK_THRESHOLD);
        int count = 1 + Math.round(bite * 3f);
        Random random = new Random(CORRUPTION_SEED * 31L);
        int color = MenuTheme.withAlpha(MenuTheme.shade(accent, -0.35f), 0.25f + 0.35f * bite);
        for (int i = 0; i < count; i++) {
            double from = random.nextDouble() * TAU;
            double to = from + 0.8 + random.nextDouble() * 1.6;
            EffectPaint.line(graphics,
                    (float) (geo.cx() + Math.cos(from) * geo.radius()),
                    (float) (geo.cy() + Math.sin(from) * geo.radius()),
                    (float) (geo.cx() + Math.cos(to) * geo.radius()),
                    (float) (geo.cy() + Math.sin(to) * geo.radius()),
                    color, 1);
        }
    }

    private static void dashedCircle(GuiGraphicsExtractor graphics, int cx, int cy,
                                     int radius, float rotation, int color) {
        if (radius < 2) return;
        float step = TAU / INNER_DASHES;
        for (int i = 0; i < INNER_DASHES; i++) {
            double from = rotation + i * step;
            double to = from + step * 0.45;
            EffectPaint.line(graphics,
                    (float) (cx + Math.cos(from) * radius), (float) (cy + Math.sin(from) * radius),
                    (float) (cx + Math.cos(to) * radius), (float) (cy + Math.sin(to) * radius),
                    color, 1);
        }
    }

    /** Chosen once, not per frame, so the same emblems keep failing instead of the whole wheel
     * shimmering. {@code offset} staggers them so the cycles never agree. */
    private record Flicker(int index, long period, long offset) {
    }
}
