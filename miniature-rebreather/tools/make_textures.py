#!/usr/bin/env python3
"""Draw the rebreather item icon and the Modrinth project icon.

    python3 tools/make_textures.py

The item is a 16x16 pixel drawing, so it is cheaper to spell it out as a character grid than to
re-shade a vanilla texture: twin steel bottles with brass valves and a rubber mouthpiece.
"""
import pathlib

from PIL import Image

OUT = pathlib.Path(__file__).resolve().parent.parent / "src/main/resources/assets/rebreather"

PALETTE = {
    ".": None,                  # transparent
    "k": (26, 28, 32),          # outline
    "s": (104, 112, 122),       # steel, shaded side
    "S": (158, 168, 178),       # steel, body
    "h": (214, 222, 230),       # steel, lit edge
    "r": (170, 58, 52),         # painted band
    "b": (40, 40, 46),          # rubber
    "B": (72, 72, 80),          # rubber, lit edge
    "c": (198, 152, 66),        # brass valve
}

ART = [
    "................",
    "..kck......kck..",
    ".kkkkk....kkkkk.",
    ".ksShk....ksShk.",
    ".ksShk....ksShk.",
    ".krrrk....krrrk.",
    ".ksShk....ksShk.",
    ".ksShkbbbbksShk.",
    ".ksShk.bb.ksShk.",
    ".ksShkbBBbksShk.",
    ".ksShkbBBbksShk.",
    ".ksShkbBBbksShk.",
    ".kkkkkbBBbkkkkk.",
    "......kbbk......",
    ".......kk.......",
    "................",
]


def draw(art):
    image = Image.new("RGBA", (len(art[0]), len(art)))
    for y, row in enumerate(art):
        assert len(row) == len(art[0]), f"row {y} is {len(row)} wide, not {len(art[0])}"
        for x, char in enumerate(row):
            color = PALETTE[char]
            if color:
                image.putpixel((x, y), color + (255,))
    return image


def main():
    item = draw(ART)
    path = OUT / "textures/item/rebreather.png"
    path.parent.mkdir(parents=True, exist_ok=True)
    item.save(path)
    print("wrote", path)

    # Modrinth wants something bigger than 16 pixels; nearest-neighbour keeps it crisp.
    icon = Image.new("RGBA", (256, 256))
    for y in range(256):
        value = y / 255
        icon.paste(tuple(round(d + (l - d) * value)
                         for d, l in zip((10, 34, 58), (28, 96, 122))) + (255,), (0, y, 256, y + 1))
    icon.alpha_composite(item.resize((224, 224), Image.NEAREST), (16, 16))
    icon.save(OUT / "icon.png")
    print("wrote", OUT / "icon.png")


if __name__ == "__main__":
    main()
