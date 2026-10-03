package dev.ua.ikeepcalm.coi.domain.form;

import dev.ua.ikeepcalm.coi.domain.form.model.VisionaryLowerModel;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.Identifier;

/**
 * Registration and placement of the baked models used by partial creature forms. The placement
 * constants were tuned by eye — <b>read the comment on them before changing one</b>; several
 * sit in different coordinate spaces and do not mean what their names suggest in isolation.
 */
public class FormModelLayers {

    public static final ModelLayerLocation VISIONARY_LOWER =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath("coi-client", "visionary_lower"), "main");

    // hipRaise shifts the player's body AND (PartialFormLayer inherits the same un-popped pose
    // stack) the dragon by the same absolute amount, so it cancels out of the relationship
    // between them. Two independent knobs follow:
    //   - hipRaise: how high the whole assembly floats off the ground. The hitbox bottom stays
    //               pinned to the true ground, so this must be at least the dragon's scaled leg
    //               length or the legs go through the floor — inherently a bigger number than it
    //               looks. Positive = up: it is applied to submit()'s outermost pose stack,
    //               pre-setupRotations, which is empirically Y-up, unlike the Y-down convention
    //               ModelPart offsets use. Got this backwards once; don't re-flip untested.
    //   - offsetY:  how the dragon sits relative to the player, unaffected by hipRaise.
    // All four also feed PartialFormSpec#modelToPlayer, so changing one shifts the walk-cycle
    // sway to match rather than needing a second round of tuning.
    public static final PartialFormSpec VISIONARY_LOWER_SPEC = new PartialFormSpec(
            VISIONARY_LOWER,
            Identifier.fromNamespaceAndPath("coi-client", "textures/entity/forms/visionary_lower.png"),
            VisionaryLowerModel::new,
            VisionaryLowerModel::createBodyLayer,
            0.12F,
            0.03F,
            2.6F,
            1.7F,
            1.2F
    );

    private FormModelLayers() {
    }

    public static void register() {
        ModelLayerRegistry.registerModelLayer(VISIONARY_LOWER, VisionaryLowerModel::createBodyLayer);
    }
}
