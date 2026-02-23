"""
Generate Google Play Store assets for Word Journey:
  - 512x512 launcher icon  → store-assets/icon_512.png
  - 1024x500 feature graphic → store-assets/feature_graphic.png
"""
import math
import os
from PIL import Image, ImageDraw, ImageFont

# ── Output directory ──────────────────────────────────────────────────────────
SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(SCRIPT_DIR)
OUT_DIR = os.path.join(ROOT, "store-assets")
os.makedirs(OUT_DIR, exist_ok=True)

# ── Brand colours ─────────────────────────────────────────────────────────────
BG_DARK   = (28, 22, 16)      # #1C1610  parchment dark
GOLD      = (201, 168, 76)    # #C9A84C
RED_N     = (239, 68, 68)     # #EF4444
GREEN     = (83, 141, 78)     # #538D4E
GREY_TILE = (85, 87, 89)      # #555759
WHITE     = (255, 255, 255)
NAVY      = (26, 26, 46)      # #1a1a2e  (website bg, feature graphic bg)

# ── Font helper ───────────────────────────────────────────────────────────────
def load_font(size, bold=False):
    candidates = [
        "C:/Windows/Fonts/segoeui.ttf"  if not bold else "C:/Windows/Fonts/segoeuib.ttf",
        "C:/Windows/Fonts/arial.ttf"    if not bold else "C:/Windows/Fonts/arialbd.ttf",
        "C:/Windows/Fonts/calibri.ttf"  if not bold else "C:/Windows/Fonts/calibrib.ttf",
    ]
    for path in candidates:
        if os.path.exists(path):
            try:
                return ImageFont.truetype(path, size)
            except Exception:
                pass
    return ImageFont.load_default()

# ── Rounded rectangle helper ──────────────────────────────────────────────────
def rounded_rect(draw, xy, radius, fill, outline=None, outline_width=0):
    x0, y0, x1, y1 = xy
    r = radius
    draw.rectangle([x0+r, y0, x1-r, y1], fill=fill)
    draw.rectangle([x0, y0+r, x1, y1-r], fill=fill)
    draw.ellipse([x0, y0, x0+2*r, y0+2*r], fill=fill)
    draw.ellipse([x1-2*r, y0, x1, y0+2*r], fill=fill)
    draw.ellipse([x0, y1-2*r, x0+2*r, y1], fill=fill)
    draw.ellipse([x1-2*r, y1-2*r, x1, y1], fill=fill)
    if outline and outline_width > 0:
        draw.arc([x0, y0, x0+2*r, y0+2*r], 180, 270, fill=outline, width=outline_width)
        draw.arc([x1-2*r, y0, x1, y0+2*r], 270, 360, fill=outline, width=outline_width)
        draw.arc([x0, y1-2*r, x0+2*r, y1], 90, 180, fill=outline, width=outline_width)
        draw.arc([x1-2*r, y1-2*r, x1, y1], 0, 90, fill=outline, width=outline_width)
        draw.line([x0+r, y0, x1-r, y0], fill=outline, width=outline_width)
        draw.line([x0+r, y1, x1-r, y1], fill=outline, width=outline_width)
        draw.line([x0, y0+r, x0, y1-r], fill=outline, width=outline_width)
        draw.line([x1, y0+r, x1, y1-r], fill=outline, width=outline_width)

