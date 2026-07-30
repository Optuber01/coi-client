package dev.ua.ikeepcalm.coi.client.appearance;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.ua.ikeepcalm.coi.client.ClientAppearanceState;
import dev.ua.ikeepcalm.coi.client.appearance.renderers.FemaleTraitsRenderer;
import dev.ua.ikeepcalm.coi.client.appearance.renderers.HornsTraitRenderer;
import dev.ua.ikeepcalm.coi.client.appearance.renderers.MushroomTraitRenderer;
import dev.ua.ikeepcalm.coi.client.mcf.AvatarRenderStateAccessor;
import dev.ua.ikeepcalm.coi.client.mcf.MythicalFormManager;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.jspecify.annotations.NonNull;

import java.util.List;

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
    public void submit(@NonNull PoseStack poseStack, @NonNull SubmitNodeCollector collector, int light, AvatarRenderState state, float yRot, float xRot) {
        String playerUuid = ((AvatarRenderStateAccessor) state).coi$getPlayerUuid();
        if (playerUuid == null
                || state.isInvisible
                || MythicalFormManager.getForm(playerUuid) != null) {
            return;
        }

        PlayerModel model = getParentModel();
        for (AppearanceTraitRenderer renderer : TRAIT_RENDERERS) {
            if (ClientAppearanceState.hasTrait(playerUuid, renderer.traitId())) {
                renderer.submit(poseStack, collector, state, model);
            }
        }
    }
}
