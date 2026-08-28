package dev.ua.ikeepcalm.coi.client.appearance.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.ua.ikeepcalm.coi.client.appearance.AppearanceTraitRenderer;
import dev.ua.ikeepcalm.coi.client.appearance.TraitGeometry;
import dev.ua.ikeepcalm.coi.client.config.AppearanceConfig;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;

/**
 * Colored irises with a normal or vertical-slit pupil, drawn as shallow 3D boxes so
 * they keep depth at grazing angles. Fullbright variants ignore scene lighting.
 * Size, spacing and vertical position are adjustable per skin via the settings screen.
 */
public final class EyeTraitRenderer implements AppearanceTraitRenderer {

    public enum Style {NORMAL, SLIT}

    private final String traitId;
    private final Style style;
    private final TraitGeometry.Tint iris;
    private final TraitGeometry.Tint pupil = new TraitGeometry.Tint(0.005f, 0.005f, 0.008f, 1.0f);
    private final boolean fullBright;

    public EyeTraitRenderer(String traitId, Style style, float red, float green, float blue, boolean fullBright) {
        this.traitId = traitId;
        this.style = style;
        this.iris = new TraitGeometry.Tint(red, green, blue, 1.0f);
        this.fullBright = fullBright;
    }

    @Override
    public String traitId() {
        return traitId;
    }

    @Override
    public void submit(PoseStack stack, SubmitNodeCollector collector, AvatarRenderState state, PlayerModel model) {
        stack.pushPose();
        model.head.translateAndRotate(stack);
        int light = fullBright ? TraitRenderSupport.FULL_BRIGHT : state.lightCoords;
        collector.order(3).submitCustomGeometry(
                stack,
                TraitRenderSupport.TRANSLUCENT,
                (pose, consumer) -> drawEyes(pose, consumer, light)
        );
        stack.popPose();
    }

    private void drawEyes(PoseStack.Pose pose, com.mojang.blaze3d.vertex.VertexConsumer consumer, int light) {
        TraitGeometry g = TraitGeometry.INSTANCE;
        AppearanceConfig.Settings settings = AppearanceConfig.get();
        float halfHeight = 0.62f * settings.eyeScale;
        float centerY = -3.10f + settings.eyeYOffsetPixels;
        for (float side : new float[]{-1.0f, 1.0f}) {
            float center = side * 1.55f * settings.eyeSpacing;
            float irisHalfWidth = 0.82f * settings.eyeScale;
            g.boxPixels(pose, consumer, center - irisHalfWidth, centerY - halfHeight, -4.24f,
                    center + irisHalfWidth, centerY + halfHeight, -4.02f, iris, light);
            float pupilHalfWidth = (style == Style.SLIT ? 0.12f : 0.36f) * settings.eyeScale;
            g.boxPixels(pose, consumer, center - pupilHalfWidth, centerY - halfHeight * 0.92f, -4.31f,
                    center + pupilHalfWidth, centerY + halfHeight * 0.92f, -4.23f, pupil, light);
        }
    }
}
