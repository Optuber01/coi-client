package dev.ua.ikeepcalm.coi.client.appearance.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.ua.ikeepcalm.coi.client.appearance.AppearanceTraitRenderer;
import dev.ua.ikeepcalm.coi.client.appearance.BodyBoxes;
import dev.ua.ikeepcalm.coi.client.appearance.FeminineBodyGeometry;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;

/**
 * Demoness feminine body profile: the chest bump on the player's own skin. The cat ears
 * that used to live here were split into {@link DemonessEarsRenderer} so they can be
 * toggled (and prioritized) independently of the body shape.
 */
public class FemaleTraitsRenderer implements AppearanceTraitRenderer {

    private static final FeminineBodyGeometry.ChestProfile PROFILE = FeminineBodyGeometry.ChestProfile.DEMONESS;

    @Override
    public String traitId() {
        return "female_traits";
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, AvatarRenderState state, PlayerModel model) {
        drawChestProfile(poseStack, collector, state, model, PROFILE);
    }

    /**
     * Shared by all body-shape traits. Chest armor owns the visible torso surface, so the
     * bump hides under chestplates and elytra instead of poking through them.
     */
    static void drawChestProfile(PoseStack poseStack, SubmitNodeCollector collector,
                                 AvatarRenderState state, PlayerModel model,
                                 FeminineBodyGeometry.ChestProfile profile) {
        if (!state.chestEquipment.isEmpty()) {
            return;
        }

        poseStack.pushPose();
        model.body.translateAndRotate(poseStack);
        collector.order(0).submitCustomGeometry(
                poseStack,
                BodyBoxes.skinType(state),
                (pose, consumer) -> FeminineBodyGeometry.drawChest(pose, consumer, state, profile, false)
        );
        if (state.showJacket) {
            collector.order(0).submitCustomGeometry(
                    poseStack,
                    BodyBoxes.skinType(state),
                    (pose, consumer) -> FeminineBodyGeometry.drawChest(pose, consumer, state, profile, true)
            );
        }
        poseStack.popPose();
    }
}
