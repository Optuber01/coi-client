package dev.ua.ikeepcalm.coi.hud.layout;

import dev.ua.ikeepcalm.coi.config.HudConfig;
import dev.ua.ikeepcalm.coi.domain.ability.model.Pathways;
import dev.ua.ikeepcalm.coi.hud.HudAnchor;
import dev.ua.ikeepcalm.coi.hud.HudScale;
import dev.ua.ikeepcalm.coi.hud.overlay.*;
import dev.ua.ikeepcalm.coi.hud.render.HealthStyle;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.Locale;

/**
 * Every bar-and-overlay descriptor the layout editor positions, plus the two bases they are
 * built on. The draw order is {@link HudElements}' and the {@code id} strings are
 * {@link ElementIds}', which is what {@code config/coi_hud.json} and the <em>Align</em> buttons
 * are written against.
 */
public final class BarElements {

    private BarElements() {
    }

    public abstract static class AbstractElement implements HudElement {

        private final String id;

        protected AbstractElement(String id) {
            this.id = id;
        }

        @Override
        public final String id() {
            return id;
        }
    }

    /**
     * A 182-wide bar with its label 10px above it. Every pad goes through
     * {@link HudScale#size}, because {@link #bounds} and {@link #moveTo} have to stay
     * <b>exact inverses</b> at any {@link #scale} — an unscaled pad makes the element jump the
     * moment it is dragged.
     */
    public abstract static class BarElement extends AbstractElement {

        private final int barHeight;

        protected BarElement(String id, int barHeight) {
            super(id);
            this.barHeight = barHeight;
        }

        /**
         * Fill origin, already resolved against the scaled bar width.
         */
        protected abstract int[] fillOrigin(int w, int h, HudConfig.HudSettings s);

        protected abstract void writeAnchor(HudConfig.HudSettings s, String anchor);

        protected abstract void writeOffsets(HudConfig.HudSettings s, int x, int y);

        protected abstract float scale(HudConfig.HudSettings s);

        protected final int barWidth() {
            return MadnessOverlay.BAR_WIDTH;
        }

        protected final int boundsHeight() {
            return LayoutGeometry.BAR_LABEL_PAD + barHeight + LayoutGeometry.BAR_FRAME_PAD;
        }

        @Override
        public int[] bounds(int w, int h, HudConfig.HudSettings s) {
            int[] pos = fillOrigin(w, h, s);
            float scale = scale(s);
            return new int[]{pos[0] - HudScale.size(LayoutGeometry.BAR_FRAME_PAD, scale),
                    pos[1] - HudScale.size(LayoutGeometry.BAR_LABEL_PAD, scale),
                    HudScale.size(barWidth() + LayoutGeometry.BAR_FRAME_PAD * 2, scale),
                    HudScale.size(boundsHeight(), scale)};
        }

        @Override
        public void moveTo(int newX, int newY, int w, int h, HudConfig.HudSettings s) {
            float scale = scale(s);
            LayoutGeometry.applyAnchoredMove(newX + HudScale.size(LayoutGeometry.BAR_FRAME_PAD, scale),
                    newY + HudScale.size(LayoutGeometry.BAR_LABEL_PAD, scale),
                    HudScale.size(barWidth(), scale), HudScale.size(barHeight, scale), w, h,
                    anchor -> writeAnchor(s, anchor),
                    x -> writeOffsets(s, x, Integer.MIN_VALUE),
                    y -> writeOffsets(s, Integer.MIN_VALUE, y));
        }
    }

    /**
     * Height is the <em>preview's</em>, not the live card's, since that is what the editor draws.
     */
    public static class CharacterPlateElement extends AbstractElement {

        public CharacterPlateElement() {
            super(ElementIds.CHARACTER_PLATE);
        }

        @Override
        public boolean visible(HudConfig.HudSettings s) {
            return s.showCharacterPlate;
        }

        @Override
        public int[] bounds(int w, int h, HudConfig.HudSettings s) {
            int[] pos = CharacterPlateOverlay.anchor(w, h, s);
            float scale = s.characterPlateScale;
            return new int[]{pos[0], pos[1],
                    HudScale.size(CharacterPlateOverlay.CARD_W, scale),
                    HudScale.size(CharacterPlateOverlay.previewHeight(), scale)};
        }

