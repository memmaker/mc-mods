# Seamless Crafting

Fabric mod for Minecraft 26.2. The vanilla crafting table reaches into the chests around it: the
recipe book counts nearby items as available, clicking a recipe fills the grid from those chests,
and a panel beside the screen lists what is in range.

Ported from [DerkOttersberg/Seamless-Crafting](https://github.com/DerkOttersberg/Seamless-Crafting)
(CC0-1.0), rewritten for 26.2's rendering and screen APIs.

## What it does

| Feature | Where |
|---|---|
| Nearby chests count as crafting ingredients | Crafting table, both the grid and the recipe book |
| Recipe book placement pulls from those chests | Click a recipe with an empty inventory and the grid fills |
| Nearby-items panel with search, scrolling and counts | Right of the crafting and inventory screens |
| Click an item to locate its chest | Highlights the chest, optionally aims at it and draws a particle trail |
| Cancel button returns borrowed items | The `X` beside the panel, handled server-side so nothing duplicates |
| Spacebar fills one set of the selected recipe | Recipe book |
| Active potion effects move to the top left | Inventory screen, where vanilla would draw them under the panel |

Only block containers count — chests, barrels, shulker boxes, hoppers. The scan walks the loaded
chunks around the table, so unloaded chunks are simply not there.

Whatever the mod borrows from a chest is tracked per grid slot. Closing the screen, crafting, or
pressing cancel puts back exactly what is still sitting unused in the grid.

## Settings

Mod Menu opens a settings screen built from vanilla option widgets; the values live in
`config/seamlesscrafting.json` and work without Mod Menu too.

| Setting | Default | What |
|---|---|---|
| Nearby radius | 16 | How far from the table to look, in blocks. The server's value wins in multiplayer |
| Auto refresh | 1 s | How often the panel asks for fresh counts while open |
| Chest highlighter | on | Draw the located chest |
| Highlight duration / opacity / colour | 5 s, 35%, gold | Appearance of that highlight |
| Distance label | on | Floating distance over the located chest |
| Snap aim to chest | off | Turn the camera toward the chest on click |
| Locate trail + particle | on, cloud | Particle trail from the player to the chest |
| Panel open by default | on | Whether the panel starts expanded |

The highlight is drawn as a gizmo, so the game fades and expires it — nothing is rendered per
frame by the mod.

## Debug commands

| Command | What |
|---|---|
| `/seamlesscrafting` | Prints the configured radius, how many containers are in range and how many kinds of item they hold |

Needs permission level 2 — op, or a single-player world with cheats.

## Building

```
JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home ./gradlew build
```

Output: `build/libs/seamlesscrafting-1.0.0.jar`. Requires Fabric Loader >= 0.19.3, Fabric API,
Java 25. Mod Menu is optional.

## Tests

```
./gradlew runGametest
```

Headless server tests cover the scanner, the radius cut-off, the debug command and the whole
recipe-book path: place a recipe with an empty inventory and the planks come out of the chest,
cancel and they go back.

## Known rough edge

The panel sits to the right of the screen. At very large GUI scales in a small window it can run
off the edge — overlapping the slots would be worse, so it stays where it is.
