package dev.ua.ikeepcalm.coi.client.screen;

import dev.ua.ikeepcalm.coi.client.ClientAppearanceState;
import dev.ua.ikeepcalm.coi.client.appearance.AppearanceTraits;
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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Appearance customization. The right column only shows sections relevant to the
 * traits the player currently has enabled (chest knobs appear with a body figure,
 * hair knobs with a hair trait, and so on), laid out as one aligned column with
 * scroll support. The preview is fully drag-rotatable with snap buttons and zoom.
 */
public final class AppearanceSettingsScreen extends Screen {

    private static final int HEADER_H = 16;
    private static final int BUTTON_H = 20;
    private static final int BUTTON_GAP = 4;

    private final Screen parent;
    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;
    private int previewLeft;
    private int previewWidth;
    private int contentX;
    private int contentW;
    private float previewZoom = 1.0f;
    private float previewYaw;
    private float previewPitch;
    private boolean draggingPreview;
    private double scrollOffset;

    /** One row of the settings column: a section header or a single control. */
    private record Element(int height, Supplier<Component> label, Runnable action) {

        static Element header(String translationKey) {
            return new Element(HEADER_H, () -> Component.translatable(translationKey), null);
        }

        static Element control(Supplier<Component> label, Runnable action) {
            return new Element(BUTTON_H + BUTTON_GAP, label, action);
        }

        boolean isHeader() {
            return action == null;
        }
    }

    public AppearanceSettingsScreen(Screen parent) {
        super(Component.translatable("screen.coi.appearance_settings"));
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
        previewWidth = Math.clamp(Math.round(panelW * 0.36f), 96, 200);
        contentX = previewLeft + previewWidth + 12;
        contentW = panelX + panelW - 12 - contentX;

        List<Element> elements = buildElements();
        int listTop = panelY + 44;
        int listBottom = panelY + panelH - 34;
        int visibleHeight = listBottom - listTop;

        int totalHeight = 0;
        for (Element element : elements) {
            totalHeight += element.height();
        }
        scrollOffset = Math.clamp(scrollOffset, 0, Math.max(0, totalHeight - visibleHeight));

        // Materialize buttons only for rows intersecting the visible window
        int y = listTop - (int) scrollOffset;
        for (Element element : elements) {
            int rowTop = y;
            int rowBottom = y + element.height();
            y = rowBottom;
            if (rowBottom < listTop || rowTop > listBottom || element.isHeader()) {
                continue;
            }
            Button button = Button.builder(element.label().get(), clicked -> {
                element.action().run();
                AppearanceConfig.save();
                rebuildWidgets();
            }).bounds(contentX, rowTop, contentW, BUTTON_H).build();
            addRenderableWidget(button);
        }

        int navY = panelY + panelH - 28;
        int gap = 6;
        int navW = (contentW - gap * 2) / 3;
        addRenderableWidget(Button.builder(
                Component.translatable("screen.coi.appearance.reset").withStyle(ChatFormatting.RED), clicked -> {
            AppearanceConfig.reset();
            rebuildWidgets();
        }).bounds(contentX, navY, navW, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.coi.appearance.front"), clicked -> resetPreview())
                .bounds(contentX + navW + gap, navY, navW, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), clicked -> onClose())
                .bounds(contentX + (navW + gap) * 2, navY, navW, 20).build());

        addPreviewControls();
    }

    /**
     * Sections are gated on the local player's active traits so only relevant controls
     * show up: chest knobs need a body figure, hair knobs need a hair trait, wing knobs
     * need wings, the opacity knob needs a hide overlay.
     */
    private List<Element> buildElements() {
        AppearanceConfig.Settings settings = AppearanceConfig.get();
        Set<String> active = minecraft.player != null
                ? ClientAppearanceState.getTraits(minecraft.player.getUUID().toString())
                : Set.of();
        Set<String> families = activeFamilies(active);

        List<Element> elements = new ArrayList<>();
        addGeneralElements(elements, settings);
        addBodyElements(elements, settings, families);
        addHairElements(elements, settings, families);
        addWingElements(elements, settings, families);
        addSkinElements(elements, settings, families);
        if (active.isEmpty()) {
            elements.add(Element.header("screen.coi.appearance.no_traits"));
        }
        addUniquenessElements(elements, settings);
        return elements;
    }

