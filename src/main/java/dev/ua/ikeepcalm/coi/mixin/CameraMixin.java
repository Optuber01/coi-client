package dev.ua.ikeepcalm.coi.mixin;

import dev.ua.ikeepcalm.coi.domain.ceremony.CameraCinematics.CameraOrbitState;
import dev.ua.ikeepcalm.coi.domain.ceremony.CameraCinematics.ScreenShakeState;

import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Both moves land at the end of {@code alignWithEntity}, not of {@code update}:
 * {@code update} goes on to build the cull frustum and the projection from the camera,
 * so a change made later leaves the frustum describing a camera that is no longer there.
 * A three-degree shake survives that; a seven-block orbit would cull the chunks out of a
 * shot pointed straight at them.
 * <p>
 * Neither move touches the player entity — view only, so no movement check trips.
 */
@Mixin(Camera.class)
public abstract class CameraMixin {

    @Shadow
    private boolean detached;

    @Shadow
    public abstract Vec3 position();

    @Shadow
    public abstract float xRot();

    @Shadow
    public abstract float yRot();

    @Shadow
    protected abstract void setRotation(float yRot, float xRot);

    @Shadow
    protected abstract void setPosition(double x, double y, double z);

    @Inject(method = "alignWithEntity", at = @At("RETURN"))
    private void coi$ceremonyCamera(float partialTicks, CallbackInfo ci) {
        coi$orbit();
        coi$shake();
    }

    /**
     * {@code resolve} is handed where the camera currently is, so the blend resolves
     * to the player's own view at either end and there is no cut.
     */
    @Unique
    private void coi$orbit() {
        Vec3 eye = position();
        CameraOrbitState.Placement placement =
                CameraOrbitState.resolve(eye.x, eye.y, eye.z, yRot(), xRot());
        if (placement == null) return;

        setRotation(placement.yRot(), placement.xRot());
        setPosition(placement.x(), placement.y(), placement.z());
        // third person for the length of the shot, so the witness is in it too
        if (CameraOrbitState.displacing()) {
            detached = true;
        }
    }

    @Unique
    private void coi$shake() {
        float yaw = ScreenShakeState.yawDegrees();
        float pitch = ScreenShakeState.pitchDegrees();
        if (yaw == 0f && pitch == 0f) return;
        setRotation(yRot() + yaw, xRot() + pitch);
    }
}
