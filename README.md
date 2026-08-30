# mc-mods

Fabric mods for Minecraft **26.2**, built as one repository. Each mod is a self-contained Gradle
project in its own folder; the scripts at the root build and publish all of them together.

| Mod | Folder | What it does |
|---|---|---|
| [White Flag](white-flag) | `white-flag/` | Craft a white flag. Carry it in your hotbar and every hostile mob stops attacking you. |
| [Photo Safari](photo-safari) | `photo-safari/` | Photograph every kind of living creature in the world. Turns [Camerapture](https://modrinth.com/mod/camerapture) photography into a collection game. |
| [Plush Boots](plush-boots) | `plush-boots/` | Craft plush boots. Wear them and falling stops hurting. |
| [Glider](glider) | `glider/` | Craft a glider. Hold it as you fall and the canopy opens: you drift down slowly and steer where you like. |
| [Miniature Rebreather](miniature-rebreather) | `miniature-rebreather/` | Craft a miniature rebreather. Wear it in the helmet slot and you never run out of air underwater. |
| [Climbing Claws](climbing-claws) | `climbing-claws/` | Craft climbing claws from any metal. Keep them in your hotbar and every vertical wall becomes a ladder. |
| [FX Globals](fx-globals) | `fx-globals/` | World pacing: longer days, slower hunger, and every arrow in the world can be picked up. All configurable, with a Mod Menu screen. |
| [Seamless Crafting](seamless-crafting) | `seamless-crafting/` | The crafting table uses the chests around it: the recipe book counts nearby items, clicking a recipe fills the grid from them, and a panel lists what is in range. |
| [Fireproof Suit](fireproof-suit) | `fireproof-suit/` | Craft a magma-forged armour set. Each piece cuts fire and lava damage by a quarter; the full suit makes you immune. |
| [Postbote](postbote) | `postbote/` | Craft a satchel. Take delivery orders from villagers and follow a compass to villages you've never been near, for emeralds. |
| [Bot Build](bot-build) | `bot-build/` | Craft a build wand and a build bot. Outline a box, ghost blocks show the plan, and the bot fetches the real blocks from a nearby chest and places them one by one. |

## Building

```bash
./build.sh
```

Builds every subfolder that has a `gradlew` and copies the release jars to the repository root.
One mod failing does not stop the others; failures are listed at the end.

Needs a JDK 25. If `java` is not on your `PATH`, the script falls back to
`/opt/homebrew/opt/openjdk@25` — override it by exporting `JAVA_HOME` yourself.

Individual mods build the usual way:

```bash
cd white-flag && ./gradlew build
```

## Testing

```bash
./test.sh
```

Runs every mod's game tests — a headless server per mod, no client and no EULA prompt. Same
failure handling as `build.sh`, and the same thing CI runs.

## Running in a dev environment

```bash
cd white-flag && ./gradlew runClient
```

`runServer` boots a dedicated server against the same code, which is the faster loop for anything
server-side.

## Debug commands

Every mod registers a command for testing without crafting or waiting for the situation the mod
reacts to. All of them need permission level 2 — op, or a single-player world with cheats.

| Mod | Command | What |
|---|---|---|
| White Flag | `/whiteflag` | Gives a flag |
| Photo Safari | `/photosafari camera [paper]`, `progress`, `reset` | Gives a camera, shows or wipes progress |
| Plush Boots | `/nofall [give]`, `/nofall status` | Gives boots; prints the live fall damage multiplier |
| Glider | `/glider [give]`, `/glider status` | Gives a glider; prints canopy state and fall distance |
| Miniature Rebreather | `/rebreather [give]`, `/rebreather status` | Gives a rebreather; prints air supply |
| Climbing Claws | `/climbingclaws [metal]`, `/climbingclaws status` | Gives claws of any metal; prints wall and hotbar state |
| FX Globals | `/fxglobals [status]` | Prints the day-length factor, live clock rate, hunger factor and clock tick |
| Seamless Crafting | `/seamlesscrafting` | Prints the nearby radius, container count and item kinds in range |
| Fireproof Suit | `/fireproofsuit [give\|status]` | Gives the full suit; prints fire protection, burning time and whether on fire |
| Postbote | `/postbote [give\|status\|teleport]` | Gives a satchel; prints the active delivery's destination and reward; teleports within 10 blocks of it |

The `status` subcommands read back the exact values their mod acts on, which is usually faster than
guessing why an effect did not fire. Each one is covered by a game test, so a command that stops
registering fails the build rather than going quiet.

## Publishing to Modrinth

```bash
MODRINTH_TOKEN='mrp_...' ./publish.sh            # every mod
MODRINTH_TOKEN='mrp_...' ./publish.sh white-flag # just one
```

Creates the project as a **draft** and uploads the current jar as a version. The draft is not
submitted for review — open the project page and submit it yourself, which is the step that
eventually makes it searchable on Modrinth.

The token needs the `PROJECT_CREATE` and `VERSION_CREATE` scopes and comes from
<https://modrinth.com/settings/pats>.

## CI

[`.github/workflows/build.yml`](.github/workflows/build.yml) builds every mod on each push.

- Every push and pull request uploads the jars as workflow artifacts.
- Pushes to `main` additionally cut a GitHub release tagged `build-<run number>` with the jars
  attached.

## Layout

```
build.sh                 build every mod, collect jars at the root
test.sh                  run every mod's game tests
publish.sh               upload mods to Modrinth
white-flag/              White Flag
photo-safari/            Photo Safari
plush-boots/             Plush Boots
glider/                  Glider
miniature-rebreather/    Miniature Rebreather
climbing-claws/          Climbing Claws
fx-globals/              FX Globals
seamless-crafting/       Seamless Crafting
fireproof-suit/          Fireproof Suit
postbote/                Postbote
```
