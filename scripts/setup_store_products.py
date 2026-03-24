#!/usr/bin/env python3
"""
setup_store_products.py
=======================
Creates all missing in-app products and activates subscription base plans.

Products required:
  ONE-TIME (onetimeproducts API):
    coins_500, coins_1500, coins_5000
    diamonds_10, diamonds_50, diamonds_200
    lives_pack_5
    bundle_starter, bundle_adventurer, bundle_champion

  SUBSCRIPTIONS (already created, base plans need to be ACTIVE):
    vip_monthly  (base plan: monthly)
    vip_yearly   (base plan: yearly)

Prices (USD):
  coins_500      = $0.99    coins_1500     = $2.49    coins_5000     = $6.99
  diamonds_10    = $0.99    diamonds_50    = $3.99    diamonds_200   = $12.99
  lives_pack_5   = $0.99
  bundle_starter = $2.99    bundle_adventurer = $7.99  bundle_champion = $19.99
  vip_monthly    = $4.99/mo  vip_yearly     = $39.99/yr
"""
import sys
from google.oauth2 import service_account
from googleapiclient.discovery import build

creds = service_account.Credentials.from_service_account_file(
    "word-journey-488202-e738649def06.json",
    scopes=["https://www.googleapis.com/auth/androidpublisher"]
)
svc = build("androidpublisher", "v3", credentials=creds, cache_discovery=False)
pkg = "com.djtaylor.wordjourney"

# ── 1. List existing onetimeproducts ─────────────────────────────────────────
print("=== Existing onetimeproducts ===")
existing_otps = set()
try:
    resp = svc.monetization().onetimeproducts().list(packageName=pkg).execute()
    print(f"  raw keys: {list(resp.keys())}")
    # May return 'inappproduct' or 'onetimeproduct' key
    products = (resp.get("onetimeproduct")
             or resp.get("inappproduct")
             or resp.get("products")
             or [])
    for p in products:
        sku = p.get("sku") or p.get("productId") or "?"
        status = p.get("status", "?")
        print(f"  {sku:30s}  status={status}")
        existing_otps.add(sku)
    if not existing_otps:
        print("  (none found — will create all)")
except Exception as e:
    print(f"  Error listing onetimeproducts: {e}")

# ── 2. Subscription base plan state ──────────────────────────────────────────
print("\n=== Subscription base plan states ===")
for sub_id, bp_id in [("vip_monthly", "monthly"), ("vip_yearly", "yearly")]:
    try:
        resp = svc.monetization().subscriptions().get(
            packageName=pkg, productId=sub_id
        ).execute()
        for bp in resp.get("basePlans", []):
            if bp.get("basePlanId") == bp_id:
                print(f"  {sub_id}/{bp_id}  state={bp.get('state','?')}")
    except Exception as e:
        print(f"  {sub_id}: {e}")

# ── 3. Activate DRAFT subscription base plans ────────────────────────────────
print("\n=== Activating DRAFT subscription base plans ===")
for sub_id, bp_id in [("vip_monthly", "monthly"), ("vip_yearly", "yearly")]:
    try:
        svc.monetization().subscriptions().basePlans().activate(
            packageName=pkg, productId=sub_id, basePlanId=bp_id, body={}
        ).execute()
        print(f"  {sub_id}/{bp_id} → ACTIVATED ✓")
    except Exception as e:
        print(f"  {sub_id}/{bp_id} activate error: {e}")

# ── 4. Create missing onetimeproducts ────────────────────────────────────────
ONE_TIME_PRODUCTS = {
    "coins_500":          ("500 Coins",           "500 coin pack",           "0.99"),
    "coins_1500":         ("1500 Coins",          "1500 coin pack",          "2.49"),
    "coins_5000":         ("5000 Coins",          "5000 coin pack",          "6.99"),
    "diamonds_10":        ("10 Diamonds",         "10 diamond pack",         "0.99"),
    "diamonds_50":        ("50 Diamonds",         "50 diamond pack",         "3.99"),
    "diamonds_200":       ("200 Diamonds",        "200 diamond pack",       "12.99"),
    "lives_pack_5":       ("5 Lives Pack",        "5 extra lives",           "0.99"),
    "bundle_starter":     ("Starter Bundle",      "1000 coins + 5 diamonds + 5 of each item", "2.99"),
    "bundle_adventurer":  ("Adventurer Bundle",   "3000 coins + 20 diamonds + 10 lives", "7.99"),
    "bundle_champion":    ("Champion Bundle",     "10000 coins + 100 diamonds + 25 lives", "19.99"),
}

