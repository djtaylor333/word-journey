#!/usr/bin/env python3
"""
activate_store_products.py
==========================
Activates purchase options for all one-time in-app products,
creates any missing products (coins_500), and verifies subscriptions
are in ACTIVE state.

Run after all products have been created in Play Console.
"""
import sys
import json
sys.stdout.reconfigure(encoding="utf-8", errors="replace")

from google.oauth2 import service_account
from google.auth.transport.requests import AuthorizedSession
from googleapiclient.discovery import build

KEY = "word-journey-488202-e738649def06.json"
PKG = "com.djtaylor.wordjourney"

creds = service_account.Credentials.from_service_account_file(
    KEY, scopes=["https://www.googleapis.com/auth/androidpublisher"]
)
session = AuthorizedSession(creds)
svc = build("androidpublisher", "v3", credentials=creds, cache_discovery=False)

BASE = "https://androidpublisher.googleapis.com/androidpublisher/v3/applications"

# ── 1. List existing one-time products ───────────────────────────────────────
print("=== Existing one-time products ===")
resp = svc.monetization().onetimeproducts().list(packageName=PKG).execute()
existing = {p["productId"]: p for p in resp.get("oneTimeProducts", [])}
print(f"  Found {len(existing)} products: {sorted(existing.keys())}")

# ── 2. Create missing coins_500 ───────────────────────────────────────────────
if "coins_500" not in existing:
    print("\n=== Creating missing coins_500 ===")
    try:
        create_body = {
            "requests": [
                {
                    "oneTimeProduct": {
                        "packageName": PKG,
                        "productId": "coins_500",
                        "listings": [
                            {
                                "languageCode": "en-US",
                                "title": "500 Coins",
                                "description": "Add 500 coins to your wallet."
                            }
                        ],
                    },
                    "regionsVersion": {"version": "2025/03"},
                    "updateMask": "listings",
                    "allowMissing": True,
                    "latencyTolerance": "PRODUCT_UPDATE_LATENCY_TOLERANCE_LATENCY_TOLERANT"
                }
            ]
        }
        r = svc.monetization().onetimeproducts().batchUpdate(
            packageName=PKG, body=create_body
        ).execute()
        print(f"  coins_500 created: {json.dumps(r, indent=2)[:300]}")
    except Exception as e:
        print(f"  coins_500 create failed: {e}")
else:
    print("\n=== coins_500 already exists ===")

# ── 3. Activate purchase options for ALL products ────────────────────────────
EXPECTED_PRODUCTS = [
    "coins_500", "coins_1500", "coins_5000",
    "diamonds_10", "diamonds_50", "diamonds_200",
    "lives_pack_5",
    "bundle_starter", "bundle_adventurer", "bundle_champion",
]

print("\n=== Activating purchase options ===")
ok = 0
fail = 0

# Re-fetch after potential creation
resp2 = svc.monetization().onetimeproducts().list(packageName=PKG).execute()
current_products = {p["productId"]: p for p in resp2.get("oneTimeProducts", [])}

for product_id in EXPECTED_PRODUCTS:
    if product_id not in current_products:
        print(f"  {product_id:30s} MISSING — skipping (create in Play Console)")
        fail += 1
        continue

    product = current_products[product_id]
    opts = product.get("purchaseOptions", [])

    if not opts:
        print(f"  {product_id:30s} no purchaseOptions — skipping")
        fail += 1
        continue

    for opt in opts:
        opt_id = opt.get("purchaseOptionId", "default")
        state = opt.get("state", "?")

        if state == "ACTIVE":
            print(f"  {product_id:30s} [{opt_id}] already ACTIVE")
            ok += 1
            continue

        # Activate via batchUpdateStates
        url = f"{BASE}/{PKG}/oneTimeProducts/{product_id}/purchaseOptions:batchUpdateStates"
        body = {
            "requests": [
                {
                    "activatePurchaseOptionRequest": {
                        "packageName": PKG,
                        "productId": product_id,
                        "purchaseOptionId": opt_id
                    }
                }
            ]
        }
        r = session.post(url, json=body, timeout=30)
        if r.status_code == 200:
            new_state = (r.json().get("oneTimeProducts", [{}])[0]
                         .get("purchaseOptions", [{}])[0]
                         .get("state", "?"))
            print(f"  {product_id:30s} [{opt_id}] {state} -> {new_state} OK")
            ok += 1
        else:
            print(f"  {product_id:30s} [{opt_id}] FAILED {r.status_code}: {r.text[:150]}")
            fail += 1

# ── 4. Verify subscriptions ───────────────────────────────────────────────────
print("\n=== Subscription base plan states ===")
for sub_id, bp_id in [("vip_monthly", "monthly"), ("vip_yearly", "yearly")]:
    try:
        resp = svc.monetization().subscriptions().get(packageName=PKG, productId=sub_id).execute()
        for bp in resp.get("basePlans", []):
            if bp.get("basePlanId") == bp_id:
                state = bp.get("state", "?")
                if state != "ACTIVE":
                    # Try activating again
                    svc.monetization().subscriptions().basePlans().activate(
                        packageName=PKG, productId=sub_id, basePlanId=bp_id, body={}
                    ).execute()
                    print(f"  {sub_id}/{bp_id}  was {state} -> ACTIVATED")
                else:
                    print(f"  {sub_id}/{bp_id}  state=ACTIVE OK")
    except Exception as e:
        print(f"  {sub_id}: {e}")

print(f"\n=== Done. Active: {ok}  Problems: {fail} ===")