    private static void addGeneralElements(List<Element> elements, AppearanceConfig.Settings settings) {
        elements.add(Element.header("screen.coi.appearance.section.general"));
        elements.add(toggleControl("screen.coi.appearance.traits_enabled",
                () -> settings.enabled, value -> settings.enabled = value));
        elements.add(toggleControl("screen.coi.appearance.show_self",
                () -> settings.showSelf, value -> settings.showSelf = value));
        elements.add(toggleControl("screen.coi.appearance.show_others",
                () -> settings.showOthers, value -> settings.showOthers = value));
        elements.add(toggleControl("screen.coi.appearance.show_body_changes",
                () -> settings.showBodyChanges, value -> settings.showBodyChanges = value));
    }

    private static void addBodyElements(List<Element> elements, AppearanceConfig.Settings settings,
                                        Set<String> families) {
        if (!families.contains("body")) {
            return;
        }
        elements.add(Element.header("screen.coi.appearance.section.chest"));
        elements.add(cycleControl("screen.coi.appearance.size",
                () -> settings.chestScale, value -> settings.chestScale = value, CHEST_SIZE));
        elements.add(pixelControl("screen.coi.appearance.separation",
                () -> settings.chestSeparationPixels, value -> settings.chestSeparationPixels = value, SEPARATION));
        elements.add(pixelControl("screen.coi.appearance.vertical_position",
                () -> settings.chestYOffsetPixels, value -> settings.chestYOffsetPixels = value, VERTICAL));
        elements.add(cycleControl("screen.coi.appearance.roundness",
                () -> settings.chestFullness, value -> settings.chestFullness = value, FULLNESS));
        elements.add(toggleControl("screen.coi.appearance.project_jacket",
                () -> settings.projectJacket, value -> settings.projectJacket = value));
    }

    private static void addHairElements(List<Element> elements, AppearanceConfig.Settings settings,
                                        Set<String> families) {
        if (!families.contains("hair")) {
            return;
        }
        elements.add(Element.header("screen.coi.appearance.section.hair"));
        elements.add(cycleControl("screen.coi.appearance.length",
                () -> settings.hairLength, value -> settings.hairLength = value, HAIR_LENGTH));
        elements.add(pixelControl("screen.coi.appearance.vertical_position",
                () -> settings.hairYOffsetPixels, value -> settings.hairYOffsetPixels = value, HALF_PX));
    }

    private static void addWingElements(List<Element> elements, AppearanceConfig.Settings settings,
                                        Set<String> families) {
        if (!families.contains("wings")) {
            return;
        }
        elements.add(Element.header("screen.coi.appearance.section.wings"));
        elements.add(cycleControl("screen.coi.appearance.scale",
                () -> settings.wingScale, value -> settings.wingScale = value, SCALE));
        elements.add(cycleControl("screen.coi.appearance.flap_speed",
                () -> settings.wingFlapSpeed, value -> settings.wingFlapSpeed = value, FLAP_SPEED));
    }

    private static void addSkinElements(List<Element> elements, AppearanceConfig.Settings settings,
                                        Set<String> families) {
        if (!families.contains("skin") && !families.contains("chained")) {
            return;
        }
        elements.add(Element.header("screen.coi.appearance.section.skin"));
        elements.add(cycleControl("screen.coi.appearance.opacity",
                () -> settings.overlayOpacity, value -> settings.overlayOpacity = value, OPACITY));
    }

    private static void addUniquenessElements(List<Element> elements, AppearanceConfig.Settings settings) {
        elements.add(Element.header("screen.coi.appearance.section.uniqueness"));
        elements.add(toggleControl("screen.coi.appearance.uniqueness_enabled",
                () -> settings.enableUniquenessEffects, value -> settings.enableUniquenessEffects = value));
        elements.add(toggleControl("screen.coi.appearance.uniqueness_self",
                () -> settings.uniquenessShowSelf, value -> settings.uniquenessShowSelf = value));
        elements.add(toggleControl("screen.coi.appearance.uniqueness_others",
                () -> settings.uniquenessShowOthers, value -> settings.uniquenessShowOthers = value));
    }

    private static Set<String> activeFamilies(Set<String> activeTraits) {
        Set<String> families = new HashSet<>();
        for (String traitId : activeTraits) {
            AppearanceTraits.Family family = AppearanceTraits.familyOf(traitId);
            if (family != null) {
                families.add(family.id());
            }
        }
        return families;
    }

