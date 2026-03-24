#!/usr/bin/env python3
"""Set prices on VIP subscription base plans."""
import sys, time
sys.stdout.reconfigure(encoding="utf-8", errors="replace")
import warnings; warnings.filterwarnings("ignore")
from google.oauth2 import service_account
from google.auth.transport.requests import AuthorizedSession
from googleapiclient.discovery import build

creds = service_account.Credentials.from_service_account_file(
    "word-journey-488202-e738649def06.json",
    scopes=["https://www.googleapis.com/auth/androidpublisher"]
)
svc = build("androidpublisher", "v3", credentials=creds, cache_discovery=False)
session = AuthorizedSession(creds)
PKG = "com.djtaylor.wordjourney"

def usd_money(price_str):
    d, c = price_str.split(".")
    return {"currencyCode": "USD", "units": str(int(d)), "nanos": int(c.ljust(9, "0"))}

def convert_prices(price_str):
    r = svc.monetization().convertRegionPrices(
        packageName=PKG, body={"price": usd_money(price_str)}
    ).execute()
    return [
        {"regionCode": k, "price": v.get("price", {}), "newSubscriberAvailability": True}
        for k, v in r.get("convertedRegionPrices", {}).items()
    ]

for sub_id, bp_id, usd_price in [
    ("vip_monthly", "monthly",  "4.99"),
    ("vip_yearly",  "yearly",  "39.99"),
]:
    configs = convert_prices(usd_price)
    print(f"{sub_id}/{bp_id}  ${usd_price}  {len(configs)} regions")
    sub = svc.monetization().subscriptions().get(packageName=PKG, productId=sub_id).execute()
    for bp in sub.get("basePlans", []):
        if bp.get("basePlanId") == bp_id:
            bp["regionalConfigs"] = configs
    url = (
        f"https://androidpublisher.googleapis.com/androidpublisher/v3/applications"
        f"/{PKG}/subscriptions/{sub_id}"
        "?updateMask=basePlans"
        "&regionsVersion.version=2025%2F03"
        "&latencyTolerance=PRODUCT_UPDATE_LATENCY_TOLERANCE_LATENCY_TOLERANT"
    )
    r = session.patch(url, json=sub, timeout=30)
    if r.status_code == 200:
        result = r.json()
        for bp in result.get("basePlans", []):
            if bp.get("basePlanId") == bp_id:
                state = bp.get("state", "?")
                n = len(bp.get("regionalConfigs", []))
                print(f"  OK  state={state}  regions={n}")
    else:
        print(f"  FAILED {r.status_code}: {r.text[:400]}")
    time.sleep(0.2)

print("Done!")
