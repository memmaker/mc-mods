# Craft Tracker

A shopping list that lives on your HUD.

Point at any item in any container screen — inventory, chest, creative tab — and press **C** to
track it. The overlay lists each entry as "0 of 3 Stick": how many you carry against how many you
want, turning green when you are done. Your inventory counts, and so does a worn Traveler's
Backpack when that mod is installed.

Everything is on the one key:

| Key | Where | What |
| --- | --- | --- |
| **C** | over an item | Track it, one more each press |
| **Shift+C** | over a tracked item | Drop it from the list |
| **C** | no screen open | Show or hide the overlay |
| **Shift+C** / **Ctrl+C** | no screen open | Clear the list |

The list stops growing once it reaches the bottom of the screen: further items are refused rather
than drawn where you cannot see them. Every press answers with a click — high for tracked, low for
refused — because the overlay is hidden behind an open screen. It is saved to `config/crafttracker.json` and survives
restarts.

## From the recipe book

Point at a recipe in the recipe book and press **C**: the whole crafting chain is followed down
to what you actually have to go and find — ores, logs, mob drops, anything no recipe makes — and
those get tracked with the counts one craft needs. **Shift+C** on a recipe tracks the finished item instead, without
expanding it.

Each craft is rounded up, so five sticks asks for a whole log rather than a quarter of one.
Recipes that only take a block apart again (nine iron ingots out of an iron block) are not
treated as a way to make the ingots. Tag ingredients pick their first item, so "any planks"
becomes oak planks. Only recipes the client knows are followed — the same ones the recipe book
shows — so a chain stops at anything still locked.

Client side only: it works on any server, and the server does not need the mod.

Inspired by [SweetRPG's Craft Tracker](https://github.com/sweetrpg/CraftTracker) (Forge, MIT).
This is a fresh Fabric implementation, not a port of that code.
