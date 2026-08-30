# White Flag

Craft a white flag. Keep it anywhere in your hotbar and hostile mobs stop attacking you.

- Minecraft 26.2, Fabric Loader 0.19.3+, Fabric API
- Server-side authoritative; works in singleplayer and on servers (install on the server)

## Crafting

A pole of 3 sticks with cloth at the top:

```
| #
|
|
```

`|` stick, `#` any of: white wool, paper, white carpet, white banner.

Every recipe this mod adds is unlocked from the start — no prerequisite, nothing to discover
first.

## Bleaching

Bone meal plus wool of any colour, shapeless, gives white wool back — so the flag stays
craftable from whatever wool you happen to be carrying. White dye already does this in
vanilla; this adds the bone meal half.

## Debug commands

`/whiteflag` puts one flag in your inventory (drops at your feet if it is full).
Requires permission level 2, so op or cheats.

## How it works

One mixin on `LivingEntity.canBeSeenAsEnemy()`. A flag bearer is not a valid enemy, so
`Mob.asValidTarget` filters them out — which blocks new target acquisition *and* drops targets
mobs already hold, for goal-driven mobs and brain-driven ones (piglins, hoglins, warden) alike.

Not covered on purpose: projectiles already in flight, a creeper already mid-fuse, and damage
from other players.

## Tests

```bash
./gradlew runGametest
```

Headless — no client, no EULA.

## Building

```
./gradlew build
```

Jar lands in `build/libs/`.
