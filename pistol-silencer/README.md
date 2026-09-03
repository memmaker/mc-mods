# Pistol & Silencer

Fabric mod for Minecraft 26.2. A pistol with a detachable silencer, ported from
[MrCrayfish's Gun Mod](https://github.com/MrCrayfish/MrCrayfishGunMod) (CGM).

Only the pistol exists here. Its stats are taken straight from CGM's
`data/cgm/guns/pistol.json` and hardcoded, rather than porting CGM's whole
JSON-datapack gun system for one gun.

## The pistol

| | |
|---|---|
| Damage | 9.0 |
| Range | 60 blocks |
| Magazine | 16 rounds |
| Fire rate | one shot per 4 ticks |
| Spread | 1° |
| Recoil | 2.5° upward |

- **Right-click** fires. Shooting is a hitscan raycast, not a projectile entity:
  CGM's bullet covers 250 blocks in 25 ticks, which is indistinguishable from
  instant at any range you actually fight at.
- **R** (rebindable) reloads from Pistol Ammo in your inventory. Creative mode
  reloads for free.
- **Sneak + right-click** attaches a silencer from your off hand, or takes an
  attached one back off. A silenced shot is quieter and has its own sound; the
  attachment is stored as vanilla `custom_model_data`, so the item model swaps
  natively.
- Ammo count shows in the tooltip and as the item's durability bar.

## Crafting

All three recipes are unlocked on join, so they show in the recipe book from the
start. The items are also in the Combat creative tab.

**Pistol**

```
I I     I = iron ingot
R F     R = redstone, F = flint
```

**Silencer**

```
N N N   N = iron nugget
W W W   W = any wool
```

**Pistol Ammo** — shapeless, makes 8: one iron nugget + two gunpowder.

## Headshots

If [FX Globals](../fx-globals) is installed with headshots on, a shot through a
mob's head kills it outright. A hitscan raycast never becomes an arrow entity,
so fx-globals' own mixin cannot see it; this mod calls its public
`Headshots.apply` by reflection, which keeps every mod in the pack building on
its own with no cross-dependency.

The client also helps aim: while you hold a pistol it reads the real head box
off the target's render model (fx-globals tracks it) and sends it to the server,
so the hit test uses where the head actually is instead of the top slice of the
hitbox. Without fx-globals or without a client hint, nothing breaks — there is
simply no headshot.

## Build

```
JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home ./gradlew build
```

Output: `build/libs/pistol-silencer-1.0.0.jar`. Requires Fabric Loader >= 0.19.3,
Fabric API, Java 25.

## Tests

```
JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home ./gradlew runGametest
```

Runs each of the three crafting grids through the server's own recipe lookup, so
a typo in the hand-written recipe JSON shows up as a failed test rather than as
an item that quietly cannot be crafted.

## License

GPL-3.0, see [LICENSE](LICENSE) — inherited from upstream CGM, and what
`fabric.mod.json` declares.
