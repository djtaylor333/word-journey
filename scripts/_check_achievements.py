#!/usr/bin/env python3
"""Diagnostic: show existing achievements and test creating a failing one."""
from google.oauth2 import service_account
from googleapiclient.discovery import build
import json, time

creds = service_account.Credentials.from_service_account_file(
    'word-journey-488202-e738649def06.json',
    scopes=['https://www.googleapis.com/auth/androidpublisher',
            'https://www.googleapis.com/auth/games']
)
cfg = build('gamesConfiguration', 'v1configuration', credentials=creds)

# Check what exists
existing = cfg.achievementConfigurations().list(applicationId='1097721266836').execute()
items = existing.get('items', [])
print(f'Currently {len(items)} achievements created:')
for item in items:
    print(f"  {item['id']}  type={item.get('achievementType')}  state={item.get('initialState')}")

print()

# Test threshold: try 100 and 50 to figure out remaining budget
for xp_val in [100, 50, 10, 5]:
    body_test = {
        'kind': 'gamesConfiguration#achievementConfiguration',
        'achievementType': 'STANDARD',
        'initialState': 'REVEALED',
        'draft': {
            'name': {'translations': [{'locale': 'en-US', 'value': f'Test {xp_val}xp'}]},
            'description': {'translations': [{'locale': 'en-US', 'value': 'test'}]},
            'pointValue': xp_val,
        },
    }
    try:
        result = cfg.achievementConfigurations().insert(applicationId='1097721266836', body=body_test).execute()
        print(f'  xp={xp_val} -> SUCCESS id={result.get("id")}')
        cfg.achievementConfigurations().delete(achievementId=result['id']).execute()
    except Exception as e:
        err = str(e)
        val = err[err.find('value ')+6:err.find('value ')+12] if 'value ' in err else '?'
        print(f'  xp={xp_val} -> FAILED (attempted value={val})')

# Also try VIP_SUBSCRIBER (standard)
body2 = {
    'kind': 'gamesConfiguration#achievementConfiguration',
    'achievementType': 'STANDARD',
    'initialState': 'REVEALED',
    'draft': {
        'name': {
            'kind': 'gamesConfiguration#localizedStringBundle',
            'translations': [{'kind': 'gamesConfiguration#localizedString',
                              'locale': 'en-US', 'value': 'VIP Member'}]
        },
        'description': {
            'kind': 'gamesConfiguration#localizedStringBundle',
            'translations': [{'kind': 'gamesConfiguration#localizedString',
                              'locale': 'en-US', 'value': 'Activate a VIP subscription.'}]
        },
        'pointValue': 500,
    },
}
try:
    result = cfg.achievementConfigurations().insert(applicationId='1097721266836', body=body2).execute()
    print('VIP_SUBSCRIBER SUCCESS:', result.get('id'))
    # Clean up
    cfg.achievementConfigurations().delete(achievementId=result['id']).execute()
    print('(cleaned up)')
except Exception as e:
    print('VIP_SUBSCRIBER FULL ERROR:', str(e))
