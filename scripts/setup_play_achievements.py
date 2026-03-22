#!/usr/bin/env python3
"""
setup_play_achievements.py
==========================
Creates Word Journeys Play Games achievements via the Google Play Game Services
Publishing API (gamesConfiguration v1).

PREREQUISITES (one-time setup in Play Console):
  1. Go to Play Console → Word Journeys → Grow users → Play Games Services
  2. Click "Set up Play Games Services" if not already set up
  3. Choose "Use an existing Google Play game" or create a new one
  4. Link your app (com.djtaylor.wordjourney) to the game
  5. On the game overview page, note the APPLICATION_ID (numeric, e.g. "123456789012")
  6. Make sure the service account (api-access@word-journey-488202.iam.gserviceaccount.com)
     has "Release Manager" or higher access in Play Console

USAGE:
  python scripts/setup_play_achievements.py \\
      --key word-journey-488202-e738649def06.json \\
      --app-id YOUR_PLAY_GAMES_APP_ID

AFTER RUNNING:
  Copy the printed achievement IDs into:
  app/src/main/java/com/djtaylor/wordjourney/auth/AchievementIds.kt
"""

import argparse
import json
import sys
import time

# ── Configuration ──────────────────────────────────────────────────────────────
SERVICE_ACCOUNT_KEY_PATH = ""
PLAY_GAMES_APP_ID        = ""   # Numeric ID from Play Console → Play Games Services
PACKAGE_NAME             = "com.djtaylor.wordjourney"
# ───────────────────────────────────────────────────────────────────────────────

