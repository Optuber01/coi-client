package dev.ua.ikeepcalm.coi.screen;

import dev.ua.ikeepcalm.coi.config.ClientStateStore;
import dev.ua.ikeepcalm.coi.input.CoiKeyBindings;
import dev.ua.ikeepcalm.coi.network.ServerCapabilities;
import dev.ua.ikeepcalm.coi.ui.CoiIcons;
import dev.ua.ikeepcalm.coi.ui.CoiStyle;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

/**
 * Explains the empty hotbar slot: the plugin deletes the "Mystery Arts" item for any client
 * advertising {@code menu_action}, and nothing else says why.
 * <p>
 * Dismissal is state, not a preference, so it lives in {@code coi_client_state.json} beside the
 * tour flag — resetting the HUD config must not bring the hint back.
 */
public class InventoryHint {

    /**
     * {@code AbstractContainerScreen}'s default panel and centring rule, recomputed because both
     * fields are protected with no accessor. The recipe book shifts {@code leftPos} only, which
     * is why nothing below reads the panel's horizontal position.
     */
    private static final int PANEL_H = 166;

    private static final int PAD = 5;
    private static final int ICON_GAP = 5;
    private static final int BTN_GAP = 7;
    private static final int LINE = 10;
    private static final int FONT_H = 9;
    private static final int BTN_H = 13;
    private static final int BTN_MIN_W = 38;

    /** Between the strip and the panel it is attached to. */
    private static final int GAP = 4;
    /** Between the strip and the edge of the screen. */
    private static final int MARGIN = 2;

    /** Below this the text column is too narrow to read; the strip stands down. */
    private static final int MIN_TEXT_W = 48;

    private InventoryHint() {
    }

