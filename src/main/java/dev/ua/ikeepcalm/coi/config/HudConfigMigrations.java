package dev.ua.ikeepcalm.coi.config;

import dev.ua.ikeepcalm.coi.config.HudConfig.HudSettings;
import dev.ua.ikeepcalm.coi.hud.HudAnchor;
import dev.ua.ikeepcalm.coi.hud.overlay.BeyonderHealthOverlay;
import dev.ua.ikeepcalm.coi.hud.render.HealthStyle;

/**
 * Brings an older {@code coi_hud.json} up to {@link HudConfig#LAYOUT_VERSION}. Each step is
 * guarded by the version that introduced it and they run in ascending order, so a file at
 * version 0 gets <em>all</em> of them. Collapsing or reordering the guards breaks that.
 */
final class HudConfigMigrations {

    private HudConfigMigrations() {
    }

    /**
     * v2 — a TOP-anchored madness bar used to ignore {@code madnessYOffset}, so the value in
     * the file is whatever the BOTTOM anchor would have used; keeping it would teleport every
     * existing bar the moment the offset started mattering.
     * <p>
     * v3 — retired {@code hudScale} (which <em>divided</em> the row's offsets rather than
     * scaling anything) and {@code slotSpacing} (a second size knob free to disagree with
     * {@code slotSize}). Neither has a position-preserving conversion, so the offsets are kept
     * verbatim and the keys dropped on the next save: <b>v3 deliberately has no step.</b>
     * <p>
     * v4 — {@code HudAnchor.resolve} centres on the element's width, so a stored X offset is
     * only meaningful against the width it was calibrated for; widening the health bar left
     * every existing config 9px off. The offset is now derived, and a pre-v4 file is reset.
     * <p>
     * v5 — a pre-v5 file predates {@link HealthStyle} entirely, so its placement was calibrated
     * for a bar sitting <em>on</em> the hearts' row, where the new default's readout would land
     * on the hearts it has just handed back.
     */
    static void migrate(HudSettings s) {
        if (s.layoutVersion < 2 && HudAnchor.parse(s.madnessAnchor).isTop()) {
            s.madnessYOffset = HudConfig.DEFAULT_MADNESS_Y;
        }
        if (s.layoutVersion < 4) {
            s.beyonderHealthAnchor = BeyonderHealthOverlay.DEFAULT_ANCHOR;
            s.beyonderHealthXOffset = BeyonderHealthOverlay.DEFAULT_X_OFFSET;
            s.beyonderHealthYOffset = HealthStyle.BAR.defaultYOffset();
        }
        if (s.layoutVersion < 5) {
            s.beyonderHealthStyle = HealthStyle.HEARTS.name();
            BeyonderHealthOverlay.resetPosition(s, HealthStyle.HEARTS);
        }
        s.layoutVersion = HudConfig.LAYOUT_VERSION;
    }
}
