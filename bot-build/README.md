# Bot Build

Outline what you want built. Ghost blocks show the plan, then a bot flies the real blocks in and
places them one at a time.

## Use

Craft the **Build Wand** (two blaze rods, one amethyst shard) and a **Build Bot** (four iron
ingots, two redstone).

1. Right-click a block with the bot item to place the bot, spawn-egg style. The item is spent; the
   bot stays where you put it and waits.
2. Right-click a block with the wand. That block is the corner *and* the material.
3. Right-click the opposite corner. Every empty spot in the box between them gets a shrunken
   ghost block, and every idle bot within 24 blocks joins the job.
4. Each bot takes a ghost of its own and works it one block at a time: walk to the nearest
   container within 12 blocks that holds the block — or to you, if there is none — carry it over,
   place it. Each placement retires that ghost and the bot picks up the next free one. More bots
   means more ghosts going up at once.

Bots pathfind like any mob, so they go around walls rather than through them. A bot that can't
reach its spot for ten seconds drops off the job and hands the spot back to the others; the job
only stops when the last one is gone.

Between jobs the bots feed themselves: an idle bot walks to the nearest dropped food within 10
blocks and eats one. Bots on a job stay on the job.

Sneak + right-click cancels: ghosts vanish, a carried block drops, the bot goes back to idle.

Limits: 512 blocks per outline. A bot works one outline at a time. Running out of blocks cancels
the rest.

## Prior art and credits

The outline-then-preview idea comes from [Effortless Building](https://github.com/Requios/effortless-building);
the idea of a small helper acting for you comes from [BlockBots](https://github.com/Deadlydiamond98/BlockBots).
No code from either — this is a small independent take on the combination.

The Build Bot item icon is BlockBots' block bot spawn egg art by **Deadlydiamond98**, used with
their permission. BlockBots' art is licensed All Rights Reserved and is *not* covered by this
mod's MIT licence — do not reuse it from here.

## Build

```bash
./gradlew build
```
