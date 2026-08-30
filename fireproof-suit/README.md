# Fireproof Suit

Fabric mod for Minecraft 26.2. A magma-forged armour set that takes the sting
out of fire and lava — a quarter of it per piece worn, all of it in the full
suit.

| Worn | Fire & lava damage | Time spent on fire |
|---|---|---|
| Nothing | 100% | 100% |
| One piece | 75% | 75% |
| Two pieces | 50% | 50% |
| Three pieces | 25% | 25% |
| All four | none — you are immune | you never catch fire |

Immunity covers every fire damage type: standing in fire, burning, lava,
campfires, magma blocks and fireballs. It does not cover anything else — a
full suit still drowns, still falls and still takes a hit.

The pieces themselves survive being dropped in lava, the way netherite gear
does.

## Crafting

Vanilla armour patterns in magma cream:

```
MMM      M M      MMM      . . .
M M      MMM      M M      M M
. .      MMM      M M      M M
helmet   chest    leggings  boots
```

Twenty-four magma cream for the set, which is nether-gated: blaze powder plus
slimeballs. Magma cream also repairs the suit on an anvil or a grindstone.

Defence is iron-grade on purpose. The suit is a sidegrade to your late-game
armour, not a replacement for it — you wear it for the thing armour points
cannot buy.

All four recipes unlock in the recipe book as soon as you join a world.

## How it works

There is no vanilla attribute for fire resistance, so the reduction happens in
the damage pipeline: a mixin on `LivingEntity.getDamageAfterMagicAbsorb` scales
fire-tagged damage by whatever fraction of the suit is worn, and a second one on
`isInvulnerableTo` lets a full suit refuse the damage outright rather than take
zero of it — no hurt flash, no sound, no hunger drain.

Catching fire is separate, and vanilla does have an attribute for it: each piece
takes a quarter off `minecraft:burning_time`, so a full suit never ignites.
Without that you would stand in lava unhurt but permanently ablaze, which reads
as a bug rather than as a suit.

Both hooks sit on `LivingEntity`, so a mob wearing the armour is protected on
the same terms.

## Debug commands

| Command | What |
|---|---|
| `/fireproofsuit` | The full suit into your inventory |
| `/fireproofsuit give` | Same thing, spelled out |
| `/fireproofsuit status` | Prints `fireProtection`, `burningTime` and `onFire` |

Permission level 2, so op or cheats. `status` reads back the two numbers the
suit acts on, which is quicker than guessing why you still burned.

## Textures

`tools/make_textures.py` draws every texture by re-shading vanilla's netherite
armour onto a basalt-to-ember ramp — cooled crust with the glow showing in the
cracks. Re-run it against the client jar if you want to retune the palette:

```
python3 tools/make_textures.py
```

## Build

```
JAVA_HOME=/opt/homebrew/opt/openjdk@25 ./gradlew build
```

Output: `build/libs/fireproofsuit-1.0.0.jar`. Requires Fabric Loader >= 0.19.3,
Fabric API, Java 25.

## Tests

Headless, no client, no EULA — spins up a game-test server, runs the tests in a
real world, exits:

```
JAVA_HOME=/opt/homebrew/opt/openjdk@25 ./gradlew runGametest
```

Covers: the damage scaling at zero, half and full suit; a full suit refusing
lava but not drowning; the burning-time attribute reaching zero and the wearer
failing to ignite; all four recipes; and the debug command. JUnit XML lands in
`build/gametest-report.xml`.

The damage tests burn cows with the `on_fire` damage type, which is tagged
`bypasses_armor` — so the suit's own armour points cannot muddy the numbers and
what the test measures is the reduction alone.
