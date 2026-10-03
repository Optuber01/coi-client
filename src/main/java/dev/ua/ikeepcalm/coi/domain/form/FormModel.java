package dev.ua.ikeepcalm.coi.domain.form;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import org.joml.Matrix4f;

/**
 * Exposes the motion of the bone the player's upper body rides on — the <em>carrier</em> — so
 * the vanilla player can be moved by that exact transform and the halves read as one body.
 *
 * <p>Angle-only mimicry is not enough: the carrier's pivot is generally nowhere near the
 * player's waist (the Visionary rig's is over a block off to the side) and animations move its
 * <em>position</em> too, so copying the angle alone reads as the two halves shearing apart.
 */
public interface FormModel {

    /**
     * {@link #carrierDelta()} for a carrier reached by walking {@code chain} from the model root
     * (root-most first, the carrier last), so an animation that moves a <em>parent</em> of the
     * carrier is picked up too. Part scaling is deliberately ignored: a carrier that changed
     * scale would scale the player with it.
     */
    static Matrix4f carrierDelta(ModelPart... chain) {
        Matrix4f current = new Matrix4f();
        Matrix4f rest = new Matrix4f();
        for (ModelPart part : chain) {
            translateAndRotate(current, part.x, part.y, part.z, part.xRot, part.yRot, part.zRot);
            PartPose initial = part.getInitialPose();
            translateAndRotate(rest, initial.x(), initial.y(), initial.z(), initial.xRot(), initial.yRot(), initial.zRot());
        }
        // current maps carrier-local -> model space now, rest did at bake time, so
        // current * rest^-1 is what points glued to the carrier have moved by
        return current.mul(rest.invertAffine());
    }

    /**
     * Mirrors {@link ModelPart#translateAndRotate}: translate, then Z, then Y, then X.
     */
    private static void translateAndRotate(Matrix4f matrix, float x, float y, float z, float xRot, float yRot, float zRot) {
        matrix.translate(x / 16.0F, y / 16.0F, z / 16.0F);
        if (zRot != 0.0F) {
            matrix.rotateZ(zRot);
        }
        if (yRot != 0.0F) {
            matrix.rotateY(yRot);
        }
        if (xRot != 0.0F) {
            matrix.rotateX(xRot);
        }
    }

    /**
     * Rigid transform the carrier has moved through relative to its baked pose, in this model's
     * own space, block units ({@link ModelPart} coordinates divided by 16).
     */
    default Matrix4f carrierDelta() {
        return new Matrix4f();
    }
}
