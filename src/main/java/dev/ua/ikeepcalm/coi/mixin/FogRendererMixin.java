package dev.ua.ikeepcalm.coi.mixin;

import dev.ua.ikeepcalm.coi.domain.ceremony.SkyTintState;

import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.FogRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Half of {@code coi-client:sky_tint} ({@link SkyRendererMixin} is the other).
 * {@code setupFog} returns a mutable {@link FogData} on its way to the GPU buffer, so its
 * RETURN is the one place the fog's colour and all four distances can be changed together.
 */
@Mixin(FogRenderer.class)
public class FogRendererMixin {

    @Inject(method = "setupFog", at = @At("RETURN"))
    private void coi$ceremonyFog(Camera camera, int renderDistanceInChunks, DeltaTracker deltaTracker,
                                 float darkenWorldAmount, ClientLevel level, CallbackInfoReturnable<FogData> cir) {
        SkyTintState.applyFog(cir.getReturnValue());
    }
}
