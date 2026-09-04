#!/usr/bin/env python3
"""Draw the climbing claws item icon and the Modrinth project icon.

    python3 tools/make_textures.py

The item is two 16x16 layers. `climbing_claws` is the metal, which the item model tints with
`minecraft:dyed_color` so one texture covers iron, copper, gold and netherite — it has to stay
near-white, because a tint multiplies and cannot brighten. `climbing_claws_grip` is the leather
hand loop, which stays untinted. The loop over the knuckle bar is what says these are gripped in
the hand rather than strapped to a boot.

The claws carry no outline on purpose: at three pixels apart, an outline on both sides of each
claw closes the gaps between them and the whole row turns to mush.
"""
import pathlib

from PIL import Image

OUT = pathlib.Path(__file__).resolve().parent.parent / "src/main/resources/assets/climbingclaws"

CLAWS_PALETTE = {
    ".": None,
    "n": (105, 105, 105),       # outline
    "M": (255, 255, 255),       # lit face
    "m": (200, 200, 200),       # shaded face
}

CLAWS = [
    "................",
    "................",
    "................",
    "................",
    "................",
    ".nnnnnnnnnnnnnn.",
    ".nMMMMMMMMMMMMn.",
    ".nmmmmmmmmmmmmn.",
    ".nnMmnnMmnnMmnn.",
    "...Mm..Mm..Mm...",
    "..Mm..Mm..Mm....",
    "..Mm..Mm..Mm....",
    "..M...M...M.....",
    "..m...m...m.....",
    "................",
    "................",
]

GRIP_PALETTE = {
    ".": None,
    "o": (46, 32, 20),          # outline
    "l": (134, 94, 54),         # leather
}

GRIP = [
    "................",
    ".....oooooo.....",
    "....ollllllo....",
    "...olloooollo...",
    "..ollo....ollo..",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
]

# The project icon shows iron claws — the same tint the iron recipe stamps on the item.
IRON_TINT = 0xD8D8D8


def draw(art, palette, tint=None):
    image = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(art):
        for x, char in enumerate(row):
            colour = palette[char]
            if colour:
                if tint:
                    colour = tuple(c * t // 255 for c, t in zip(colour, tint))
                image.putpixel((x, y), colour + (255,))
    return image


def main():
    items = OUT / "textures/item"
    # Flipped so the claws point up in the inventory, matching how they sit in the hand.
    draw(CLAWS, CLAWS_PALETTE).transpose(Image.FLIP_TOP_BOTTOM).save(items / "climbing_claws.png")
    draw(GRIP, GRIP_PALETTE).transpose(Image.FLIP_TOP_BOTTOM).save(items / "climbing_claws_grip.png")

    tint = tuple((IRON_TINT >> shift) & 0xFF for shift in (16, 8, 0))
    icon = draw(CLAWS, CLAWS_PALETTE, tint)
    icon.alpha_composite(draw(GRIP, GRIP_PALETTE))
    icon.resize((128, 128), Image.NEAREST).save(OUT / "icon.png")


if __name__ == "__main__":
    main()
