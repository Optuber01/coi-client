package dev.ua.ikeepcalm.coi.hud.overlay;

import dev.ua.ikeepcalm.coi.config.HudConfig;
import dev.ua.ikeepcalm.coi.domain.beyonder.model.BeyonderState;
import dev.ua.ikeepcalm.coi.hud.HudAnchor;
import dev.ua.ikeepcalm.coi.hud.HudGate;
import dev.ua.ikeepcalm.coi.hud.HudScale;
import dev.ua.ikeepcalm.coi.hud.render.CoiBar;
import dev.ua.ikeepcalm.coi.hud.render.HealthBarPaint;
import dev.ua.ikeepcalm.coi.hud.render.HealthStyle;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;

/**
 * The Beyonder's real HP pool, on the vanilla hearts' row. The one HUD element in the mod that
 * <b>replaces</b> a vanilla element ({@link VanillaHudElements#HEALTH_BAR}) rather than
 * attaching beside one; it keeps the displaced element and calls it whenever the hearts are
 * still wanted.
 * <p>
 * <b>The current value is derived, not received:</b>
 * <pre>poolCurrent = poolMax * (getHealth() / getMaxHealth())</pre>
 * Only {@code poolMax} comes over the wire; the server recomputes the pool <em>from</em> vanilla
 * health after every hit, so deriving is both cheaper and fresher than the server's own copy,
 * which only re-derives every 200 ticks. The denominator is {@code getMaxHealth()}, never a
 * hardcoded 20 — some abilities apply a negative max-health modifier.
 */
public class BeyonderHealthOverlay {

    /**
     * The box every style occupies: the widest it can be on the hearts' row, from the hotbar's
     * left edge to the food bar at {@code screenW / 2 + 10}. <b>Must stay even</b> —
     * {@link HudAnchor#resolve} centres with {@code (screenW - barW) / 2}, and an odd width
     * lands a pixel off vanilla's {@code screenW / 2 - 91} at odd window widths. 95 shipped in
     * 1.2.0 and broke exactly that.
     */
    public static final int BAR_WIDTH = 96;
    /**
     * 9, because {@link CoiBar#frame} draws its border <em>outside</em> the box: the drawn
     * footprint is {@code BAR_HEIGHT + 2}, and the heart row has only {@code h-40 .. h-30}
     * before the XP bar at {@code h-29}, which draws after us and silently clips the overflow.
     */
    public static final int BAR_HEIGHT = 9;

    /**
     * <b>Derived, never typed:</b> a stored X offset is only meaningful against the width it was
     * calibrated for. Hard-coding it once already went wrong — widening the bar from 82 to 100
     * left the old {@code -50} behind and pushed every existing config 9px left. The vertical
     * default is the style's, not the element's; see {@link HealthStyle#defaultYOffset()}.
     */
    public static final int DEFAULT_X_OFFSET = BAR_WIDTH / 2 - 91;
    public static final String DEFAULT_ANCHOR = "BOTTOM_CENTER";

    private static final long FLASH_MS = 260;

    private static double lastValue = -1;
    private static long lastDropMs = 0;

    private BeyonderHealthOverlay() {
    }

    /**
     * <b>The replacement has three answers, not two.</b> {@link HealthStyle#HEARTS} needs both
     * ours and vanilla's, with the hearts down <em>first</em> or they land on the readout — so
     * the displaced element is handed in as a {@code Runnable} the gate calls at the moment it
     * decides. A boolean return could express neither the third case nor the ordering.
     */
    public static void initialize() {
        HudElementRegistry.replaceElement(VanillaHudElements.HEALTH_BAR, original -> (ctx, tickCounter) ->
                render(ctx, () -> original.extractRenderState(ctx, tickCounter)));
    }

