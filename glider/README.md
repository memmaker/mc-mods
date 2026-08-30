# Glider

A craftable glider for Minecraft **26.2** (Fabric).

Hold it in your main hand and step off something high. Once you have dropped about a block and a
half the canopy opens on its own: you descend at a steady, survivable **1 block per second** and
steer freely the whole way down. Fall damage does not apply while the canopy is open. Land, or
switch to another hotbar slot, and it folds away.

No stamina bar, no meter, no upgrades to grind. Craft one and go.

The canopy takes dye. Combine the glider with any dye in a crafting grid, the same way you
would colour leather armour — colours mix, and the frame stays wood.

## Crafting

Sticks and leather:

```
| # |
# | #
|   |
```

`|` stick, `#` leather.

## Debug commands

| Command | What |
|---|---|
| `/glider` or `/glider give` | A glider into your inventory |
| `/glider status` | Held item, canopy open or closed, ground contact and fall distance |

Permission level 2, so op or cheats. The canopy field reads the synced marker the
client renders from, which separates a glide that behaves wrong from one that only
looks wrong.

## Tests

```bash
./gradlew runGametest
```

Headless — no client, no EULA.

## Credits

Derived from [Paraglider](https://github.com/Tictim/Paraglider) by **Tictim**, which is licensed
GPL-3.0. The glide physics, the item model and its textures come from that mod; this is a port of
its core to Minecraft 26.2 with the stamina system, heart containers, vessels and the statue
worldgen removed. Licensed GPL-3.0-only in turn, as that license requires.
