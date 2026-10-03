package dev.ua.ikeepcalm.coi;

import dev.ua.ikeepcalm.coi.config.ClientDataLoader;
import dev.ua.ikeepcalm.coi.config.ClientStateStore;
import dev.ua.ikeepcalm.coi.config.HudConfig;
import dev.ua.ikeepcalm.coi.domain.ability.service.AbilityBindings;
import dev.ua.ikeepcalm.coi.domain.ability.service.AbilityRegistry;
import dev.ua.ikeepcalm.coi.domain.beyonder.model.*;
import dev.ua.ikeepcalm.coi.domain.ceremony.CeremonyEffects;
import dev.ua.ikeepcalm.coi.domain.effect.EffectManager;
import dev.ua.ikeepcalm.coi.domain.effect.HallucinationManager;
import dev.ua.ikeepcalm.coi.domain.form.FormModelLayers;
import dev.ua.ikeepcalm.coi.domain.form.MythicalFormManager;
import dev.ua.ikeepcalm.coi.domain.menu.service.MenuState;
import dev.ua.ikeepcalm.coi.hud.overlay.*;
import dev.ua.ikeepcalm.coi.input.CoiKeyBindings;
import dev.ua.ikeepcalm.coi.network.CoiNetworking;
import dev.ua.ikeepcalm.coi.network.ServerCapabilities;
import dev.ua.ikeepcalm.coi.screen.InventoryHint;
import dev.ua.ikeepcalm.coi.screen.TourScreen;
import dev.ua.ikeepcalm.coi.screen.debug.ArchivePreviewSmoke;
import dev.ua.ikeepcalm.coi.ui.IconModels;
import dev.ua.ikeepcalm.coi.ui.IngredientTooltips;
import dev.ua.ikeepcalm.coi.util.hooks.DiscordPresenceManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;

/**
 * The mod's client entry point: load what is on disk, register what draws and what listens,
 * wire the two connection events. It holds no state of its own except the pending tour.
 */
public class CoiClient implements ClientModInitializer {

    private static final ClientDataLoader CLIENT_DATA_LOADER = new ClientDataLoader();

    private static final long TOUR_DELAY_MS = 3000;

    // When > 0, the first-join tour opens at this timestamp
    private static long tourPendingAt = 0;

    private static void registerOverlays() {
        AbilityOverlay.initialize();
        // The one element that replaces a vanilla one rather than attaching beside it
        BeyonderHealthOverlay.initialize();
        CharacterPlateOverlay.initialize();
        MadnessOverlay.initialize();
        SpiritualityOverlay.initialize();
        ActingOverlay.initialize();
        ResourceOverlay.initialize();
        ActionBarOverlay.initialize();
        TargetHealthOverlay.initialize();
        CogitationOverlay.initialize();
        NotificationOverlay.initialize();
        EffectManager.initialize();
        InventoryHint.register();
        FormModelLayers.register();
    }

    private static void registerConnectionEvents() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            CoiNetworking.sendHello();
            CoiNetworking.requestAbilitiesFromServer();
            var server = client.getCurrentServer();
            DiscordPresenceManager.onServerJoin(
                    server != null ? server.name : "Singleplayer",
                    server != null ? server.ip : null
            );
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> onDisconnect());
    }

    /**
     * Deliberately exhaustive rather than clever: a holder missing from this list is the bug
     * that shows the previous server's madness on the next one.
     */
    private static void onDisconnect() {
        // The title screen holds a grudge, and lights that emblem on its wheel
        ClientStateStore.setLastMadness(BeyonderState.getMadness());
        ClientStateStore.setLastPathway(BeyonderState.getPathway());
        BeyonderState.reset();
        ActingState.reset();
        ResourceState.reset();
        ActionBarState.reset();
        TargetState.reset();
        CogitationState.reset();
        NotificationState.reset();
        SheetState.reset();
        MenuState.reset();
        AppearanceState.reset();
        ServerCapabilities.reset();
        AbilityRegistry.reset();
        SpiritualityOverlay.reset();
        BeyonderHealthOverlay.reset();
        IconModels.clearCache();
        EffectManager.stopAll();
        MythicalFormManager.clearAll();
        tourPendingAt = 0;
        DiscordPresenceManager.onDisconnect();
    }

    /**
     * The mod's one client-tick subscription, so the order is readable in one place.
     */
    private static void registerTickHandler() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            HallucinationManager.tick(client);
            DiscordPresenceManager.tick();
            // Before the player check: a world going away is when a ceremony must be released
            CeremonyEffects.tick(client);

            if (client.player == null) return;

            tickTour(client);
            CoiKeyBindings.tickKeys(client);
        });
    }

    public static void scheduleTourIfFirstList() {
        if (AbilityRegistry.hasAbilities() && ClientStateStore.isTourNotCompleted() && tourPendingAt == 0) {
            tourPendingAt = System.currentTimeMillis() + TOUR_DELAY_MS;
        }
    }

    /**
     * Stays pending until no other screen is in the way.
     */
    private static void tickTour(Minecraft client) {
        if (tourPendingAt > 0 && System.currentTimeMillis() >= tourPendingAt && client.gui.screen() == null) {
            tourPendingAt = 0;
            if (ClientStateStore.isTourNotCompleted()) {
                client.gui.setScreen(new TourScreen());
            }
        }
    }

    @Override
    public void onInitializeClient() {
        HudConfig.load();
        ClientStateStore.load();
        AbilityBindings.load();
        CoiNetworking.registerPayloads();
        CoiKeyBindings.registerKeybindings();
        ArchivePreviewSmoke.register();
        registerTickHandler();
        registerOverlays();
        registerConnectionEvents();

        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> DiscordPresenceManager.shutdown());

        ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloadListener(
                Identifier.fromNamespaceAndPath("coi-client", "client_json_loader"), CLIENT_DATA_LOADER);

        IngredientTooltips.register(CLIENT_DATA_LOADER);
    }
}
