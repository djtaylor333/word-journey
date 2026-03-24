#!/usr/bin/env python3
"""
set_product_prices.py
=====================
Sets prices for all one-time products and subscriptions across all
available regions using Play Console's convertRegionPrices API.

Workflow:
  1. Call convertRegionPrices(usd_price) to get all regional currencies
  2. batchUpdate all one-time products with full regional pricing
  3. Patch subscription base plans with regional pricing
"""
import sys, json, time
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

# ── Product prices in USD ────────────────────────────────────────────────────
PRODUCT_PRICES_USD = {
    "coin_pack_500":     "0.99",
    "coins_1500":        "2.49",
    "coins_5000":        "6.99",
    "diamonds_10":       "0.99",
    "diamonds_50":       "3.99",
    "diamonds_200":     "12.99",
    "lives_pack_5":      "0.99",
    "bundle_starter":    "2.99",
    "bundle_adventurer": "7.99",
    "bundle_champion":  "19.99",
}

SUB_PRICES_USD = {
    "vip_monthly": ("monthly",  "4.99"),
    "vip_yearly":  ("yearly",  "39.99"),
}

def usd_money(price_str: str) -> dict:
    """Convert '4.99' → Money object."""
    dollars, cents = price_str.split(".")
    nanos = int(cents.ljust(9, "0"))
    return {"currencyCode": "USD", "units": str(int(dollars)), "nanos": nanos}

def convert_prices(price_str: str) -> list:
    """
    Call convertRegionPrices to get a list of
    {regionCode, price{currencyCode,units,nanos}} for all regions.
    """
    body = {"price": usd_money(price_str)}
    result = svc.monetization().convertRegionPrices(
        packageName=PKG, body=body
    ).execute()
    converted = result.get("convertedRegionPrices", {})
    regions = []
    for region_code, info in converted.items():
        price = info.get("price", {})
        regions.append({
            "regionCode": region_code,
            "price": price,
            "availability": "AVAILABLE"
        })
    return regions

# ── 1. Convert prices and build update requests for all one-time products ────
print(f"Converting regional prices for {len(PRODUCT_PRICES_USD)} one-time products...")
otp_requests = []
for product_id, usd_price in PRODUCT_PRICES_USD.items():
    print(f"  {product_id:30s} ${usd_price} → ", end="")
    configs = convert_prices(usd_price)
    print(f"{len(configs)} regions")
    otp_requests.append({
        "oneTimeProduct": {
            "packageName": PKG,
            "productId": product_id,
            "purchaseOptions": [
                {
                    "purchaseOptionId": "default",
                    "buyOption": {"legacyCompatible": True},
                    "regionalPricingAndAvailabilityConfigs": configs
                }
            ]
        },
        "regionsVersion": {"version": "2025/03"},
        "updateMask": "purchaseOptions",
        "allowMissing": False,
        "latencyTolerance": "PRODUCT_UPDATE_LATENCY_TOLERANCE_LATENCY_TOLERANT"
    })
    time.sleep(0.1)   # avoid API rate limits

# ── 2. batchUpdate all one-time products ─────────────────────────────────────
print(f"\nApplying prices to {len(otp_requests)} one-time products...")
# API limit is 100 per batch; we have 10, so one batch is fine
result = svc.monetization().onetimeproducts().batchUpdate(
    packageName=PKG,
    body={"requests": otp_requests}
).execute()
updated = result.get("oneTimeProducts", [])
print(f"  batchUpdate returned {len(updated)} products")
for p in updated:
    pid = p.get("productId", "?")
    opts = p.get("purchaseOptions", [])
    state = opts[0].get("state", "?") if opts else "?"
    n_regions = len(opts[0].get("regionalPricingAndAvailabilityConfigs", [])) if opts else 0
    print(f"  {pid:30s}  purchaseOption={state}  regions={n_regions}")

# ── 3. Set subscription prices ────────────────────────────────────────────────
print(f"\nSetting subscription prices...")
for sub_id, (bp_id, usd_price) in SUB_PRICES_USD.items():
    print(f"  {sub_id}/{bp_id}  ${usd_price} → ", end="")
    # For subscriptions, pricing is in basePlans[].regionalConfigs
    configs = convert_prices(usd_price)
    regional_configs = [
        {
            "regionCode": c["regionCode"],
            "price": c["price"],
            "newSubscriberAvailability": True
        }
        for c in configs
    ]
    print(f"{len(regional_configs)} regions → ", end="")
    # Get current subscription to preserve base plan structure
    sub = svc.monetization().subscriptions().get(packageName=PKG, productId=sub_id).execute()
    base_plans = sub.get("basePlans", [])
    for bp in base_plans:
        if bp.get("basePlanId") == bp_id:
            bp["regionalConfigs"] = regional_configs
    sub["basePlans"] = base_plans
    # Patch the subscription — regionsVersion must go as query param via _execute_request
    from google.auth.transport.requests import AuthorizedSession
    import urllib.parse
    session = AuthorizedSession(creds)
    url = (
        "https://androidpublisher.googleapis.com/androidpublisher/v3/applications"
        f"/{PKG}/subscriptions/{sub_id}"
        "?updateMask=basePlans.regionalConfigs"
        "&regionsVersion.version=2025%2F03"
        "&latencyTolerance=PRODUCT_UPDATE_LATENCY_TOLERANCE_LATENCY_TOLERANT"
    )
    r = session.patch(url, json=sub, timeout=30)
    if r.status_code != 200:
        print(f"FAILED {r.status_code}: {r.text[:300]}")
        continue
    result = r.json()
    updated_bps = result.get("basePlans", [])
    for bp in updated_bps:
        if bp.get("basePlanId") == bp_id:
            n = len(bp.get("regionalConfigs", []))
            state = bp.get("state", "?")
            print(f"state={state}  regions={n}  OK")
    time.sleep(0.2)

print("\nDone!")
