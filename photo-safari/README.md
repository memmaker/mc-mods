# Photo Safari

A Fabric mod for Minecraft 26.2. Turns [Camerapture](https://modrinth.com/mod/camerapture) photography into a
collection game: photograph every kind of living creature in the world.

- Every mob that ends up **in frame and not hidden behind blocks** counts as a species on film.
- Advancements at 1, 10, 25 and 50 species, each with an experience reward; the last one also drops loot.
- One advancement per mob under the **Photo Safari** tab, so the advancement screen *is* the checklist:
  earned = photographed, greyed out = still missing.
- Photos, picture frames and albums come from Camerapture, which is a required dependency.
- Alex's Mobs Continued is supported: its 88 mobs get their own checklist entries, gated behind a
  `fabric:all_mods_loaded` condition so the same jar works with or without it installed.
- Every 10 species photographed pays out an **Eye of Ender**.
- The camera has a second mode: **peaceful loot**, below.

## Peaceful loot

Hold the camera up and press the **Cycle Camera Mode** key (`C` by default) to switch between
photograph mode and loot mode; the mode sticks until you change it again.

In loot mode the camera outlines every mob it is currently framing — green for lootable, red for
one on cooldown — and pressing the trigger drops that mob's death loot straight into your
inventory instead of taking a picture. The mob is not hurt, no paper is spent and nothing is saved
to disk; you get the shutter sound, the swing and the same 3-second cooldown a real photo has.

One mob per trigger, and a mob you have just looted goes on cooldown until 99 other mobs have been
looted after it. Loot mode grants the loot table, worn equipment and mob-specific bonus drops, and
awards the same experience the mob would have dropped. It never counts toward the species
checklist — only photographs do.

Loot mode is enforced server-side: with `peaceful_loot=false` the server ignores the packet
entirely, so a modified client cannot re-enable it.

## Configuration

`config/photosafari.properties`, written on first launch:

```properties
peaceful_loot=true
```

With [Mod Menu](https://modrinth.com/mod/modmenu) installed the same toggle gets a config screen
in the mod list. Mod Menu is optional — the jar works fine without it.

## Commands

| Command | Who | What |
|---|---|---|
| `/photosafari camera [paper]` | anyone | Gives a camera (and paper, default 1) — handy in survival for testing |
| `/photosafari progress` | anyone | Species photographed so far, out of all registered mob types |
| `/photosafari reset` | gamemasters | Wipes your own progress |

## How detection works

The client knows what the photo actually looked like, so it does the framing check
(field of view including viewfinder zoom, screen aspect ratio, block occlusion raycast)
and sends the entity IDs to the server. The server never trusts that list: it re-runs the same
check with a wide 110° cone and its own raycasts before crediting anything. Loot mode goes through
the exact same check, so what the outline shows is what the trigger acts on.

## Building

```
./gradlew build
```

The jar lands in `build/libs/`. Requires JDK 25.

The advancements in `src/main/generated` are datagen output, built from the entity registry
(every mob that has a spawn egg, so mobs from other mods come along). Regenerate after a
Minecraft update:

```
./gradlew runDatagen
```

## Tests

```
./gradlew test runGametest
```

`test` covers the framing math, `runGametest` runs the real thing in a headless world:
a mob in the open counts, the same mob behind a stone wall does not, a mob behind the
camera does not, a verified photo grants its advancement, a client claiming a mob it
cannot see is rejected, milestones pay out one Eye of Ender per 10 species, and loot mode
grants drops without killing, respects the cooldown, and does nothing at all with
`peaceful_loot` off.

## Installing

1. Install Fabric Loader 0.19.4+ for Minecraft 26.2.
2. From Modrinth, install [Fabric API](https://modrinth.com/mod/fabric-api) and
   [Camerapture](https://modrinth.com/mod/camerapture) (both for 26.2).
3. Drop `photosafari-<version>.jar` into your `mods` folder.
