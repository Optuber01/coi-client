package dev.ua.ikeepcalm.coi.mixin;

import dev.ua.ikeepcalm.coi.screen.title.TitleTakeover;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.SplashRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Vanilla pins the splash to {@code (width / 2 + 123, 69)}, inside the mod's much larger wordmark.
 * Redrawn from here keeps vanilla's point in the layer order and only moves the position.
 */
@Mixin(SplashRenderer.class)
public class SplashRendererMixin {

    @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
    private void coi$relocateSplash(GuiGraphicsExtractor ctx, int screenWidth, Font font,
                                    float alpha, CallbackInfo ci) {
        if (!TitleTakeover.onTitleScreen()) return;
        TitleTakeover.drawSplash(ctx, font, screenWidth, ctx.guiHeight());
        ci.cancel();
    }
}
