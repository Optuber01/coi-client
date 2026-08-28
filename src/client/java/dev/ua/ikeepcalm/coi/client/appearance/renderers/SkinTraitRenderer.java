package dev.ua.ikeepcalm.coi.client.appearance.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.ua.ikeepcalm.coi.client.appearance.AppearanceTraitRenderer;
import dev.ua.ikeepcalm.coi.client.appearance.TraitGeometry;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.player.PlayerModelType;

/**
 * Whole-body hide overlays: pale, devil armor, wood, stone, chitin, zombie, wraith and
 * regrowth vines. Body-hugging with per-style inflation so stacked categories never
 * z-fight, detail patches for material texture, and slim-model aware arms.
 */
public final class SkinTraitRenderer implements AppearanceTraitRenderer {

    public enum Style {PALE, DEVIL_ARMOR, WOOD, STONE, CHITIN, ZOMBIE, WRAITH, REGROWTH}

    private final String traitId;
    private final Style style;
    private final TraitGeometry.Tint primary;
    private final TraitGeometry.Tint detail;

    public SkinTraitRenderer(String traitId, Style style) {
        this.traitId = traitId;
        this.style = style;
        this.primary = switch (style) {
            case PALE -> new TraitGeometry.Tint(0.82f, 0.86f, 0.90f, 0.28f);
            case DEVIL_ARMOR -> new TraitGeometry.Tint(0.025f, 0.018f, 0.028f, 0.94f);
            case WOOD -> new TraitGeometry.Tint(0.29f, 0.15f, 0.055f, 0.86f);
            case STONE -> new TraitGeometry.Tint(0.38f, 0.40f, 0.43f, 0.88f);
            case CHITIN -> new TraitGeometry.Tint(0.10f, 0.055f, 0.13f, 0.92f);
            case ZOMBIE -> new TraitGeometry.Tint(0.24f, 0.40f, 0.25f, 0.58f);
            case WRAITH -> new TraitGeometry.Tint(0.35f, 0.70f, 0.82f, 0.24f);
            case REGROWTH -> new TraitGeometry.Tint(0.08f, 0.36f, 0.12f, 0.92f);
        };
        this.detail = switch (style) {
            case PALE -> new TraitGeometry.Tint(0.70f, 0.75f, 0.82f, 0.18f);
            case DEVIL_ARMOR -> new TraitGeometry.Tint(0.30f, 0.025f, 0.035f, 0.96f);
            case WOOD -> new TraitGeometry.Tint(0.48f, 0.28f, 0.075f, 0.92f);
            case STONE -> new TraitGeometry.Tint(0.59f, 0.61f, 0.64f, 0.90f);
            case CHITIN -> new TraitGeometry.Tint(0.34f, 0.08f, 0.39f, 0.96f);
            case ZOMBIE -> new TraitGeometry.Tint(0.11f, 0.21f, 0.12f, 0.72f);
            case WRAITH -> new TraitGeometry.Tint(0.72f, 0.92f, 1.0f, 0.32f);
            case REGROWTH -> new TraitGeometry.Tint(0.24f, 0.66f, 0.18f, 0.94f);
        };
    }

    @Override
    public String traitId() {
        return traitId;
    }

    @Override
    public void submit(PoseStack stack, SubmitNodeCollector collector, AvatarRenderState state, PlayerModel model) {
        boolean slim = state.skin.model() == PlayerModelType.SLIM;
        submitPart(stack, collector, state, model.head, Part.HEAD, slim);
        submitPart(stack, collector, state, model.body, Part.BODY, slim);
        submitPart(stack, collector, state, model.leftArm, Part.ARM, slim);
        submitPart(stack, collector, state, model.rightArm, Part.ARM, slim);
        if (style != Style.PALE && style != Style.REGROWTH) {
            submitPart(stack, collector, state, model.leftLeg, Part.LEG, slim);
            submitPart(stack, collector, state, model.rightLeg, Part.LEG, slim);
        }
    }

