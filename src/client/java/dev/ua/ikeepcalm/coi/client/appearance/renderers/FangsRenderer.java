package dev.ua.ikeepcalm.coi.client.appearance.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.ua.ikeepcalm.coi.client.appearance.AppearanceTraitRenderer;
import dev.ua.ikeepcalm.coi.client.appearance.BodyBoxes;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

/**
 * A pair of small canines poking below the lip line — vampiric Moon flavor. Rendered as
 * flat front-facing shards against the face plane so they stay legible at player scale.
 */
public class FangsRenderer implements AppearanceTraitRenderer {

    private static final Identifier WHITE_TEXTURE =
            Identifier.fromNamespaceAndPath("coi-client", "textures/entity/white.png");
    private static final RenderType RENDER_TYPE = RenderTypes.entityCutout(WHITE_TEXTURE);

    private static final float ENAMEL_R = 0.97f;
    private static final float ENAMEL_G = 0.95f;
    private static final float ENAMEL_B = 0.90f;

    @Override
    public String traitId() {
        return "fangs";
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, AvatarRenderState state, PlayerModel model) {
        poseStack.pushPose();
        model.head.translateAndRotate(poseStack);
        collector.order(0).submitCustomGeometry(
                poseStack,
                RENDER_TYPE,
                (pose, consumer) -> {
                    drawFang(pose, consumer, state.lightCoords, -1.75f);
                    drawFang(pose, consumer, state.lightCoords, 1.75f);
                }
        );
        poseStack.popPose();
    }

    private void drawFang(PoseStack.Pose pose, com.mojang.blaze3d.vertex.VertexConsumer consumer,
                          int light, float centerX) {
        float halfWidth = 0.42f;
        float topY = -1.45f;
        float tipY = 0.55f;
        float z = -4.05f;
        float backZ = -3.55f;

        // Front face shard
        BodyBoxes.quad(pose, consumer,
                centerX - halfWidth, topY, z,
                centerX + halfWidth, topY, z,
                centerX, tipY, z,
                centerX - halfWidth, topY, z,
                ENAMEL_R, ENAMEL_G, ENAMEL_B, 1.0f, light);
        // Slight side depth so the fang isn't paper-thin at grazing angles
        BodyBoxes.quad(pose, consumer,
                centerX + halfWidth, topY, z,
                centerX + halfWidth * 0.4f, topY, backZ,
                centerX, tipY, z,
                centerX + halfWidth, topY, z,
                0.82f, 0.80f, 0.76f, 1.0f, light);
    }
}
