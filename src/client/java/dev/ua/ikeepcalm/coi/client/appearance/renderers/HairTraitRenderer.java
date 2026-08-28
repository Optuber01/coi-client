package dev.ua.ikeepcalm.coi.client.appearance.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.ua.ikeepcalm.coi.client.appearance.AppearanceTraitRenderer;
import dev.ua.ikeepcalm.coi.client.appearance.TraitGeometry;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

/**
 * Pathway hair: a thin crown slab, side locks, and a broken fringe that leaves most
 * custom-skin facial features visible — long styles add a back panel with strands
 * parented to the body so it sways with the torso instead of the head.
 */
public final class HairTraitRenderer implements AppearanceTraitRenderer {

    public enum Style {SHORT, LONG}

    private static final Identifier WHITE_TEXTURE =
            Identifier.fromNamespaceAndPath("coi-client", "textures/entity/white.png");
    private static final RenderType RENDER_TYPE = RenderTypes.entityTranslucent(WHITE_TEXTURE);

    private final String traitId;
    private final Style style;
    private final TraitGeometry.Tint base;
    private final TraitGeometry.Tint highlight;

    public HairTraitRenderer(String traitId, Style style, float red, float green, float blue) {
        this.traitId = traitId;
        this.style = style;
        this.base = new TraitGeometry.Tint(red, green, blue, 0.98f);
        this.highlight = new TraitGeometry.Tint(
                Math.min(1.0f, red * 1.35f + 0.035f),
                Math.min(1.0f, green * 1.35f + 0.035f),
                Math.min(1.0f, blue * 1.35f + 0.035f),
                0.96f
        );
    }

    @Override
    public String traitId() {
        return traitId;
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, AvatarRenderState state, PlayerModel model) {
        TraitGeometry g = TraitGeometry.INSTANCE;
        int light = state.lightCoords;

        poseStack.pushPose();
        model.head.translateAndRotate(poseStack);
        collector.order(1).submitCustomGeometry(
                poseStack,
                RENDER_TYPE,
                (pose, consumer) -> drawHeadHair(g, pose, consumer, light)
        );
        poseStack.popPose();

        if (style == Style.LONG) {
            poseStack.pushPose();
            model.body.translateAndRotate(poseStack);
            collector.order(1).submitCustomGeometry(
                    poseStack,
                    RENDER_TYPE,
                    (pose, consumer) -> drawLongHair(g, pose, consumer, light)
            );
            poseStack.popPose();
        }
    }

    private void drawHeadHair(TraitGeometry g, PoseStack.Pose pose,
                              com.mojang.blaze3d.vertex.VertexConsumer consumer, int light) {
        // Crown slab over the top of the head
        g.boxPixels(pose, consumer, -4.18f, -8.42f, -4.18f, 4.18f, -6.55f, 4.18f, base, light);
        // Back panel hanging to the nape
        g.boxPixels(pose, consumer, -4.22f, -6.75f, 3.72f, 4.22f, 1.0f, 4.35f, base, light);
        // Side locks sweeping past the temples
        g.boxPixels(pose, consumer, -4.28f, -6.7f, -3.5f, -3.70f, 0.55f, 4.05f, base, light);
        g.boxPixels(pose, consumer, 3.70f, -6.7f, -3.5f, 4.28f, 0.55f, 4.05f, base, light);

        // Broken fringe reads as hair while leaving most custom-skin facial features visible
        g.boxPixels(pose, consumer, -4.05f, -6.75f, -4.34f, -1.35f, -4.25f, -4.05f, base, light);
        g.boxPixels(pose, consumer, -1.45f, -6.72f, -4.35f, 0.15f, -3.55f, -4.04f, highlight, light);
        g.boxPixels(pose, consumer, 0.08f, -6.72f, -4.34f, 2.05f, -4.70f, -4.04f, base, light);
        g.boxPixels(pose, consumer, 1.95f, -6.72f, -4.33f, 4.04f, -5.25f, -4.04f, highlight, light);
        // Highlight strip along the crown
        g.boxPixels(pose, consumer, -2.8f, -8.50f, -2.2f, 2.3f, -8.30f, 2.5f, highlight, light);
    }

    private void drawLongHair(TraitGeometry g, PoseStack.Pose pose,
                              com.mojang.blaze3d.vertex.VertexConsumer consumer, int light) {
        // Back panel down the torso
        g.boxPixels(pose, consumer, -3.85f, -0.3f, 2.03f, 3.85f, 11.7f, 2.72f, base, light);
        // Long side strands
        g.boxPixels(pose, consumer, -4.18f, 0.35f, 1.55f, -3.42f, 10.4f, 2.55f, base, light);
        g.boxPixels(pose, consumer, 3.42f, 0.35f, 1.55f, 4.18f, 10.4f, 2.55f, base, light);
        // Highlight streaks
        g.boxPixels(pose, consumer, -2.65f, 0.0f, 2.73f, -2.10f, 10.9f, 2.90f, highlight, light);
        g.boxPixels(pose, consumer, 1.95f, 0.0f, 2.73f, 2.43f, 9.8f, 2.90f, highlight, light);
    }
}
