package dev.ua.ikeepcalm.coi.hud.overlay;

import dev.ua.ikeepcalm.coi.config.HudConfig;
import dev.ua.ikeepcalm.coi.domain.beyonder.model.ActionBarState;
import dev.ua.ikeepcalm.coi.hud.HudGate;
import dev.ua.ikeepcalm.coi.hud.HudScale;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.List;

/**
 * COI's own sorted action-bar entries, one line each. Only COI's entries move here; vanilla
 * action bars from other plugins are untouched.
 */
public class ActionBarOverlay {

    private static final Identifier ACTION_BAR_LAYER = Identifier.fromNamespaceAndPath("coi-client", "actionbar");

    public static final int LINE_SPACING = 10;
    private static final int TICK_WIDTH = 2;
    private static final int TICK_GAP = 4;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int DEFAULT_TICK = 0xFF999999;

    private ActionBarOverlay() {
    }

    public static void initialize() {
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, ACTION_BAR_LAYER, ActionBarOverlay::render);
    }

    private static void render(GuiGraphicsExtractor ctx, DeltaTracker tickCounter) {
        Minecraft client = Minecraft.getInstance();
        HudConfig.HudSettings settings = HudConfig.getSettings();

        if (HudGate.blocked(client, settings) || !settings.showActionBar) {
            return;
        }

        // 0 = follow the server's own maxVisible
        int serverMax = ActionBarState.getMaxVisible();
        int limit = settings.actionBarLines > 0 ? Math.min(serverMax, settings.actionBarLines) : serverMax;

        List<ActionBarState.Entry> entries = ActionBarState.visibleEntries(System.currentTimeMillis(), limit);
        if (entries.isEmpty()) return;

        int w = client.getWindow().getGuiScaledWidth();
        int h = client.getWindow().getGuiScaledHeight();
        int baseY = h - settings.actionBarYOffset;
        int centerX = w / 2 + settings.actionBarXOffset;

        // Stacked upward, so the highest-priority entry sits closest to the hotbar
        HudScale.push(ctx, centerX, baseY, settings.actionBarScale);
        for (int i = 0; i < entries.size(); i++) {
            ActionBarState.Entry entry = entries.get(i);
            drawLineAt(ctx, entry.text(), entry.channel(), centerX, baseY - i * LINE_SPACING);
        }
        HudScale.pop(ctx);
    }

    /**
     * Plain values, so previews need not fabricate entries.
     */
    public static void drawLineAt(GuiGraphicsExtractor ctx, Component text, String channel, int centerX, int y) {
        Minecraft client = Minecraft.getInstance();
        int x = centerX - client.font.width(text) / 2;
        ctx.fill(x - TICK_GAP - TICK_WIDTH, y - 1, x - TICK_GAP, y + client.font.lineHeight - 1, tickColor(channel));
        ctx.text(client.font, text, x, y, TEXT_COLOR, true);
    }

    /** One colour per plugin channel. */
    private static int tickColor(String channel) {
        if (channel == null) return DEFAULT_TICK;
        return switch (channel.toUpperCase()) {
            case "RESERVE" -> 0xFF7FB2FF;
            case "SENSORY" -> 0xFFB57FFF;
            case "COOLDOWN" -> 0xFFFFD870;
            case "STATUS" -> 0xFF7FFF9F;
            case "CATEGORY" -> 0xFFFFA64D;
            case "NOTIFICATION" -> 0xFFFF5555;
            case "MINIGAME" -> 0xFF55FFFF;
            case "SYSTEM" -> 0xFFCCCCCC;
            default -> DEFAULT_TICK;
        };
    }
}
