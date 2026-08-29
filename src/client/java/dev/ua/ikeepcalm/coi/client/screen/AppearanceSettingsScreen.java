package dev.ua.ikeepcalm.coi.client.screen;

import dev.ua.ikeepcalm.coi.client.config.AppearanceConfig;
import dev.ua.ikeepcalm.coi.util.CoiStyle;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import org.jspecify.annotations.NonNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Appearance customization: visibility switches plus per-element fit knobs (chest
 * shape, hair length/offset, eye fit, wing scale) so traits can be tuned to the
 * player's own skin. The preview is fully drag-rotatable (front to back), with
 * snap buttons and scroll zoom.
 */
public final class AppearanceSettingsScreen extends Screen {

    private static final int PAGES = 3;

    private static final float[] CHEST_SIZE = {0.80f, 0.95f, 1.12f, 1.25f, 1.40f, 1.50f};
    private static final float[] SEPARATION = {-0.40f, -0.15f, 0.15f, 0.45f, 0.70f, 1.0f};
    private static final float[] VERTICAL = {-1.5f, -0.75f, 0.0f, 0.75f, 1.5f};
    private static final float[] FULLNESS = {0.75f, 0.90f, 1.0f, 1.10f, 1.22f, 1.35f};
    private static final float[] HAIR_LENGTH = {0.50f, 0.75f, 1.0f, 1.2f, 1.4f, 1.6f};
    private static final float[] HALF_PX = {-1.0f, -0.5f, 0.0f, 0.5f, 1.0f};
    private static final float[] SCALE = {0.6f, 0.8f, 1.0f, 1.2f, 1.4f};
    private static final float[] EYE_SPACING = {0.7f, 0.85f, 1.0f, 1.15f, 1.3f};

    private final Screen parent;
    private int page;
    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;
    private int previewLeft;
    private int previewRight;
    private int contentX;
    private int contentW;
    private float previewZoom = 1.0f;
    private float previewYaw;
    private float previewPitch;
    private boolean draggingPreview;

