package dev.ua.ikeepcalm.coi.domain.form;

import dev.ua.ikeepcalm.coi.util.duck.AvatarRenderStateAccessor;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import org.joml.Matrix4f;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Shared lookups for the partial mythical forms — see {@link PartialFormSpec}.
 */
public class PartialForms {

    /**
     * One query-only baked model per spec. Kept apart from {@link PartialFormLayer}'s render
     * instance because that one is only posed at draw time, long after the player's own body has
     * to be transformed to match it.
     */
    private static final Map<PartialFormSpec, Model<AvatarRenderState>> QUERY_MODELS = new ConcurrentHashMap<>();

    private static final Matrix4f IDENTITY = new Matrix4f();

    private PartialForms() {
    }

    /** Null when the player is in no form. */
    public static MythicalCreatureForm form(EntityRenderState state) {
        if (!(state instanceof AvatarRenderState avatarState)) {
            return null;
        }
        AvatarRenderStateAccessor identity = (AvatarRenderStateAccessor) avatarState;
        if (identity.coi$getPreviewForm() != null) {
            return MythicalFormManager.getRegisteredForm(identity.coi$getPreviewForm());
        }
        String playerUuid = identity.coi$getPlayerUuid();
        if (playerUuid == null) {
            return null;
        }
        return MythicalFormManager.getRegisteredForm(MythicalFormManager.getForm(playerUuid));
    }

    /** Null for full forms and for no form. */
    public static PartialFormSpec partial(EntityRenderState state) {
        MythicalCreatureForm form = form(state);
        return form == null ? null : form.partialForm();
    }

    /**
     * The transform to move the vanilla player by so it rides the baked model's carrier bone
     * rigidly, in player-model space (blocks: {@code ModelPart} coordinates divided by 16). Null
     * when the carrier has not moved from its baked pose — the whole idle animation.
     *
     * <p>{@link FormModel#carrierDelta()} is measured in the baked model's own space, so it is
     * conjugated by the placement that space is drawn through ({@code L}, from
     * {@link PartialFormSpec#modelToPlayer()}): {@code L * delta * L^-1}. That scales the
     * translation down by the render scale and moves the rotation pivot to where the player
     * actually sees it — the pivot is what angle-copying got wrong.
     */
    public static Matrix4f carrierTransform(PartialFormSpec spec, AvatarRenderState state) {
        Model<AvatarRenderState> query = QUERY_MODELS.computeIfAbsent(spec,
                s -> s.factory().apply(s.bodyLayer().get().bakeRoot()));
        query.setupAnim(state);
        if (!(query instanceof FormModel formModel)) {
            return null;
        }
        Matrix4f delta = formModel.carrierDelta();
        if (delta.equals(IDENTITY, 1.0E-5F)) {
            return null;
        }
        Matrix4f placement = spec.modelToPlayer();
        Matrix4f carrier = new Matrix4f(placement).mul(delta);
        return carrier.mul(placement.invertAffine());
    }
}
