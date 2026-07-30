package dev.ua.ikeepcalm.coi.client.mcf;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;

public interface MythicalCreatureForm extends Coi3dPrimitives {

    String getPathwayName();

    void render(AvatarRenderState state, PoseStack.Pose pose, VertexConsumer consumer);

}
