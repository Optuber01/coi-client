# Config files and localization

Everything this client writes to disk, and where its strings live.

## Config files

`config/coi_abilities.json` — bound ability ids per key slot (`abilityN`), wheel slot (`wheelN`), and gesture
(`gesture_<id>`)
`config/coi_hud.json` — HUD settings (position/size/scale, display toggles, epilepsy mode, madness bar, spirituality bar
(`showSpiritualityBar`, `spiritualityAnchor`, `spiritualityXOffset`, `spiritualityYOffset`, `spiritualityHideWhenFull`),
acting bar (`showActingBar`, `actingAnchor`, `actingXOffset`, `actingYOffset`), resource bars (`showResourceBars`,
`resourceAnchor`, `resourceXOffset`, `resourceYOffset`, `resourceMaxBars`), overlays (`showActionBar`,
`actionBarXOffset`, `actionBarYOffset`, `actionBarLines`, `showTargetHealth`, `targetHealthXOffset`,
`targetHealthYOffset`, `showCogitationOverlay`, `cogitationXOffset`, `cogitationYOffset`, `showNotifications`,
`notificationXOffset`, `notificationYOffset`), `slotPlacements` (a 10-entry array, each entry `null` for "stay in the
row" or
`{"anchor":"TOP_LEFT","x":0,"y":0}`), the character plate (`showCharacterPlate`, `characterPlateAnchor`,
`characterPlateXOffset`, `characterPlateYOffset`, `characterPlateScale`, `characterPlateOpacity`), the health element
(`showBeyonderHealth`,
`beyonderHealthStyle` — `HEARTS` | `BAR` | `ORNATE` | `PIPS`, default `HEARTS` —
`beyonderHealthAnchor`, `beyonderHealthXOffset`, `beyonderHealthYOffset`, `beyonderHealthScale`),
`showAbilityHud`, `slotSize` (the
ability slots' only size knob, 20–80) plus the nine per-element scales (`madnessScale`, `spiritualityScale`,
`actingScale`, `resourceScale`, `actionBarScale`, `targetHealthScale`, `cogitationScale`, `notificationScale` — all
`1.0`, clamped 0.5–2.0), `layoutVersion`, `effectSoundVolume`, `enableHallucinations`, `activeAbilitySlots`,
`wheelSlots`, `enableDiscordPresence`, `presenceShowMadness`, `useServerMenus`, `coiTitleScreen`,
the ascension ceremony's three comfort settings (`enableCameraShake`, `enableCinematicCamera`,
`ceremonyFogDensity` — see docs/ASCENSION_CEREMONY.md))
`config/coi_client_state.json` — persistent state, not preferences (`ClientStateStore`): last madness values for title
haunting, `lastPathway` for the emblem the title wheel lights, `tourCompleted` for the first-join tour,
`inventoryHintDismissed` for the shortcut-item hint (all survive HUD config resets)

## Localization

`src/main/resources/assets/coi-client/lang/en_us.json` + `uk_ua.json`
Key format: `key.coi.*`, `screen.coi.*`, `notification.coi.*`
