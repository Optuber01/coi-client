package dev.ua.ikeepcalm.coi.mixin;

import dev.ua.ikeepcalm.coi.domain.ceremony.SkyTintState;

import net.minecraft.client.renderer.SkyRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * The other half of {@code coi-client:sky_tint}: the sky disc. Recolouring the fog alone
 * turns the horizon and leaves the dome overhead unchanged, which reads as a rendering
 * fault â€” the two mixins are a pair, and a change to one wants the other.
 */
@Mixin(SkyRenderer.class)
public class SkyRendererMixin {

    @ModifyVariable(method = "renderSkyDisc", at = @At("HEAD"), argsOnly = true, index = 1)
    private int coi$tintSkyDisc(int color) {
        return SkyTintState.skyColor(color);
    }
}
