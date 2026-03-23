#!/usr/bin/env python3
"""
Upload an icon to every achievement and publish them all.

Usage:
    python scripts/publish_achievements.py
        [--key  word-journey-488202-e738649def06.json]
        [--app-id 1097721266836]
        [--icon  scripts/achievement_icon.png]   # generated if absent
"""
import argparse
import math
import os
import sys
import tempfile
import time

from google.oauth2 import service_account
from googleapiclient.discovery import build

# ---------------------------------------------------------------------------
# All 31 achievement IDs (matches AchievementIds.kt)
# ---------------------------------------------------------------------------
ACHIEVEMENTS = {
    "FIRST_WIN":          "CgkIlIWlqvkfEAIQAg",
    "WIN_10":             "CgkIlIWlqvkfEAIQAw",
    "WIN_50":             "CgkIlIWlqvkfEAIQBA",
    "WIN_1":              "CgkIlIWlqvkfEAIQBQ",
    "STREAK_7":           "CgkIlIWlqvkfEAIQBg",
    "USE_HINT":           "CgkIlIWlqvkfEAIQBw",
    "BROWSE_STORE":       "CgkIlIWlqvkfEAIQCA",
    "MAKE_PURCHASE":      "CgkIlIWlqvkfEAIQCQ",
    "WIN_100":            "CgkIlIWlqvkfEAIQDw",
    "WIN_250":            "CgkIlIWlqvkfEAIQEA",
    "WIN_500":            "CgkIlIWlqvkfEAIQEQ",
    "WIN_1000":           "CgkIlIWlqvkfEAIQEg",
    "GUESS_1ST":          "CgkIlIWlqvkfEAIQFA",
    "GUESS_2ND":          "CgkIlIWlqvkfEAIQFQ",
    "GUESS_3RD":          "CgkIlIWlqvkfEAIQFg",
    "LAST_GUESS":         "CgkIlIWlqvkfEAIQFw",
    "STREAK_14":          "CgkIlIWlqvkfEAIQGA",
    "STREAK_30":          "CgkIlIWlqvkfEAIQGQ",
    "STREAK_60":          "CgkIlIWlqvkfEAIQGg",
    "PLAY_100":           "CgkIlIWlqvkfEAIQGw",
    "PLAY_365":           "CgkIlIWlqvkfEAIQHA",
    "LEVEL_10":           "CgkIlIWlqvkfEAIQHQ",
    "LEVEL_25":           "CgkIlIWlqvkfEAIQHg",
    "COLLECT_10_PACKS":   "CgkIlIWlqvkfEAIQHw",
    "SEASONAL":           "CgkIlIWlqvkfEAIQIA",
    "DAILY_5":            "CgkIlIWlqvkfEAIQIQ",
    "DAILY_20":           "CgkIlIWlqvkfEAIQIg",
    "COINS_1000":         "CgkIlIWlqvkfEAIQIw",
    "COINS_10000":        "CgkIlIWlqvkfEAIQJA",
    "SPEND_100":          "CgkIlIWlqvkfEAIQJQ",
    "VIP_SUBSCRIBER":     "CgkIlIWlqvkfEAIQJg",
}


