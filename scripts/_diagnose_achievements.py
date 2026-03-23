#!/usr/bin/env python3
"""Check current achievements status in Play Console."""
import sys
sys.path.insert(0, ".")
from google.oauth2 import service_account
from googleapiclient.discovery import build

KEY = "word-journey-488202-e738649def06.json"
APP_ID = "1097721266836"

creds = service_account.Credentials.from_service_account_file(
    KEY,
    scopes=["https://www.googleapis.com/auth/androidpublisher",
            "https://www.googleapis.com/auth/games"]
)
svc = build("gamesConfiguration", "v1configuration", credentials=creds)

items = svc.achievementConfigurations().list(applicationId=APP_ID).execute().get("items", [])
print(f"Total achievements: {len(items)}")
print()

for item in items:
    ach_id = item.get("id", "?")
    published = item.get("published")
    draft = item.get("draft", {})
    name_obj = draft.get("name", {})
    name = ""
    if name_obj:
        translations = name_obj.get("translations", [{}])
        name = translations[0].get("value", "?") if translations else "?"
    has_image = bool(draft.get("iconImage"))
    print(f"  {ach_id}  published={published}  hasIcon={has_image}  name={name}")