def price_micros(price_str):
    """Convert '4.99' → '4990000' (micros without the trailing 6 zeros issue)."""
    # priceMicros is price in millionths: $0.99 = 990000
    dollars, cents = price_str.split(".")
    cents_padded = cents.ljust(2, "0")[:2]
    return str((int(dollars) * 100 + int(cents_padded)) * 10000)


def otp_body(product_id, title, description, price_str):
    """Build an onetimeproduct body with USA price."""
    return {
        "packageName": pkg,
        "productId": product_id,
        "taxAndComplianceSettings": {
            "taxRateInfoByRegionCode": {
                "US": {"taxTier": "TAX_TIER_BOOKS_1"}
            },
            "isTokenizedDigitalAsset": False
        },
        "listings": {
            "en-US": {
                "title": title,
                "description": description,
                "benefits": []
            }
        }
    }


def price_body(price_str):
    """Return a ListingPrice body."""
    return {
        "oneTimeCode": {
            "regionVersionMapping": {
                "US": {
                    "price": {
                        "currencyCode": "USD",
                        "units": price_str.split(".")[0],
                        "nanos": int(price_str.split(".")[1].ljust(9, "0"))
                    }
                }
            }
        }
    }


print("\n=== Creating/activating one-time products via batchUpdate ===")
ok = 0
fail = 0

# Build request items for all missing products
requests_list = []
for product_id, (title, description, price) in ONE_TIME_PRODUCTS.items():
    if product_id in existing_otps:
        print(f"  {product_id:30s} → already exists (skipping)")
        ok += 1
        continue

    dollars, cents = price.split(".")
    nanos = int(cents.ljust(9, "0"))

    product_body = {
        "packageName": pkg,
        "productId":   product_id,
        "status":      "ACTIVE",
        "taxAndComplianceSettings": {
            "isTokenizedDigitalAsset": False
        },
        "listings": {
            "en-US": {
                "title":       title,
                "description": description,
                "benefits":    []
            }
        }
    }
    requests_list.append({
        "product": product_body,
        "updateMask": "listings,taxAndComplianceSettings,status"
    })

if not requests_list:
    print("  Nothing to create.")
else:
    try:
        batch_body = {
            "requests": [
                {
                    "oneTimeProduct": {
                        "packageName": pkg,
                        "productId":   r["product"]["productId"],
                        "listings": [
                            {
                                "languageCode": "en-US",
                                "title":        r["product"]["listings"]["en-US"]["title"],
                                "description":  r["product"]["listings"]["en-US"]["description"]
                            }
                        ],
                    },
                    "regionsVersion": {"version": "2022/02"},
                    "updateMask": "listings",
                    "allowMissing": True,
                    "latencyTolerance": "PRODUCT_UPDATE_LATENCY_TOLERANCE_LATENCY_TOLERANT"
                }
                for r in requests_list
            ]
        }
        result = svc.monetization().onetimeproducts().batchUpdate(
            packageName=pkg,
            body=batch_body
        ).execute()
        updated = result.get("onetimeproducts", []) or result.get("results", []) or []
        print(f"  batchUpdate returned {len(updated)} product(s)")
        for p in updated:
            pid = (p.get("product") or p).get("productId", "?")
            status = (p.get("product") or p).get("status", "?")
            print(f"  {pid:30s} → {status}")
        ok += len(requests_list)
    except Exception as e:
        print(f"  batchUpdate failed: {e}")
        fail += len(requests_list)

print(f"\nDone. OK={ok}  Failed={fail}")
