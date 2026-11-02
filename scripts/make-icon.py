"""Schreibt das App-Icon: Adaptive-Vektoren, PNGs für Android 7 und eine Vorschau.

Eine Quelle für alle Fassungen: Maße hier ändern, nicht in den XML-Dateien.
Aufruf aus dem Projektstamm: python scripts/make-icon.py
"""
from pathlib import Path
from PIL import Image, ImageDraw

RES = Path('app/src/main/res')
PREVIEW = Path('docs/app-icon-vorschau.png')
TOP, BOTTOM = (0x1F, 0x1B, 0x5C), (0x7A, 0x3F, 0xE0)
ACCENT = '#5B33C9'
SCALE = 8
GRID = 108
FIT = 0.82  # zieht das Motiv in den sicheren Kreis (Radius 33 um die Mitte)

BACKGROUND = f'''<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:aapt="http://schemas.android.com/aapt"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path android:pathData="M0,0h108v108h-108z">
        <aapt:attr name="android:fillColor">
            <gradient
                android:type="linear"
                android:startX="0"
                android:startY="0"
                android:endX="108"
                android:endY="108"
                android:startColor="#FF{TOP[0]:02X}{TOP[1]:02X}{TOP[2]:02X}"
                android:endColor="#FF{BOTTOM[0]:02X}{BOTTOM[1]:02X}{BOTTOM[2]:02X}" />
        </aapt:attr>
    </path>
</vector>
'''

FOREGROUND = f'''<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <group
        android:pivotX="54"
        android:pivotY="54.5"
        android:scaleX="{FIT}"
        android:scaleY="{FIT}">
    <path
        android:fillAlpha="0.4"
        android:fillColor="#FFFFFF"
        android:pathData="M30,29h36a6,6 0 0 1 6,6v26a6,6 0 0 1 -6,6h-36a6,6 0 0 1 -6,-6v-26a6,6 0 0 1 6,-6z" />
    <path
        android:fillColor="#FFFFFF"
        android:pathData="M44,44h34a6,6 0 0 1 6,6v24a6,6 0 0 1 -6,6h-34a6,6 0 0 1 -6,-6v-24a6,6 0 0 1 6,-6z" />
    <path
        android:fillColor="{ACCENT}"
        android:pathData="M50,67a6,5 0 1 0 12,0a6,5 0 1 0 -12,0zM59,50h14v6h-11v11h-3z" />
    </group>
</vector>
'''


def background(size):
    image = Image.new('RGB', (size, size))
    pixels = image.load()
    for y in range(size):
        for x in range(size):
            t = (x + y) / (2 * size - 2)
            pixels[x, y] = tuple(round(a + (b - a) * t) for a, b in zip(TOP, BOTTOM))
    return image.convert('RGBA')


def artwork(size):
    """Zeichnet Hintergrund und Motiv im 108er-Raster, wie es der Vektor tut."""
    s = size / GRID
    fit = lambda x, y: ((54 + (x - 54) * FIT) * s, (54.5 + (y - 54.5) * FIT) * s)
    box = lambda a, b, c, d: [*fit(a, b), *fit(c, d)]
    image = background(size)
    back = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    ImageDraw.Draw(back).rounded_rectangle(
        box(24, 29, 72, 67), radius=6 * s * FIT, fill=(255, 255, 255, 102)
    )
    image.alpha_composite(back)
    draw = ImageDraw.Draw(image)
    draw.rounded_rectangle(box(38, 44, 84, 80), radius=6 * s * FIT, fill='white')
    draw.ellipse(box(50, 62, 62, 72), fill=ACCENT)
    draw.rectangle(box(59, 50, 62, 67), fill=ACCENT)
    draw.rectangle(box(59, 50, 73, 56), fill=ACCENT)
    return image


def masked(size, round_icon):
    """Legt die Form für Android 7 an; dort gibt es noch keine Adaptive Icons."""
    big = size * SCALE
    # Der Launcher zeigt nur die mittleren 72 von 108 Einheiten.
    art = artwork(round(big * 108 / 72))
    inset = (art.width - big) // 2
    art = art.crop((inset, inset, inset + big, inset + big))
    mask = Image.new('L', (big, big), 0)
    if round_icon:
        ImageDraw.Draw(mask).ellipse([0, 0, big - 1, big - 1], fill=255)
    else:
        ImageDraw.Draw(mask).rounded_rectangle([0, 0, big - 1, big - 1], radius=big // 5, fill=255)
    art.putalpha(mask)
    return art.resize((size, size), Image.LANCZOS)


(RES / 'drawable/ic_launcher_background.xml').write_text(BACKGROUND, encoding='utf-8')
(RES / 'drawable/ic_launcher_foreground.xml').write_text(FOREGROUND, encoding='utf-8')
for density, size in {'mdpi': 48, 'hdpi': 72, 'xhdpi': 96, 'xxhdpi': 144, 'xxxhdpi': 192}.items():
    folder = RES / f'mipmap-{density}'
    masked(size, False).save(folder / 'ic_launcher.png')
    masked(size, True).save(folder / 'ic_launcher_round.png')

PREVIEW.parent.mkdir(exist_ok=True)
sheet = Image.new('RGBA', (1000, 420), (245, 245, 247, 255))
sheet.alpha_composite(masked(340, True), (40, 40))
sheet.alpha_composite(masked(340, False), (420, 40))
sheet.alpha_composite(masked(96, True), (800, 60))
sheet.alpha_composite(masked(48, True), (824, 200))
sheet.convert('RGB').save(PREVIEW)
print('Icon geschrieben:', PREVIEW)