# ── Achievement definitions ────────────────────────────────────────────────────
# Each dict maps to the AchievementConfiguration resource.
# type: "STANDARD" (unlock once) or "INCREMENTAL" (unlock after N steps)
ACHIEVEMENTS = [
    # ── First steps ──────────────────────────────────────────────────────────
    {
        "key":          "FIRST_WIN",
        "type":         "STANDARD",
        "name":         "First Victory",
        "description":  "Solve your first Word Journeys puzzle.",
        "xp":           100,
    },
    # ── Win count milestones ──────────────────────────────────────────────────
    {
        "key":          "WIN_10",
        "type":         "INCREMENTAL",
        "steps":        10,
        "name":         "Word Novice",
        "description":  "Solve 10 puzzles.",
        "xp":           200,
    },
    {
        "key":          "WIN_50",
        "type":         "INCREMENTAL",
        "steps":        50,
        "name":         "Word Apprentice",
        "description":  "Solve 50 puzzles.",
        "xp":           500,
    },
    {
        "key":          "WIN_100",
        "type":         "INCREMENTAL",
        "steps":        100,
        "name":         "Word Master",
        "description":  "Solve 100 puzzles.",
        "xp":           1000,
    },
    {
        "key":          "WIN_250",
        "type":         "INCREMENTAL",
        "steps":        250,
        "name":         "Veteran Wordsmith",
        "description":  "Solve 250 puzzles. A true veteran!",
        "xp":           2500,
    },
    {
        "key":          "WIN_500",
        "type":         "INCREMENTAL",
        "steps":        500,
        "name":         "Word Legend",
        "description":  "Solve 500 puzzles. You are a legend!",
        "xp":           5000,
    },
    # ── Skill ─────────────────────────────────────────────────────────────────
    {
        "key":          "FIRST_GUESS_WIN",
        "type":         "STANDARD",
        "name":         "Mind Reader",
        "description":  "Solve a puzzle in a single guess.",
        "xp":           500,
    },
    {
        "key":          "TWO_GUESS_WIN",
        "type":         "STANDARD",
        "name":         "Sharp Mind",
        "description":  "Solve a puzzle using only 2 guesses.",
        "xp":           300,
    },
    {
        "key":          "NO_POWERUP_WIN",
        "type":         "STANDARD",
        "name":         "Pure Skill",
        "description":  "Solve a puzzle without using any power-ups.",
        "xp":           250,
    },
    {
        "key":          "LAST_GUESS_WIN",
        "type":         "STANDARD",
        "name":         "Down to the Wire",
        "description":  "Solve a puzzle on your very last available guess!",
        "xp":           300,
    },
    # ── Daily challenge streaks ───────────────────────────────────────────────
    {
        "key":          "STREAK_3",
        "type":         "STANDARD",
        "name":         "On a Roll",
        "description":  "Solve the daily challenge 3 days in a row.",
        "xp":           200,
    },
    {
        "key":          "STREAK_7",
        "type":         "STANDARD",
        "name":         "Week Warrior",
        "description":  "Solve the daily challenge 7 days in a row.",
        "xp":           500,
    },
    {
        "key":          "STREAK_14",
        "type":         "STANDARD",
        "name":         "Two-Week Warrior",
        "description":  "Solve the daily challenge 14 days in a row!",
        "xp":           1000,
    },
    {
        "key":          "STREAK_30",
        "type":         "STANDARD",
        "name":         "Monthly Maven",
        "description":  "Solve the daily challenge 30 days in a row!",
        "xp":           2000,
    },
    # ── Login streaks ─────────────────────────────────────────────────────────
    {
        "key":          "LOGIN_STREAK_7",
        "type":         "STANDARD",
        "name":         "Frequent Flyer",
        "description":  "Log in to Word Journeys 7 days in a row.",
        "xp":           300,
    },
    {
        "key":          "LOGIN_STREAK_30",
        "type":         "STANDARD",
        "name":         "Dedicated Player",
        "description":  "Log in to Word Journeys 30 days in a row!",
        "xp":           1000,
    },
    # ── Level pack progress ───────────────────────────────────────────────────
    {
        "key":          "REACH_LEVEL_10",
        "type":         "STANDARD",
        "name":         "Getting Started",
        "description":  "Reach level 10 in any word pack.",
        "xp":           100,
    },
    {
        "key":          "REACH_LEVEL_25",
        "type":         "STANDARD",
        "name":         "Levelling Up",
        "description":  "Reach level 25 in any word pack.",
        "xp":           250,
    },
    {
        "key":          "REACH_LEVEL_50",
        "type":         "STANDARD",
        "name":         "Halfway There",
        "description":  "Reach level 50 in any word pack.",
        "xp":           500,
    },
    {
        "key":          "PACK_MASTER",
        "type":         "STANDARD",
        "name":         "Pack Master",
        "description":  "Complete all 100 levels in a word pack!",
        "xp":           1000,
    },
    # ── Seasonal packs ────────────────────────────────────────────────────────
    {
        "key":          "SEASONAL_CHAMPION",
        "type":         "STANDARD",
        "name":         "Seasonal Champion",
        "description":  "Win your first level in a seasonal word pack.",
        "xp":           300,
    },
    # ── Daily challenge volume ────────────────────────────────────────────────
    {
        "key":          "FIRST_DAILY",
        "type":         "STANDARD",
        "name":         "Daily Dedication",
        "description":  "Complete your first daily challenge.",
        "xp":           100,
    },
    {
        "key":          "DAILY_10",
        "type":         "INCREMENTAL",
        "steps":        10,
        "name":         "Daily Regular",
        "description":  "Complete 10 daily challenges.",
        "xp":           500,
    },
    {
        "key":          "DAILY_100",
        "type":         "INCREMENTAL",
        "steps":        100,
        "name":         "Daily Devotee",
        "description":  "Complete 100 daily challenges. Impressive dedication!",
        "xp":           2000,
    },
    # ── Power-up / item usage ─────────────────────────────────────────────────
    {
        "key":          "FIRST_ITEM_USED",
        "type":         "STANDARD",
        "name":         "Tool of the Trade",
        "description":  "Use a power-up item for the first time.",
        "xp":           100,
    },
    {
        "key":          "ITEMS_USED_50",
        "type":         "INCREMENTAL",
        "steps":        50,
        "name":         "Power Player",
        "description":  "Use 50 power-up items in total.",
        "xp":           500,
    },
    # ── Economy ──────────────────────────────────────────────────────────────
    {
        "key":          "COIN_EARNER_10000",
        "type":         "INCREMENTAL",
        "steps":        10000,
        "name":         "Coin Magnate",
        "description":  "Earn 10,000 coins in total.",
        "xp":           1000,
    },
    # ── Ad rewards ───────────────────────────────────────────────────────────
    {
        "key":          "FIRST_AD_WATCHED",
        "type":         "STANDARD",
        "name":         "Window Shopper",
        "description":  "Watch a rewarded ad for the first time.",
        "xp":           50,
    },
    # ── In-app purchases ─────────────────────────────────────────────────────
    {
        "key":          "FIRST_PURCHASE",
        "type":         "STANDARD",
        "name":         "Supporter",
        "description":  "Make your first in-app purchase. Thank you!",
        "xp":           200,
    },
    {
        "key":          "BUNDLE_BUYER",
        "type":         "STANDARD",
        "name":         "Big Spender",
        "description":  "Purchase a bundle pack (Starter, Adventurer, or Champion).",
        "xp":           500,
    },
    # ── VIP ───────────────────────────────────────────────────────────────────
    {
        "key":          "VIP_SUBSCRIBER",
        "type":         "STANDARD",
        "name":         "VIP Member",
        "description":  "Activate a VIP subscription. Welcome to the club!",
        "xp":           500,
    },
]


