#!/usr/bin/env python3
"""Draw the Lift Plate block texture and the Modrinth project icon.

    python3 tools/make_textures.py

A mottled grey stone base (procedural, not copied from any game asset) with a small red up-arrow
rune engraved in the middle, so the plate reads as "stone pressure plate, rigged with redstone"
rather than a wholly new material. Fixed seed keeps the output reproducible. The icon is the same
16x16 art, upscaled nearest-neighbour onto a dark-to-ember gradient — the plate glowing as if
lifting off.
"""
import pathlib
import random

from PIL import Image

ROOT = pathlib.Path(__file__).resolve().parent.parent
OUT = ROOT / "src/main/resources/assets/lift/textures/block/lift_plate.png"
ICON_OUT = ROOT / "src/main/resources/assets/lift/icon.png"

STONE_BASE = (130, 130, 130)
STONE_NOISE = 14
SEED = 20260830

RUNE_PALETTE = {
    ".": None,
    "o": (86, 20, 12),      # dark outline
    "R": (216, 45, 27),     # lit redstone red
    "r": (168, 32, 20),     # shaded redstone red
}

# A simple up-arrow: "ascending" is the whole point of the plate. Head and shaft both centered on
# column 7 (the head's base runs columns 4-10, dead center at 7; the shaft is 3-wide at 6-8) —
# earlier the shaft sat at columns 8-9, a column and a half off-center, which read as crooked.
RUNE = [
    "................",
    "................",
    "................",
    "................",
    ".......o........",
    "......oRo.......",
    ".....oRRRo......",
    "....oRrrrRo.....",
    "......oRo.......",
    "......oRo.......",
    "......oRo.......",
    "......oRo.......",
    "......ooo.......",
    "................",
    "................",
    "................",
]


def stone_pixel(random_source):
    offset = random_source.randint(-STONE_NOISE, STONE_NOISE)
    return tuple(max(0, min(255, channel + offset)) for channel in STONE_BASE)


def draw_plate():
    random_source = random.Random(SEED)
    image = Image.new("RGBA", (16, 16), (0, 0, 0, 255))
    for y in range(16):
        for x in range(16):
            image.putpixel((x, y), (*stone_pixel(random_source), 255))

    for y, row in enumerate(RUNE):
        for x, char in enumerate(row):
            colour = RUNE_PALETTE[char]
            if colour is not None:
                image.putpixel((x, y), (*colour, 255))

    return image


def ramp(value, dark, light):
    return tuple(round(d + (l - d) * value) for d, l in zip(dark, light))


def draw_icon(plate):
    icon = Image.new("RGBA", (256, 256))
    for y in range(256):
        icon.paste(ramp(y / 255, (32, 33, 38), (92, 28, 20)) + (255,), (0, y, 256, y + 1))
    icon.alpha_composite(plate.resize((224, 224), Image.NEAREST), (16, 16))
    return icon


def main():
    plate = draw_plate()
    OUT.parent.mkdir(parents=True, exist_ok=True)
    plate.save(OUT)
    print(f"wrote {OUT}")

    icon = draw_icon(plate)
    ICON_OUT.parent.mkdir(parents=True, exist_ok=True)
    icon.save(ICON_OUT)
    print(f"wrote {ICON_OUT}")


if __name__ == "__main__":
    main()
