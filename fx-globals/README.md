# FX Globals

Fabric mod for Minecraft 26.2. Pack-wide pacing and combat tweaks: days last
1.5x longer, hunger drains at a quarter of the usual rate, every arrow lying in
the world can be collected, an arrow through a mob's head kills it outright, and
new players get a starter kit on first join. All five are configurable.

## What it changes

| Setting | Vanilla | Default here | Range |
|---|---|---|---|
| Day-night cycle | 20 min (24000 ticks) | 30 min — the overworld clock runs at rate `0.6667` | 25%–800% |
| Hunger | 4 exhaustion per saturation/food point | 16 — exhaustion is scaled by `0.25` | 0%–400% |
| Arrow pickup | Only your own arrows | Mob-shot arrows too | on/off |
| Headshots | No hit location | An arrow through a mob's head kills it | on/off |
| Starter gear | Nothing on first join | A kit drawn from the installed pack mods | on/off |

The day length uses the vanilla world clock's own rate, the same knob
`/time rate` writes, so sleeping, daylight sensors, mob spawning and the
in-game clock all stay consistent. Hunger is scaled at `FoodData.addExhaustion`,
which every source of hunger routes through: sprinting, jumping, mining,
swimming and natural regeneration all slow down together.

Arrow pickup answers vanilla's own `AbstractArrow.tryPickup` for the arrows it
marks `DISALLOWED` — the ones mobs shot. Creative-spawned arrows (`CREATIVE_ONLY`)
are left alone, since harvesting those into a survival inventory would be an item
duplicator, and the check is limited to the `minecraft:arrows` tag, so a drowned's
trident is still loot rather than a free trident.

The clock rate is world save data. Uninstalling the mod leaves the world slow
until you run `/time rate 1`.

## Headshots

An arrow whose flight path actually crosses a mob's head box kills it outright.
Players are never affected, and neither is anything that is not a mob.

The head box comes from the client, which is the only side that knows where a
mob's head model really sits: `LivingEntityRendererHeadMixin` measures the head
part off the render model and reports it. Without a client hint — a headless
server, or a mob whose model has no head part — the server falls back to the top
quarter of the mob's hitbox, narrowed to 60% of its width.

`Headshots.apply` is public on purpose. Other mods in the pack hit mobs with
their own raycasts rather than arrow entities (the pistol, for one), and call it
by name through reflection so they need no build-time dependency on this mod.

## Starter gear

The first time a player ever joins, they get a kit assembled from whichever
companion mods are installed — each item is looked up by ID, so a missing mod
just means a missing item, never a crash:

| Item | From |
|---|---|
| Postbote satchel | `postbote` |
| Camera + 24 paper | `camerapture` (the paper only comes with the camera) |
| Photo album | `camerapture` |
| Compass, lodestone | vanilla |
| White flag | `whiteflag` |
| Climbing claws | `climbingclaws` |
| Plush boots | `nofall` |
| Standard backpack | `travelersbackpack` |
| Recipe book | this mod |

An attachment on the player records that the kit was handed out, so it never
happens twice — it is persistent and survives death and rejoin. The **Recipe
Book** item is single-use: right-click it and it awards every recipe currently
loaded, this pack's and every other mod's alike, then is consumed.

## Configuration

`config/fxglobals.properties`, written on first launch:

```properties
day_length_factor=1.5
hunger_factor=0.25
pickup_arrows=true
headshots=true
starter_gear=true
```

`1.0` is vanilla for both factors, and `false` is vanilla for all three toggles.
Values outside the ranges above are clamped, and anything unparseable falls back
to the default — the day-length floor is what keeps a clock rate of `1/0` from
ever happening. A boolean that is neither `true` nor `false` is logged rather
than silently read as `false`.

With [Mod Menu](https://modrinth.com/mod/modmenu) installed, the same five
settings get a config screen — a slider each for the factors (in whole percent)
and a toggle for arrow pickup, headshots and starter gear — reachable from the
mod list. Mod Menu is optional: without it the mod reads the file and nothing
else changes.

Hunger, arrow pickup, headshots and starter gear take effect immediately. Day
length takes effect immediately in single-player; on a dedicated server the file
that counts is the server's, and a client-side screen cannot change it.

## Debug commands

| Command | What |
|---|---|
| `/fxglobals`, `/fxglobals status` | Prints the day-length factor, the live clock rate, the hunger factor, the arrow-pickup and headshot settings, and the current clock tick |
| `/fxglobals startergear [targets]` | Hands the starter kit to you, or to the given players. Ignores both the `starter_gear` setting and the once-per-player flag, so the join-time kit can be inspected without making a fresh player |

Needs permission level 2 — op, or a single-player world with cheats.

## Building

```
JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home ./gradlew build
```

Output: `build/libs/fxglobals-1.0.0.jar`. Requires Fabric Loader >= 0.19.3,
Fabric API, Java 25.

## Tests

```
JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home ./gradlew runGametest
```

Covers: the overworld clock ending up at the configured rate, hunger costing
16 exhaustion per saturation point at 25% and 4 at 100%, the config clamping
out-of-range values on a save/load round trip, a mob's arrow being collected
with the setting on and staying put with it off, tridents staying uncollected
either way, a head hit killing a mob while a body hit does not (and neither
does with headshots off), the starter kit landing exactly once and not at all
with the setting off, the recipe book awarding every recipe and being consumed,
and both debug commands — `status` reporting, and `startergear` handing out the
kit again past the once-per-player flag. JUnit XML lands in `build/gametest-report.xml`.

## License

MIT, see [LICENSE](LICENSE).
