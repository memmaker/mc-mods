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
check with a wide 110° cone and its own raycasts before crediting anything.

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
camera does not, a verified photo grants its advancement, and a client claiming a mob it
cannot see is rejected.

## Installing

1. Install Fabric Loader 0.19.4+ for Minecraft 26.2.
2. From Modrinth, install [Fabric API](https://modrinth.com/mod/fabric-api) and
   [Camerapture](https://modrinth.com/mod/camerapture) (both for 26.2).
3. Drop `photosafari-<version>.jar` into your `mods` folder.
