# Postbote

Fabric mod for Minecraft 26.2. Craft a satchel; right-click a villager while
holding it to take a delivery order for a village you've never been near.
Follow the compass it hands you and turn it in at any villager once you
arrive, for emeralds.

The point is exploration, not the cargo — every delivery is flavour text,
nothing more. Postbote doesn't build any new way to get around; it just gives
you a reason to go somewhere you haven't, on foot, by boat, by glider, by
whatever else is already in your world.

## How it works

- Right-click any villager with the satchel to accept an order (only one
  active order at a time). It picks a village at least 750 blocks away, in a
  random direction, and hands you a Delivery Compass pointed straight at it.
- While the compass is in either hand, the distance to the destination is
  shown in the upper-left corner of the screen, always, no keypress needed.
- The compass needle works exactly like a recovery compass — it's the same
  vanilla lodestone-tracker mechanism, just aimed at the delivery instead of
  a death location.
- Once the compass locks onto a specific villager near the destination (it
  keeps looking every second until one's found), that villager is the only
  one who can take the delivery — right-click them, wherever they've
  wandered to. If they die, the compass re-homes onto the nearest alive
  villager. Until one's ever been found, right-click any villager within 150
  blocks of the destination instead.
- Payout is `4 + distance / 200` emeralds, capped at 64 — the farther the
  delivery, the more it pays. Every second completed delivery also pays a
  bonus **Eye of Ender**.
- Each completed delivery increments the `postbote:delivered_packages`
  statistic, and the first one earns the **POST IST DA!** advancement.
- Nobody checks a delivery route against where you've already been; "at
  least 750 blocks away" stands in for "somewhere new." See the `ponytail:`
  comment on `Postbote.startOrder` if that heuristic ever needs to become a
  real per-player discovery check.

## Crafting

```
L L
 W
L L
```

`L` = leather, `W` = any wool. Unlocks in the recipe book on join.

## Debug commands

| Command | What |
|---|---|
| `/postbote` / `/postbote give` | Gives a satchel |
| `/postbote status` | Prints the active order's destination and reward, or `none` |
| `/postbote teleport` | Drops you within 10 blocks of the current delivery target, on the surface |

Permission level 2, so op or cheats.

## Build

```
JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home ./gradlew build
```

Output: `build/libs/postbote-1.1.1.jar`. Requires Fabric Loader >= 0.19.3,
Fabric API, Java 25.

## Tests

```
JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home ./gradlew runGametest
```

Covers the debug command, the one-active-order guard, the completion
gate (pays and consumes the compass in range, refuses and leaves it alone
out of range), and the every-second-delivery Eye of Ender bonus. The village search itself calls vanilla's own
`findNearestMapStructure` directly and isn't re-tested here.