# ── Draw letter centred in a tile ─────────────────────────────────────────────
def draw_letter(draw, letter, cx, cy, font, color=WHITE):
    bbox = draw.textbbox((0, 0), letter, font=font)
    w = bbox[2] - bbox[0]
    h = bbox[3] - bbox[1]
    draw.text((cx - w//2, cy - h//2), letter, font=font, fill=color)

# ─────────────────────────────────────────────────────────────────────────────
#  ICON  512 × 512
# ─────────────────────────────────────────────────────────────────────────────
def draw_icon(size=512):
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    S = size / 108.0   # scale factor from 108dp viewport
    cx = cy = size // 2

    # ── Background circle (clipped to circle shape, as many launchers show) ──
    bg_img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    bg_draw = ImageDraw.Draw(bg_img)
    bg_draw.ellipse([0, 0, size-1, size-1], fill=BG_DARK + (255,))
    img.paste(bg_img, mask=bg_img)

    # ── Subtle radial gradient overlay (lighter centre) ───────────────────────
    for r in range(int(size*0.5), 0, -3):
        alpha = int(18 * (1 - r / (size * 0.5)))
        overlay = Image.new("RGBA", (size, size), (0, 0, 0, 0))
        odraw = ImageDraw.Draw(overlay)
        odraw.ellipse([cx-r, cy-r, cx+r, cy+r], fill=(255, 220, 120, alpha))
        img = Image.alpha_composite(img, overlay)
    draw = ImageDraw.Draw(img)

    # ── Compass outer ring ────────────────────────────────────────────────────
    ring_r = int(44 * S)
    ring_w = max(3, int(3.5 * S))
    draw.ellipse([cx-ring_r, cy-ring_r, cx+ring_r, cy+ring_r],
                 outline=GOLD, width=ring_w)

    # ── Cardinal diamond helper ───────────────────────────────────────────────
    def diamond(pts, color):
        scaled = [(p[0]*S, p[1]*S) for p in pts]
        draw.polygon(scaled, fill=color)

    # North (red)
    diamond([(54,12),(58,26),(54,30),(50,26)], RED_N)
    # South (gold)
    diamond([(54,78),(58,82),(54,96),(50,82)], GOLD)
    # West (gold)
    diamond([(12,54),(26,50),(30,54),(26,58)], GOLD)
    # East (gold)
    diamond([(78,54),(82,50),(96,54),(82,58)], GOLD)

    # ── 2x2 letter tiles ──────────────────────────────────────────────────────
    tile_pad = int(2 * S)
    tile_r   = max(4, int(2 * S))

    tiles = [
        # (x0, y0, x1, y1 in 108-viewport coords, fill, letter)
        (40, 38, 52, 52, GREEN,     "W"),
        (56, 38, 70, 52, GOLD,      "O"),
        (40, 56, 52, 70, GOLD,      "R"),
        (56, 56, 70, 70, GREY_TILE, "D"),
    ]
    font_tile = load_font(max(8, int(10 * S)), bold=True)

    for (vx0, vy0, vx1, vy1, color, letter) in tiles:
        px0 = int(vx0 * S) + tile_pad
        py0 = int(vy0 * S) + tile_pad
        px1 = int(vx1 * S) - tile_pad
        py1 = int(vy1 * S) - tile_pad
        rounded_rect(draw, (px0, py0, px1, py1), tile_r, fill=color)
        draw_letter(draw, letter, (px0+px1)//2, (py0+py1)//2, font_tile)

    # ── Thin inner glow ring inside compass ring ──────────────────────────────
    glow_r = ring_r - ring_w - 2
    draw.ellipse([cx-glow_r, cy-glow_r, cx+glow_r, cy+glow_r],
                 outline=(201, 168, 76, 60), width=1)

    return img


# ─────────────────────────────────────────────────────────────────────────────
#  FEATURE GRAPHIC  1024 × 500
# ─────────────────────────────────────────────────────────────────────────────
def draw_feature_graphic():
    W, H = 1024, 500
    img = Image.new("RGB", (W, H), NAVY)
    draw = ImageDraw.Draw(img)

    # ── Background: subtle diagonal gradient ─────────────────────────────────
    for i in range(H):
        t = i / H
        r = int(26 + t * 12)
        g = int(26 + t * 8)
        b = int(46 + t * 20)
        draw.line([(0, i), (W, i)], fill=(r, g, b))

    # ── Decorative background tiles (scattered, low opacity) ─────────────────
    bg_tiles = [
        (30, 40, 90, 95,   (83, 141, 78, 35),   "W"),
        (100, 20, 160, 75, (201, 168, 76, 30),   "O"),
        (30, 110, 90, 165, (201, 168, 76, 25),   "R"),
        (100, 110,160,165, (85, 87, 89, 30),     "D"),
        (860, 340,920,395, (83, 141, 78, 35),    "W"),
        (930, 340,990,395, (201, 168, 76, 30),   "O"),
        (860, 410,920,465, (201, 168, 76, 25),   "R"),
        (930, 410,990,465, (85, 87, 89, 30),     "D"),
        (170, 380,220,428, (83, 141, 78, 20),    ""),
        (230, 380,280,428, (201, 168, 76, 20),   ""),
        (780, 50, 830, 98, (201, 168, 76, 20),   ""),
        (840, 50, 890, 98, (85, 87, 89, 20),     ""),
    ]
    for (x0, y0, x1, y1, color, letter) in bg_tiles:
        overlay = Image.new("RGBA", (W, H), (0, 0, 0, 0))
        odraw = ImageDraw.Draw(overlay)
        rounded_rect(odraw, (x0, y0, x1, y1), 8, fill=color[:3] + (color[3],))
        img = img.convert("RGBA")
        img = Image.alpha_composite(img, overlay)
    img = img.convert("RGB")
    draw = ImageDraw.Draw(img)

    # ── Icon (scaled down) on left ────────────────────────────────────────────
    icon = draw_icon(320)
    icon_rgb = icon.convert("RGBA")
    icon_x = 60
    icon_y = (H - 320) // 2
    img.paste(icon_rgb, (icon_x, icon_y), mask=icon_rgb)
    draw = ImageDraw.Draw(img)

    # ── Title: "Word Journey" ─────────────────────────────────────────────────
    title_font = load_font(92, bold=True)
    title = "Word Journey"
    tx = 430
    ty = 110
    # Shadow
    draw.text((tx+3, ty+3), title, font=title_font, fill=(0, 0, 0, 120) if False else (10,10,20))
    # Main text in gold
    draw.text((tx, ty), title, font=title_font, fill=GOLD)

    # ── Subtitle ──────────────────────────────────────────────────────────────
    sub_font = load_font(34)
    subtitle = "Guess the Word. Explore the Journey."
    draw.text((tx, ty + 110), subtitle, font=sub_font, fill=(220, 200, 160))

    # ── Row of 5 Wordle-style tiles showing "WORD?" ───────────────────────────
    tile_colors = [GREEN, GOLD, GOLD, GREY_TILE, (50, 50, 70)]
    tile_letters = ["W", "O", "R", "D", "?"]
    tile_size = 56
    tile_gap = 10
    tile_y0 = ty + 175
    tile_x_start = tx

    tile_font = load_font(32, bold=True)
    for i, (col, letter) in enumerate(zip(tile_colors, tile_letters)):
        tx0 = tile_x_start + i * (tile_size + tile_gap)
        ty0 = tile_y0
        tx1 = tx0 + tile_size
        ty1 = ty0 + tile_size
        rounded_rect(draw, (tx0, ty0, tx1, ty1), 6, fill=col,
                     outline=(255,255,255,80) if False else None)
        draw_letter(draw, letter, (tx0+tx1)//2, (ty0+ty1)//2, tile_font)

    # ── Thin gold divider line under tiles ───────────────────────────────────
    line_y = tile_y0 + tile_size + 22
    draw.line([(tx, line_y), (tx + 5*(tile_size+tile_gap)-tile_gap, line_y)],
              fill=GOLD, width=2)

    # ── Feature bullets ───────────────────────────────────────────────────────
    bullet_font = load_font(26)
    bullets = [
        "📅  Daily Challenges",
        "🏆  Hundreds of Levels",
        "⚡  Timer Mode",
    ]
    for j, b in enumerate(bullets):
        draw.text((tx, line_y + 18 + j * 44), b, font=bullet_font, fill=(180, 170, 150))

    # ── Version badge ─────────────────────────────────────────────────────────
    badge_font = load_font(22, bold=True)
    badge_text = "v2.15.0"
    badge_bbox = draw.textbbox((0, 0), badge_text, font=badge_font)
    bw = badge_bbox[2] - badge_bbox[0] + 24
    bh = badge_bbox[3] - badge_bbox[1] + 12
    bx = W - bw - 24
    by = H - bh - 20
    rounded_rect(draw, (bx, by, bx+bw, by+bh), 8, fill=(40, 35, 20))
    rounded_rect(draw, (bx, by, bx+bw, by+bh), 8, fill=None,
                 outline=GOLD, outline_width=1)
    draw.text((bx + 12, by + 6), badge_text, font=badge_font, fill=GOLD)

    return img


# ─────────────────────────────────────────────────────────────────────────────
#  Main
# ─────────────────────────────────────────────────────────────────────────────
if __name__ == "__main__":
    print("Generating 512x512 launcher icon...")
    icon = draw_icon(512)
    icon_path = os.path.join(OUT_DIR, "icon_512.png")
    icon.save(icon_path, "PNG")
    print(f"  Saved: {icon_path}")

    print("Generating 1024x500 feature graphic...")
    fg = draw_feature_graphic()
    fg_path = os.path.join(OUT_DIR, "feature_graphic.png")
    fg.save(fg_path, "PNG")
    print(f"  Saved: {fg_path}")

    print("\nDone! Files are in store-assets/")
