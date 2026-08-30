# Climbing Claws

Fabric mod for Minecraft 26.2. Craft metal climbing claws; while they sit
anywhere in your hotbar, a solid wall behaves like a ladder — but only while
you are facing it. Turn away or look sideways along the wall and the grip drops.

Climbing uses vanilla ladder physics: walk into the wall to go up, sneak to
hold position, release to slide down.

## Crafting

Three ingots of one metal in a triangle. Both orientations work, and shaped
recipes mirror, so all four rotations are covered:

```
I .        . I .
. I        I . I
I .
```

Iron, copper, gold and netherite each produce claws tinted to that metal
(`minecraft:dyed_color`), named accordingly. One item id, four looks.

All eight recipes unlock in the recipe book as soon as you join a world.

## Debug commands

| Command | What |
|---|---|
| `/climbingclaws` | Iron claws into your inventory |
| `/climbingclaws <iron\|copper\|gold\|netherite>` | Claws of that metal, tinted and named |
| `/climbingclaws status` | Prints `clawsInHotbar`, `touchingWall`, `facingWall` and `onClimbable` — the four inputs the mixin decides on |

Permission level 2, so op or cheats. `status` is the quick way to tell a climb that
failed on the hotbar check from one that failed on the wall.

## Build

```
JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home ./gradlew build
```

Output: `build/libs/climbingclaws-1.0.0.jar`. Requires Fabric Loader >= 0.19.3,
Fabric API, Java 25.

## Tests

Headless, no client, no EULA — spins up a game-test server, runs the tests in a
real world, exits:

```
JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home ./gradlew runGametest
```

Covers: every metal crafting in both orientations with the right tint, mixed
metals refusing to craft, claws gripping walls only from the hotbar, the grip
dropping when the player turns away from or sideways to the wall, and claws
not climbing thin air. JUnit XML lands in `build/gametest-report.xml`.
