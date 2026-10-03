package dev.ua.ikeepcalm.coi.mixin;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.resource.CrossFrameResourcePool;
import dev.ua.ikeepcalm.coi.domain.ceremony.CameraCinematics.CameraOrbitState;
import dev.ua.ikeepcalm.coi.domain.ceremony.PostFx;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The ceremony's post pass, in the gap vanilla uses for the creeper and spider views:
 * after the entity outlines, before the GUI. Later would process the HUD with the world.
 * <p>
 * It deliberately does <em>not</em> write {@code GameRenderer.postEffectId}: that field is
 * a single slot vanilla re-asserts from {@code checkEntityPostEffect} whenever the camera
 * entity changes. Running our chain beside it costs one {@code process} call and leaves
 * both free. {@code PostChain#process} is deprecated in favour of the level's frame graph,
 * which is not reachable from here — and is the same call vanilla makes two lines above.
 */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    @Final
    private RenderTarget mainRenderTarget;

    @Shadow
    @Final
    private CrossFrameResourcePool resourcePool;

    @SuppressWarnings("deprecation")
    @Inject(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/LevelRenderer;doEntityOutline()V", shift = At.Shift.AFTER))
    private void coi$ceremonyPostFx(DeltaTracker deltaTracker, boolean shouldRenderLevel, CallbackInfo ci) {
        Identifier id = PostFx.chain();
        if (id == null) return;
        PostChain chain = minecraft.getShaderManager().getPostChain(id, LevelTargetBundle.MAIN_TARGETS);
        if (chain != null) {
            chain.process(mainRenderTarget, resourcePool);
        }
    }

    /**
     * Vanilla gates the hand on camera <em>type</em>, not {@code Camera#isDetached}, so
     * detaching alone leaves an arm floating over the middle of the cinematic.
     */
    @Inject(method = "renderItemInHand", at = @At("HEAD"), cancellable = true)
    private void coi$hideHandDuringOrbit(CameraRenderState cameraState, float deltaPartialTick,
                                         Matrix4fc modelViewMatrix, CallbackInfo ci) {
        if (CameraOrbitState.displacing()) ci.cancel();
    }
}
