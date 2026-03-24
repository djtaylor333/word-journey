import sys
sys.stdout.reconfigure(encoding='utf-8', errors='replace')
import warnings
warnings.filterwarnings('ignore')
from google.oauth2 import service_account
from googleapiclient.discovery import build
from googleapiclient.http import MediaFileUpload

creds = service_account.Credentials.from_service_account_file(
    'word-journey-488202-e738649def06.json',
    scopes=['https://www.googleapis.com/auth/androidpublisher']
)
svc = build('androidpublisher', 'v3', credentials=creds, cache_discovery=False)
pkg = 'com.djtaylor.wordjourney'

# Create edit
edit = svc.edits().insert(packageName=pkg, body={}).execute()
edit_id = edit['id']
print(f'Edit created: {edit_id}')

# Upload AAB
aab_path = 'app/build/outputs/bundle/release/word-journey-release.aab'
media = MediaFileUpload(aab_path, mimetype='application/octet-stream', resumable=False)
bundle = svc.edits().bundles().upload(packageName=pkg, editId=edit_id, media_body=media).execute()
vc = bundle.get('versionCode')
print(f'Uploaded AAB versionCode: {vc}')

# Assign to production track
release_notes = ('v2.30.3 \u2014 UI fixes & VIP improvements\n'
                 '\u2022 Fix: level number no longer flashes \u20181\u2019 before showing correct level\n'
                 '\u2022 Fix: future themed packs are now locked until their season begins\n'
                 '\u2022 VIP: 2\u00d7 rewards on themed pack milestones (every 10 levels)\n'
                 '\u2022 VIP: 2\u00d7 grand prize when completing all 100 levels of a themed pack\n'
                 '\u2022 VIP: 2\u00d7 diamonds for completing every 10-level stage')

track_body = {
    'releases': [{
        'versionCodes': [str(vc)],
        'status': 'completed',
        'releaseNotes': [{'language': 'en-US', 'text': release_notes}]
    }]
}
track = svc.edits().tracks().update(packageName=pkg, editId=edit_id, track='production', body=track_body).execute()
track_name = track['track']
print(f'Track updated: {track_name}')

# Commit
commit_result = svc.edits().commit(packageName=pkg, editId=edit_id).execute()
commit_id = commit_result['id']
print(f'Committed edit: {commit_id}')
print('SUCCESS: v2.30.2 deployed to production!')
