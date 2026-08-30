#!/usr/bin/env python3
"""Draw the fireproof suit textures by re-shading vanilla's netherite armour.

    python3 tools/make_textures.py [path/to/minecraft-client.jar]

Netherite armour is already the right silhouette for heavy nether gear and is shaded almost
flat, which makes it a clean height map to re-colour: mapping its brightness onto a basalt-to-
ember ramp turns the plates into cooled crust and the highlights into the glow in the cracks.
Writes the four item icons, all three worn layers and the Modrinth project icon.
"""
import hashlib
import io
import pathlib
import sys
import zipfile

from PIL import Image

DEFAULT_JAR = pathlib.Path.home() / ".gradle/caches/fabric-loom/26.2/minecraft-client.jar"
OUT = pathlib.Path(__file__).resolve().parent.parent / "src/main/resources/assets/fireproofsuit"

# Brightness -> colour. Most of the range stays dark rock so the ember only reads on the raised
# edges; a linear two-colour ramp would wash the whole suit orange and lose the plating.
EMBER_RAMP = [
    (0.00, (22, 18, 22)),      # unlit basalt
    (0.55, (54, 44, 50)),      # plate face
    (0.72, (108, 52, 34)),     # heat creeping up an edge
    (0.88, (206, 92, 30)),     # ember
    (1.00, (255, 186, 74)),    # the crack itself
]
FUZZ = 7                       # per-pixel jitter, reads as a crusted surface at 16x16


def ramp(value):
    """Piecewise lookup into EMBER_RAMP."""
    for (low, dark), (high, light) in zip(EMBER_RAMP, EMBER_RAMP[1:]):
        if value <= high:
            t = (value - low) / (high - low)
            return tuple(round(d + (l - d) * t) for d, l in zip(dark, light))
    return EMBER_RAMP[-1][1]


def jitter(x, y, salt):
    """Deterministic +/-FUZZ so the texture is identical on every run."""
    digest = hashlib.md5(f"{salt}:{x}:{y}".encode()).digest()
    return digest[0] / 255 * 2 * FUZZ - FUZZ


def reshade(image, salt):
    """Map each pixel's brightness onto the ember ramp, keeping alpha and relative shading."""
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
                min(255, max(0, round(c + noise))) for c in ramp(value)
            ) + (a,))
    return out


def main():
    jar = pathlib.Path(sys.argv[1]) if len(sys.argv) > 1 else DEFAULT_JAR
    if not jar.exists():
        sys.exit(f"{jar} not found — pass the Minecraft client jar as an argument")

    with zipfile.ZipFile(jar) as zf:
        def vanilla(path):
            return Image.open(io.BytesIO(zf.read(f"assets/minecraft/textures/{path}")))

        sources = {}
        for piece in ("helmet", "chestplate", "leggings", "boots"):
            sources[f"textures/item/fireproof_{piece}.png"] = (
                vanilla(f"item/netherite_{piece}.png"), piece)

        for layer in ("humanoid", "humanoid_baby", "humanoid_leggings"):
            sources[f"textures/entity/equipment/{layer}/fireproof.png"] = (
                vanilla(f"entity/equipment/{layer}/netherite.png"), layer)

    written = {}
    for name, (source, salt) in sources.items():
        image = reshade(source, salt)
        path = OUT / name
        path.parent.mkdir(parents=True, exist_ok=True)
        image.save(path)
        written[name] = image
        print("wrote", path)

    # Modrinth wants something bigger than 16 pixels; nearest-neighbour keeps it crisp.
    icon = Image.new("RGBA", (256, 256))
    for y in range(256):
        icon.paste(ramp(1.0 - y / 255 * 0.72) + (255,), (0, y, 256, y + 1))
    icon.alpha_composite(
        written["textures/item/fireproof_chestplate.png"].resize((224, 224), Image.NEAREST), (16, 16))
    icon.save(OUT / "icon.png")
    print("wrote", OUT / "icon.png")


if __name__ == "__main__":
    main()