# ---------------------------------------------------------------------------
# Icon generation (Pillow)
# ---------------------------------------------------------------------------
def generate_icon(path: str, size: int = 512) -> None:
    """Draw the Word Journey compass / tile logo at *size* x *size* pixels."""
    from PIL import Image, ImageDraw, ImageFont

    img = Image.new("RGBA", (size, size), (26, 26, 26, 255))  # dark bg
    draw = ImageDraw.Draw(img)

    cx, cy = size // 2, size // 2

    # ── Outer compass ring ──────────────────────────────────────────────────
    ring_r = int(size * 0.44)
    stroke = max(4, int(size * 0.028))
    gold = (201, 168, 76, 255)
    draw.ellipse(
        [cx - ring_r, cy - ring_r, cx + ring_r, cy + ring_r],
        outline=gold,
        width=stroke,
    )

    # ── Cardinal-point triangles (N / S / E / W) ────────────────────────────
    tip_r  = int(size * 0.48)   # tip of triangle (just outside ring)
    base_r = int(size * 0.36)   # base edge distance from centre
    half_b = int(size * 0.04)   # half base width
    for angle_deg in (90, 270, 0, 180):   # N, S, E, W
        θ = math.radians(angle_deg)
        perp = math.radians(angle_deg + 90)
        tip  = (cx + tip_r  * math.cos(θ),  cy - tip_r  * math.sin(θ))
        bl   = (cx + base_r * math.cos(θ) + half_b * math.cos(perp),
                cy - base_r * math.sin(θ) - half_b * math.sin(perp))
        br   = (cx + base_r * math.cos(θ) - half_b * math.cos(perp),
                cy - base_r * math.sin(θ) + half_b * math.sin(perp))
        draw.polygon([tip, bl, br], fill=gold)

    # ── 2×2 Wordle tile grid ─────────────────────────────────────────────────
    tile = int(size * 0.155)
    gap  = int(size * 0.025)
    total_grid = 2 * tile + gap
    x0_grid = cx - total_grid // 2
    y0_grid = cy - total_grid // 2

    tile_colors = [
        (83,  141, 78,  255),   # green  (top-left)
        (201, 168, 76,  255),   # gold   (top-right)
        (201, 168, 76,  255),   # gold   (bottom-left)
        (85,  87,  89,  255),   # gray   (bottom-right)
    ]
    tile_letters = ["W", "J", "?", "!"]
    positions = [
        (x0_grid,           y0_grid),
        (x0_grid + tile + gap, y0_grid),
        (x0_grid,           y0_grid + tile + gap),
        (x0_grid + tile + gap, y0_grid + tile + gap),
    ]
    radius = max(2, int(tile * 0.15))

    # Try to load a font; fall back to default
    font = None
    font_size = int(tile * 0.56)
    for name in ("arialbd.ttf", "Arial Bold.ttf", "DejaVuSans-Bold.ttf", "FreeSansBold.ttf"):
        try:
            from PIL import ImageFont as _IF
            font = _IF.truetype(name, font_size)
            break
        except Exception:
            pass
    if font is None:
        try:
            from PIL import ImageFont as _IF
            font = _IF.load_default(size=font_size)
        except Exception:
            font = None

    for (tx, ty), color, letter in zip(positions, tile_colors, tile_letters):
        draw.rounded_rectangle([tx, ty, tx + tile, ty + tile], radius=radius, fill=color)
        # Centre the letter in the tile
        if font:
            bbox = draw.textbbox((0, 0), letter, font=font)
            tw, th = bbox[2] - bbox[0], bbox[3] - bbox[1]
            lx = tx + (tile - tw) // 2 - bbox[0]
            ly = ty + (tile - th) // 2 - bbox[1]
            draw.text((lx, ly), letter, fill=(255, 255, 255, 255), font=font)

    # Save
    img.save(path, "PNG")
    print(f"[icon] Saved {size}×{size} PNG → {path}")


# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------
def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--key",
                        default="word-journey-488202-e738649def06.json",
                        help="Path to service-account JSON key")
    parser.add_argument("--app-id", default="1097721266836")
    parser.add_argument("--icon",   default="scripts/achievement_icon.png",
                        help="Path for the icon PNG (generated if absent)")
    parser.add_argument("--icon-only", action="store_true",
                        help="Only generate the icon, don't upload/publish")
    args = parser.parse_args()

    # ── Generate icon if not present ────────────────────────────────────────
    if not os.path.exists(args.icon):
        print(f"[icon] Generating icon at {args.icon} …")
        generate_icon(args.icon)
    else:
        print(f"[icon] Using existing icon: {args.icon}")

    if args.icon_only:
        print("Done (--icon-only).")
        return

    # ── Authenticate ────────────────────────────────────────────────────────
    creds = service_account.Credentials.from_service_account_file(
        args.key,
        scopes=[
            "https://www.googleapis.com/auth/androidpublisher",
            "https://www.googleapis.com/auth/games",
        ],
    )
    svc = build("gamesConfiguration", "v1configuration", credentials=creds,
                cache_discovery=False)

    # AuthorizedSession for direct HTTP uploads (imageConfigurations is not in
    # the discovery doc but the REST endpoint still works).
    from google.auth.transport.requests import AuthorizedSession
    session = AuthorizedSession(creds)

    ok = 0
    fail = 0

    with open(args.icon, "rb") as fh:
        icon_bytes = fh.read()

    for name, ach_id in ACHIEVEMENTS.items():
        # ── Upload icon (direct HTTP multipart) ──────────────────────────────
        upload_url = (
            "https://gamesconfiguration.googleapis.com/upload/games/v1configuration"
            f"/images/{ach_id}/imageType/ACHIEVEMENT_ICON"
            "?uploadType=media"
        )
        try:
            resp = session.post(
                upload_url,
                data=icon_bytes,
                headers={"Content-Type": "image/png"},
                timeout=30,
            )
            resp.raise_for_status()
            print(f"  [upload] {name} ({ach_id}) ✓ icon uploaded  (HTTP {resp.status_code})")
        except Exception as exc:
            print(f"  [upload] {name} ({ach_id}) ✗ icon FAILED: {exc}")
            fail += 1
            continue

        # ── Publish via gamesConfiguration discovery API ──────────────────
        try:
            svc.achievementConfigurations().publish(
                achievementId=ach_id
            ).execute()
            print(f"  [publish] {name} ({ach_id}) ✓ published")
            ok += 1
        except Exception as exc:
            print(f"  [publish] {name} ({ach_id}) ✗ publish FAILED: {exc}")
            fail += 1

        time.sleep(0.15)   # stay well within API rate limits

    print()
    print(f"Done. Published: {ok}/{len(ACHIEVEMENTS)}   Failed: {fail}")

    if fail:
        sys.exit(1)


if __name__ == "__main__":
    main()