        @Override
        public void moveTo(int newX, int newY, int w, int h, HudConfig.HudSettings s) {
            float scale = s.characterPlateScale;
            LayoutGeometry.applyAnchoredMove(newX, newY,
                    HudScale.size(CharacterPlateOverlay.CARD_W, scale),
                    HudScale.size(CharacterPlateOverlay.previewHeight(), scale), w, h,
                    anchor -> s.characterPlateAnchor = anchor,
                    x -> s.characterPlateXOffset = x,
                    y -> s.characterPlateYOffset = y);
        }

        @Override
        public void renderPreview(GuiGraphicsExtractor ctx, int w, int h, HudConfig.HudSettings s, long timeMs) {
            CharacterPlateOverlay.renderPreview(ctx, w, h, s, timeMs);
        }

        @Override
        public void resetPosition(HudConfig.HudSettings s) {
            s.characterPlateAnchor = "TOP_LEFT";
            s.characterPlateXOffset = 0;
            s.characterPlateYOffset = CharacterPlateOverlay.DEFAULT_TOP_Y;
        }
    }

    /**
     * All four {@link HealthStyle}s occupy the same box, so nothing here asks which is on —
     * only {@link #resetPosition}, because the styles ship at different heights.
     */
    public static class BeyonderHealthElement extends AbstractElement {

        public BeyonderHealthElement() {
            super(ElementIds.BEYONDER_HEALTH);
        }

        @Override
        public boolean visible(HudConfig.HudSettings s) {
            return s.showBeyonderHealth;
        }

        @Override
        public int[] bounds(int w, int h, HudConfig.HudSettings s) {
            int[] pos = BeyonderHealthOverlay.anchor(w, h, s);
            float scale = s.beyonderHealthScale;
            int pad = HudScale.size(LayoutGeometry.BAR_FRAME_PAD, scale);
            return new int[]{pos[0] - pad, pos[1] - pad,
                    HudScale.size(BeyonderHealthOverlay.BAR_WIDTH + LayoutGeometry.BAR_FRAME_PAD * 2, scale),
                    HudScale.size(BeyonderHealthOverlay.BAR_HEIGHT + LayoutGeometry.BAR_FRAME_PAD * 2, scale)};
        }

        @Override
        public void moveTo(int newX, int newY, int w, int h, HudConfig.HudSettings s) {
            float scale = s.beyonderHealthScale;
            int pad = HudScale.size(LayoutGeometry.BAR_FRAME_PAD, scale);
            LayoutGeometry.applyAnchoredMove(newX + pad, newY + pad,
                    HudScale.size(BeyonderHealthOverlay.BAR_WIDTH, scale),
                    HudScale.size(BeyonderHealthOverlay.BAR_HEIGHT, scale), w, h,
                    anchor -> s.beyonderHealthAnchor = anchor,
                    x -> s.beyonderHealthXOffset = x,
                    y -> s.beyonderHealthYOffset = y);
        }

        @Override
        public void renderPreview(GuiGraphicsExtractor ctx, int w, int h, HudConfig.HudSettings s, long timeMs) {
            BeyonderHealthOverlay.renderPreview(ctx, w, h, s);
        }

        @Override
        public void resetPosition(HudConfig.HudSettings s) {
            BeyonderHealthOverlay.resetPosition(s, HealthStyle.parse(s.beyonderHealthStyle));
        }
    }

    public static class MadnessElement extends BarElement {

        private static final double PREVIEW_MADNESS = 42;
        private static final double PREVIEW_PERMANENT = 10;

        public MadnessElement() {
            super(ElementIds.MADNESS, MadnessOverlay.BAR_HEIGHT);
        }

        @Override
        public boolean visible(HudConfig.HudSettings s) {
            return s.showMadnessBar && !s.showCharacterPlate;
        }

        @Override
        protected int[] fillOrigin(int w, int h, HudConfig.HudSettings s) {
            return MadnessOverlay.anchor(w, h, s);
        }

        @Override
        protected void writeAnchor(HudConfig.HudSettings s, String anchor) {
            s.madnessAnchor = anchor;
        }

        @Override
        protected void writeOffsets(HudConfig.HudSettings s, int x, int y) {
            if (x != Integer.MIN_VALUE) s.madnessXOffset = x;
            if (y != Integer.MIN_VALUE) s.madnessYOffset = y;
        }

        @Override
        protected float scale(HudConfig.HudSettings s) {
            return s.madnessScale;
        }

