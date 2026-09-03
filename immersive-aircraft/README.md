# Immersive Aircraft

A port of [Luke100000/ImmersiveAircraft](https://github.com/Luke100000/ImmersiveAircraft) to
Minecraft **26.2** / Fabric Loader 0.19.3 / Fabric API 0.158.0+26.2 / Java 25, restructured as a
plain single-loader Fabric mod (no Architectury, no Forge target).

Upstream is GPL-3.0-only; the original `LICENSE` is kept as-is. All credit for design, art, and
the original implementation goes to Luke100000 and contributors.

## Status: partial port, work in progress

This is a mechanical/API port from upstream's `1.20.1` branch, not a feature-complete release.
See the porting notes below for what works, what's stubbed, and what's dropped.

### What works
- Builds cleanly (`./gradlew build`) and launches (`./gradlew runClient`) with no crash on mod
  init or world join (compile- and boot-verified; see caveats below for what's behavior-verified
  vs reasoned-through-code-only).
- Item and entity registration (aircraft items, weapon items, upgrade items, all vehicle/bullet
  entity types), creative tab, recipes/data loaders, sounds.
- Vehicle server-side logic: the custom "cobalt" flight/physics model, damage & repair, fuel
  consumption, boosting, engine simulation, inventory & upgrade slots, weapon mounts and firing,
  networking (ported to the modern `CustomPacketPayload` API).
- Basic interaction: place the item to spawn a vehicle, mount/dismount, take damage, drop on
  destruction.
- Vehicle entity rendering: all 7 vehicle types, weapons, and bullets render via the current
  `createRenderState()`/`submit(SubmitNodeCollector)` deferred pipeline
  (`client/render/entity/renderer/`).
- **Vehicle inventory GUI** (`client/gui/VehicleScreen.java`, `SlotRenderer.java`). Ported to
  `AbstractContainerScreen`'s new `extractContents`/`GuiGraphicsExtractor` API (the old
  `GuiGraphics`/`render()` split is gone — screens now build a render state ahead of time, same
  idea as the entity renderer pipeline). `ClientNetworkManager.handleOpenGuiRequest` now actually
  opens the screen instead of no-op'ing. The old `VehicleScreenRegistry` indirection was dropped
  (only one implementation ever existed) in favor of opening `VehicleScreen` directly. Fuel
  slots accept items and `EngineVehicle.refuel()`/`Utils.getFuelTime()` (level-based
  `Level.fuelValues()` lookup) were already wired server-side — compile-verified, boot-verified
  to the title screen; not verified in an actual play session (no GUI-driving harness available
  in this environment, see below).
- **HUD flight overlay** (`client/OverlayRenderer.java`, `client/hud/*`). Speed/altitude/attitude/
  compass/warning instruments, ported to `GuiGraphicsExtractor` (mostly a rename of
  `GuiGraphics`→`GuiGraphicsExtractor` and `drawString`→`text`; `FastColor.ARGB32`→`ARGB`;
  `GuiGraphics#renderOutline` was dropped from the API and reimplemented as 4 fills in
  `OverlayRenderer.renderOutline`). Registered via the new Fabric
  `HudElementRegistry.attachElementAfter(VanillaHudElements.MOUNT_HEALTH, ...)` (the old
  `GuiMixin` injecting into `Gui#renderVehicleHealth` doesn't apply here — HUD rendering moved to
  a layered `HudElement` registry). Compile-verified only; not visually confirmed in-flight.