    public static void register() {
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (!(screen instanceof InventoryScreen)) return;
            // per-screen events are rebuilt on every init and resize, so this re-registers
            // rather than stacking up
            ScreenEvents.afterExtract(screen).register(InventoryHint::render);
            ScreenMouseEvents.allowMouseClick(screen).register(InventoryHint::click);
        });
    }

    private static boolean shouldShow() {
        // a vanilla server never took the item away, so it owes no explanation
        return ServerCapabilities.has("menu_action")
                && !ClientStateStore.isInventoryHintDismissed();
    }

    private record Strip(int x, int y, int w, int h,
                         int iconX, int iconY,
                         int textX, int textY,
                         int btnX, int btnY, int btnW,
                         List<FormattedCharSequence> head,
                         List<FormattedCharSequence> tail) {

        boolean inButton(double mx, double my) {
            return mx >= btnX && mx < btnX + btnW && my >= btnY && my < btnY + BTN_H;
        }

        boolean contains(double mx, double my) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }
    }

    /**
     * Measures the strip, or null when there is nowhere it fits: above the panel first, below
     * second, nowhere third. Minecraft never scales below 320×240 scaled pixels, putting the
     * panel's top at 37 at worst against a 35-tall strip — so "nowhere" is a guard, not common.
     */
    private static Strip layout(Screen screen, Font font) {
        Component headText = Component.translatable("screen.coi.inv_hint_line1");
        Component tailText = keyLine();
        Component dismiss = Component.translatable("screen.coi.inv_hint_dismiss");

        int btnW = Math.max(BTN_MIN_W, font.width(dismiss) + 12);
        int fixed = PAD + CoiIcons.COG_SIZE + ICON_GAP + BTN_GAP + btnW + PAD;
        int wanted = Math.max(font.width(headText), font.width(tailText));

        int maxW = screen.width - MARGIN * 2;
        int cardW = Math.min(fixed + wanted, maxW);
        int textW = cardW - fixed;
        if (textW < MIN_TEXT_W) return null;

        List<FormattedCharSequence> head = new ArrayList<>(font.split(headText, textW));
        List<FormattedCharSequence> tail = new ArrayList<>(font.split(tailText, textW));
        if (head.isEmpty() && tail.isEmpty()) return null;

        int textH = (head.size() + tail.size()) * LINE - (LINE - FONT_H);
        int contentH = Math.max(textH, Math.max(CoiIcons.COG_SIZE, BTN_H));
        int cardH = contentH + PAD * 2;

        int panelTop = (screen.height - PANEL_H) / 2;
        int panelBottom = panelTop + PANEL_H;
        int cardY;
        if (panelTop - GAP - cardH >= MARGIN) {
            cardY = panelTop - GAP - cardH;
        } else if (panelBottom + GAP + cardH <= screen.height - MARGIN) {
            cardY = panelBottom + GAP;
        } else {
            // overlapping the inventory would be worse than staying quiet
            return null;
        }

        int cardX = (screen.width - cardW) / 2;
        int contentY = cardY + PAD;
        return new Strip(cardX, cardY, cardW, cardH,
                cardX + PAD, contentY + (contentH - CoiIcons.COG_SIZE) / 2,
                cardX + PAD + CoiIcons.COG_SIZE + ICON_GAP, contentY + (contentH - textH) / 2,
                cardX + cardW - PAD - btnW, contentY + (contentH - BTN_H) / 2, btnW,
                head, tail);
    }

    private static Component keyLine() {
        KeyMapping mapping = CoiKeyBindings.openMenu;
        if (mapping == null || mapping.isUnbound()) {
            return Component.translatable("screen.coi.inv_hint_line2_unbound");
        }
        Component key = KeyMappingHelper.getBoundKeyOf(mapping).getDisplayName();
        return Component.translatable("screen.coi.inv_hint_line2",
                key.copy().withStyle(ChatFormatting.GOLD));
    }

    private static void render(Screen screen, GuiGraphicsExtractor graphics,
                               int mouseX, int mouseY, float tickProgress) {
        if (!shouldShow()) return;
        Font font = Minecraft.getInstance().font;
        Strip strip = layout(screen, font);
        if (strip == null) return;

        CoiStyle.drawCard(graphics, strip.x(), strip.y(), strip.w(), strip.h());

        if (!CoiIcons.draw(graphics, CoiIcons.COG, strip.iconX(), strip.iconY(), CoiIcons.COG_SIZE)) {
            // no pack defines the cog: keep the column and mark it with the accent
            int cx = strip.iconX() + CoiIcons.COG_SIZE / 2;
            int cy = strip.iconY() + CoiIcons.COG_SIZE / 2;
            graphics.fill(cx - 3, cy - 3, cx + 3, cy + 3, CoiStyle.ACCENT);
            graphics.fill(cx - 1, cy - 1, cx + 1, cy + 1, CoiStyle.CARD_BG);
        }

        int y = strip.textY();
        for (FormattedCharSequence line : strip.head()) {
            graphics.text(font, line, strip.textX(), y, CoiStyle.TEXT_BODY, false);
            y += LINE;
        }
        for (FormattedCharSequence line : strip.tail()) {
            graphics.text(font, line, strip.textX(), y, CoiStyle.TEXT_MUTED, false);
            y += LINE;
        }

        boolean hovered = strip.inButton(mouseX, mouseY);
        graphics.fill(strip.btnX(), strip.btnY(), strip.btnX() + strip.btnW(), strip.btnY() + BTN_H,
                hovered ? CoiStyle.ROW_HOVER : CoiStyle.TAB_BG_UNSELECTED);
        graphics.outline(strip.btnX(), strip.btnY(), strip.btnW(), BTN_H,
                hovered ? CoiStyle.ACCENT : CoiStyle.BORDER);
        graphics.centeredText(font, Component.translatable("screen.coi.inv_hint_dismiss"),
                strip.btnX() + strip.btnW() / 2, strip.btnY() + (BTN_H - FONT_H) / 2 + 1,
                hovered ? CoiStyle.ACCENT : CoiStyle.TEXT_BODY);
    }

    /**
     * <b>The whole strip is consumed, not just the button.</b> A click outside the inventory
     * panel is how {@code AbstractContainerScreen} drops whatever is on the cursor, so a player
     * mid-drag clicking this card would have thrown their item on the floor.
     */
    private static boolean click(Screen screen, MouseButtonEvent event) {
        if (!shouldShow() || event.button() != 0) return true;
        Strip strip = layout(screen, Minecraft.getInstance().font);
        if (strip == null || !strip.contains(event.x(), event.y())) return true;

        if (strip.inButton(event.x(), event.y())) {
            ClientStateStore.setInventoryHintDismissed(true);
            Minecraft.getInstance().getSoundManager()
                    .play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
        return false;
    }
}