        @Override
        public void renderPreview(GuiGraphicsExtractor ctx, int w, int h, HudConfig.HudSettings s, long timeMs) {
            int[] pos = fillOrigin(w, h, s);
            HudScale.push(ctx, pos[0], pos[1], s.madnessScale);
            MadnessOverlay.drawBarAt(ctx, pos[0], pos[1], PREVIEW_MADNESS, PREVIEW_PERMANENT, timeMs);
            HudScale.pop(ctx);
        }

        @Override
        public void resetPosition(HudConfig.HudSettings s) {
            s.madnessAnchor = "TOP_LEFT";
            s.madnessXOffset = 0;
            s.madnessYOffset = MadnessOverlay.DEFAULT_TOP_Y;
        }
    }

    /**
     * The frame sprite overhangs the fill on every side, so the pads come from the overlay.
     */
    public static class SpiritualityElement extends AbstractElement {

        private static final int DEFAULT_TOP_Y = 50;

        public SpiritualityElement() {
            super(ElementIds.SPIRITUALITY);
        }

        private static int[] fillOrigin(int w, int h, HudConfig.HudSettings s) {
            return SpiritualityOverlay.anchor(w, h, s);
        }

        @Override
        public boolean visible(HudConfig.HudSettings s) {
            return s.showSpiritualityBar;
        }

        @Override
        public int[] bounds(int w, int h, HudConfig.HudSettings s) {
            return SpiritualityOverlay.bounds(w, h, s);
        }

        @Override
        public void moveTo(int newX, int newY, int w, int h, HudConfig.HudSettings s) {
            int[] fill = fillOrigin(w, h, s);
            int[] box = bounds(w, h, s);
            LayoutGeometry.applyAnchoredMove(newX + (fill[0] - box[0]), newY + (fill[1] - box[1]),
                    HudScale.size(SpiritualityOverlay.BAR_WIDTH, s.spiritualityScale),
                    HudScale.size(SpiritualityOverlay.BAR_HEIGHT, s.spiritualityScale), w, h,
                    anchor -> s.spiritualityAnchor = anchor,
                    x -> s.spiritualityXOffset = x,
                    y -> s.spiritualityYOffset = y);
        }

        @Override
        public void renderPreview(GuiGraphicsExtractor ctx, int w, int h, HudConfig.HudSettings s, long timeMs) {
            SpiritualityOverlay.renderPreview(ctx, w, h, s, timeMs);
        }

        @Override
        public void resetPosition(HudConfig.HudSettings s) {
            s.spiritualityAnchor = "TOP_LEFT";
            s.spiritualityXOffset = 0;
            s.spiritualityYOffset = DEFAULT_TOP_Y;
        }
    }

    public static class ActingElement extends BarElement {

        private static final double PREVIEW_PERCENT = 63.0;
        private static final String PREVIEW_PATHWAY = "fool";

        private static final int DEFAULT_TOP_Y = 80;

        public ActingElement() {
            super(ElementIds.ACTING, ActingOverlay.BAR_HEIGHT);
        }

        @Override
        public boolean visible(HudConfig.HudSettings s) {
            return s.showActingBar && !s.showCharacterPlate;
        }

        @Override
        protected int[] fillOrigin(int w, int h, HudConfig.HudSettings s) {
            return ActingOverlay.anchor(w, h, s);
        }

        @Override
        protected void writeAnchor(HudConfig.HudSettings s, String anchor) {
            s.actingAnchor = anchor;
        }

        @Override
        protected void writeOffsets(HudConfig.HudSettings s, int x, int y) {
            if (x != Integer.MIN_VALUE) s.actingXOffset = x;
            if (y != Integer.MIN_VALUE) s.actingYOffset = y;
        }

        @Override
        protected float scale(HudConfig.HudSettings s) {
            return s.actingScale;
        }

        @Override
        public void renderPreview(GuiGraphicsExtractor ctx, int w, int h, HudConfig.HudSettings s, long timeMs) {
            int[] pos = fillOrigin(w, h, s);
            HudScale.push(ctx, pos[0], pos[1], s.actingScale);
            ActingOverlay.drawBarAt(ctx, pos[0], pos[1], PREVIEW_PERCENT,
                    Pathways.pathwayRgb(PREVIEW_PATHWAY),
                    I18n.get("hud.coi.acting_label", String.format(Locale.ROOT, "%.1f", PREVIEW_PERCENT)));
            HudScale.pop(ctx);
        }

