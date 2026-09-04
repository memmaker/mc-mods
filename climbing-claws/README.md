# Climbing Claws

Fabric mod for Minecraft 26.2. Craft metal climbing claws; while you hold them
in your main hand or off hand, a solid wall behaves like a ladder — but only
while you are facing it. Turn away or look sideways along the wall and the grip
drops. Carrying them in the hotbar or the backpack does nothing: they have to be
in a hand.

Climbing uses vanilla ladder physics: walk into the wall to go up, sneak to
hold position, release to slide down.

## Crafting

Three units of one material in a triangle — ingots, diamonds or obsidian blocks.
Both orientations work, and shaped recipes mirror, so all four rotations are
covered:

```
I .        . I .
. I        I . I
I .
```

Copper, iron, gold, diamond, netherite and obsidian each produce claws tinted to
that material (`minecraft:dyed_color`), named accordingly. One item id, six
looks — and the tint is also the tier: the material decides how fast you climb.

| Material | Climb speed |
|---|---|
| Copper | 1.0x (vanilla ladder) |
| Iron | 1.5x |
| Gold | 1.75x |
| Diamond | 2.0x |
| Netherite | 2.25x |
| Obsidian | 2.5x |

Copper is the baseline — vanilla ladder speed — and every tier above it climbs
faster. Only upward movement scales; sliding down and sneak-to-hold stay vanilla.

All twelve recipes unlock in the recipe book as soon as you join a world.

## Debug commands

| Command | What |
|---|---|
| `/climbingclaws` | Iron claws into your inventory |
| `/climbingclaws <copper\|iron\|gold\|diamond\|netherite\|obsidian>` | Claws of that material, tinted and named |
| `/climbingclaws status` | Prints `clawsHeld`, `climbSpeed`, `touchingWall`, `facingWall` and `onClimbable` — the inputs the mixin decides on |

Permission level 2, so op or cheats. `status` is the quick way to tell a climb that
failed on the held-claws check from one that failed on the wall.

## Build

```
JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home ./gradlew build
```

Output: `build/libs/climbingclaws-1.3.0.jar`. Requires Fabric Loader >= 0.19.3,
Fabric API, Java 25.

## Tests

Headless, no client, no EULA — spins up a game-test server, runs the tests in a
real world, exits:

```
JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home ./gradlew runGametest
```

Covers: every material crafting in both orientations with the right tint, mixed
metals refusing to craft, claws gripping walls only while held in a hand (and not
when merely sitting in the hotbar), climb speed rising across all six tiers, the grip
dropping when the player turns away from or sideways to the wall, and claws
not climbing thin air. JUnit XML lands in `build/gametest-report.xml`.
