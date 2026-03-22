#!/usr/bin/env python3
"""
deploy_to_play.py
=================
Uploads a signed AAB to Google Play Console and submits it to the
production track for review.

PREREQUISITES:
  pip install google-api-python-client google-auth

USAGE:
  python scripts/deploy_to_play.py \\
      --key   word-journey-488202-e738649def06.json \\
      --aab   app/build/outputs/bundle/release/word-journey-release.aab \\
      --track production

  Or for a staged rollout to 10 % before full release:
      --track production --rollout 0.10

WHAT IT DOES:
  1. Creates a new edit in Play Console
  2. Uploads the AAB to the edit
  3. Assigns the AAB to the requested track (default: production)
  4. Sets release notes (en-US)
  5. Commits the edit → Google begins the review process
  6. Once approved in Play Console, the update goes live automatically

NOTES:
  - The service account must have "Release Manager" (or higher) role in Play Console.
  - A full production rollout (rollout=1.0) still requires Google's review;
    the release will go live automatically once the review passes.
"""

import argparse
import json
import os
import sys

PACKAGE_NAME = "com.djtaylor.wordjourney"

RELEASE_NOTES = """v2.20.0 - Achievements Expansion

• 31 Google Play Games achievements across all game areas
• Daily streak milestones: 3, 7, 14, 30 days
• Login streak achievements: 7 and 30 days
• VIP subscriber & seasonal champion achievements
• Power-up usage milestones (first use, 50 items)
• Coin earning milestone (10,000 total coins)
• Achievements for watching ads & in-app purchases
• New "Down to the Wire" last-guess win achievement
• Bug fixes and performance improvements"""


def build_service(key_path: str):
    from google.oauth2 import service_account
    from googleapiclient.discovery import build

    scopes = ["https://www.googleapis.com/auth/androidpublisher"]
    creds = service_account.Credentials.from_service_account_file(key_path, scopes=scopes)
    return build("androidpublisher", "v3", credentials=creds)


def deploy(key_path: str, aab_path: str, track: str, rollout_fraction: float,
           dry_run: bool = False):
    if dry_run:
        print("=== DRY RUN — no API calls will be made ===")
        print(f"  Package:  {PACKAGE_NAME}")
        print(f"  AAB:      {aab_path}")
        print(f"  Track:    {track}")
        print(f"  Rollout:  {rollout_fraction * 100:.0f}%")
        print(f"\nRelease notes:\n{RELEASE_NOTES}")
        return

    if not os.path.exists(aab_path):
        print(f"ERROR: AAB file not found: {aab_path}")
        sys.exit(1)

    service = build_service(key_path)
    edits = service.edits()

    # 1. Create a new edit
    print("Creating edit...")
    edit = edits.insert(packageName=PACKAGE_NAME, body={}).execute()
    edit_id = edit["id"]
    print(f"  Edit ID: {edit_id}")

    try:
        # 2. Upload the AAB
        print(f"Uploading AAB ({os.path.getsize(aab_path) / 1_000_000:.1f} MB)...")
        from googleapiclient.http import MediaFileUpload
        media = MediaFileUpload(aab_path, mimetype="application/octet-stream", resumable=True)
        upload_result = edits.bundles().upload(
            packageName=PACKAGE_NAME,
            editId=edit_id,
            media_body=media
        ).execute()
        version_code = upload_result["versionCode"]
        print(f"  Uploaded versionCode: {version_code}")

        # 3. Build the track release body
        release_body = {
            "name": "v2.20.0",
            "status": "completed" if rollout_fraction >= 1.0 else "inProgress",
            "versionCodes": [str(version_code)],
            "releaseNotes": [
                {
                    "language": "en-US",
                    "text": RELEASE_NOTES
                }
            ],
        }
        if rollout_fraction < 1.0:
            release_body["userFraction"] = rollout_fraction

        # 4. Assign to track
        print(f"Assigning to track: {track} ({rollout_fraction * 100:.0f}% rollout)...")
        edits.tracks().update(
            packageName=PACKAGE_NAME,
            editId=edit_id,
            track=track,
            body={"releases": [release_body]}
        ).execute()
        print("  Track updated.")

        # 5. Commit the edit
        print("Committing edit...")
        commit_result = edits.commit(
            packageName=PACKAGE_NAME,
            editId=edit_id
        ).execute()
        print(f"  Edit committed: {commit_result.get('id', edit_id)}")

        print(f"\n✅ Successfully submitted v2.20.0 to Play Console ({track} track).")
        print("   Google will review the update. Once approved it will go live automatically.")
        print(f"   Review status: https://play.google.com/console/u/0/developers/"
              f"9022665278248042/app-list")

    except Exception as e:
        # Delete the edit on failure to avoid orphaned edits
        print(f"\nERROR during upload/commit: {e}")
        try:
            edits.delete(packageName=PACKAGE_NAME, editId=edit_id).execute()
            print("  Cleaned up failed edit.")
        except Exception:
            pass
        sys.exit(1)


def main():
    parser = argparse.ArgumentParser(description="Deploy AAB to Google Play Console")
    parser.add_argument("--key",     required=True,  help="Path to service account JSON key")
    parser.add_argument("--aab",     required=True,  help="Path to signed AAB file")
    parser.add_argument("--track",   default="production",
                        choices=["internal", "alpha", "beta", "production"],
                        help="Release track (default: production)")
    parser.add_argument("--rollout", type=float, default=1.0,
                        help="Rollout fraction 0.0-1.0 (default: 1.0 = 100%%)")
    parser.add_argument("--dry-run", action="store_true",
                        help="Print what would happen without calling the API")
    args = parser.parse_args()

    deploy(
        key_path=args.key,
        aab_path=args.aab,
        track=args.track,
        rollout_fraction=args.rollout,
        dry_run=args.dry_run,
    )


if __name__ == "__main__":
    main()