        @Override
        public void resetPosition(HudConfig.HudSettings s) {
            s.actingAnchor = "TOP_LEFT";
            s.actingXOffset = 0;
            s.actingYOffset = DEFAULT_TOP_Y;
        }
    }

    public static class ResourceElement extends AbstractElement {

        private static final int GHOST = 0x60FFFFFF;

        private static final int DEFAULT_TOP_Y = 100;

        public ResourceElement() {
            super(ElementIds.RESOURCES);
        }

        private static int rows(HudConfig.HudSettings s) {
            return Math.max(1, s.resourceMaxBars);
        }

        private static int stride(HudConfig.HudSettings s) {
            return HudScale.size(ResourceOverlay.STRIDE, s.resourceScale);
        }

        private static int stackHeight(HudConfig.HudSettings s) {
            return (rows(s) - 1) * stride(s) + HudScale.size(
                    LayoutGeometry.BAR_LABEL_PAD + ResourceOverlay.BAR_HEIGHT + LayoutGeometry.BAR_FRAME_PAD,
                    s.resourceScale);
        }

        @Override
        public boolean visible(HudConfig.HudSettings s) {
            return s.showResourceBars && !s.showCharacterPlate;
        }

        @Override
        public int[] bounds(int w, int h, HudConfig.HudSettings s) {
            int[] pos = ResourceOverlay.anchor(w, h, s);
            float scale = s.resourceScale;
            int grow = (rows(s) - 1) * stride(s);
            // BOTTOM anchors stack upward, so the first bar is the stack's floor
            int top = pos[1] - HudScale.size(LayoutGeometry.BAR_LABEL_PAD, scale)
                    - (HudAnchor.parse(s.resourceAnchor).isTop() ? 0 : grow);
            return new int[]{pos[0] - HudScale.size(LayoutGeometry.BAR_FRAME_PAD, scale), top,
                    HudScale.size(ResourceOverlay.BAR_WIDTH + LayoutGeometry.BAR_FRAME_PAD * 2, scale), stackHeight(s)};
        }

        @Override
        public void moveTo(int newX, int newY, int w, int h, HudConfig.HudSettings s) {
            float scale = s.resourceScale;
            boolean top = newY + stackHeight(s) / 2 < h / 2;
            int grow = (rows(s) - 1) * stride(s);
            int fillY = newY + HudScale.size(LayoutGeometry.BAR_LABEL_PAD, scale) + (top ? 0 : grow);
            LayoutGeometry.applyAnchoredMove(newX + HudScale.size(LayoutGeometry.BAR_FRAME_PAD, scale), fillY,
                    HudScale.size(ResourceOverlay.BAR_WIDTH, scale), w, h, top,
                    anchor -> s.resourceAnchor = anchor,
                    x -> s.resourceXOffset = x,
                    y -> s.resourceYOffset = y);
        }

        @Override
        public void renderPreview(GuiGraphicsExtractor ctx, int w, int h, HudConfig.HudSettings s, long timeMs) {
            int[] pos = ResourceOverlay.anchor(w, h, s);
            int step = HudAnchor.parse(s.resourceAnchor).isTop()
                    ? ResourceOverlay.STRIDE : -ResourceOverlay.STRIDE;

            HudScale.push(ctx, pos[0], pos[1], s.resourceScale);
            ResourceOverlay.drawBarAt(ctx, pos[0], pos[1], "Rage Meter", 62.5, 100, 0xFF5555, true);
            if (rows(s) > 1) {
                ResourceOverlay.drawBarAt(ctx, pos[0], pos[1] + step, "Seeds", 3, 5, 0x55FF7F, false);
            }
            for (int i = 2; i < rows(s); i++) {
                LayoutGeometry.ghostOutline(ctx, pos[0] - LayoutGeometry.BAR_FRAME_PAD,
                        pos[1] + step * i - LayoutGeometry.BAR_FRAME_PAD,
                        ResourceOverlay.BAR_WIDTH + LayoutGeometry.BAR_FRAME_PAD * 2,
                        ResourceOverlay.BAR_HEIGHT + LayoutGeometry.BAR_FRAME_PAD * 2, GHOST);
            }
            HudScale.pop(ctx);
        }

        @Override
        public void resetPosition(HudConfig.HudSettings s) {
            s.resourceAnchor = "TOP_LEFT";
            s.resourceXOffset = 0;
            s.resourceYOffset = DEFAULT_TOP_Y;
        }
    }

