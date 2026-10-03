package dev.ua.ikeepcalm.coi.mixin;

import dev.ua.ikeepcalm.coi.domain.form.PartialForms;
import dev.ua.ikeepcalm.coi.util.duck.AvatarRenderStateAccessor;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hides the player's own legs under a partial form. Has to run in {@code setupAnim}, not around
 * the render call: geometry submission is deferred, so toggling shared model state at render time
 * and restoring it before the deferred draw runs has no effect at all.
 */
@Mixin(PlayerModel.class)
public abstract class PlayerModelMixin {

    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)V", at = @At("TAIL"))
    private void coi$hideLegsForPartialForm(AvatarRenderState state, CallbackInfo ci) {
        PlayerModel self = (PlayerModel) (Object) this;
        boolean hidden = PartialForms.partial(state) != null;
        self.leftLeg.visible = !hidden;
        self.rightLeg.visible = !hidden;

        // re-derived, not un-hidden: restoring these to visible would force pants back on for
        // players who have that skin-customisation layer switched off
        self.leftPants.visible = !hidden && state.showLeftPants;
        self.rightPants.visible = !hidden && state.showRightPants;
        var portrait = ((AvatarRenderStateAccessor) state).coi$getPortraitPose();
        if (portrait != null) portrait.apply(self, state.ageInTicks);
    }
}