    private void submitPart(PoseStack stack, SubmitNodeCollector collector, AvatarRenderState state, ModelPart part,
                            Part partType, boolean slim) {
        stack.pushPose();
        part.translateAndRotate(stack);
        collector.order(1).submitCustomGeometry(
                stack,
                TraitRenderSupport.TRANSLUCENT,
                (pose, consumer) -> drawPart(pose, consumer, state.lightCoords, partType, slim)
        );
        stack.popPose();
    }

    private void drawPart(PoseStack.Pose pose, com.mojang.blaze3d.vertex.VertexConsumer consumer, int light, Part part, boolean slim) {
        TraitGeometry g = TraitGeometry.INSTANCE;
        if (style == Style.REGROWTH) {
            drawVines(g, pose, consumer, light, part, slim);
            return;
        }

        float inflate = style == Style.DEVIL_ARMOR || style == Style.CHITIN ? 0.35f : 0.14f;
        switch (part) {
            case HEAD -> g.boxPixels(pose, consumer, -4 - inflate, -8 - inflate, -4 - inflate,
                    4 + inflate, inflate, 4 + inflate, primary, light);
            case BODY -> g.boxPixels(pose, consumer, -4 - inflate, -inflate, -2 - inflate,
                    4 + inflate, 12 + inflate, 2 + inflate, primary, light);
            case ARM -> {
                float halfWidth = slim ? 1.5f : 2.0f;
                g.boxPixels(pose, consumer, -halfWidth - inflate, -2 - inflate, -2 - inflate,
                        halfWidth + inflate, 12 + inflate, 2 + inflate, primary, light);
            }
            case LEG -> g.boxPixels(pose, consumer, -2 - inflate, -inflate, -2 - inflate,
                    2 + inflate, 12 + inflate, 2 + inflate, primary, light);
        }

        if (style == Style.PALE || style == Style.WRAITH) return;
        switch (part) {
            case HEAD -> {
                g.boxPixels(pose, consumer, -3.35f, -7.95f, -4.48f, -1.95f, -6.7f, -4.16f, detail, light);
                g.boxPixels(pose, consumer, 1.55f, -1.65f, -4.48f, 3.45f, -0.4f, -4.16f, detail, light);
            }
            case BODY -> {
                g.boxPixels(pose, consumer, -3.75f, 1.1f, -2.48f, -0.35f, 2.0f, -2.16f, detail, light);
                g.boxPixels(pose, consumer, 0.35f, 5.2f, -2.48f, 3.75f, 6.15f, -2.16f, detail, light);
                g.boxPixels(pose, consumer, -2.2f, 9.2f, -2.48f, 1.25f, 10.15f, -2.16f, detail, light);
            }
            case ARM, LEG -> {
                float edge = part == Part.ARM && slim ? 1.92f : 2.42f;
                g.boxPixels(pose, consumer, -edge, 1.1f, -2.48f, 0.55f, 2.0f, -2.16f, detail, light);
                g.boxPixels(pose, consumer, -0.55f, 7.1f, -2.48f, edge, 8.0f, -2.16f, detail, light);
            }
        }
    }

    private void drawVines(TraitGeometry g, PoseStack.Pose pose, com.mojang.blaze3d.vertex.VertexConsumer consumer, int light, Part part, boolean slim) {
        if (part == Part.HEAD) {
            g.boxPixels(pose, consumer, -4.35f, -7.6f, 3.9f, -3.85f, -0.6f, 4.35f, primary, light);
            g.boxPixels(pose, consumer, 3.72f, -5.8f, 3.9f, 4.3f, -1.2f, 4.35f, detail, light);
        } else if (part == Part.BODY) {
            g.boxPixels(pose, consumer, -3.7f, 0.2f, -2.32f, -3.1f, 11.8f, -2.05f, primary, light);
            g.boxPixels(pose, consumer, -3.15f, 4.5f, -2.34f, 2.6f, 5.15f, -2.05f, detail, light);
            g.boxPixels(pose, consumer, 2.15f, 4.7f, -2.34f, 2.75f, 10.9f, -2.05f, primary, light);
        } else if (part == Part.ARM) {
            float edge = slim ? -1.75f : -2.25f;
            g.boxPixels(pose, consumer, edge, -1.6f, -2.28f, edge + 0.55f, 11.7f, -2.02f, primary, light);
        }
    }

    private enum Part {HEAD, BODY, ARM, LEG}
}