    /**
     * No anchor: x is a signed offset from the centre line, y a distance up from the bottom.
     */
    public static class ActionBarElement extends AbstractElement {

        private static final int SAMPLE_W = 200;
        private static final int SAMPLE_H = 20;
        private static final int ABOVE_BASE = 11;

        private static final int DEFAULT_Y_OFFSET = 72;

        public ActionBarElement() {
            super(ElementIds.ACTION_BAR);
        }

        @Override
        public boolean visible(HudConfig.HudSettings s) {
            return s.showActionBar;
        }

        @Override
        public int[] bounds(int w, int h, HudConfig.HudSettings s) {
            float scale = s.actionBarScale;
            int sampleW = HudScale.size(SAMPLE_W, scale);
            return new int[]{w / 2 + s.actionBarXOffset - sampleW / 2,
                    h - s.actionBarYOffset - HudScale.size(ABOVE_BASE, scale),
                    sampleW, HudScale.size(SAMPLE_H, scale)};
        }

        @Override
        public void moveTo(int newX, int newY, int w, int h, HudConfig.HudSettings s) {
            float scale = s.actionBarScale;
            int xo = newX + HudScale.size(SAMPLE_W, scale) / 2 - w / 2;
            s.actionBarXOffset = Math.abs(xo) <= LayoutGeometry.CENTER_SNAP ? 0 : xo;
            s.actionBarYOffset = Mth.clamp(h - newY - HudScale.size(ABOVE_BASE, scale), 0, h);
        }

        @Override
        public void renderPreview(GuiGraphicsExtractor ctx, int w, int h, HudConfig.HudSettings s, long timeMs) {
            int centerX = w / 2 + s.actionBarXOffset;
            int baseY = h - s.actionBarYOffset;
            HudScale.push(ctx, centerX, baseY, s.actionBarScale);
            ActionBarOverlay.drawLineAt(ctx, Component.literal("Cooldown · Sword 1.8s"), "COOLDOWN", centerX, baseY);
            ActionBarOverlay.drawLineAt(ctx, Component.literal("Status · Active"), "STATUS",
                    centerX, baseY - ActionBarOverlay.LINE_SPACING);
            HudScale.pop(ctx);
        }

        @Override
        public void resetPosition(HudConfig.HudSettings s) {
            s.actionBarXOffset = 0;
            s.actionBarYOffset = DEFAULT_Y_OFFSET;
        }
    }

    /**
     * Signed offsets from the screen centre in both axes: it belongs to the crosshair.
     */
    public static class TargetHealthElement extends AbstractElement {

        private static final int ABOVE = 11;
        private static final int BELOW = 12;

        public TargetHealthElement() {
            super(ElementIds.TARGET_HEALTH);
        }

        @Override
        public boolean visible(HudConfig.HudSettings s) {
            return s.showTargetHealth;
        }

        @Override
        public int[] bounds(int w, int h, HudConfig.HudSettings s) {
            int[] pos = TargetHealthOverlay.anchor(w, h, s);
            float scale = s.targetHealthScale;
            return new int[]{pos[0] - HudScale.size(LayoutGeometry.BAR_FRAME_PAD, scale),
                    pos[1] - HudScale.size(ABOVE, scale),
                    HudScale.size(TargetHealthOverlay.BAR_WIDTH + LayoutGeometry.BAR_FRAME_PAD * 2, scale),
                    HudScale.size(ABOVE + TargetHealthOverlay.BAR_HEIGHT + BELOW, scale)};
        }

        @Override
        public void moveTo(int newX, int newY, int w, int h, HudConfig.HudSettings s) {
            float scale = s.targetHealthScale;
            s.targetHealthXOffset = newX + HudScale.size(LayoutGeometry.BAR_FRAME_PAD, scale)
                    - (w / 2 - HudScale.size(TargetHealthOverlay.BAR_WIDTH, scale) / 2);
            s.targetHealthYOffset = newY + HudScale.size(ABOVE, scale) - h / 2;
        }

        @Override
        public void renderPreview(GuiGraphicsExtractor ctx, int w, int h, HudConfig.HudSettings s, long timeMs) {
            int[] pos = TargetHealthOverlay.anchor(w, h, s);
            HudScale.push(ctx, pos[0], pos[1], s.targetHealthScale);
            TargetHealthOverlay.drawBarAt(ctx, pos[0], pos[1], "Steve", 0.68f, 0.85f, 13.6, 20, true);
            HudScale.pop(ctx);
        }

