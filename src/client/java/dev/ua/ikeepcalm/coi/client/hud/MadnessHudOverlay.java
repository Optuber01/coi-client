package dev.ua.ikeepcalm.coi.client.hud;

import dev.ua.ikeepcalm.coi.client.ClientBeyonderState;
import dev.ua.ikeepcalm.coi.client.config.HudConfig;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

import java.util.Random;

public class MadnessHudOverlay {

    private static final Identifier MADNESS_LAYER = Identifier.fromNamespaceAndPath("coi-client", "madness");

    public static void initialize() {
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, MADNESS_LAYER, MadnessHudOverlay::render);
    }

    private static void render(GuiGraphicsExtractor ctx, DeltaTracker tickCounter) {
        Minecraft client = Minecraft.getInstance();
        HudConfig.HudSettings settings = HudConfig.getSettings();

        if (client.player == null || client.options.hideGui || !settings.enabled || !settings.showMadnessBar) {
            return;
        }

        int w = client.getWindow().getGuiScaledWidth();
        int h = client.getWindow().getGuiScaledHeight();

        double madness = ClientBeyonderState.getMadness();
        double permMadness = ClientBeyonderState.getPermanentMadness();
        int freezeStacks = ClientBeyonderState.getFreezeStacks();
        int mentalPressure = ClientBeyonderState.getMentalPressure();
        double tiredness = ClientBeyonderState.getTiredness();

        // 1. Determine Madness Stage
        int stage = 0;
        if (madness >= 100.0) stage = 4;
        else if (madness >= 75.0) stage = 3;
        else if (madness >= 50.0) stage = 2;
        else if (madness >= 25.0) stage = 1;

        // 2. Render Screen-wide Effects based on Stage
        renderScreenEffects(ctx, w, h, stage, madness);

        // 3. Render the Madness Bar
        renderMadnessBar(ctx, client, w, h, settings, madness, permMadness, freezeStacks, mentalPressure, tiredness, stage);
    }

    private static void renderScreenEffects(GuiGraphicsExtractor ctx, int w, int h, int stage, double madness) {
        if (stage < 2) return; // No screen effects for stages 0 and 1

        long time = System.currentTimeMillis();

        if (stage == 2) {
            // Stage 2: Faint screen vignette (black/dark red)
            float intensity = 0.35f;
            int maxA = (int) (120 * intensity);
            int colorRGB = 0x1A0000; // faint red-black
            drawVignette(ctx, w, h, maxA, colorRGB, intensity);
        } else if (stage == 3) {
            // Stage 3: High intensity blood-red vignette, pulsating
            float pulse = (float) Math.sin(time * 0.008) * 0.25f + 0.75f;
            float intensity = 0.65f * pulse;
            int maxA = (int) (200 * intensity);
            int colorRGB = 0x660000; // blood red
            drawVignette(ctx, w, h, maxA, colorRGB, intensity);
        } else if (stage == 4) {
            // MAX (Stage 4 / Rampager): Full screen border overlay + VHS glitches
            float pulse = (float) Math.sin(time * 0.015) * 0.15f + 0.85f;
            float intensity = 0.85f * pulse;
            int maxA = (int) (230 * intensity);
            int colorRGB = 0x2A082E; // Dark purple
            drawVignette(ctx, w, h, maxA, colorRGB, intensity);

            // VHS Glitch rendering
            renderGlitches(ctx, w, h, intensity);
        }
    }

    private static void drawVignette(GuiGraphicsExtractor ctx, int w, int h, int maxA, int colorRGB, float intensity) {
        int vigW = (int) (w * 0.35f * intensity);
        int vigH = (int) (h * 0.4f * intensity);

        int color1 = (maxA << 24) | colorRGB;
        int color2 = colorRGB; // 0 alpha

        // Top and bottom gradients
        ctx.fillGradient(0, 0, w, vigH, color1, color2);
        ctx.fillGradient(0, h - vigH, w, h, color2, color1);

        // Left and right bands
        int bands = 12;
        int stepW = Math.max(1, vigW / bands);
        for (int i = 0; i < bands; i++) {
            float t = (float) (bands - i) / bands;
            int a = (int) (maxA * t * t);
            int col = (a << 24) | colorRGB;
            ctx.fill(i * stepW, 0, i * stepW + stepW, h, col);
            ctx.fill(w - i * stepW - stepW, 0, w - i * stepW, h, col);
        }
    }

    private static void renderGlitches(GuiGraphicsExtractor ctx, int w, int h, float intensity) {
        long elapsed = System.currentTimeMillis();
        // Pattern: 250ms burst, 400ms calm
        long cycleLen = 650;
        long burstLen = 250;
        long phasePos = elapsed % cycleLen;
        if (phasePos >= burstLen) return;

        Random rng = new Random((elapsed / 60) * 0x9E3779B97F4A7C15L);
        int lineCount = 3 + (int) (intensity * 4);
        float alpha = 0.6f + 0.4f * intensity;

        for (int i = 0; i < lineCount; i++) {
            int y = rng.nextInt(h);
            int bh = 1 + rng.nextInt(3);
            int type = rng.nextInt(4);

            switch (type) {
                case 0 -> {
                    int a = (int) (90 * alpha);
                    ctx.fill(0, y, w, y + bh, a << 24);
                }
                case 1 -> {
                    int a = (int) (40 * alpha);
                    ctx.fill(0, y, w, y + bh, (a << 24) | 0xFFFFFF);
                }
                case 2 -> {
                    int ra = (int) (50 * alpha);
                    ctx.fill(0, y, w, y + 1, (ra << 24) | 0x8A0E8E);
                }
                case 3 -> {
                    int splitX = w / 3 + rng.nextInt(w / 3);
                    int a = (int) (35 * alpha);
                    ctx.fill(splitX, y, w, y + bh, (a << 24) | 0x222222);
                }
            }
        }
    }

    private static void renderMadnessBar(GuiGraphicsExtractor ctx, Minecraft client, int w, int h, HudConfig.HudSettings settings,
                                         double madness, double permMadness, int freezeStacks, int mentalPressure, double tiredness, int stage) {
        Font textRenderer = client.font;

        // Position coordinates
        int barWidth = 182;
        int barHeight = 6;
        int barX;
        int barY;

        String anchor = settings.madnessAnchor != null ? settings.madnessAnchor.toUpperCase() : "TOP_LEFT";
        switch (anchor) {
            case "TOP_LEFT" -> {
                barX = 10;
                barY = 20;
            }
            case "TOP_CENTER" -> {
                barX = (w - barWidth) / 2;
                barY = 20;
            }
            case "BOTTOM_LEFT" -> {
                barX = 10;
                barY = h - settings.madnessYOffset;
            }
            default -> { // BOTTOM_CENTER
                barX = (w - barWidth) / 2;
                barY = h - settings.madnessYOffset;
            }
        }
        barX = Mth.clamp(barX + settings.madnessXOffset, 0, Math.max(0, w - barWidth));

        // Apply screen shake offset on madness increase
        float flash = ClientBeyonderState.getFlashIntensity();
        if (flash > 0) {
            Random rand = new Random();
            barX += (int) ((rand.nextFloat() * 2 - 1) * 3 * flash);
            barY += (int) ((rand.nextFloat() * 2 - 1) * 3 * flash);
        }

        // 1. Draw Drop Shadow / Border
        ctx.fill(barX - 1, barY - 1, barX + barWidth + 1, barY + barHeight + 1, 0xCC000000);

        // 2. Draw Background Bar (represent full 100 limit)
        ctx.fill(barX, barY, barX + barWidth, barY + barHeight, 0xFF151515);

        // Determine colors based on stage and animations
        long time = System.currentTimeMillis();
        int mainColorTop;
        int mainColorBottom;
        int textColor;
        String statusName;

        switch (stage) {
            case 1 -> {
                // Stage 1: Warning (Yellow/Orange) - gentle pulse
                float pulse = (float) Math.sin(time * 0.003) * 0.15f + 0.85f;
                int r = (int) (255 * pulse);
                int g = (int) (170 * pulse);
                mainColorTop = (255 << 24) | (r << 16) | (g << 8);
                mainColorBottom = (255 << 24) | ((int) (204 * pulse) << 16) | ((int) (119 * pulse) << 8);
                textColor = 0xFFFFAA00;
                statusName = "Sane";
            }
            case 2 -> {
                // Stage 2: Partial Loss (Red) - static dark red
                mainColorTop = 0xFFDD2222;
                mainColorBottom = 0xFF991111;
                textColor = 0xFFDD2222;
                statusName = "Unstable";
            }
            case 3 -> {
                // Stage 3: Critical (Deep Crimson) - fast pulse
                float pulse = (float) Math.sin(time * 0.01) * 0.2f + 0.8f;
                int r = (int) (255 * pulse);
                mainColorTop = (255 << 24) | (r << 16);
                mainColorBottom = (255 << 24) | ((int) (139 * pulse) << 16);
                textColor = 0xFFFF0055;
                statusName = "Unhinged";
            }
            case 4 -> {
                // MAX: Rampager (Dark Purple/Black)
                float pulse = (float) Math.sin(time * 0.015) * 0.15f + 0.85f;
                int r = (int) (153 * pulse);
                int b = (int) (153 * pulse);
                mainColorTop = (255 << 24) | (r << 16) | (51 << 8) | b;
                mainColorBottom = (255 << 24) | ((int) (58 * pulse) << 16) | ((int) (58 * pulse));
                textColor = 0xFF993399;
                statusName = "Gone Mad";
            }
            default -> {
                // Stage 0: Stable (Green/Cyan)
                mainColorTop = 0xFF00FFCC;
                mainColorBottom = 0xFF00AA88;
                textColor = 0xFF00FFCC;
                statusName = "Stable";
            }
        }

        // 3. Draw permanentMadness region (Min Floor)
        int permWidth = (int) (barWidth * (permMadness / 100.0));
        permWidth = Mth.clamp(permWidth, 0, barWidth);
        if (permWidth > 0) {
            // 50% opacity of top color
            int permColor = (mainColorTop & 0x00FFFFFF) | (0x60 << 24);
            ctx.fill(barX, barY, barX + permWidth, barY + barHeight, permColor);
        }

        // 4. Draw primary filled region for current madness
        int madnessWidth = (int) (barWidth * (madness / 100.0));
        madnessWidth = Mth.clamp(madnessWidth, 0, barWidth);
        if (madnessWidth > 0) {
            ctx.fillGradient(barX, barY, barX + madnessWidth, barY + barHeight, mainColorTop, mainColorBottom);
        }

        // 5. Draw permanentMadness marker line (if set)
        if (permWidth > 0 && permWidth <= barWidth) {
            ctx.fill(barX + permWidth - 1, barY - 1, barX + permWidth + 1, barY + barHeight + 1, 0xDDFFFFFF);
        }

        // 6. Draw Brief Flash overlay on madness increase
        if (flash > 0 && madnessWidth > 0) {
            int flashColor = ((int) (flash * 160) << 24) | 0xFFFFFF;
            ctx.fill(barX, barY, barX + madnessWidth, barY + barHeight, flashColor);
        }

        // 7. Render Text Indicator: Madness: 42.5% / 100% (Min: 5.0%)
        String text = String.format("Madness: %.1f%% / 100%% (%s, Min: %.1f%%)", madness, statusName, permMadness);
        int textWidth = textRenderer.width(text);
        int textX = barX + (barWidth - textWidth) / 2;
        int textY = barY - 10;
        ctx.text(textRenderer, text, textX, textY, textColor, true);

        // 8. Render other conditions (Freeze, Mental Pressure, Tiredness) below the bar
        StringBuilder extraInfo = new StringBuilder();
        if (freezeStacks > 0) {
            extraInfo.append("❄ Frz: ").append(freezeStacks).append("%  ");
        }
        if (mentalPressure > 0) {
            extraInfo.append("🧠 Prs: ").append(mentalPressure).append("  ");
        }
        if (tiredness > 0.0) {
            extraInfo.append("💤 Trd: ").append(String.format("%.1f%%", tiredness)).append("  ");
        }
        String extraText = extraInfo.toString().trim();
        if (!extraText.isEmpty()) {
            int extraWidth = textRenderer.width(extraText);
            int extraX = barX + (barWidth - extraWidth) / 2;
            int extraY = barY + barHeight + 3;
            ctx.text(textRenderer, extraText, extraX, extraY, 0xFF77AADD, true);
        }
    }
}
