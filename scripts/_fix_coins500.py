#!/usr/bin/env python3
"""Check coins_500 and activate coin_pack_500."""
import sys, json
sys.stdout.reconfigure(encoding="utf-8", errors="replace")
import warnings; warnings.filterwarnings("ignore")

from google.oauth2 import service_account
from google.auth.transport.requests import AuthorizedSession
from googleapiclient.discovery import build

creds = service_account.Credentials.from_service_account_file(
    "word-journey-488202-e738649def06.json",
    scopes=["https://www.googleapis.com/auth/androidpublisher"]
)
session = AuthorizedSession(creds)
svc = build("androidpublisher", "v3", credentials=creds, cache_discovery=False)
PKG = "com.djtaylor.wordjourney"
BASE = "https://androidpublisher.googleapis.com/androidpublisher/v3/applications"

# 1. Try direct REST for coins_500
print("--- coins_500 direct REST GET ---")
r = session.get(f"{BASE}/{PKG}/oneTimeProducts/coins_500", timeout=15)
print(f"  status: {r.status_code}")
print(f"  body: {r.text[:600]}")

# 2. Check and activate coin_pack_500
print()
print("--- coin_pack_500 ---")
try:
    prod = svc.monetization().onetimeproducts().get(packageName=PKG, productId="coin_pack_500").execute()
    for opt in prod.get("purchaseOptions", []):
        opt_id = opt.get("purchaseOptionId", "default")
        state = opt.get("state", "?")
        print(f"  [{opt_id}] state={state}")
        if state != "ACTIVE":
            url = f"{BASE}/{PKG}/oneTimeProducts/coin_pack_500/purchaseOptions:batchUpdateStates"
            body = {"requests": [{"activatePurchaseOptionRequest": {
                "packageName": PKG, "productId": "coin_pack_500", "purchaseOptionId": opt_id
            }}]}
            r2 = session.post(url, json=body, timeout=15)
            new_state = (r2.json().get("oneTimeProducts", [{}])[0]
                         .get("purchaseOptions", [{}])[0].get("state", "?"))
            print(f"  activate -> {r2.status_code}: {new_state}")
except Exception as e:
    print(f"  error: {e}")

# 3. Try to activate coins_500 directly even if GET returns 404
print()
print("--- Trying to activate coins_500 directly ---")
url = f"{BASE}/{PKG}/oneTimeProducts/coins_500/purchaseOptions:batchUpdateStates"
body = {"requests": [{"activatePurchaseOptionRequest": {
    "packageName": PKG, "productId": "coins_500", "purchaseOptionId": "default"
}}]}
r3 = session.post(url, json=body, timeout=15)
print(f"  status: {r3.status_code}  body: {r3.text[:400]}")
