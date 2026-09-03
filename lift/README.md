# Lift

Fabric mod for Minecraft 26.2. Craft a Lift Plate; step on it and it carries you
up. One block, no redstone, no multiblock.

## How it works

- Step on a parked plate and it lifts out of the world as an entity with you
  riding it, rising at 0.1 blocks per tick — about two blocks a second.
- Where it stops depends on what is beside it. With a solid block cardinally
  adjacent, the plate climbs alongside that wall and stops level with its top,
  which is the height you can step off at. In an open column it stops two blocks
  below the first solid block overhead, and with neither wall nor ceiling it
  stops at the world's build height rather than climbing forever. A ceiling
  always wins over the wall height.
- At the top it hovers, still carrying you, until you step off. Dismounting is
  how you get off, and an empty plate goes straight back down and turns back
  into a block where it started.
- If you ride it back down, it waits — holding you up as an entity — until you
  walk off before restoring the block, since a block placed under your feet is a
  block you are standing on, which would start the trip over forever.
- Unlike a vanilla pressure plate it keeps its collision box, so a parked plate
  is something you stand *on*, not something you sink into.

## Crafting

Shapeless: redstone + any pressure plate (`#lift:pressure_plates`). Appears in
the Redstone creative tab.

## Build

```
JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home ./gradlew build
```

Output: `build/libs/lift-1.0.0.jar`. Requires Fabric Loader >= 0.19.3, Fabric
API, Java 25.

## Tests

```
JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home ./gradlew runGametest
```

Covers every stop-height case — two blocks below a ceiling, level with an
adjacent block, alongside a tall wall, a ceiling clamping a wall that runs too
high, an open shaft staying within build height — plus a full ride that ends
with the plate back where it started.

## License

MIT, see [LICENSE](LICENSE).
