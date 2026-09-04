#!/usr/bin/env python3
"""Draw the plush boot textures by re-shading vanilla's leather armour.

    python3 tools/make_textures.py [path/to/minecraft-client.jar]

Leather armour is already shaped like a boot and split the way dyeing needs it — a near-grey
base the dye colour multiplies into, plus a fixed overlay for the sole — so re-shading it costs
a ramp lookup instead of a pixel artist. Writes both layers of the item icon, both layers of the
worn humanoid texture, and the Modrinth project icon.
"""
import hashlib
import io
import pathlib
import sys
import zipfile

from PIL import Image

DEFAULT_JAR = pathlib.Path.home() / ".gradle/caches/fabric-loom/26.2/minecraft-client.jar"
OUT = pathlib.Path(__file__).resolve().parent.parent / "src/main/resources/assets/plushboots"

# The base is left grey: the dye colour is multiplied into it at render time, so any colour it
# carries would tint every dye job.
BASE_DARK = (120, 116, 120)
BASE_LIGHT = (255, 250, 252)
# The overlay is never dyed, so it stays a neutral sole that reads under every colour.
TRIM_DARK = (74, 68, 72)
TRIM_LIGHT = (126, 116, 122)
UNDYED = (231, 145, 180)   # plush pink, what a freshly crafted pair looks like
FUZZ = 9                   # per-pixel jitter, reads as a fuzzy surface at 16x16


def ramp(value, dark, light):
    return tuple(round(d + (l - d) * value) for d, l in zip(dark, light))


def jitter(x, y, salt):
    """Deterministic ±FUZZ so the texture is identical on every run."""
    digest = hashlib.md5(f"{salt}:{x}:{y}".encode()).digest()
    return digest[0] / 255 * 2 * FUZZ - FUZZ


def reshade(image, dark, light, salt):
    """Map each pixel's brightness onto a new ramp, keeping alpha and relative shading."""
    pixels = image.convert("RGBA")
    lums = [(r + g + b) / 3 for r, g, b, a in
            (pixels.getpixel((x, y)) for y in range(pixels.height) for x in range(pixels.width))
            if a > 0]
    low, span = min(lums), max(max(lums) - min(lums), 1)

    out = Image.new("RGBA", pixels.size)
    for y in range(pixels.height):
        for x in range(pixels.width):
            r, g, b, a = pixels.getpixel((x, y))
            if a == 0:
                continue
            value = ((r + g + b) / 3 - low) / span
            noise = jitter(x, y, salt)
            out.putpixel((x, y), tuple(
                min(255, max(0, round(c + noise))) for c in ramp(value, dark, light)
            ) + (a,))
    return out


def tint(image, color):
    """What the game does with a dye colour: multiply it into the base texture."""
    out = Image.new("RGBA", image.size)
    for y in range(image.height):
        for x in range(image.width):
            r, g, b, a = image.getpixel((x, y))
            out.putpixel((x, y), (r * color[0] // 255, g * color[1] // 255, b * color[2] // 255, a))
    return out


def main():
    jar = pathlib.Path(sys.argv[1]) if len(sys.argv) > 1 else DEFAULT_JAR
    if not jar.exists():
        sys.exit(f"{jar} not found — pass the Minecraft client jar as an argument")

    with zipfile.ZipFile(jar) as zf:
        def vanilla(path):
            return Image.open(io.BytesIO(zf.read(f"assets/minecraft/textures/{path}")))

        sources = {
            "textures/item/plush_boots.png": (vanilla("item/leather_boots.png"), False, "item"),
            "textures/item/plush_boots_overlay.png":
                (vanilla("item/leather_boots_overlay.png"), True, "item-trim"),
            "textures/entity/equipment/humanoid/plush.png":
                (vanilla("entity/equipment/humanoid/leather.png"), False, "worn"),
            "textures/entity/equipment/humanoid/plush_overlay.png":
                (vanilla("entity/equipment/humanoid/leather_overlay.png"), True, "worn-trim"),
        }

    written = {}
    for name, (source, is_trim, salt) in sources.items():
        colors = (TRIM_DARK, TRIM_LIGHT) if is_trim else (BASE_DARK, BASE_LIGHT)
        image = reshade(source, *colors, salt)
        path = OUT / name
        path.parent.mkdir(parents=True, exist_ok=True)
        image.save(path)
        written[name] = image
        print("wrote", path)

    # Modrinth wants something bigger than 16 pixels; nearest-neighbour keeps it crisp.
    item = tint(written["textures/item/plush_boots.png"], UNDYED)
    item.alpha_composite(written["textures/item/plush_boots_overlay.png"])
    icon = Image.new("RGBA", (256, 256))
    for y in range(256):
        icon.paste(ramp(y / 255, (68, 44, 62), (126, 78, 104)) + (255,), (0, y, 256, y + 1))
    icon.alpha_composite(item.resize((224, 224), Image.NEAREST), (16, 16))
    icon.save(OUT / "icon.png")
    print("wrote", OUT / "icon.png")


if __name__ == "__main__":
    main()
