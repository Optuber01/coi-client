package dev.ua.ikeepcalm.coi.mixin;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;
import net.minecraft.client.multiplayer.ServerData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerSelectionList.OnlineServerEntry.class)
public abstract class OnlineServerEntryMixin {

    @Shadow
    @Final
    private ServerData serverData;

    @Inject(method = "extractContent", at = @At("HEAD"))
    private void onExtractContent(GuiGraphicsExtractor guiGraphicsExtractor, int mouseX, int mouseY, boolean isSelected, float partialTick, CallbackInfo ci) {
        if (serverData.ip.equalsIgnoreCase("mc.mysterria.net") || serverData.ip.equalsIgnoreCase("play.mysterria.net")) {
            SelectionListEntryInvoker invoker = (SelectionListEntryInvoker) (Object) this;
            int left = invoker.callGetContentX();
            int top = invoker.callGetContentY();
            int width = invoker.callGetContentWidth();
            int height = 32;

            long time = System.currentTimeMillis();
            double angleTop = (time / 2500.0) * Math.PI * 2.0;
            double angleBottom = ((time - 600) / 2500.0) * Math.PI * 2.0;

            double blendTop = 0.5 + 0.5 * Math.sin(angleTop);
            double blendBottom = 0.5 + 0.5 * Math.sin(angleBottom);

            int rTop = (int) ((1.0 - blendTop) * 0x5B + blendTop * 0x1A);
            int gTop = (int) ((1.0 - blendTop) * 0x2F + blendTop * 0x4B);
            int bTop = (int) ((1.0 - blendTop) * 0x91 + blendTop * 0xA0);

            int rBottom = (int) ((1.0 - blendBottom) * 0x5B + blendBottom * 0x1A);
            int gBottom = (int) ((1.0 - blendBottom) * 0x2F + blendBottom * 0x4B);
            int bBottom = (int) ((1.0 - blendBottom) * 0x91 + blendBottom * 0xA0);

            int colorTop = (0x90 << 24) | (rTop << 16) | (gTop << 8) | bTop;
            int colorBottom = (0x50 << 24) | (rBottom << 16) | (gBottom << 8) | bBottom;

            guiGraphicsExtractor.fillGradient(left - 4, top - 4, left + width + 4, top + height + 4, colorTop, colorBottom);

            double pulseAngle = (time / 1500.0) * Math.PI * 2.0;
            int alpha = 0x70 + (int) (0x20 * Math.sin(pulseAngle));
            int borderColor = (alpha << 24) | (rTop << 16) | (gTop << 8) | bTop;
            guiGraphicsExtractor.outline(left - 4, top - 4, width + 8, height + 8, borderColor);

            for (int i = 0; i < 5; i++) {
                double speed = 0.04 + (i * 0.015);
                double xOffset = (time * speed + i * 120) % (width + 40) - 20;
                int px = left + (int) xOffset;
                int py = top + 3 + (i * 7) % 26;

                if (px >= left - 2 && px <= left + width + 2) {
                    double twinkleAngle = (time / 200.0) * Math.PI * 2.0 + i;
                    int starAlpha = 0x90 + (int) (0x6F * Math.sin(twinkleAngle));

                    int centerColor = (starAlpha << 24) | 0xFFFFFF;
                    int sideColor = ((starAlpha / 2) << 24) | 0xFFFFFF;

                    guiGraphicsExtractor.fill(px, py, px + 1, py + 1, centerColor);
                    guiGraphicsExtractor.fill(px - 1, py, px, py + 1, sideColor);
                    guiGraphicsExtractor.fill(px + 1, py, px + 2, py + 1, sideColor);
                    guiGraphicsExtractor.fill(px, py - 1, px + 1, py, sideColor);
                    guiGraphicsExtractor.fill(px, py + 1, px + 1, py + 2, sideColor);
                }
            }
        }
    }
}
