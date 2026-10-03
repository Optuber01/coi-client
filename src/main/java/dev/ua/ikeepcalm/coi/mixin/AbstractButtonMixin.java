package dev.ua.ikeepcalm.coi.mixin;

import dev.ua.ikeepcalm.coi.screen.title.TitleButtons;
import dev.ua.ikeepcalm.coi.screen.title.TitleTakeover;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * {@code extractDefaultSprite} draws the button background and nothing else — the label comes from
 * {@code extractContents} afterwards — so cancelling it replaces the chrome and nothing more.
 * Every button in the game runs through this method, hence the
 * {@link TitleTakeover#onTitleScreen()} screen check alongside the setting.
 */
@Mixin(AbstractButton.class)
public class AbstractButtonMixin {

    @Inject(method = "extractDefaultSprite", at = @At("HEAD"), cancellable = true)
    private void coi$titleChrome(GuiGraphicsExtractor ctx, CallbackInfo ci) {
        if (!TitleTakeover.onTitleScreen()) return;
        AbstractWidget self = (AbstractWidget) (Object) this;
        TitleButtons.plate(ctx, self.getX(), self.getY(), self.getWidth(), self.getHeight(),
                self.isHoveredOrFocused(), self.isActive());
        ci.cancel();
    }
}
