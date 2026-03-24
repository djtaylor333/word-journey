#!/usr/bin/env python3
"""Check in-app products via the new monetization API and subscription details."""
import json
from google.oauth2 import service_account
from googleapiclient.discovery import build

creds = service_account.Credentials.from_service_account_file(
    "word-journey-488202-e738649def06.json",
    scopes=["https://www.googleapis.com/auth/androidpublisher"]
)
svc = build("androidpublisher", "v3", credentials=creds, cache_discovery=False)
pkg = "com.djtaylor.wordjourney"

# In-app products via monetization API
print("=== IN-APP PRODUCTS (monetization API) ===")
try:
    resp = svc.monetization().inappproducts().list(packageName=pkg).execute()
    products = resp.get("inappproduct", [])
    if not products:
        print("  (none configured)")
    for p in products:
        sku = p.get("sku", "?")
        status = p.get("status", "?")
        prices = list(p.get("prices", {}).keys())[:3]
        print(f"  {sku:30s}  status={status}  regions={prices}")
except Exception as e:
    print("  Error:", e)

# Check subscription base plan details
print("\n=== SUBSCRIPTION BASE PLAN DETAILS ===")
expected_subs = ["vip_monthly", "vip_yearly"]
for sub_id in expected_subs:
    print(f"\n  {sub_id}:")
    try:
        resp = svc.monetization().subscriptions().get(
            packageName=pkg,
            productId=sub_id
        ).execute()
        for bp in resp.get("basePlans", []):
            bp_id = bp.get("basePlanId", "?")
            state = bp.get("state", "?")
            offers = bp.get("offerTags", [])
            print(f"    basePlan={bp_id}  state={state}  offers={offers}")
            # Prices
            regional = bp.get("regionalConfigs", [])
            for rc in regional[:3]:
                print(f"      region={rc.get('regionCode')}  price={rc.get('price',{}).get('units','?')}.{rc.get('price',{}).get('nanos','?')[:2] if rc.get('price',{}).get('nanos') else '00'}")
    except Exception as e:
        print(f"    Error: {e}")
