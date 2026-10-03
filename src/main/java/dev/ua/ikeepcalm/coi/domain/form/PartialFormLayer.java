package dev.ua.ikeepcalm.coi.domain.form;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.joml.Matrix4f;
import org.jspecify.annotations.NonNull;

import java.util.HashMap;
import java.util.Map;

/**
 * The baked model for the active player's {@link PartialFormSpec}, drawn alongside the vanilla
 * player. {@link dev.ua.ikeepcalm.coi.mixin.PlayerModelMixin} hides the legs for the duration.
 */
public class PartialFormLayer extends RenderLayer<AvatarRenderState, PlayerModel> {

    private record Baked(Model<AvatarRenderState> model, RenderType renderType) {
    }

    private final Map<ModelLayerLocation, Baked> baked = new HashMap<>();

    public PartialFormLayer(RenderLayerParent<AvatarRenderState, PlayerModel> renderer, EntityRendererProvider.Context context) {
        super(renderer);
        for (PartialFormSpec spec : MythicalFormManager.getPartialForms().values()) {
            Model<AvatarRenderState> model = spec.factory().apply(context.bakeLayer(spec.layer()));
            RenderType renderType = RenderTypes.entityCutout(spec.texture());
            baked.put(spec.layer(), new Baked(model, renderType));
        }
    }

    @Override
    public void submit(@NonNull PoseStack poseStack, @NonNull SubmitNodeCollector collector, int light, AvatarRenderState state, float yRot, float xRot) {
        PartialFormSpec spec = PartialForms.partial(state);
        if (spec == null) {
            return;
        }
        Baked entry = baked.get(spec.layer());
        if (entry == null) {
            return;
        }

        poseStack.pushPose();
        // The renderer mixin already moved this whole pose by the carrier transform, so the
        // player's upper body follows this model's torso. This model animates that motion
        // itself, so it has to come back out here or the torso travels twice as far.
        Matrix4f carrier = PartialForms.carrierTransform(spec, state);
        if (carrier != null) {
            poseStack.mulPose(carrier.invertAffine());
        }
        poseStack.translate(spec.offsetX(), spec.offsetY(), spec.offsetZ());
        poseStack.scale(spec.scale(), spec.scale(), spec.scale());
        // submitModel, not submitCustomGeometry: submission is deferred and this instance is
        // shared by every player drawn, so posing it here and reading it from a captured lambda
        // later would give every transformed player on screen the last one's pose.
        collector.order(0).submitModel(
                entry.model(),
                state,
                poseStack,
                entry.renderType(),
                light,
                OverlayTexture.NO_OVERLAY,
                -1,
                null,
                state.outlineColor,
                null
        );
        poseStack.popPose();
    }
}