    private void addPreviewControls() {
        int gap = 3;
        int available = previewWidth - 8;
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

    private static final float[] CHEST_SIZE = {0.80f, 0.95f, 1.12f, 1.25f, 1.40f, 1.50f};
    private static final float[] SEPARATION = {-0.40f, -0.15f, 0.15f, 0.45f, 0.70f, 1.0f};
    private static final float[] VERTICAL = {-1.5f, -0.75f, 0.0f, 0.75f, 1.5f};
    private static final float[] FULLNESS = {0.75f, 0.90f, 1.0f, 1.10f, 1.22f, 1.35f};
    private static final float[] HAIR_LENGTH = {0.50f, 0.75f, 1.0f, 1.2f, 1.4f, 1.6f};
    private static final float[] HALF_PX = {-1.0f, -0.5f, 0.0f, 0.5f, 1.0f};
    private static final float[] SCALE = {0.6f, 0.8f, 1.0f, 1.2f, 1.4f};
    private static final float[] FLAP_SPEED = {0.2f, 0.5f, 1.0f, 1.5f, 2.0f, 3.0f};
    private static final float[] OPACITY = {0.2f, 0.35f, 0.5f, 0.65f, 0.8f, 1.0f};

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor graphics,
                                   int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0x90000000);
        CoiStyle.drawCard(graphics, panelX, panelY, panelW, panelH);
        graphics.fill(previewLeft, panelY + 42, previewLeft + previewWidth, panelY + panelH - 18, 0xAA050507);

        if (minecraft.player != null) {
            int previewScale = Math.round(Math.min(
                    Math.min(72, Math.max(42, (panelH - 86) / 3)),
                    Math.max(30, previewWidth / 3)) * previewZoom);
            extractPreviewEntity(graphics,
                    previewLeft + 4,
                    panelY + 48,
                    previewLeft + previewWidth - 4,
                    panelY + panelH - 48,
                    previewScale,
                    previewYaw,
                    previewPitch,
                    minecraft.player);
        }

        graphics.centeredText(font, title.copy().withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                width / 2, panelY + 12, 0xFFFFFFFF);
        graphics.centeredText(font,
                Component.translatable("screen.coi.appearance_settings.hint")
                        .withStyle(ChatFormatting.GRAY),
                contentX + contentW / 2, panelY + 28, 0xFFAAAAAA);

        // Section headers behind the buttons (buttons are widgets and draw on top)
        List<Element> elements = buildElements();
        int listTop = panelY + 44;
        int y = listTop - (int) scrollOffset;
        for (Element element : elements) {
            int rowTop = y;
            y += element.height();
            if (element.isHeader() && rowTop >= listTop - HEADER_H && rowTop <= panelY + panelH - 34) {
                graphics.text(font, element.label().get().copy().withStyle(ChatFormatting.GOLD),
                        contentX + 2, rowTop + 3, 0xFFFFFFFF);
            }
        }

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
        if (mouseX >= contentX && mouseX <= contentX + contentW
                && mouseY >= panelY + 44 && mouseY <= panelY + panelH - 34
                && verticalAmount != 0.0) {
            scrollOffset -= verticalAmount * 24.0;
            rebuildWidgets();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    private boolean insidePreview(double x, double y) {
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
        state.shadowPieces.clear();
        state.outlineColor = 0;
        if (state instanceof LivingEntityRenderState living) {
            living.bodyRot = 180.0f + yawDegrees;
            living.yRot = yawDegrees;
            living.xRot = -pitchDegrees;
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
        minecraft.gui.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static Element toggleControl(String nameKey, BoolGetter getter, BoolSetter setter) {
        return Element.control(
                () -> Component.translatable(
                        getter.get() ? "screen.coi.appearance.toggle_on" : "screen.coi.appearance.toggle_off",
                        Component.translatable(nameKey)),
                () -> setter.set(!getter.get()));
    }

    private static Element cycleControl(String nameKey, FloatGetter getter, FloatSetter setter, float[] values) {
        return Element.control(
                () -> Component.translatable("screen.coi.appearance.percent_value",
                        Component.translatable(nameKey), Math.round(getter.get() * 100.0f)),
                () -> setter.set(nextValue(getter.get(), values)));
    }

    private static Element pixelControl(String nameKey, FloatGetter getter, FloatSetter setter, float[] values) {
        return Element.control(
                () -> Component.translatable("screen.coi.appearance.pixel_value",
                        Component.translatable(nameKey), String.format(Locale.ROOT, "%+.2f", getter.get())),
                () -> setter.set(nextValue(getter.get(), values)));
    }

    private static float nextValue(float current, float[] values) {
        for (int index = 0; index < values.length; index++) {
            if (Math.abs(values[index] - current) < 0.01f) return values[(index + 1) % values.length];
            if (values[index] > current) return values[index];
        }
        return values[0];
    }

    @FunctionalInterface
    private interface FloatGetter {
        float get();
    }

    @FunctionalInterface
    private interface FloatSetter {
        void set(float value);
    }

    @FunctionalInterface
    private interface BoolGetter {
        boolean get();
    }

    @FunctionalInterface
    private interface BoolSetter {
        void set(boolean value);
    }
}
