package dev.ua.ikeepcalm.coi.hud;

import dev.ua.ikeepcalm.coi.config.HudConfig;
import dev.ua.ikeepcalm.coi.hud.layout.HudLayout;

import net.minecraft.client.Minecraft;

/**
 * The opening of every COI overlay's render gate. The {@link HudLayout#editing()} term is the
 * one that matters and the easiest to leave out: an overlay that forgets it draws its live self
 * underneath the editor's preview, which is the one way to break the layout editor. Call this
 * rather than retyping the chain.
 */
public class HudGate {

    private HudGate() {
    }

    public static boolean blocked(Minecraft client, HudConfig.HudSettings settings) {
        return client.player == null
                || client.gui.hud.isHidden()
                || HudLayout.editing()
                || !settings.enabled;
    }
}
