package dev.ua.ikeepcalm.coi.client.appearance.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.ua.ikeepcalm.coi.client.appearance.AppearanceTraitRenderer;
import dev.ua.ikeepcalm.coi.client.appearance.TraitGeometry;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;

/**
 * Lifeless aura — three thin rectangular mist rings around the body, pulsing slowly.
 * Deliberately subtle: translucent frame edges that read as presence rather than paint.
 */
public final class AuraTraitRenderer implements AppearanceTraitRenderer {

    private final String traitId;

    public AuraTraitRenderer(String traitId) {
        this.traitId = traitId;
    }

    @Override
    public String traitId() {
        return traitId;
    }

    @Override
    public void submit(PoseStack stack, SubmitNodeCollector collector, AvatarRenderState state, PlayerModel model) {
        stack.pushPose();
        model.body.translateAndRotate(stack);
        collector.order(5).submitCustomGeometry(stack, TraitRenderSupport.TRANSLUCENT,
                (pose, consumer) -> drawAura(pose, consumer, state.lightCoords));
        stack.popPose();
    }

    private void drawAura(PoseStack.Pose pose, com.mojang.blaze3d.vertex.VertexConsumer consumer, int light) {
        TraitGeometry g = TraitGeometry.INSTANCE;
        float pulse = 0.10f + (float) (Math.sin(System.currentTimeMillis() * 0.003) + 1.0) * 0.035f;
        TraitGeometry.Tint mist = new TraitGeometry.Tint(0.18f, 0.22f, 0.28f, pulse);
        for (int level = 0; level < 3; level++) {
            float y = 2.0f + level * 4.1f;
            float radius = 5.0f + level * 0.75f;
            float thickness = 0.18f;
            g.boxPixels(pose, consumer, -radius, y, -radius, radius, y + thickness, -radius + thickness, mist, light);
            g.boxPixels(pose, consumer, -radius, y, radius - thickness, radius, y + thickness, radius, mist, light);
            g.boxPixels(pose, consumer, -radius, y, -radius, -radius + thickness, y + thickness, radius, mist, light);
            g.boxPixels(pose, consumer, radius - thickness, y, -radius, radius, y + thickness, radius, mist, light);
        }
    }
}
