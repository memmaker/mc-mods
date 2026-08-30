# FX Globals

Fabric mod for Minecraft 26.2. World pacing, nothing else: days last 1.5x
longer, hunger drains at a quarter of the usual rate, and every arrow lying in
the world can be collected. All three are configurable. No items, no recipes.

## What it changes

| Setting | Vanilla | Default here | Range |
|---|---|---|---|
| Day-night cycle | 20 min (24000 ticks) | 30 min — the overworld clock runs at rate `0.6667` | 25%–800% |
| Hunger | 4 exhaustion per saturation/food point | 16 — exhaustion is scaled by `0.25` | 0%–400% |
| Arrow pickup | Only your own arrows | Mob-shot arrows too | on/off |

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

## Configuration

`config/fxglobals.properties`, written on first launch:

```properties
day_length_factor=1.5
hunger_factor=0.25
pickup_arrows=true
```

`1.0` is vanilla for both factors, and `pickup_arrows=false` is vanilla pickup.
Values outside the ranges above are clamped, and anything unparseable falls back
to the default — the day-length floor is what keeps a clock rate of `1/0` from
ever happening.

With [Mod Menu](https://modrinth.com/mod/modmenu) installed, the same three
settings get a config screen — a slider each for the factors (in whole percent)
and a toggle for arrow pickup — reachable from the mod list. Mod Menu is
optional: without it the mod reads the file and nothing else changes.

Hunger and arrow pickup take effect immediately. Day length takes effect
immediately in single-player; on a dedicated server the file that counts is the
server's, and a client-side screen cannot change it.

## Debug commands

| Command | What |
|---|---|
| `/fxglobals`, `/fxglobals status` | Prints the day-length factor, the live clock rate, the hunger factor, the arrow-pickup setting and the current clock tick |

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
either way, and the debug command staying registered. JUnit XML lands in `build/gametest-report.xml`.

## License

MIT, see [LICENSE](LICENSE).