    public AppearanceSettingsScreen(Screen parent) {
        super(Component.literal("Appearance Settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        clearWidgets();
        panelW = Math.min(600, Math.max(260, width - 16));
        panelH = Math.min(330, Math.max(190, height - 16));
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2;

        previewLeft = panelX + 12;
        int previewWidth = Math.clamp(Math.round(panelW * 0.38f), 100, 214);
        int previewRight = previewLeft + previewWidth;
        contentX = previewRight + 12;
        contentW = panelX + panelW - 12 - contentX;

        List<SettingEntry> entries = entriesForPage();
        int top = panelY + 58;
        int bottom = panelY + panelH - 34;
        int rowStep = Math.clamp((bottom - top - 4) / Math.max(1, entries.size()), 18, 25);
        int buttonHeight = Math.min(20, rowStep - 2);
        for (int index = 0; index < entries.size(); index++) {
            SettingEntry entry = entries.get(index);
            Button button = Button.builder(Component.literal(entry.label.get()), clicked -> {
                entry.action.run();
                AppearanceConfig.save();
                clicked.setMessage(Component.literal(entry.label.get()));
            }).bounds(contentX, top + index * rowStep, contentW, buttonHeight).build();
            addRenderableWidget(button);
        }

        int navY = panelY + panelH - 28;
        int gap = 4;
        int navW = (contentW - gap * 3) / 4;
        addRenderableWidget(Button.builder(Component.literal("◀ Page " + (page + 1) + "/" + PAGES), clicked -> {
            page = Math.floorMod(page + 1, PAGES);
            rebuildWidgets();
        }).bounds(contentX, navY, navW, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Reset").withStyle(ChatFormatting.RED), clicked -> {
            AppearanceConfig.reset();
            rebuildWidgets();
        }).bounds(contentX + navW + gap, navY, navW, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Front"), clicked -> resetPreview())
                .bounds(contentX + (navW + gap) * 2, navY, navW, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), clicked -> onClose())
                .bounds(contentX + (navW + gap) * 3, navY, navW, 20).build());

        addPreviewControls(previewRight);
    }

    private void addPreviewControls(int previewRight) {
        int gap = 3;
        int available = previewRight - previewLeft - 8;
        int buttonW = (available - gap * 4) / 5;
        int x = previewLeft + 4;
        int y = panelY + panelH - 42;
        addRenderableWidget(Button.builder(Component.literal("<"), clicked -> previewYaw -= 45.0f)
                .bounds(x, y, buttonW, 20).build());
        addRenderableWidget(Button.builder(Component.literal(">"), clicked -> previewYaw += 45.0f)
                .bounds(x + buttonW + gap, y, buttonW, 20).build());
        addRenderableWidget(Button.builder(Component.literal("^"), clicked ->
                        previewPitch = Math.clamp(previewPitch - 10.0f, -60.0f, 60.0f))
                .bounds(x + (buttonW + gap) * 2, y, buttonW, 20).build());
        addRenderableWidget(Button.builder(Component.literal("v"), clicked ->
                        previewPitch = Math.clamp(previewPitch + 10.0f, -60.0f, 60.0f))
                .bounds(x + (buttonW + gap) * 3, y, buttonW, 20).build());
        addRenderableWidget(Button.builder(Component.literal("±"), clicked -> {
            previewZoom += 0.2f;
            if (previewZoom > 1.6f) previewZoom = 0.6f;
        }).bounds(x + (buttonW + gap) * 4, y, buttonW, 20).build());
    }

    private List<SettingEntry> entriesForPage() {
        AppearanceConfig.Settings settings = AppearanceConfig.get();
        List<SettingEntry> entries = new ArrayList<>();
        switch (page) {
            case 0 -> { // Visibility
                entries.add(toggle("All appearance rendering", () -> settings.enabled,
                        value -> settings.enabled = value));
                entries.add(toggle("Show on yourself", () -> settings.showSelf,
                        value -> settings.showSelf = value));
                entries.add(toggle("Show on other players", () -> settings.showOthers,
                        value -> settings.showOthers = value));
                entries.add(toggle("Body enhancements (skin, scales, physique)", () -> settings.showBodyChanges,
                        value -> settings.showBodyChanges = value));
                entries.add(toggle("Project jacket pixels", () -> settings.projectJacket,
                        value -> settings.projectJacket = value));
            }
            case 1 -> { // Chest + hair fit
                entries.add(cyclePercent("Chest size", () -> settings.chestScale,
                        value -> settings.chestScale = value, CHEST_SIZE));
                entries.add(cyclePixels("Chest separation", () -> settings.chestSeparationPixels,
                        value -> settings.chestSeparationPixels = value, SEPARATION));
                entries.add(cyclePixels("Chest vertical position", () -> settings.chestYOffsetPixels,
                        value -> settings.chestYOffsetPixels = value, VERTICAL));
                entries.add(cyclePercent("Chest roundness / fullness", () -> settings.chestFullness,
                        value -> settings.chestFullness = value, FULLNESS));
                entries.add(cyclePercent("Hair length", () -> settings.hairLength,
                        value -> settings.hairLength = value, HAIR_LENGTH));
                entries.add(cyclePixels("Hair vertical position", () -> settings.hairYOffsetPixels,
                        value -> settings.hairYOffsetPixels = value, HALF_PX));
            }
            default -> { // Eyes, wings, uniqueness
                entries.add(cyclePercent("Eye size", () -> settings.eyeScale,
                        value -> settings.eyeScale = value, SCALE));
                entries.add(cyclePercent("Eye spacing", () -> settings.eyeSpacing,
                        value -> settings.eyeSpacing = value, EYE_SPACING));
                entries.add(cyclePixels("Eye vertical position", () -> settings.eyeYOffsetPixels,
                        value -> settings.eyeYOffsetPixels = value, HALF_PX));
                entries.add(cyclePercent("Wing scale", () -> settings.wingScale,
                        value -> settings.wingScale = value, SCALE));
                entries.add(toggle("Uniqueness effects", () -> settings.enableUniquenessEffects,
                        value -> settings.enableUniquenessEffects = value));
                entries.add(toggle("Uniqueness: show on yourself", () -> settings.uniquenessShowSelf,
                        value -> settings.uniquenessShowSelf = value));
                entries.add(toggle("Uniqueness: show on others", () -> settings.uniquenessShowOthers,
                        value -> settings.uniquenessShowOthers = value));
            }
        }
        return entries;
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor graphics,
                                   int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0x90000000);
        CoiStyle.drawCard(graphics, panelX, panelY, panelW, panelH);
        int previewWidth = Math.clamp(Math.round(panelW * 0.38f), 100, 214);
        int previewRight = previewLeft + previewWidth;
        graphics.fill(previewLeft, panelY + 42, previewRight, panelY + panelH - 18, 0xAA050507);

        if (minecraft.player != null) {
            int previewScale = Math.round(Math.min(
                    Math.min(72, Math.max(42, (panelH - 86) / 3)),
                    Math.max(30, (previewRight - previewLeft) / 3)) * previewZoom);
            extractPreviewEntity(graphics,
                    previewLeft + 4,
                    panelY + 48,
                    previewRight - 4,
                    panelY + panelH - 48,
                    previewScale,
                    previewYaw,
                    previewPitch,
                    minecraft.player);
        }

        graphics.centeredText(font, title.copy().withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                width / 2, panelY + 12, 0xFFFFFFFF);
        String subtitle = switch (page) {
            case 0 -> "Visibility and skin layer";
            case 1 -> "Chest and hair fit";
            default -> "Eyes, wings and uniqueness";
        };
        graphics.centeredText(font,
                Component.literal(subtitle + " — drag the preview to rotate, scroll to zoom")
                        .withStyle(ChatFormatting.GRAY),
                contentX + contentW / 2, panelY + 36, 0xFFAAAAAA);

        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) return true;
        if (event.button() == 0 && insidePreview(event.x(), event.y())) {
            draggingPreview = true;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (draggingPreview && event.button() == 0) {
            previewYaw += (float) dragX * 1.5f;
            previewPitch = Math.clamp(previewPitch + (float) dragY, -60.0f, 60.0f);
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (draggingPreview && event.button() == 0) {
            draggingPreview = false;
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY,
                                 double horizontalAmount, double verticalAmount) {
        if (insidePreview(mouseX, mouseY) && verticalAmount != 0.0) {
            previewZoom = Math.clamp(previewZoom + (verticalAmount > 0 ? 0.1f : -0.1f), 0.6f, 1.6f);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    private boolean insidePreview(double x, double y) {
        int previewWidth = Math.clamp(Math.round(panelW * 0.38f), 100, 214);
        return x >= previewLeft && x <= previewLeft + previewWidth
                && y >= panelY + 42 && y <= panelY + panelH - 18;
    }

    private void resetPreview() {
        previewYaw = 0.0f;
        previewPitch = 0.0f;
        previewZoom = 1.0f;
    }

    /**
     * Fixed-angle variant of the inventory player preview: unlike the mouse-following
     * helper (which caps yaw at ~±31°), this draws the entity at an explicit yaw/pitch
     * so the preview can spin the full 360°. Mirrors the vanilla extraction math —
     * the model is flipped on Z and the living render state's rotations are set directly.
     */
    private static void extractPreviewEntity(GuiGraphicsExtractor graphics,
                                             int x1, int y1, int x2, int y2, int scale,
                                             float yawDegrees, float pitchDegrees,
                                             LivingEntity entity) {
        Minecraft minecraft = Minecraft.getInstance();
        EntityRenderState state = minecraft.getEntityRenderDispatcher().extractEntity(entity, 1.0f);
        if (state instanceof LivingEntityRenderState living) {
            living.bodyRot = 180.0f + yawDegrees;
            living.yRot = yawDegrees;
            living.xRot = pitchDegrees;
            living.boundingBoxWidth /= living.scale;
            living.boundingBoxHeight /= living.scale;
            living.scale = 1.0f;
        }
        Quaternionf flip = new Quaternionf().rotateZ((float) Math.PI);
        Quaternionf pitch = new Quaternionf().rotateX(pitchDegrees * ((float) Math.PI / 180.0f));
        flip.mul(pitch);
        Vector3f translation = new Vector3f(0.0f, state.boundingBoxHeight / 2.0f + 0.0625f, 0.0f);
        graphics.entity(state, scale, translation, flip, pitch, x1, y1, x2, y2);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static SettingEntry toggle(String name, BoolGetter getter, BoolSetter setter) {
        return new SettingEntry(() -> (getter.get() ? "ON  " : "OFF  ") + name,
                () -> setter.set(!getter.get()));
    }

    private static SettingEntry cyclePixels(String name, FloatGetter getter,
                                            FloatSetter setter, float[] values) {
        return new SettingEntry(() -> String.format("%s: %+.2f px", name, getter.get()),
                () -> setter.set(nextValue(getter.get(), values)));
    }

    private static SettingEntry cyclePercent(String name, FloatGetter getter,
                                             FloatSetter setter, float[] values) {
        return new SettingEntry(() -> String.format("%s: %.0f%%", name, getter.get() * 100.0f),
                () -> setter.set(nextValue(getter.get(), values)));
    }

    private static float nextValue(float current, float[] values) {
        for (int index = 0; index < values.length; index++) {
            if (Math.abs(values[index] - current) < 0.01f) return values[(index + 1) % values.length];
            if (values[index] > current) return values[index];
        }
        return values[0];
    }

    private record SettingEntry(Supplier<String> label, Runnable action) {
    }

    @FunctionalInterface
    private interface BoolGetter {
        boolean get();
    }

    @FunctionalInterface
    private interface BoolSetter {
        void set(boolean value);
    }

    @FunctionalInterface
    private interface FloatGetter {
        float get();
    }

    @FunctionalInterface
    private interface FloatSetter {
        void set(float value);
    }
}
