package dev.ua.ikeepcalm.coi.client.effects.impl;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.ua.ikeepcalm.coi.client.effects.VisualEffect;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class ImpactFrameEffect implements VisualEffect {

    public static final String ID = "impact";
    private static final Identifier WHITE_TEXTURE = Identifier.fromNamespaceAndPath("coi-client", "textures/entity/white.png");
    private static final int FULL_BRIGHT = 0x00F000F0;

    private static final List<WorldImpact> ACTIVE_IMPACTS = new ArrayList<>();
    private static boolean initialized = false;

    private ImpactParams activeParams;
    private long screenStartTime;

    public static void initializeWorldRenderer() {
        if (initialized) return;
        initialized = true;

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (ACTIVE_IMPACTS.isEmpty()) return;

            Iterator<WorldImpact> it = ACTIVE_IMPACTS.iterator();
            while (it.hasNext()) {
                WorldImpact impact = it.next();
                impact.tick();
                if (impact.isFinished()) {
                    it.remove();
                }
            }
        });

        LevelRenderEvents.COLLECT_SUBMITS.register(context -> {
            if (ACTIVE_IMPACTS.isEmpty()) return;

            for (WorldImpact impact : new ArrayList<>(ACTIVE_IMPACTS)) {
                PoseStack poseStack = context.poseStack();
                Vec3 cameraPos = context.levelState().cameraRenderState.pos;
                poseStack.pushPose();
                poseStack.translate(
                        impact.position.x - cameraPos.x,
                        impact.position.y - cameraPos.y,
                        impact.position.z - cameraPos.z
                );
                SubmitNodeCollector collector = context.submitNodeCollector();
                collector.order(900).submitCustomGeometry(
                        poseStack,
                        RenderTypes.entityTranslucentEmissive(WHITE_TEXTURE),
                        (pose, consumer) -> impact.render(pose, consumer)
                );
                poseStack.popPose();
            }
        });
    }

    public static void clearWorldImpacts() {
        ACTIVE_IMPACTS.clear();
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
        return "style=burst,scope=both,color=FFFFFF,accent=FF2200,intensity=0.85,radius=3.0,duration=320,frames=3";
    }

    @Override
    public void start(String params) {
        ImpactParams parsed = ImpactParams.parse(params);
        if (parsed.world) {
            parsed.resolveMissingPosition();
            if (parsed.position != null) {
                ACTIVE_IMPACTS.add(new WorldImpact(parsed));
            } else {
                System.out.println("COI Effects: Impact frame has no world position; rendering screen scope only");
            }
        }

        activeParams = parsed.screen ? parsed : null;
        screenStartTime = System.currentTimeMillis();
    }

    @Override
    public void render(GuiGraphicsExtractor ctx, int w, int h, float tickDelta) {
        if (activeParams == null) return;

        long elapsed = System.currentTimeMillis() - screenStartTime;
        float progress = Math.min(1f, elapsed / (float) activeParams.duration);
        int frame = Math.min(activeParams.frames - 1, (int) (progress * activeParams.frames));
        float framePulse = 1f - Math.min(1f, (elapsed % Math.max(1, activeParams.duration / activeParams.frames)) / (float) Math.max(1, activeParams.duration / activeParams.frames));
        float fade = Math.max(1f - easeOutCubic(progress), framePulse * 0.28f);

        switch (activeParams.style) {
            case "slash" -> renderSlashFrame(ctx, w, h, activeParams, frame, fade);
            case "void" -> renderVoidFrame(ctx, w, h, activeParams, frame, fade);
            case "holy" -> renderHolyFrame(ctx, w, h, activeParams, frame, fade);
            case "pierce" -> renderPierceFrame(ctx, w, h, activeParams, frame, fade);
            case "crush" -> renderCrushFrame(ctx, w, h, activeParams, frame, fade);
            case "ripple" -> renderRippleFrame(ctx, w, h, activeParams, frame, fade);
            case "fracture" -> renderFractureFrame(ctx, w, h, activeParams, frame, fade);
            case "blood" -> renderBloodFrame(ctx, w, h, activeParams, frame, fade);
            case "frost" -> renderFrostFrame(ctx, w, h, activeParams, frame, fade);
            default -> renderBurstFrame(ctx, w, h, activeParams, frame, fade);
        }
    }

    @Override
    public boolean isFinished() {
        return activeParams == null || System.currentTimeMillis() - screenStartTime > activeParams.duration;
    }

    @Override
    public void stop() {
        activeParams = null;
        clearWorldImpacts();
    }

    private static class WorldImpact {
        private final Vec3 position;
        private final int color;
        private final int accent;
        private final String style;
        private final float intensity;
        private final float radius;
        private final long duration;
        private final int frames;
        private final long startTime;
        private final List<LinePlane> lines = new ArrayList<>();
        private int ageTicks;

        private WorldImpact(ImpactParams params) {
            this.position = params.position;
            this.color = params.color;
            this.accent = params.accent;
            this.style = params.style;
            this.intensity = params.intensity;
            this.radius = params.radius;
            this.duration = params.duration;
            this.frames = params.frames;
            this.startTime = System.currentTimeMillis();
            generateLines(params.seed);
        }

        private void tick() {
            ageTicks++;
        }

        private boolean isFinished() {
            return System.currentTimeMillis() - startTime > duration;
        }

        private void render(PoseStack.Pose pose, VertexConsumer consumer) {
            float progress = Math.min(1f, (System.currentTimeMillis() - startTime) / (float) duration);
            float fade = 1f - easeOutCubic(progress);
            int frame = Math.min(frames - 1, (int) (progress * frames));
            int main = frame % 2 == 0 ? color : 0x060606;
            int alt = frame % 2 == 0 ? 0x050505 : accent;

            float plateAlpha = (0.50f + 0.32f * intensity) * fade;
            float ringAlpha = (0.72f + 0.20f * intensity) * fade;
            float lineAlpha = (0.82f + 0.15f * intensity) * fade;

            if (!"slash".equals(style) && !"pierce".equals(style) && !"crush".equals(style)) {
                drawBillboardQuad(pose, consumer, radius * (2.0f + progress * 1.3f), main, plateAlpha);
            }
            if ("void".equals(style) || "fracture".equals(style)) {
                drawDiamond(pose, consumer, radius * (1.25f + progress * 2.2f), 0x050505, ringAlpha);
            } else {
                drawDiamond(pose, consumer, radius * (0.85f + progress * 1.8f), alt, ringAlpha);
            }
            if (!"slash".equals(style) && !"pierce".equals(style)) {
                drawRing(pose, consumer, radius * (0.35f + progress * 2.0f), radius * 0.055f, accent, ringAlpha);
            }
            if (!"holy".equals(style) && !"ripple".equals(style) && !"frost".equals(style)) {
                drawSlashes(pose, consumer, radius, main, ringAlpha);
            }

            for (LinePlane line : lines) {
                float travel = radius * (0.35f + progress * 1.55f);
                drawLinePlane(pose, consumer, line, travel, line.frameParity == frame % 2 ? alt : main, lineAlpha);
            }
        }

        private void generateLines(long seed) {
            Random rng = new Random(seed);
            int count = Math.max(18, (int) (34 * intensity));
            for (int i = 0; i < count; i++) {
                double angle = Math.PI * 2 * i / count + rng.nextDouble() * 0.42;
                float y = (rng.nextFloat() - 0.5f) * radius * 1.15f;
                float length = radius * (0.55f + rng.nextFloat() * 0.75f);
                float width = radius * (0.035f + rng.nextFloat() * 0.055f);
                lines.add(new LinePlane((float) Math.cos(angle), y, (float) Math.sin(angle), length, width, i % 2));
            }
        }
    }

    private record LinePlane(float dirX, float y, float dirZ, float length, float width, int frameParity) {
    }

    private static void drawBillboardQuad(PoseStack.Pose pose, VertexConsumer consumer, float size, int color, float alpha) {
        PoseStack stack = new PoseStack();
        stack.mulPose(pose.pose());
        faceCamera(stack);
        PoseStack.Pose billboardPose = stack.last();
        float half = size * 0.5f;
        addVertex(billboardPose, consumer, -half, -half, 0, color, alpha, 0, 0, 0, 0, 1);
        addVertex(billboardPose, consumer, half, -half, 0, color, alpha, 1, 0, 0, 0, 1);
        addVertex(billboardPose, consumer, half, half, 0, color, alpha, 1, 1, 0, 0, 1);
        addVertex(billboardPose, consumer, -half, half, 0, color, alpha, 0, 1, 0, 0, 1);
    }

    private static void drawDiamond(PoseStack.Pose pose, VertexConsumer consumer, float size, int color, float alpha) {
        PoseStack stack = new PoseStack();
        stack.mulPose(pose.pose());
        faceCamera(stack);
        stack.mulPose(Axis.ZP.rotationDegrees(45.0f));
        PoseStack.Pose diamondPose = stack.last();
        float half = size * 0.5f;
        addVertex(diamondPose, consumer, -half, -half, 0.01f, color, alpha, 0, 0, 0, 0, 1);
        addVertex(diamondPose, consumer, half, -half, 0.01f, color, alpha, 1, 0, 0, 0, 1);
        addVertex(diamondPose, consumer, half, half, 0.01f, color, alpha, 1, 1, 0, 0, 1);
        addVertex(diamondPose, consumer, -half, half, 0.01f, color, alpha, 0, 1, 0, 0, 1);
    }

    private static void drawRing(PoseStack.Pose pose, VertexConsumer consumer, float radius, float thickness, int color, float alpha) {
        int segments = 40;
        for (int i = 0; i < segments; i++) {
            double a1 = Math.PI * 2 * i / segments;
            double a2 = Math.PI * 2 * (i + 1) / segments;
            float ix1 = (float) Math.cos(a1) * (radius - thickness);
            float iz1 = (float) Math.sin(a1) * (radius - thickness);
            float ox1 = (float) Math.cos(a1) * (radius + thickness);
            float oz1 = (float) Math.sin(a1) * (radius + thickness);
            float ix2 = (float) Math.cos(a2) * (radius - thickness);
            float iz2 = (float) Math.sin(a2) * (radius - thickness);
            float ox2 = (float) Math.cos(a2) * (radius + thickness);
            float oz2 = (float) Math.sin(a2) * (radius + thickness);

            addVertex(pose, consumer, ix1, 0, iz1, color, alpha, 0, 0, 0, 1, 0);
            addVertex(pose, consumer, ox1, 0, oz1, color, alpha, 1, 0, 0, 1, 0);
            addVertex(pose, consumer, ox2, 0, oz2, color, alpha, 1, 1, 0, 1, 0);
            addVertex(pose, consumer, ix2, 0, iz2, color, alpha, 0, 1, 0, 1, 0);
        }
    }

    private static void drawSlashes(PoseStack.Pose pose, VertexConsumer consumer, float radius, int color, float alpha) {
        drawSlash(pose, consumer, -25.0f, radius * 1.35f, radius * 0.16f, color, alpha);
        drawSlash(pose, consumer, 18.0f, radius * 1.05f, radius * 0.11f, color, alpha * 0.75f);
    }

    private static void drawSlash(PoseStack.Pose pose, VertexConsumer consumer, float degrees, float length, float width, int color, float alpha) {
        PoseStack stack = new PoseStack();
        stack.mulPose(pose.pose());
        faceCamera(stack);
        stack.mulPose(Axis.ZP.rotationDegrees(degrees));
        PoseStack.Pose slashPose = stack.last();
        addVertex(slashPose, consumer, -length, -width, 0.03f, color, alpha, 0, 0, 0, 0, 1);
        addVertex(slashPose, consumer, length, -width, 0.03f, color, alpha, 1, 0, 0, 0, 1);
        addVertex(slashPose, consumer, length, width, 0.03f, color, alpha, 1, 1, 0, 0, 1);
        addVertex(slashPose, consumer, -length, width, 0.03f, color, alpha, 0, 1, 0, 0, 1);
    }

    private static void drawLinePlane(PoseStack.Pose pose, VertexConsumer consumer, LinePlane line, float travel, int color, float alpha) {
        float startX = line.dirX * travel;
        float startZ = line.dirZ * travel;
        float endX = line.dirX * (travel + line.length);
        float endZ = line.dirZ * (travel + line.length);
        float sideX = -line.dirZ * line.width;
        float sideZ = line.dirX * line.width;

        addVertex(pose, consumer, startX - sideX, line.y, startZ - sideZ, color, alpha, 0, 0, 0, 1, 0);
        addVertex(pose, consumer, endX - sideX, line.y, endZ - sideZ, color, alpha, 1, 0, 0, 1, 0);
        addVertex(pose, consumer, endX + sideX, line.y, endZ + sideZ, color, alpha, 1, 1, 0, 1, 0);
        addVertex(pose, consumer, startX + sideX, line.y, startZ + sideZ, color, alpha, 0, 1, 0, 1, 0);
    }

    private static void renderBurstFrame(GuiGraphicsExtractor ctx, int w, int h, ImpactParams params, int frame, float fade) {
        int main = frame % 2 == 0 ? params.color : 0x050505;
        int accent = frame % 2 == 0 ? 0x050505 : params.accent;
        ctx.fill(0, 0, w, h, argb(main, (int) (210 * params.intensity * fade)));
        drawLetterbox(ctx, w, h, (int) (52 * params.intensity * fade));

        int cx = w / 2;
        int cy = h / 2;
        int rays = Math.max(16, (int) (34 * params.intensity));
        for (int i = 0; i < rays; i++) {
            float angle = (float) (Math.PI * 2 * i / rays);
            int len = (int) (Math.max(w, h) * (0.35f + 0.32f * ((i % 5) / 4f)));
            int thick = 2 + (i % 3);
            drawScreenLine(ctx, cx, cy, angle, len, thick, argb(accent, (int) (220 * fade)));
        }

        drawScreenLine(ctx, cx, cy, -0.28f, (int) (w * 0.34f), Math.max(8, h / 45), argb(params.accent, (int) (235 * fade)));
        drawScreenLine(ctx, cx, cy, 0.22f, (int) (w * 0.22f), Math.max(5, h / 70), argb(params.color, (int) (200 * fade)));
    }

    private static void renderSlashFrame(GuiGraphicsExtractor ctx, int w, int h, ImpactParams params, int frame, float fade) {
        ctx.fill(0, 0, w, h, argb(frame % 2 == 0 ? 0x070707 : params.color, (int) (190 * params.intensity * fade)));
        drawLetterbox(ctx, w, h, (int) (130 * fade));
        int slashColor = argb(frame % 2 == 0 ? params.color : params.accent, (int) (255 * fade));
        drawScreenLine(ctx, w / 2, h / 2, -0.55f, (int) (w * 0.70f), Math.max(14, h / 22), slashColor);
        drawScreenLine(ctx, w / 2, h / 2 + h / 14, -0.55f, (int) (w * 0.48f), Math.max(6, h / 48), argb(0x050505, (int) (230 * fade)));
        drawScreenLine(ctx, w / 2, h / 2 - h / 10, 0.35f, (int) (w * 0.32f), Math.max(5, h / 60), argb(params.accent, (int) (190 * fade)));
    }

    private static void renderVoidFrame(GuiGraphicsExtractor ctx, int w, int h, ImpactParams params, int frame, float fade) {
        ctx.fill(0, 0, w, h, argb(0x000000, (int) (235 * params.intensity * fade)));
        int cx = w / 2;
        int cy = h / 2;
        int ringColor = argb(frame % 2 == 0 ? params.accent : params.color, (int) (220 * fade));
        int rings = 4;
        for (int i = 0; i < rings; i++) {
            int rw = (int) (w * (0.10f + i * 0.085f + (1f - fade) * 0.12f));
            int rh = (int) (h * (0.08f + i * 0.060f + (1f - fade) * 0.08f));
            drawScreenRectOutline(ctx, cx - rw, cy - rh, cx + rw, cy + rh, Math.max(2, h / 130), ringColor);
        }
        drawScreenLine(ctx, cx, cy, 0f, (int) (w * 0.42f), Math.max(3, h / 95), argb(params.accent, (int) (170 * fade)));
        drawScreenLine(ctx, cx, cy, (float) Math.PI / 2f, (int) (h * 0.35f), Math.max(3, h / 95), argb(params.accent, (int) (170 * fade)));
    }

    private static void renderHolyFrame(GuiGraphicsExtractor ctx, int w, int h, ImpactParams params, int frame, float fade) {
        ctx.fill(0, 0, w, h, argb(frame % 2 == 0 ? 0xFFF6AA : 0xFFFFFF, (int) (180 * params.intensity * fade)));
        int cx = w / 2;
        int cy = h / 2;
        int gold = argb(params.accent, (int) (230 * fade));
        drawScreenLine(ctx, cx, cy, 0f, (int) (w * 0.42f), Math.max(6, h / 42), gold);
        drawScreenLine(ctx, cx, cy, (float) Math.PI / 2f, (int) (h * 0.42f), Math.max(6, h / 42), gold);
        for (int i = 0; i < 12; i++) {
            float angle = (float) (Math.PI * 2 * i / 12);
            drawScreenLine(ctx, cx, cy, angle, (int) (Math.min(w, h) * 0.34f), Math.max(2, h / 110), argb(params.color, (int) (160 * fade)));
        }
    }

    private static void renderPierceFrame(GuiGraphicsExtractor ctx, int w, int h, ImpactParams params, int frame, float fade) {
        int cx = w / 2;
        int cy = h / 2;
        ctx.fill(0, 0, w, h, argb(frame % 2 == 0 ? 0x050505 : params.color, (int) (170 * params.intensity * fade)));
        drawLetterbox(ctx, w, h, (int) (155 * fade));
        int lance = argb(frame % 2 == 0 ? params.color : params.accent, (int) (255 * fade));
        drawScreenLine(ctx, cx, cy, 0f, (int) (w * 0.92f), Math.max(4, h / 44), lance);
        drawScreenLine(ctx, cx, cy, (float) Math.PI / 2f, (int) (h * 0.46f), Math.max(2, h / 95), argb(params.accent, (int) (190 * fade)));
        drawScreenRectOutline(ctx, cx - w / 18, cy - h / 12, cx + w / 18, cy + h / 12, Math.max(2, h / 110), argb(0x050505, (int) (235 * fade)));
        drawScreenRectOutline(ctx, cx - w / 28, cy - h / 18, cx + w / 28, cy + h / 18, Math.max(1, h / 180), lance);
    }

    private static void renderCrushFrame(GuiGraphicsExtractor ctx, int w, int h, ImpactParams params, int frame, float fade) {
        int cx = w / 2;
        int cy = h / 2;
        int black = argb(0x000000, (int) (240 * params.intensity * fade));
        int block = argb(frame % 2 == 0 ? params.color : params.accent, (int) (190 * fade));
        ctx.fill(0, 0, w, h, argb(0x111111, (int) (145 * fade)));
        int squeeze = (int) (h * (0.18f + 0.15f * fade));
        ctx.fill(0, 0, w, squeeze, black);
        ctx.fill(0, h - squeeze, w, h, black);
        ctx.fill(0, cy - h / 16, w, cy + h / 16, block);
        ctx.fill(cx - w / 5, cy - h / 5, cx + w / 5, cy + h / 5, argb(0x000000, (int) (165 * fade)));
        drawScreenRectOutline(ctx, cx - w / 4, cy - h / 4, cx + w / 4, cy + h / 4, Math.max(4, h / 70), block);
    }

    private static void renderRippleFrame(GuiGraphicsExtractor ctx, int w, int h, ImpactParams params, int frame, float fade) {
        int cx = w / 2;
        int cy = h / 2;
        ctx.fill(0, 0, w, h, argb(frame % 2 == 0 ? params.color : 0x050505, (int) (135 * params.intensity * fade)));
        int rings = 7;
        for (int i = 0; i < rings; i++) {
            float t = i / (float) rings;
            int rw = (int) (w * (0.05f + t * 0.42f + (1f - fade) * 0.08f));
            int rh = (int) (h * (0.04f + t * 0.32f + (1f - fade) * 0.06f));
            int color = argb(i % 2 == 0 ? params.accent : params.color, (int) ((210 - i * 18) * fade));
            drawScreenRectOutline(ctx, cx - rw, cy - rh, cx + rw, cy + rh, Math.max(1, h / 150), color);
        }
        for (int i = 0; i < 10; i++) {
            float angle = (float) (Math.PI * 2 * i / 10);
            drawScreenLine(ctx, cx, cy, angle, (int) (Math.min(w, h) * 0.22f), Math.max(1, h / 180), argb(params.accent, (int) (120 * fade)));
        }
    }

    private static void renderFractureFrame(GuiGraphicsExtractor ctx, int w, int h, ImpactParams params, int frame, float fade) {
        int cx = w / 2;
        int cy = h / 2;
        ctx.fill(0, 0, w, h, argb(frame % 2 == 0 ? 0xF3F3F3 : 0x050505, (int) (185 * params.intensity * fade)));
        int crack = argb(frame % 2 == 0 ? 0x050505 : params.accent, (int) (245 * fade));
        drawScreenLine(ctx, cx, cy, -0.85f, (int) (w * 0.52f), Math.max(3, h / 95), crack);
        drawScreenLine(ctx, cx - w / 8, cy - h / 12, 0.42f, (int) (w * 0.38f), Math.max(2, h / 115), crack);
        drawScreenLine(ctx, cx + w / 9, cy + h / 10, 1.15f, (int) (h * 0.36f), Math.max(2, h / 130), crack);
        drawScreenLine(ctx, cx - w / 5, cy + h / 9, -0.12f, (int) (w * 0.28f), Math.max(1, h / 150), crack);
        drawScreenLine(ctx, cx + w / 5, cy - h / 7, -0.42f, (int) (w * 0.26f), Math.max(1, h / 150), crack);
    }

    private static void renderBloodFrame(GuiGraphicsExtractor ctx, int w, int h, ImpactParams params, int frame, float fade) {
        int red = frame % 2 == 0 ? 0x7A0000 : params.accent;
        ctx.fill(0, 0, w, h, argb(red, (int) (205 * params.intensity * fade)));
        drawLetterbox(ctx, w, h, (int) (165 * fade));
        int cx = w / 2;
        int cy = h / 2;
        drawScreenLine(ctx, cx, cy, -0.38f, (int) (w * 0.65f), Math.max(16, h / 20), argb(0x150000, (int) (230 * fade)));
        drawScreenLine(ctx, cx, cy, -0.38f, (int) (w * 0.55f), Math.max(6, h / 55), argb(params.color, (int) (150 * fade)));
        for (int i = 0; i < 8; i++) {
            int x = (i * 97) % w;
            int top = (i * 37) % Math.max(1, h / 3);
            int length = h / 8 + (i % 4) * h / 18;
            ctx.fill(x, top, x + Math.max(2, w / 180), top + length, argb(0x2A0000, (int) (150 * fade)));
        }
    }

    private static void renderFrostFrame(GuiGraphicsExtractor ctx, int w, int h, ImpactParams params, int frame, float fade) {
        ctx.fill(0, 0, w, h, argb(frame % 2 == 0 ? 0xDDF8FF : 0xFFFFFF, (int) (165 * params.intensity * fade)));
        int ice = argb(params.accent, (int) (220 * fade));
        int cx = w / 2;
        int cy = h / 2;
        for (int i = 0; i < 12; i++) {
            float angle = (float) (Math.PI * 2 * i / 12);
            drawScreenLine(ctx, cx, cy, angle, (int) (Math.min(w, h) * (0.24f + (i % 3) * 0.045f)), Math.max(2, h / 120), ice);
        }
        drawScreenRectOutline(ctx, w / 18, h / 14, w - w / 18, h - h / 14, Math.max(3, h / 95), argb(0xEFFFFF, (int) (210 * fade)));
        drawScreenRectOutline(ctx, w / 10, h / 8, w - w / 10, h - h / 8, Math.max(1, h / 170), ice);
    }

    private static void drawLetterbox(GuiGraphicsExtractor ctx, int w, int h, int alpha) {
        int bar = Math.max(12, h / 9);
        int color = argb(0x000000, alpha);
        ctx.fill(0, 0, w, bar, color);
        ctx.fill(0, h - bar, w, h, color);
    }

    private static void drawScreenRectOutline(GuiGraphicsExtractor ctx, int x1, int y1, int x2, int y2, int thickness, int color) {
        ctx.fill(x1, y1, x2, y1 + thickness, color);
        ctx.fill(x1, y2 - thickness, x2, y2, color);
        ctx.fill(x1, y1, x1 + thickness, y2, color);
        ctx.fill(x2 - thickness, y1, x2, y2, color);
    }

    private static void drawScreenLine(GuiGraphicsExtractor ctx, int cx, int cy, float angle, int length, int thickness, int color) {
        var matrices = ctx.pose();
        matrices.pushMatrix();
        matrices.translate(cx, cy);
        matrices.rotate(angle);
        int half = length / 2;
        int halfT = Math.max(1, thickness / 2);
        ctx.fill(-half, -halfT, half, halfT, color);
        matrices.popMatrix();
    }

    private static int argb(int rgb, int alpha) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | (rgb & 0xFFFFFF);
    }

    private static void faceCamera(PoseStack stack) {
        Entity camera = Minecraft.getInstance().getCameraEntity();
        if (camera == null) return;
        stack.mulPose(Axis.YP.rotationDegrees(-camera.getYRot()));
        stack.mulPose(Axis.XP.rotationDegrees(camera.getXRot()));
    }

    private static void addVertex(PoseStack.Pose pose, VertexConsumer consumer, float x, float y, float z, int color, float alpha, float u, float v, float nx, float ny, float nz) {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        int a = (int) (255 * clamp(alpha, 0f, 1f));
        consumer.addVertex(pose, x, y, z)
                .setColor(r, g, b, a)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(FULL_BRIGHT)
                .setNormal(pose, nx, ny, nz);
    }

    private static float easeOutCubic(float t) {
        float inv = 1f - t;
        return 1f - inv * inv * inv;
    }

    private static class ImpactParams {
        private Vec3 position;
        private String style = "burst";
        private boolean screen = true;
        private boolean world = true;
        private int color = 0xFFFFFF;
        private int accent = 0xFF2200;
        private float intensity = 0.85f;
        private float radius = 3.0f;
        private long duration = 320;
        private int frames = 3;
        private long seed = System.nanoTime();

        private static ImpactParams parse(String params) {
            ImpactParams parsed = new ImpactParams();
            if (params == null || params.isBlank()) return parsed;

            Double x = null;
            Double y = null;
            Double z = null;

            for (String part : params.split(",")) {
                String[] kv = part.split("=", 2);
                if (kv.length != 2) continue;

                String key = kv[0].trim();
                String value = kv[1].trim();
                switch (key) {
                    case "style" -> parsed.style = value.toLowerCase();
                    case "scope" -> {
                        String scope = value.toLowerCase();
                        parsed.screen = !"world".equals(scope);
                        parsed.world = !"screen".equals(scope);
                    }
                    case "x" -> x = Double.parseDouble(value);
                    case "y" -> y = Double.parseDouble(value);
                    case "z" -> z = Double.parseDouble(value);
                    case "color" -> parsed.color = Integer.parseInt(value, 16);
                    case "accent" -> parsed.accent = Integer.parseInt(value, 16);
                    case "intensity" -> parsed.intensity = clamp(Float.parseFloat(value), 0f, 1f);
                    case "radius" -> parsed.radius = Math.max(0.25f, Float.parseFloat(value));
                    case "duration" -> parsed.duration = Math.max(80, Long.parseLong(value));
                    case "frames" -> parsed.frames = Math.max(1, Math.min(8, Integer.parseInt(value)));
                    case "seed" -> parsed.seed = Long.parseLong(value);
                }
            }

            if (x != null && y != null && z != null) {
                parsed.position = new Vec3(x, y, z);
            }

            return parsed;
        }

        private void resolveMissingPosition() {
            if (position != null) return;

            Minecraft client = Minecraft.getInstance();
            if (client.hitResult != null && client.hitResult.getType() != HitResult.Type.MISS) {
                position = client.hitResult.getLocation();
                return;
            }

            Entity camera = client.getCameraEntity();
            if (camera != null) {
                position = camera.getEyePosition().add(camera.getLookAngle().scale(4.0));
            }
        }
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
