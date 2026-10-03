package dev.ua.ikeepcalm.coi.domain.form;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;

/**
 * A form either replaces the player outright — {@link #render} draws procedural geometry and
 * the vanilla render is cancelled — or, when {@link #partialForm} is non-null, contributes a
 * baked lower body while the player's own head, torso and arms keep rendering.
 */
public interface MythicalCreatureForm extends FormPrimitives {

    String getPathwayName();

    void render(AvatarRenderState state, PoseStack.Pose pose, VertexConsumer consumer);

    /**
     * When present, {@link #render} is not called.
     */
    default PartialFormSpec partialForm() {
        return null;
    }

}
