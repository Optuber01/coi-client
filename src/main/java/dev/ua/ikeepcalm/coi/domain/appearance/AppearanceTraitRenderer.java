package dev.ua.ikeepcalm.coi.domain.appearance;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;

/**
 * One additive appearance trait. Implementations are stateless and shared across every player
 * wearing the trait, so nothing about a particular player may be cached in one.
 */
public interface AppearanceTraitRenderer {

    /**
     * Wire id the server grants this trait under.
     */

    String traitId();

    void submit(PoseStack poseStack, SubmitNodeCollector collector, AvatarRenderState state, PlayerModel model);
}