def enable_games_api(key_path: str, project_number: str = "1097721266836"):
    """Enable the Game Services Publishing API in the GCP project."""
    from google.oauth2 import service_account
    from googleapiclient.discovery import build

    creds = service_account.Credentials.from_service_account_file(
        key_path, scopes=["https://www.googleapis.com/auth/cloud-platform"]
    )
    svc = build("serviceusage", "v1", credentials=creds)
    api = f"projects/{project_number}/services/gamesconfiguration.googleapis.com"
    try:
        result = svc.services().enable(name=api).execute()
        state = result.get("response", {}).get("service", {}).get("state", "?")
        print(f"  Games Configuration API: {state}")
    except Exception as e:
        if "already enabled" in str(e).lower() or "ALREADY_ENABLED" in str(e):
            print("  Games Configuration API: already enabled")
        else:
            print(f"  Warning enabling API: {e}")


def build_games_service(key_path: str):
    from google.oauth2 import service_account
    from googleapiclient.discovery import build

    scopes = [
        "https://www.googleapis.com/auth/androidpublisher",
        "https://www.googleapis.com/auth/games",
    ]
    creds = service_account.Credentials.from_service_account_file(key_path, scopes=scopes)
    return build("gamesConfiguration", "v1configuration", credentials=creds)


def create_achievement(service, app_id: str, ach: dict) -> str | None:
    """Create one achievement; returns its Play Games ID or None on failure."""
    is_incremental = ach["type"] == "INCREMENTAL"

    body = {
        "kind":          "gamesConfiguration#achievementConfiguration",
        "achievementType": ach["type"],
        "initialState":  "REVEALED",
        "published":     {
            "name":        {"kind": "gamesConfiguration#localizedStringBundle",
                            "translations": [{"kind": "gamesConfiguration#localizedString",
                                              "locale": "en-US",
                                              "value":  ach["name"]}]},
            "description": {"kind": "gamesConfiguration#localizedStringBundle",
                            "translations": [{"kind": "gamesConfiguration#localizedString",
                                              "locale": "en-US",
                                              "value":  ach["description"]}]},
            "pointValue":  ach.get("xp", 100),
        },
    }
    if is_incremental:
        body["stepsToUnlock"] = ach["steps"]

    try:
        result = service.achievementConfigurations().insert(
            applicationId=app_id,
            body=body
        ).execute()
        ach_id = result.get("id", "")
        print(f"  + {ach['key']:25s} -> {ach_id}  ({ach['name']})")
        return ach_id
    except Exception as e:
        print(f"  x {ach['key']:25s} FAILED: {str(e)[:120]}")
        return None


def main():
    parser = argparse.ArgumentParser(description="Create Word Journeys Play Games achievements")
    parser.add_argument("--key",    default=SERVICE_ACCOUNT_KEY_PATH,
                        help="Path to service account JSON key file")
    parser.add_argument("--app-id", default=PLAY_GAMES_APP_ID,
                        help="Play Games application ID (numeric, from Play Console)")
    parser.add_argument("--dry-run", action="store_true",
                        help="Print what would be created without calling the API")
    args = parser.parse_args()

    if args.dry_run:
        print("=== DRY RUN ===\n")
        for a in ACHIEVEMENTS:
            inc = f"  ({a['steps']} steps)" if a.get("steps") else ""
            print(f"  [{a['type']:11s}] {a['key']:25s}  {a['name']}{inc}")
        print(f"\nTotal: {len(ACHIEVEMENTS)} achievements")
        return

    if not args.key:
        print("ERROR: --key required"); sys.exit(1)
    if not args.app_id:
        print("ERROR: --app-id required.")
        print("\nTo find your app ID:")
        print("  1. Go to https://play.google.com/console")
        print("  2. Select Word Journeys")
        print("  3. In the left sidebar: Grow users → Play Games Services")
        print("  4. The numeric ID is shown in the URL or game overview page")
        sys.exit(1)

    print("Enabling Games Configuration API...")
    enable_games_api(args.key)

    print(f"\nCreating {len(ACHIEVEMENTS)} achievements for app {args.app_id}...\n")
    service = build_games_service(args.key)

    results = {}
    for ach in ACHIEVEMENTS:
        ach_id = create_achievement(service, args.app_id, ach)
        if ach_id:
            results[ach["key"]] = ach_id
        time.sleep(0.3)

    print(f"\n{'─'*60}")
    print(f"Created {len(results)}/{len(ACHIEVEMENTS)} achievements.\n")

    if results:
        print("Copy these lines into AchievementIds.kt:\n")
        for key, ach_id in results.items():
            print(f'    const val {key:25s} = "{ach_id}"')

        # Also write to a file for easy copy-paste
        out_path = "scripts/achievement_ids.txt"
        with open(out_path, "w") as f:
            for key, ach_id in results.items():
                f.write(f'    const val {key:25s} = "{ach_id}"\n')
        print(f"\nAlso saved to {out_path}")


if __name__ == "__main__":
    main()
