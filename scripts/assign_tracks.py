import sys
sys.stdout.reconfigure(encoding='utf-8', errors='replace')
import warnings
warnings.filterwarnings('ignore')
from google.oauth2 import service_account
from googleapiclient.discovery import build

creds = service_account.Credentials.from_service_account_file(
    'word-journey-488202-e738649def06.json',
    scopes=['https://www.googleapis.com/auth/androidpublisher']
)
svc = build('androidpublisher', 'v3', credentials=creds, cache_discovery=False)
pkg = 'com.djtaylor.wordjourney'
vc = '87'
release_notes = 'v2.51.1 - 500 hardcoded VIP levels (levels 106-500 added, all validated)'

for track_name in ['alpha', 'beta', 'production']:
    edit = svc.edits().insert(packageName=pkg, body={}).execute()
    eid = edit['id']
    track_body = {
        'releases': [{
            'versionCodes': [vc],
            'status': 'completed',
            'releaseNotes': [{'language': 'en-US', 'text': release_notes}]
        }]
    }
    svc.edits().tracks().update(packageName=pkg, editId=eid, track=track_name, body=track_body).execute()
    svc.edits().commit(packageName=pkg, editId=eid).execute()
    print(f'Assigned versionCode {vc} to {track_name} track.')

print('All tracks updated.')
