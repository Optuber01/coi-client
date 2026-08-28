package dev.ua.ikeepcalm.coi.client.appearance.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.ua.ikeepcalm.coi.client.appearance.AppearanceTraitRenderer;
import dev.ua.ikeepcalm.coi.client.appearance.BakedAccessoryModel;
import dev.ua.ikeepcalm.coi.client.appearance.LayeredHairModel;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

/**
 * Shared submission for the fixed 3D hair traits: one baked mesh per style/color/layer,
 * drawn against the blank texture with vertex-color shading. The hat-layer variant is
 * submitted when the player's hat layer is on so hair never clips through it.
 */
public abstract class AbstractHairRenderer implements AppearanceTraitRenderer {

    private static final Identifier WHITE_TEXTURE =
            Identifier.fromNamespaceAndPath("coi-client", "textures/entity/white.png");
    private static final RenderType RENDER_TYPE = RenderTypes.entityTranslucent(WHITE_TEXTURE);

    private final BakedAccessoryModel baseModel;
    private final BakedAccessoryModel hatModel;

    protected AbstractHairRenderer(LayeredHairModel.Style style, LayeredHairModel.Palette palette) {
        this.baseModel = LayeredHairModel.build(style, palette, false);
        this.hatModel = LayeredHairModel.build(style, palette, true);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, AvatarRenderState state, PlayerModel model) {
        poseStack.pushPose();
        model.head.translateAndRotate(poseStack);
        collector.order(0).submitCustomGeometry(
                poseStack,
                RENDER_TYPE,
                (pose, consumer) -> baseModel.render(pose, consumer, state.lightCoords)
        );
        if (state.showHat) {
            collector.order(0).submitCustomGeometry(
                    poseStack,
                    RENDER_TYPE,
                    (pose, consumer) -> hatModel.render(pose, consumer, state.lightCoords)
            );
        }
        poseStack.popPose();
    }
}
