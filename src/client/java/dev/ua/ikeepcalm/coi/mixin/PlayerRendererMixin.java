package dev.ua.ikeepcalm.coi.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.ua.ikeepcalm.coi.client.mcf.AvatarRenderStateAccessor;
import dev.ua.ikeepcalm.coi.client.mcf.MythicalFormManager;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class PlayerRendererMixin {

    @Unique
    private static final float COI_MYTHICAL_WALK_EPSILON = 0.015F;

    @Unique
    private static void coi$applyMythicalWalkAnimation(LivingEntityRenderState state, PoseStack poseStack) {
        float walkAmount = Mth.clamp(state.walkAnimationSpeed, 0.0F, 1.0F);
        if (walkAmount <= COI_MYTHICAL_WALK_EPSILON) {
            return;
        }

        float stride = state.walkAnimationPos * 0.6662F;
        float swing = Mth.sin(stride);
        float step = Mth.abs(Mth.cos(stride));
        float intensity = walkAmount * walkAmount;

        poseStack.translate(swing * 0.025F * intensity, step * 0.055F * intensity, 0.0F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(swing * 3.25F * intensity));
        poseStack.mulPose(Axis.XP.rotationDegrees(Mth.cos(stride) * 1.75F * intensity));
    }

    @Unique
    private static void coi$submitMythicalHeldItems(AvatarRenderState state, PoseStack poseStack, SubmitNodeCollector collector) {
        if (!state.rightHandItemState.isEmpty()) {
            coi$submitMythicalHeldItem(state, poseStack, collector, HumanoidArm.RIGHT);
        }

        if (!state.leftHandItemState.isEmpty()) {
            coi$submitMythicalHeldItem(state, poseStack, collector, HumanoidArm.LEFT);
        }
    }

    @Unique
    private static void coi$submitMythicalHeldItem(AvatarRenderState state, PoseStack poseStack, SubmitNodeCollector collector, HumanoidArm arm) {
        boolean left = arm == HumanoidArm.LEFT;
        float side = left ? 1.0F : -1.0F;

        poseStack.pushPose();
        poseStack.translate(side * 0.68F, 1.22F, -0.28F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(side * -10.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        poseStack.scale(0.9F, 0.9F, 0.9F);

        if (left) {
            state.leftHandItemState.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
        } else {
            state.rightHandItemState.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
        }

        poseStack.popPose();
    }

    @Inject(method = "submit*", at = @At("HEAD"), cancellable = true)
    private void coi$renderMythicalForm(LivingEntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState, CallbackInfo ci) {
        if (state instanceof AvatarRenderState avatarState) {
            String playerUuid = ((AvatarRenderStateAccessor) avatarState).coi$getPlayerUuid();
            if (playerUuid != null) {
                String form = MythicalFormManager.getForm(playerUuid);
                if (form != null) {
                    poseStack.pushPose();
                    poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - state.bodyRot));
                    coi$applyMythicalWalkAnimation(state, poseStack);

                    collector.order(0).submitCustomGeometry(
                            poseStack,
                            RenderTypes.entityTranslucent(Identifier.fromNamespaceAndPath("coi-client", "textures/entity/white.png")),
                            (pose, consumer) -> MythicalFormManager.renderFormSubmit(form, avatarState, pose, consumer)
                    );
                    coi$submitMythicalHeldItems(avatarState, poseStack, collector);

                    poseStack.popPose();

                    // Preserve the name tag display via accessor invoker
                    ((EntityRendererAccessor) this).callSubmitNameDisplay(avatarState, poseStack, collector, cameraState);

                    ci.cancel();
                }
            }
        }
    }
}