- **Item dye tinting** (`assets/immersiveaircraft/items/{warship,airship,cargo_airship}.json`).
  The old `ItemColor`/`ColorProviderRegistry`/`DyeableLeatherItem` system is gone; tinting is now
  a data-driven `"minecraft:dye"` tint source on the item model (same pattern as the sibling
  `glider` mod's `glider.json`), which reads the vanilla `DataComponents.DYED_COLOR` component
  automatically — no Java-side color provider needed. `VehicleEntity.createItemStack()` (used by
  both `drop()` and `getPickResult()`) now sets that component from the vehicle's tracked
  `DyeableVehicleEntity#getDyeColor()` when dropping/picking. (One asymmetry left as-is: a stack
  re-dyed after pickup, e.g. in a cauldron, won't feed back into the entity's tracked color on
  placement — only entity→item was requested/ported.)
- **TinyTNT projectile renderer** (`client/render/entity/renderer/bullet/TinyTNTRenderer.java`).
  Replaced `PlaceholderEntityRenderer` with a real renderer mirroring vanilla's
  `TntMinecartRenderer`: resolves a `BlockModelRenderState` for `Blocks.TNT.defaultBlockState()`
  via `BlockModelResolver` (from `EntityRendererProvider.Context#getBlockModelResolver()`) once
  per frame in `extractRenderState`, then draws it with the same
  `TntMinecartRenderer.submitWhiteSolidBlock(...)` vanilla now exposes for exactly this purpose.
  Compile-verified only.

### Stubbed / dropped
- **Camera/FOV/player-pose mixins — mostly ported, one dropped** (see
  `mixin/client/{AbstractClientPlayerMixin,CameraMixin,AvatarRendererMixin}.java`):
  - `AbstractClientPlayer#getFieldOfViewModifier` — ported. Signature gained two params
    (`boolean firstPerson, float effectScale`); behavior (0.05 zoom while scoping a weapon) is
    unchanged.
  - Camera roll/pitch matching the vehicle's bank angle — ported, but retargeted. The old
    `GameRendererMixin` injected extra `PoseStack` rotations into `GameRenderer#renderLevel` right
    after its `Camera#setup(...)` call; that call shape is gone entirely (`renderLevel` now takes
    just a `DeltaTracker`, camera state is produced once via
    `Camera#extractRenderState(CameraRenderState, float)` and consumed later by the deferred
    pipeline). Re-implemented as a `Camera` mixin injecting into `extractRenderState`, rotating
    `CameraRenderState.orientation` by the same roll/pitch upstream applied. Compile-verified
    only — the exact sign/composition-order equivalence to the old `PoseStack.mulPose` chain is
    reasoned, not visually confirmed.
  - Third-person zoom offset while riding a vehicle (old `CameraMixin` on `Camera#setup`) —
    **ported**. The previous pass's read of `Camera#alignWithEntity(float)` as an unreachable dead
    end was wrong: it's `private`, not un-injectable — Mixin merges mixin bytecode directly into
    the target class, so a private method is just as valid an `@Inject` target as a public one
    (only the `@Shadow` stub for its private helpers needs a `private`, not `public`/`protected`,
    modifier to match). `alignWithEntity` already moves the detached third-person camera back by
    the rider's base `Attributes.CAMERA_DISTANCE`; `CameraMixin#ia$alignWithEntity` injects at its
    `TAIL` and, when the root vehicle is a `VehicleEntity`, nudges the camera back further by
    `vehicle.getZoom()` via the same private `move`/`getMaxZoom` helpers the old mixin shadowed
    (signatures changed from `double` to `float`) — additive on top of the vanilla base distance,
    matching upstream's original behavior exactly. Compile-verified; not visually confirmed
    in-flight (see caveats).
  - Hiding/adjusting the player model while seated (old `PlayerEntityRendererMixin` on
    `PlayerRenderer#setModelProperties`) — **ported**. Re-reading the original mixin: it never hid
    the player model at all, it only forced `PlayerModel#crouching = false` while the root vehicle
    was a `VehicleEntity` (suppressing the crouch pose/eye-height drop while seated) — the previous
    pass's characterization of this as "hiding/adjusting the player model" and a required
    `AvatarRenderState` field addition was an overread of a much smaller original. `PlayerRenderer`
    is now `AvatarRenderer` and `setModelProperties` is gone, but the crouch flag it used to poke
    directly on the model now lives on the render state instead:
    `AvatarRenderer#extractRenderState` (public) calls
    `HumanoidMobRenderer#extractHumanoidRenderState`, which sets
    `AvatarRenderState`/`HumanoidRenderState#isCrouching = entity.isCrouching()` — read later by
    `PlayerModel`'s pose setup and by `AvatarRenderer#getRenderOffset`. New
    `mixin/client/AvatarRendererMixin` injects at the `TAIL` of `extractRenderState` and forces
    `state.isCrouching = false` under the same root-vehicle condition. Compile-verified; not
    visually confirmed in-flight.
  - The old `LivingEntityRendererMixin` on `LivingEntityRenderer#setupRotations` (rotating the
    rider's `PoseStack` by the vehicle's view pitch/roll) has no separate equivalent needed here —
    it's superseded by the already-ported `CameraMixin#ia$extractRenderState`, which rotates the
    shared `CameraRenderState.orientation` instead (see the "camera roll/pitch" entry above); doing
    it once on the camera achieves the same visual effect as doing it per-renderer on the rider.
  - F3+B vehicle hitbox overlay (old `EntityRenderDispatcherMixin` on
    `EntityRenderDispatcher#renderHitbox`) — **dropped**. That debug rendering moved to a
    dedicated `EntityHitboxDebugRenderer`/`HitboxRenderState` subsystem, unrelated to
    `EntityRenderDispatcher`; lowest priority of the mixins (debug-only visual), not chased down.
- Ad Astra low-gravity integration dropped (Ad Astra isn't ported to this MC version).
- JEI/REI/ModMenu integrations dropped (none of those mods target this MC version either;
  `combat/CombatUtils.java`, a JEI recipe-area helper, was removed rather than ported).

### Verification caveats
None of the newly-ported client rendering in this pass was exercised in an actual play session —
this environment has no way to drive Minecraft's own window (mouse/keyboard/GUI automation is
only available for the browser and iOS simulator here, and the sibling `bot-build` module is an
unrelated in-game building assistant, not a test-automation harness). Everything above is
compile-verified (`./gradlew build`) and boot-verified (`./gradlew runClient` reaches the title
screen with no exceptions, mixin-apply failures, or mod-load errors in the log). Actually spawning
a vehicle, opening its inventory, inserting fuel, and flying it were reasoned through the code
(and, for the GUI, cross-checked against the already-working server-side `VehicleScreenHandler`)
but not clicked through by a human or a bot.

### Notable API adaptations made during the port
- `ResourceLocation` → `Identifier`, `Entity.hurt()` → `hurtServer`/`hurtClient`,
  `Entity.getGravity()` became `final` (the mod's own gravity-chain method was renamed
  `getCustomGravity()` to avoid the clash), `addAdditionalSaveData`/`readAdditionalSaveData` moved
  from raw `CompoundTag` to `ValueOutput`/`ValueInput` (bridged via one `CompoundTag`-shaped
  component per entity, see `VehicleEntity.addLegacySaveData`/`readLegacySaveData`), `ItemStack`
  NBT (`getOrCreateTag`/`hasTag`/`getTag`) replaced by a custom `DataComponentType<CompoundTag>`
  (`ItemTagCompat`), Fabric's `FuelRegistry` replaced by vanilla's data-driven `Level.fuelValues()`.
- `GuiGraphics` was removed; screens and HUD elements now build a `GuiGraphicsExtractor` render
  state in an `extractContents`/`extractRenderState` pass instead of drawing immediately in
  `render()`/`renderVehicleHealth()`-style callbacks. `drawString` → `text`,
  `FastColor.ARGB32` → `ARGB`, `GuiGraphics#renderOutline` was dropped (reimplemented locally).
  HUD elements are now registered via Fabric's `HudElementRegistry` rather than a `Gui` mixin.
  `PlayerRenderer` → `AvatarRenderer`; `LivingEntityRenderer#setupRotations` now takes a
  render-state object instead of the entity, and `#setModelProperties` was removed outright.
  `Camera#setup` was replaced by a private, attribute-driven `alignWithEntity`, with a new public
  `Camera#extractRenderState(CameraRenderState, float)` as the closest stable hook for
  camera-orientation mods. Item tinting moved from `ItemColor`/`ColorProviderRegistry` to
  data-driven `"tints"` sources on item models, reading vanilla data components
  (e.g. `DataComponents.DYED_COLOR`) directly.

## Building / running

```
./gradlew build
./gradlew runClient
```
