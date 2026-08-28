package dev.ua.ikeepcalm.coi.client.appearance.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.ua.ikeepcalm.coi.client.appearance.AppearanceTraitRenderer;
import dev.ua.ikeepcalm.coi.client.appearance.FeminineBodyGeometry;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;

/**
 * Base for the body-shape traits that differ only by their chest profile knobbing.
 */
public abstract class AbstractChestProfileRenderer implements AppearanceTraitRenderer {

    protected abstract FeminineBodyGeometry.ChestProfile profile();

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, AvatarRenderState state, PlayerModel model) {
        FemaleTraitsRenderer.drawChestProfile(poseStack, collector, state, model, profile());
    }
}
