#!/usr/bin/env python3
"""Check pricing state of all one-time products and subscriptions in Play Console."""
import sys, json
sys.stdout.reconfigure(encoding="utf-8", errors="replace")
import warnings; warnings.filterwarnings("ignore")

from google.oauth2 import service_account
from googleapiclient.discovery import build

creds = service_account.Credentials.from_service_account_file(
    "word-journey-488202-e738649def06.json",
    scopes=["https://www.googleapis.com/auth/androidpublisher"]
)
svc = build("androidpublisher", "v3", credentials=creds, cache_discovery=False)
PKG = "com.djtaylor.wordjourney"

print("=== ONE-TIME PRODUCTS — price state ===")
resp = svc.monetization().onetimeproducts().list(packageName=PKG).execute()
for p in resp.get("oneTimeProducts", []):
    pid = p.get("productId", "?")
    opts = p.get("purchaseOptions", [])
    if not opts:
        print(f"  {pid:30s}  NO purchase options at all!")
        continue
    for opt in opts:
        opt_id  = opt.get("purchaseOptionId", "?")
        state   = opt.get("state", "?")
        configs = opt.get("regionalPricingAndAvailabilityConfigs", [])
        if not configs:
            print(f"  {pid:30s}  [{opt_id}] state={state}  *** NO PRICES ***")
        else:
            regions = ", ".join(
                f"{c['regionCode']}={c.get('price',{}).get('units','?')}.{str(c.get('price',{}).get('nanos',0))[:2]}"
                for c in configs[:4]
            )
            print(f"  {pid:30s}  [{opt_id}] state={state}  regions({len(configs)}): {regions}")

print()
print("=== SUBSCRIPTIONS — base plan price state ===")
for sub_id, bp_id in [("vip_monthly", "monthly"), ("vip_yearly", "yearly")]:
    resp2 = svc.monetization().subscriptions().get(packageName=PKG, productId=sub_id).execute()
    for bp in resp2.get("basePlans", []):
        if bp.get("basePlanId") == bp_id:
            state = bp.get("state", "?")
            configs = bp.get("regionalConfigs", [])
            if not configs:
                print(f"  {sub_id}/{bp_id}  state={state}  *** NO PRICES ***")
            else:
                regions = ", ".join(
                    f"{c['regionCode']}={c.get('price',{}).get('units','?')}"
                    for c in configs[:4]
                )
                print(f"  {sub_id}/{bp_id}  state={state}  regions({len(configs)}): {regions}")
