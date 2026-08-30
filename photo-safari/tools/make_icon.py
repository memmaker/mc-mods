#!/usr/bin/env python3
"""Draw the mod icon: a camera whose lens holds a paw print.

    python3 tools/make_icon.py

Writes the in-jar icon and a bigger one for the Modrinth project page.
"""
import math
import pathlib
import struct
import zlib

SS = 4  # supersampling factor, the whole thing is drawn big and averaged down

BACKDROP_TOP = (58, 122, 96)
BACKDROP_BOTTOM = (26, 66, 58)
BODY = (44, 48, 56)
BODY_LIGHT = (66, 72, 82)
LENS_RING = (150, 160, 172)
LENS_GLASS_OUTER = (24, 96, 112)
LENS_GLASS_INNER = (12, 44, 60)
PAW = (236, 226, 196)
FLASH = (250, 226, 140)
SHUTTER = (206, 84, 74)


def blend(dst, src, alpha):
    return tuple(round(d + (s - d) * alpha) for d, s in zip(dst, src))


class Canvas:
    def __init__(self, size):
        self.size = size
        self.px = [[(0, 0, 0, 0)] * size for _ in range(size)]

    def paint(self, x, y, color, alpha=1.0):
        if not (0 <= x < self.size and 0 <= y < self.size) or alpha <= 0:
            return
        r, g, b, a = self.px[y][x]
        out_a = a / 255 + alpha * (1 - a / 255)
        rgb = blend((r, g, b), color, alpha / out_a if out_a else 0)
        self.px[y][x] = (*rgb, round(out_a * 255))

    def rounded_rect(self, x0, y0, x1, y1, radius, color, alpha=1.0):
        for y in range(int(y0), int(y1) + 1):
            for x in range(int(x0), int(x1) + 1):
                dx = max(x0 + radius - x, 0, x - (x1 - radius))
                dy = max(y0 + radius - y, 0, y - (y1 - radius))
                if math.hypot(dx, dy) <= radius:
                    self.paint(x, y, color, alpha)

    def circle(self, cx, cy, r, color, alpha=1.0):
        for y in range(int(cy - r) - 1, int(cy + r) + 2):
            for x in range(int(cx - r) - 1, int(cx + r) + 2):
                if math.hypot(x - cx, y - cy) <= r:
                    self.paint(x, y, color, alpha)

    def ellipse(self, cx, cy, rx, ry, color, alpha=1.0):
        for y in range(int(cy - ry) - 1, int(cy + ry) + 2):
            for x in range(int(cx - rx) - 1, int(cx + rx) + 2):
                if ((x - cx) / rx) ** 2 + ((y - cy) / ry) ** 2 <= 1.0:
                    self.paint(x, y, color, alpha)

    def downsample(self, factor):
        out = Canvas(self.size // factor)
        for y in range(out.size):
            for x in range(out.size):
                r = g = b = a = 0
                for sy in range(factor):
                    for sx in range(factor):
                        pr, pg, pb, pa = self.px[y * factor + sy][x * factor + sx]
                        r += pr * pa
                        g += pg * pa
                        b += pb * pa
                        a += pa
                n = factor * factor
                out.px[y][x] = ((r // a, g // a, b // a, a // n) if a else (0, 0, 0, 0))
        return out

    def write_png(self, path):
        raw = b"".join(
            b"\x00" + b"".join(struct.pack("4B", *px) for px in row) for row in self.px
        )

        def chunk(tag, data):
            body = tag + data
            return struct.pack(">I", len(data)) + body + struct.pack(">I", zlib.crc32(body))

        png = b"\x89PNG\r\n\x1a\n"
        png += chunk(b"IHDR", struct.pack(">IIBBBBB", self.size, self.size, 8, 6, 0, 0, 0))
        png += chunk(b"IDAT", zlib.compress(raw, 9))
        png += chunk(b"IEND", b"")
        pathlib.Path(path).write_bytes(png)


def draw(size):
    s = size * SS
    c = Canvas(s)
    u = s / 100.0  # one unit is a hundredth of the icon

    # Rounded backdrop with a soft vertical gradient.
    for y in range(s):
        t = y / s
        row = tuple(round(a + (b - a) * t) for a, b in zip(BACKDROP_TOP, BACKDROP_BOTTOM))
        c.rounded_rect(0, y, s - 1, y, 0, row)
    mask = Canvas(s)
    mask.rounded_rect(0, 0, s - 1, s - 1, 18 * u, (255, 255, 255))
    for y in range(s):
        for x in range(s):
            if mask.px[y][x][3] == 0:
                c.px[y][x] = (0, 0, 0, 0)

    # Camera body, with the viewfinder bump and the shutter button merged into it.
    c.rounded_rect(36 * u, 20 * u, 58 * u, 36 * u, 4 * u, BODY_LIGHT)
    c.rounded_rect(21 * u, 23 * u, 32 * u, 34 * u, 3 * u, SHUTTER)
    c.rounded_rect(14 * u, 30 * u, 86 * u, 82 * u, 10 * u, BODY)
    c.circle(75 * u, 39 * u, 3.5 * u, FLASH)

    # Lens.
    c.circle(50 * u, 57 * u, 23 * u, LENS_RING)
    c.circle(50 * u, 57 * u, 20 * u, LENS_GLASS_OUTER)
    c.circle(50 * u, 57 * u, 14 * u, LENS_GLASS_INNER)

    # Paw print inside the glass.
    c.ellipse(50 * u, 60 * u, 7.5 * u, 6.5 * u, PAW)
    for dx, dy, rx, ry in ((-8.5, -4.0, 3.2, 3.8), (-3.0, -8.0, 3.0, 3.6),
                           (3.0, -8.0, 3.0, 3.6), (8.5, -4.0, 3.2, 3.8)):
        c.ellipse((50 + dx) * u, (57 + dy) * u, rx * u, ry * u, PAW)

    # Glint on the glass.
    c.ellipse(40 * u, 45 * u, 5.5 * u, 3.0 * u, (255, 255, 255), 0.28)

    return c.downsample(SS)


if __name__ == "__main__":
    root = pathlib.Path(__file__).resolve().parent.parent
    draw(128).write_png(root / "src/main/resources/assets/photosafari/icon.png")
    draw(512).write_png(root / "icon.png")
    print("wrote src/main/resources/assets/photosafari/icon.png and icon.png")