    /**
     * @param vanillaHearts the displaced element: called instead of us, or before us under HEARTS
     */
    private static void render(GuiGraphicsExtractor ctx, Runnable vanillaHearts) {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        HudConfig.HudSettings settings = HudConfig.getSettings();

        if (HudGate.blocked(client, settings) || !settings.showBeyonderHealth
                || !BeyonderState.hasHealthData()) {
            vanillaHearts.run();
            return;
        }
        // Creative and spectator: hand the frame back rather than re-deriving vanilla's rule
        if (client.gameMode == null || !client.gameMode.canHurtPlayer()) {
            vanillaHearts.run();
            return;
        }

        double poolMax = BeyonderState.maxHealth();
        float vanillaMax = player.getMaxHealth();
        if (poolMax <= 0 || vanillaMax <= 0) {
            vanillaHearts.run();
            return;
        }

        HealthStyle style = HealthStyle.parse(settings.beyonderHealthStyle);
        if (style.drawsOverVanillaHearts()) vanillaHearts.run();

        // poolMax is read fresh every frame: True Form doubles it mid-fight, and a lerped
        // maximum would make the readout disagree with the sheet
        float fraction = Mth.clamp(player.getHealth() / vanillaMax, 0f, 1f);
        double current = poolMax * fraction;
        double absorption = player.getAbsorptionAmount() * poolMax / vanillaMax;

        long now = System.currentTimeMillis();
        noteDrop(current, now);

        boolean calm = settings.epilepsyMode;
        float flash = calm ? 0f : flashAmount(now);
        float pulse = calm ? 0f : pulseAmount(fraction, now);

        int w = client.getWindow().getGuiScaledWidth();
        int h = client.getWindow().getGuiScaledHeight();
        int[] pos = anchor(w, h, settings);

        HudScale.push(ctx, pos[0], pos[1], settings.beyonderHealthScale);
        HealthBarPaint.draw(ctx, style, pos[0], pos[1], BAR_WIDTH, BAR_HEIGHT,
                current, poolMax, absorption, flash, pulse);
        HudScale.pop(ctx);
    }

    private static void noteDrop(double current, long now) {
        if (lastValue >= 0 && current < lastValue - 0.001) lastDropMs = now;
        lastValue = current;
    }

    /**
     * 1 the instant damage lands, decaying to 0 over {@value #FLASH_MS}ms.
     */
    private static float flashAmount(long now) {
        long sinceDrop = now - lastDropMs;
        if (lastDropMs <= 0 || sinceDrop >= FLASH_MS) return 0f;
        return 1f - sinceDrop / (float) FLASH_MS;
    }

    /**
     * Threshold is {@link HealthBarPaint#LOW_FRACTION} rather than a second copy of it, so the
     * throb starts exactly where the fill finishes blending to the alarm colour: one cue, not two.
     */
    private static float pulseAmount(float fraction, long now) {
        if (fraction >= HealthBarPaint.LOW_FRACTION) return 0f;
        return (float) (0.5 + 0.5 * Math.sin(now * 0.009));
    }

    /**
     * Top-left of the box — the point the element scales about. All four styles occupy the same
     * box, so switching style never moves the element and the editor need not ask which is on.
     */
    public static int[] anchor(int screenW, int screenH, HudConfig.HudSettings s) {
        return HudAnchor.parse(s.beyonderHealthAnchor).resolve(
                screenW, screenH, HudScale.size(BAR_WIDTH, s.beyonderHealthScale),
                s.beyonderHealthXOffset, s.beyonderHealthYOffset, s.beyonderHealthYOffset);
    }

    /**
     * Switches style, <b>carrying the placement across only if the player never moved it</b>.
     * The two sets of defaults sit twenty pixels apart, so an untouched element must move with
     * the style or picking HEARTS prints the readout over the hearts it just restored — and a
     * dragged one must not, or the position the player chose is snatched back.
     */
    public static void applyStyle(HudConfig.HudSettings s, HealthStyle next) {
        boolean untouched = atDefaults(s, HealthStyle.parse(s.beyonderHealthStyle));
        s.beyonderHealthStyle = next.name();
        if (untouched) resetPosition(s, next);
    }

    public static boolean atDefaults(HudConfig.HudSettings s, HealthStyle style) {
        return DEFAULT_ANCHOR.equals(s.beyonderHealthAnchor)
                && s.beyonderHealthXOffset == DEFAULT_X_OFFSET
                && s.beyonderHealthYOffset == style.defaultYOffset();
    }

    public static void resetPosition(HudConfig.HudSettings s, HealthStyle style) {
        s.beyonderHealthAnchor = DEFAULT_ANCHOR;
        s.beyonderHealthXOffset = DEFAULT_X_OFFSET;
        s.beyonderHealthYOffset = style.defaultYOffset();
    }

    /**
     * Sample data only: a preview that read {@link BeyonderState} would flicker with every hit.
     */
    public static void renderPreview(GuiGraphicsExtractor ctx, int w, int h, HudConfig.HudSettings s) {
        int[] pos = anchor(w, h, s);
        HudScale.push(ctx, pos[0], pos[1], s.beyonderHealthScale);
        HealthBarPaint.draw(ctx, HealthStyle.parse(s.beyonderHealthStyle),
                pos[0], pos[1], BAR_WIDTH, BAR_HEIGHT, 1234, 1750, 175, 0f, 0f);
        HudScale.pop(ctx);
    }

    public static void reset() {
        lastValue = -1;
        lastDropMs = 0;
    }
}
