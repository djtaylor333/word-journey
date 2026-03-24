"""
Fix streak achievements: convert from STANDARD → INCREMENTAL with correct step counts.
Uses individual GET on each achievement to obtain its concurrency token before updating.

Achievements to fix:
  STREAK_3        (On a Roll)       → INCREMENTAL, 3  steps
  STREAK_7        (Week Warrior)    → INCREMENTAL, 7  steps
  STREAK_14       (Two-Week Warrior)→ INCREMENTAL, 14 steps
  STREAK_30       (Monthly Maven)   → INCREMENTAL, 30 steps
  LOGIN_STREAK_7  (Frequent Flyer)  → INCREMENTAL, 7  steps
  LOGIN_STREAK_30 (Dedicated Player)→ INCREMENTAL, 30 steps
"""
import sys
sys.stdout.reconfigure(encoding='utf-8', errors='replace')
import warnings; warnings.filterwarnings('ignore')
from google.oauth2 import service_account
from googleapiclient.discovery import build

APP_ID = '1097721266836'

STREAK_FIXES = {
    'CgkIlIWlqvkfEAIQBA': ('On a Roll',        3),   # STREAK_3
    'CgkIlIWlqvkfEAIQGA': ('Week Warrior',      7),   # STREAK_7
    'CgkIlIWlqvkfEAIQGQ': ('Two-Week Warrior',  14),  # STREAK_14
    'CgkIlIWlqvkfEAIQGg': ('Monthly Maven',     30),  # STREAK_30
    'CgkIlIWlqvkfEAIQGw': ('Frequent Flyer',    7),   # LOGIN_STREAK_7
    'CgkIlIWlqvkfEAIQHA': ('Dedicated Player',  30),  # LOGIN_STREAK_30
}

creds = service_account.Credentials.from_service_account_file(
    'word-journey-488202-e738649def06.json',
    scopes=[
        'https://www.googleapis.com/auth/games',
        'https://www.googleapis.com/auth/androidpublisher',
    ]
)
svc = build('gamesConfiguration', 'v1configuration', credentials=creds, cache_discovery=False)

for aid, (name, steps) in STREAK_FIXES.items():
    print(f'\n--- {name} ({aid}) ---')

    # 1. GET the current resource to read its concurrency token
    try:
        current = svc.achievementConfigurations().get(achievementId=aid).execute()
    except Exception as e:
        print(f'  GET error: {e}')
        continue

    cur_type  = current.get('achievementType', '?')
    cur_steps = current.get('stepsToUnlock', None)
    token     = current.get('token', '')
    print(f'  Current: type={cur_type}, steps={cur_steps}, token={token[:20]}...')

    if cur_type == 'INCREMENTAL' and cur_steps == steps:
        print(f'  Already INCREMENTAL with {steps} steps — skipping')
        continue

    # 2. Build update body — keep ALL fields from GET response, only change type+steps
    body = dict(current)
    body['achievementType'] = 'INCREMENTAL'
    body['stepsToUnlock']   = steps
    # Only strip top-level 'kind'; keep token for concurrency control
    body.pop('kind', None)

    try:
        updated = svc.achievementConfigurations().update(
            achievementId=aid,
            body=body
        ).execute()
        new_type  = updated.get('achievementType', '?')
        new_steps = updated.get('stepsToUnlock', '?')
        new_pub   = updated.get('published', False)
        print(f'  Updated: type={new_type}, steps={new_steps}, published={bool(new_pub)}')
    except Exception as e:
        print(f'  UPDATE error: {e}')

print('\n\nFinal state of streak achievements:')
for aid, (name, steps) in STREAK_FIXES.items():
    try:
        a = svc.achievementConfigurations().get(achievementId=aid).execute()
        atype = a.get('achievementType', '?')
        asteps = a.get('stepsToUnlock', 'N/A')
        pub = bool(a.get('published', False))
        print(f'  [{("PUB" if pub else "DRAFT")}] [{atype}] steps={asteps} | {name}')
    except Exception as e:
        print(f'  {name}: error — {e}')

