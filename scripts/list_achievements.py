import sys
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

result = svc.achievementConfigurations().list(applicationId='1097721266836').execute()
items = result.get('items', [])
print(f'Total achievements: {len(items)}')
for a in items:
    aid = a.get('id', '?')
    published = a.get('published', False)
    atype = a.get('achievementType', '?')
    steps = a.get('stepsToUnlock', 'N/A')
    draft = a.get('draft', {})
    name_obj = draft.get('name', {})
    translations = name_obj.get('translations', [])
    name = translations[0].get('value', '?') if translations else '?'
    state = 'PUBLISHED' if published else 'DRAFT'
    print(f'  [{state}] [{atype}] steps={steps} | {aid} | {name}')
