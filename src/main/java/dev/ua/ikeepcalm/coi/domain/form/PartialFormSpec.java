package dev.ua.ikeepcalm.coi.domain.form;

import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Marks a {@link MythicalCreatureForm} as rendering a baked model alongside the vanilla player
 * rather than replacing it: head/torso/arms keep rendering, only the legs are hidden.
 *
 * @param bodyLayer bakes a <em>second</em>, query-only {@code ModelPart} tree, read by
 *                  {@link PartialForms} for {@link FormModel#carrierDelta()}. It has to be
 *                  separate from {@link PartialFormLayer}'s render instance, which is only posed
 *                  at draw time — well after the player's own body must be transformed to match.
 * @param scale     uniform scale applied before submission
 * @param offsetX   blocks (already-scaled output space) on X, recentring the model under the
 *                  player against whatever pivot the rig's root bone had in the authoring tool
 * @param offsetY   blocks on Y, <b>Y-down</b> (positive moves toward the ground)
 * @param offsetZ   blocks on Z, same purpose as {@code offsetX}
 * @param hipRaise  blocks the vanilla upper body is translated <b>up</b>; 0 when the model fits
 *                  inside the player's normal leg envelope. Applied to {@code submit()}'s
 *                  outermost pose stack, before {@code setupRotations}, which is empirically
 *                  <b>Y-up</b> — not the Y-down convention ModelPart offsets use further down.
 *                  It shifts the baked model by the same amount and, being outside the model's
 *                  own pose, carries the name tag up with it.
 */
public record PartialFormSpec(
        ModelLayerLocation layer,
        Identifier texture,
        Function<ModelPart, ? extends Model<AvatarRenderState>> factory,
        Supplier<LayerDefinition> bodyLayer,
        float scale,
        float offsetX,
        float offsetY,
        float offsetZ,
        float hipRaise
) {

    /**
     * The map from this model's own space into player-model space.
     * {@link PartialForms#carrierTransform} needs it to re-express a carrier motion measured in
     * model space as one the player's body can be moved by.
     */
    public Matrix4f modelToPlayer() {
        return new Matrix4f()
                .translate(offsetX, offsetY, offsetZ)
                .scale(scale);
    }
}
