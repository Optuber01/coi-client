package dev.ua.ikeepcalm.coi.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.ua.ikeepcalm.coi.domain.form.PartialForms;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hides leggings/boots under a partial form: armor renders from its own model set, not from the
 * player parts {@link PlayerModelMixin} hides. Chest and head need no help — they ride the pose
 * {@link LivingEntityRendererMixin} pushes.
 */
@Mixin(HumanoidArmorLayer.class)
public class HumanoidArmorLayerMixin {

    @Inject(method = "renderArmorPiece", at = @At("HEAD"), cancellable = true)
    private void coi$hidePartialFormLegArmor(PoseStack poseStack, SubmitNodeCollector collector, ItemStack stack, EquipmentSlot slot, int light, HumanoidRenderState state, CallbackInfo ci) {
        if (slot != EquipmentSlot.LEGS && slot != EquipmentSlot.FEET) {
            return;
        }
        if (PartialForms.partial(state) != null) {
            ci.cancel();
        }
    }
}
