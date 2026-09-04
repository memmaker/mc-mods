#!/usr/bin/env python3
"""Write one crafting recipe per wool colour, so the boots come out the colour of the wool.

    python3 tools/make_recipes.py

A recipe result can carry components but cannot read them off an ingredient, so the colour has
to be baked into sixteen recipes — the same thing vanilla does for its own dyed crafts. Also
rewrites the advancement that unlocks them all in the recipe book.
"""
import json
import pathlib

DATA = pathlib.Path(__file__).resolve().parent.parent / "src/main/resources/data/plushboots"

# DyeColor.getTextureDiffuseColor(), the value vanilla stamps on leather armour dyed that colour.
# Checked against Minecraft 26.2; a pair crafted from red wool matches a pair dyed red.
COLORS = {
    "white": 16383998,
    "orange": 16351261,
    "magenta": 13061821,
    "light_blue": 3847130,
    "yellow": 16701501,
    "lime": 8439583,
    "pink": 15961002,
    "gray": 4673362,
    "light_gray": 10329495,
    "cyan": 1481884,
    "purple": 8991416,
    "blue": 3949738,
    "brown": 8606770,
    "green": 6192150,
    "red": 11546150,
    "black": 1908001,
}


def main():
    recipes = []
    for color, rgb in COLORS.items():
        name = f"plush_boots_from_{color}_wool"
        recipes.append(f"plushboots:{name}")
        (DATA / "recipe" / f"{name}.json").write_text(json.dumps({
            "type": "minecraft:crafting_shaped",
            "category": "equipment",
            "group": "plush_boots",
            "key": {"W": f"minecraft:{color}_wool"},
            "pattern": ["W W", "W W"],
            "result": {
                "id": "plushboots:plush_boots",
                "components": {"minecraft:dyed_color": rgb},
            },
        }, indent=2) + "\n")

    (DATA / "advancement" / "unlock_recipes.json").write_text(json.dumps({
        "criteria": {"immediately": {"trigger": "minecraft:tick"}},
        "requirements": [["immediately"]],
        "rewards": {"recipes": recipes},
    }, indent=2) + "\n")

    print(f"wrote {len(recipes)} recipes and the unlock advancement")


if __name__ == "__main__":
    main()
