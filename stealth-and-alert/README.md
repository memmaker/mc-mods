# Stealth and Alert

A stealth detection system for Minecraft **26.2** (Fabric).

A port of [Stealth and Alert](https://github.com/RedGhostRev/Stealth-and-Alert) by RedGhostRev,
originally written for NeoForge 1.21.1. MIT, same as the original.

Hostile mobs stop sensing you through walls and floors. Each one has a real vision cone — 120°
wide, 45° up, 60° down — and has to physically see you before anything happens. Break line of
sight and it loses you.

## How detection works

- **Reaction.** A mob that spots you takes half a second to react before anything starts filling.
- **Awareness.** A bar fills while it can see you. Filling is faster when you are close, lit and
  moving, slower when you are dark and far. It drains when you break line of sight.
- **Alert states.** Idle → Suspicious → Searching → Fighting. Only a full bar makes a mob attack,
  and vanilla targeting cannot skip that: an unspotted player is invisible to every target selector.
- **Last known position.** Lose it and it walks to where it last saw you, then searches around
  that spot for a while before giving up and going back to idle.
- **Multiplayer.** Each mob scores every player separately and hunts the strongest, nearest one.

## Staying hidden

The eye above your crosshair shows how visible you are, from 0 to 1:

- Darkness is the big one. Light level drives most of the number.
- Crouching cuts it a bit, crawling or swimming more, sprinting raises it.
- Tall grass, a 2x2 patch of it, or a head-and-feet block of leaves hides you completely.
- At the bottom of the scale mobs only notice you within two blocks, and their detection range
  shrinks by up to 60% on the way there.

Hitting a mob always alerts it, seen or not, and villagers remember who hit them.

## Noise

Mobs hear as well as see. Every noise you make — footsteps, a broken block, a chest lid, a bowstring,
an explosion — carries a volume and a threat level, and walls eat it: a metre of solid block costs
eight points, each new material another four. A loud enough noise makes a mob suspicious; a nearer or
nastier one sends it walking over to look. Sprinting is louder, crouching quieter, crawling quieter
still.

This rides on vanilla's game-event system, the same signal sculk sensors listen to, so anything that
makes a sculk sensor twitch is something a guard can hear.

## Assassination

Hit an unaware mob in the back with a blade and the strike lands as an assassination: armour-piercing,
no knockback, three times the damage — six times with a dagger. The mob must not be fighting you or
locked onto you, and you have to be behind it. Weapons are the `stealthandalert:can_assassinate` item
tag: swords, tridents, maces and the dagger.

## Items

- **Dagger** — iron ingot over a stick. Weak and fast in a fair fight, devastating from behind.
- **Pebble** — four from a cobblestone. Throw it: it lands loudly somewhere you are not, and every
  guard in earshot goes to look there instead.

## Symbols

A mob that has noticed something shows a symbol over its head: a white question mark while it is
suspicious, amber while it is searching, an exclamation mark once it is fighting. They are hidden
behind walls, and shrink with distance.

## Which mobs

The `stealthandalert:seekers` entity tag: zombies and variants, skeletons and variants, illagers,
creepers, witches, slimes, magma cubes, blazes, hoglins, piglin brutes, silverfish, ravagers.
`stealthandalert:protected` (villagers) only remember attackers. Both tags are datapack-editable.

## Testing

```bash
./gradlew runGametest
```
