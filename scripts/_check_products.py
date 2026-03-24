#!/usr/bin/env python3
"""Check what in-app products and subscriptions exist in Play Console."""
from google.oauth2 import service_account
from googleapiclient.discovery import build

creds = service_account.Credentials.from_service_account_file(
    "word-journey-488202-e738649def06.json",
    scopes=["https://www.googleapis.com/auth/androidpublisher"]
)
svc = build("androidpublisher", "v3", credentials=creds, cache_discovery=False)
pkg = "com.djtaylor.wordjourney"

# In-app products
try:
    resp = svc.inappproducts().list(packageName=pkg).execute()
    products = resp.get("inappproduct", [])
    print(f"=== IN-APP PRODUCTS ({len(products)}) ===")
    for p in products:
        sku = p.get("sku", "?")
        status = p.get("status", "?")
        prices = list(p.get("prices", {}).keys())[:3]
        print(f"  {sku:30s}  status={status}  regions={prices}")
    if not products:
        print("  (none configured)")
except Exception as e:
    print("inappproducts error:", e)

# Subscriptions
try:
    resp2 = svc.monetization().subscriptions().list(packageName=pkg).execute()
    subs = resp2.get("subscriptions", [])
    print(f"\n=== SUBSCRIPTIONS ({len(subs)}) ===")
    for s in subs:
        pid = s.get("productId", "?")
        archived = s.get("archived", False)
        plans = []
        for bp in s.get("basePlans", []):
            plans.append(f"{bp.get('basePlanId','?')}/{bp.get('state','?')}")
        print(f"  {pid:30s}  archived={archived}  basePlans={plans}")
    if not subs:
        print("  (none configured)")
except Exception as e:
    print("subscriptions error:", e)
