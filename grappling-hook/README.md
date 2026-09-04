# Grappling Hook

Grappling hooks for Minecraft **26.2** (Fabric).

Craft a hook, aim, throw. The rope catches on whatever you hit and you swing from it — the rope
wraps around corners, snags on ledges, and can be cut with shears. Upgrades bolt on at a vanilla
smithing table: a motor that reels you in, a rocket that pushes you along, an ender staff that
teleports the hook, a magnet that pulls blocks, and a second hook so you can throw both at once.
Long fall boots take the landing.

This is a port of [Yyon's Grappling Hook Mod](https://github.com/yyon/grapplemod) by way of
CG360's Fabric port and weaversworkshop's
[GrappleMod: Skybound](https://github.com/weaversworkshop/grapplemod-skybound) fork, which is
where the multiplayer-authoritative hook state and the rope-cutting come from. Full credits and
licences are in [ATTRIBUTIONS.md](ATTRIBUTIONS.md); the mod stays GPL-3.0.

## Crafting

| Item | Recipe |
|---|---|
| Grappling hook | Iron pickaxe + lead (shapeless) |
| Base upgrade | Gold ingots and string |
| Rocket | Iron, amethyst shard, firework rocket |
| Forcefield | Iron, amethyst shards, recovery compass |
| Ender staff | Ender pearl, amethyst shard, stick |
| Long fall boots | Smithing: boots + template + phantom membrane |

Hook upgrades apply at a smithing table: hook in the base slot, the base upgrade as the template,
and the upgrade item as the addition. Every recipe is unlocked from the first tick, so they all
show up in the recipe book straight away.

## Controls

Keys live under **Grappling Hook** in Controls. Every hook key falls back to a sensible vanilla
key until you bind it: throw is use, detach is jump, reel/dampen is sneak, ender launch and rocket
are attack.

## What changed in the port

Upstream targeted 1.21.1. 26.2 rewrote entity rendering, item models, armour, recipes, gamerules
and NBT, so a few things did not survive the move:

- **Create / Sable compatibility.** Neither mod exists for 26.2, so the two compat modules are
  gone. The hook's moving-anchor code is still there behind the integration interfaces.
- **3D long fall boots.** They use vanilla's equipment layer, so the boots render flat rather than
  through a custom model.
- **The bundled resource packs** (classic textures, simplified, classic recipes) and the F3 debug
  readout, both written against the old asset layout and the old debug overlay.
- **The customization GUI widgets**, which upstream had already disconnected from any screen.

## Building

```bash
./gradlew build
```

Game tests:

```bash
./gradlew runGametest
```
