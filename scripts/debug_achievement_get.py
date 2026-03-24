"""
Debug: fetch individual achievement via GET to see full response including token.
"""
import sys, json
sys.stdout.reconfigure(encoding='utf-8', errors='replace')
import warnings; warnings.filterwarnings('ignore')
from google.oauth2 import service_account
from googleapiclient.discovery import build

creds = service_account.Credentials.from_service_account_file(
    'word-journey-488202-e738649def06.json',
    scopes=[
        'https://www.googleapis.com/auth/games',
        'https://www.googleapis.com/auth/androidpublisher',
    ]
)
svc = build('gamesConfiguration', 'v1configuration', credentials=creds, cache_discovery=False)

# Try to GET a single achievement config
AID = 'CgkIlIWlqvkfEAIQBA'  # STREAK_3 / On a Roll
try:
    result = svc.achievementConfigurations().get(achievementId=AID).execute()
    print('GET response:')
    print(json.dumps(result, indent=2))
except Exception as e:
    print(f'GET error: {e}')
