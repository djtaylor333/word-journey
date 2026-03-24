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

print('=== ONE-TIME PRODUCTS ===')
products = svc.monetization().inappProducts().list(packageName=pkg, pageSize=100).execute()
for p in products.get('inappProduct', []):
    print(f"  {p['productId']:30s} status={p.get('status','?')}")

print('=== SUBSCRIPTIONS ===')
subs = svc.monetization().subscriptions().list(packageName=pkg).execute()
for s in subs.get('subscriptions', []):
    pid = s['productId']
    for bp in s.get('basePlans', []):
        state = bp.get('state', '?')
        bpid = bp['basePlanId']
        print(f"  {pid}/{bpid:20s} state={state}")