        @Override
        public void resetPosition(HudConfig.HudSettings s) {
            s.targetHealthXOffset = 0;
            s.targetHealthYOffset = TargetHealthOverlay.DEFAULT_Y_OFFSET;
        }
    }

    /**
     * Signed offsets from the screen centre in both axes: it stands in for a vanilla title.
     */
    public static class CogitationElement extends AbstractElement {

        public CogitationElement() {
            super(ElementIds.COGITATION);
        }

        @Override
        public boolean visible(HudConfig.HudSettings s) {
            return s.showCogitationOverlay;
        }

        @Override
        public int[] bounds(int w, int h, HudConfig.HudSettings s) {
            int[] pos = CogitationOverlay.anchor(w, h, s);
            float scale = s.cogitationScale;
            return new int[]{pos[0], pos[1],
                    HudScale.size(CogitationOverlay.CARD_W, scale),
                    HudScale.size(CogitationOverlay.CARD_H, scale)};
        }

        @Override
        public void moveTo(int newX, int newY, int w, int h, HudConfig.HudSettings s) {
            s.cogitationXOffset = newX - (w / 2 - HudScale.size(CogitationOverlay.CARD_W, s.cogitationScale) / 2);
            s.cogitationYOffset = newY - h / 2;
        }

        @Override
        public void renderPreview(GuiGraphicsExtractor ctx, int w, int h, HudConfig.HudSettings s, long timeMs) {
            int[] pos = CogitationOverlay.anchor(w, h, s);
            HudScale.push(ctx, pos[0], pos[1], s.cogitationScale);
            CogitationOverlay.drawCardAt(ctx, pos[0], pos[1], "Turn around", 3, 0.6, false);
            HudScale.pop(ctx);
        }

        @Override
        public void resetPosition(HudConfig.HudSettings s) {
            s.cogitationXOffset = 0;
            s.cogitationYOffset = CogitationOverlay.DEFAULT_Y_OFFSET;
        }
    }

    /**
     * Three slots are always reserved, so the grab box does not grow under the cursor.
     */
    public static class NotificationElement extends AbstractElement {

        private static final int SLOTS = 3;
        private static final int STRIDE = NotificationOverlay.CARD_H_1_LINE + NotificationOverlay.CARD_GAP;
        private static final int GHOST = 0x60FFFFFF;

        public NotificationElement() {
            super(ElementIds.NOTIFICATIONS);
        }

        @Override
        public boolean visible(HudConfig.HudSettings s) {
            return s.showNotifications;
        }

        @Override
        public int[] bounds(int w, int h, HudConfig.HudSettings s) {
            int[] pos = NotificationOverlay.anchor(w, s);
            float scale = s.notificationScale;
            return new int[]{pos[0], pos[1], HudScale.size(NotificationOverlay.CARD_W, scale),
                    SLOTS * HudScale.size(STRIDE, scale) - HudScale.size(NotificationOverlay.CARD_GAP, scale)};
        }

        @Override
        public void moveTo(int newX, int newY, int w, int h, HudConfig.HudSettings s) {
            // A margin from the right edge, so the stack stays in the corner when the window widens
            s.notificationXOffset = w - (newX + HudScale.size(NotificationOverlay.CARD_W, s.notificationScale));
            s.notificationYOffset = newY;
        }

        @Override
        public void renderPreview(GuiGraphicsExtractor ctx, int w, int h, HudConfig.HudSettings s, long timeMs) {
            int[] pos = NotificationOverlay.anchor(w, s);
            HudScale.push(ctx, pos[0], pos[1], s.notificationScale);
            NotificationOverlay.drawToastAt(ctx, pos[0], pos[1],
                    "Advancement", "Sequence 8 reached", 0xFFFFD870, 1f);
            for (int i = 1; i < SLOTS; i++) {
                LayoutGeometry.ghostOutline(ctx, pos[0], pos[1] + i * STRIDE,
                        NotificationOverlay.CARD_W, NotificationOverlay.CARD_H_1_LINE, GHOST);
            }
            HudScale.pop(ctx);
        }

        @Override
        public void resetPosition(HudConfig.HudSettings s) {
            s.notificationXOffset = NotificationOverlay.DEFAULT_MARGIN;
            s.notificationYOffset = NotificationOverlay.DEFAULT_MARGIN;
        }
    }
}
