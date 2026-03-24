#!/usr/bin/env python3
"""Check monetization resource methods and activate subscription base plans."""
import json
from google.oauth2 import service_account
from googleapiclient.discovery import build

creds = service_account.Credentials.from_service_account_file(
    "word-journey-488202-e738649def06.json",
    scopes=["https://www.googleapis.com/auth/androidpublisher"]
)
svc = build("androidpublisher", "v3", credentials=creds, cache_discovery=False)
pkg = "com.djtaylor.wordjourney"

# What's available on monetization?
mon = svc.monetization()
print("monetization() attrs:", [a for a in dir(mon) if not a.startswith("_")])

# Try activating vip_monthly / vip_yearly base plans
print()
for sub_id, bp_id in [("vip_monthly", "monthly"), ("vip_yearly", "yearly")]:
    try:
        result = svc.monetization().subscriptions().basePlans().activate(
            packageName=pkg,
            productId=sub_id,
            basePlanId=bp_id,
            body={}
        ).execute()
        print(f"ACTIVATED {sub_id}/{bp_id}: state={result.get('state','?')}")
    except Exception as e:
        print(f"Failed to activate {sub_id}/{bp_id}: {e}")

# Try old inappproducts API (may work with different scopes)
print()
print("Trying inappproducts.list via top-level resource...")
try:
    resource_attrs = [a for a in dir(svc) if not a.startswith("_")]
    print("Top-level svc attrs:", resource_attrs)
except Exception as e:
    print("Error:", e)
