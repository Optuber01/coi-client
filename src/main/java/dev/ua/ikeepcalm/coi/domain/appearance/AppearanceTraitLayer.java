package dev.ua.ikeepcalm.coi.domain.appearance;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.ua.ikeepcalm.coi.domain.appearance.trait.FemaleTraitsRenderer;
import dev.ua.ikeepcalm.coi.domain.appearance.trait.HornsTraitRenderer;
import dev.ua.ikeepcalm.coi.domain.appearance.trait.MushroomTraitRenderer;
import dev.ua.ikeepcalm.coi.domain.beyonder.model.AppearanceState;
import dev.ua.ikeepcalm.coi.domain.form.MythicalCreatureForm;
import dev.ua.ikeepcalm.coi.domain.form.PartialForms;
import dev.ua.ikeepcalm.coi.util.duck.AvatarRenderStateAccessor;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.jspecify.annotations.NonNull;

import java.util.List;

/**
 * Attached to every player renderer by {@code AvatarRendererMixin}.
 */
public class AppearanceTraitLayer extends RenderLayer<AvatarRenderState, PlayerModel> {

    private static final List<AppearanceTraitRenderer> TRAIT_RENDERERS = List.of(
            new FemaleTraitsRenderer(),
            new HornsTraitRenderer(),
            new MushroomTraitRenderer()
    );

    public AppearanceTraitLayer(RenderLayerParent<AvatarRenderState, PlayerModel> renderer) {
        super(renderer);
    }

    @Override
    public void submit(@NonNull PoseStack poseStack, @NonNull SubmitNodeCollector collector, int light, @NonNull AvatarRenderState state, float yRot, float xRot) {
        String playerUuid = ((AvatarRenderStateAccessor) state).coi$getPlayerUuid();
        if (playerUuid == null || state.isInvisible) {
            return;
        }

        MythicalCreatureForm form = PartialForms.form(state);
        if (form != null && form.partialForm() == null) {
            return;
        }

        PlayerModel model = getParentModel();
        for (AppearanceTraitRenderer renderer : TRAIT_RENDERERS) {
            if (AppearanceState.hasTrait(playerUuid, renderer.traitId())) {
                renderer.submit(poseStack, collector, state, model);
            }
        }
    }
}
