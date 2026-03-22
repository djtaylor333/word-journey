#!/usr/bin/env python3
"""
setup_play_products.py
======================
Creates all Word Journeys in-app products + subscriptions in Google Play Console
via the Google Play Developer API.

SETUP (one-time):
  1. Go to https://play.google.com/console
  2. Setup → API access → Link to a Google Cloud project (or create one)
  3. In the Cloud project: IAM & Admin → Service Accounts → Create service account
     - Give it any name (e.g. "play-api-bot")
     - Download the JSON key file
  4. Back in Play Console: Setup → API access → Grant access to your new service account
     - Role: "Release manager" (or at minimum: "Manage store listing, pricing & distribution")
  5. Put the JSON key file path in SERVICE_ACCOUNT_KEY_PATH below (or pass as --key argument)
  6. Put your package name in PACKAGE_NAME below (or pass as --package argument)

RUN:
  pip install google-auth google-auth-httplib2 google-api-python-client
  python setup_play_products.py
"""

import argparse
import json
import sys
import time

# ── Configuration ──────────────────────────────────────────────────────────────
SERVICE_ACCOUNT_KEY_PATH = ""          # e.g. r"C:\keys\play-service-account.json"
PACKAGE_NAME             = "com.djtaylor.wordjourney"
# ───────────────────────────────────────────────────────────────────────────────

# ── In-app products (one-time consumable) ──────────────────────────────────────
IN_APP_PRODUCTS = [
    {
        "productId":      "coins_500",
        "purchaseType":   "managedUser",
        "defaultPrice":   {"priceMicros": "990000", "currency": "USD"},
        "listings": {"en-US": {
            "title":       "500 Coins",
            "description": "Add 500 coins to your wallet."
        }},
    },
    {
        "productId":      "coins_1500",
        "purchaseType":   "managedUser",
        "defaultPrice":   {"priceMicros": "2490000", "currency": "USD"},
        "listings": {"en-US": {
            "title":       "1,500 Coins",
            "description": "Add 1,500 coins to your wallet."
        }},
    },
    {
        "productId":      "coins_5000",
        "purchaseType":   "managedUser",
        "defaultPrice":   {"priceMicros": "6990000", "currency": "USD"},
        "listings": {"en-US": {
            "title":       "5,000 Coins",
            "description": "Add 5,000 coins to your wallet."
        }},
    },
    {
        "productId":      "diamonds_10",
        "purchaseType":   "managedUser",
        "defaultPrice":   {"priceMicros": "990000", "currency": "USD"},
        "listings": {"en-US": {
            "title":       "10 Diamonds",
            "description": "Add 10 diamonds to your wallet."
        }},
    },
    {
        "productId":      "diamonds_50",
        "purchaseType":   "managedUser",
        "defaultPrice":   {"priceMicros": "3990000", "currency": "USD"},
        "listings": {"en-US": {
            "title":       "50 Diamonds",
            "description": "Add 50 diamonds to your wallet."
        }},
    },
    {
        "productId":      "diamonds_200",
        "purchaseType":   "managedUser",
        "defaultPrice":   {"priceMicros": "12990000", "currency": "USD"},
        "listings": {"en-US": {
            "title":       "200 Diamonds",
            "description": "Add 200 diamonds to your wallet."
        }},
    },
    {
        "productId":      "lives_pack_5",
        "purchaseType":   "managedUser",
        "defaultPrice":   {"priceMicros": "990000", "currency": "USD"},
        "listings": {"en-US": {
            "title":       "5 Lives",
            "description": "Instantly add 5 lives."
        }},
    },
    {
        "productId":      "bundle_starter",
        "purchaseType":   "managedUser",
        "defaultPrice":   {"priceMicros": "2990000", "currency": "USD"},
        "listings": {"en-US": {
            "title":       "Starter Bundle",
            "description": "1,000 coins + 5 diamonds + 5 of each power-up item."
        }},
    },
    {
        "productId":      "bundle_adventurer",
        "purchaseType":   "managedUser",
        "defaultPrice":   {"priceMicros": "7990000", "currency": "USD"},
        "listings": {"en-US": {
            "title":       "Adventurer Bundle",
            "description": "3,000 coins + 20 diamonds + 10 lives + 10 of each power-up item."
        }},
    },
    {
        "productId":      "bundle_champion",
        "purchaseType":   "managedUser",
        "defaultPrice":   {"priceMicros": "19990000", "currency": "USD"},
        "listings": {"en-US": {
            "title":       "Champion Bundle",
            "description": "10,000 coins + 100 diamonds + 25 lives + 25 of each power-up item."
        }},
    },
]

# ── Subscriptions (Google Play Billing Library v7 format) ──────────────────────
# The v3 Subscriptions API uses a two-call approach:
#   1. Create the subscription shell (productId + listings)
#   2. Create the base plan (billing period + price) via basePlans.activate
SUBSCRIPTIONS = [
    {
        "productId": "vip_monthly",
        "listings": {"en-US": {
            "title":       "VIP Monthly",
            "description": "Unlock VIP adventure mode, exclusive themed packs, and more. Billed monthly.",
            "benefits":    ["VIP adventure levels", "Exclusive themed packs", "Monthly coin bonus"],
        }},
        "basePlan": {
            "basePlanId":      "monthly",
            "billingPeriod":   "P1M",         # ISO 8601: 1 month
            "priceMicros":     "4990000",
            "currency":        "USD",
            "recurrenceMode":  "INFINITE_RECURRING",
        },
    },
    {
        "productId": "vip_yearly",
        "listings": {"en-US": {
            "title":       "VIP Yearly",
            "description": "Unlock VIP adventure mode and all exclusive content. Best value — 2 months free!",
            "benefits":    ["VIP adventure levels", "Exclusive themed packs", "Annual coin bonus", "Save vs. monthly"],
        }},
        "basePlan": {
            "basePlanId":      "yearly",
            "billingPeriod":   "P1Y",         # ISO 8601: 1 year
            "priceMicros":     "39990000",
            "currency":        "USD",
            "recurrenceMode":  "INFINITE_RECURRING",
        },
    },
]


def build_service(key_path: str):
    """Build authenticated Google Play Developer API service."""
    from google.oauth2 import service_account
    from googleapiclient.discovery import build

    scopes = ["https://www.googleapis.com/auth/androidpublisher"]
    creds  = service_account.Credentials.from_service_account_file(key_path, scopes=scopes)
    return build("androidpublisher", "v3", credentials=creds)


def price_to_money(price_micros: str, currency: str) -> dict:
    """Convert priceMicros string to Money object (units + nanos)."""
    micros = int(price_micros)
    units = micros // 1_000_000
    nanos = (micros % 1_000_000) * 1000
    return {"currencyCode": currency, "units": str(units), "nanos": nanos}


def create_or_update_inapp(service, package: str, product: dict):
    """Create or update a one-time product via the new monetization API."""
    pid = product["productId"]
    price = price_to_money(product["defaultPrice"]["priceMicros"],
                           product["defaultPrice"]["currency"])
    # Convert old map-style listings to array format
    listings = [
        {"languageCode": lang, "title": info["title"], "description": info["description"]}
        for lang, info in product["listings"].items()
    ]
    body = {
        "packageName": package,
        "productId":   pid,
        "listings":    listings,
        "purchaseOptions": [
            {
                "purchaseOptionId": "default",
                "buyOption":        {"legacyCompatible": True},
                "regionalPricingAndAvailabilityConfigs": [
                    {
                        "regionCode":   "US",
                        "price":        price,
                        "availability": "AVAILABLE",
                    }
                ],
            }
        ],
    }
    try:
        service.monetization().onetimeproducts().patch(
            packageName=package,
            productId=pid,
            allowMissing=True,
            regionsVersion_version="2022/02",
            updateMask="listings,purchaseOptions",
            body=body,
        ).execute()
        print(f"  + Created/updated: {pid}")
    except Exception as e:
        print(f"  x Failed to create {pid}: {e}")


def create_or_update_subscription(service, package: str, sub: dict):
    """Create a subscription shell + base plan using the v2 Monetization API."""
    pid      = sub["productId"]
    listings = sub["listings"]
    bp       = sub["basePlan"]

    # Build localizedListings array expected by v3 monetization API
    localized = []
    for lang, info in listings.items():
        entry = {"languageCode": lang, "title": info["title"], "description": info["description"]}
        if "benefits" in info:
            entry["benefits"] = info["benefits"]
        localized.append(entry)

    sub_body = {
        "packageName":        package,
        "productId":          pid,
        "listings":           localized,
        "basePlans": [
            {
                "basePlanId":     bp["basePlanId"],
                "state":          "ACTIVE",
                "autoRenewingBasePlanType": {
                    "billingPeriodDuration": bp["billingPeriod"],
                    "prorationMode":         "SUBSCRIPTION_PRORATION_MODE_CHARGE_ON_NEXT_BILLING_DATE",
                },
                "regionalConfigs": [
                    {
                        "regionCode":  "US",
                        "newSubscriberAvailability": True,
                        "price": {
                            "currencyCode": bp["currency"],
                            "units":        str(int(bp["priceMicros"]) // 1_000_000),
                            "nanos":        (int(bp["priceMicros"]) % 1_000_000) * 1000,
                        },
                    }
                ],
            }
        ],
    }

    monetization = service.monetization()
    try:
        monetization.subscriptions().create(
            packageName=package,
            productId=pid,
            regionsVersion_version="2022/02",
            body=sub_body,
        ).execute()
        print(f"  + Created subscription: {pid} (base plan: {bp['basePlanId']})")
    except Exception as e:
        if "already exists" in str(e).lower() or "409" in str(e):
            try:
                monetization.subscriptions().patch(
                    packageName=package,
                    productId=pid,
                    regionsVersion_version="2022/02",
                    body=sub_body,
                    updateMask="listings,basePlans",
                ).execute()
                print(f"  ~ Updated subscription: {pid}")
            except Exception as e2:
                print(f"  x Failed to update subscription {pid}: {e2}")
        else:
            print(f"  x Failed to create subscription {pid}: {e}")


def main():
    parser = argparse.ArgumentParser(description="Create Word Journeys Play Store products")
    parser.add_argument("--key",     default=SERVICE_ACCOUNT_KEY_PATH,
                        help="Path to service account JSON key file")
    parser.add_argument("--package", default=PACKAGE_NAME,
                        help="App package name")
    parser.add_argument("--dry-run", action="store_true",
                        help="Print what would be created without calling the API")
    args = parser.parse_args()

    if not args.key:
        print("ERROR: No service account key provided.")
        print("  Set SERVICE_ACCOUNT_KEY_PATH at the top of this script, or use --key path/to/key.json")
        sys.exit(1)

    if args.dry_run:
        print("=== DRY RUN — no API calls will be made ===\n")
        print("In-app products that would be created:")
        for p in IN_APP_PRODUCTS:
            price = int(p["defaultPrice"]["priceMicros"]) / 1_000_000
            print(f"  {p['productId']:30s}  ${price:.2f}  {p['listings']['en-US']['title']}")
        print("\nSubscriptions that would be created:")
        for s in SUBSCRIPTIONS:
            price = int(s["basePlan"]["priceMicros"]) / 1_000_000
            print(f"  {s['productId']:30s}  ${price:.2f}  {s['listings']['en-US']['title']}")
        return

    try:
        from googleapiclient.discovery import build  # noqa: F401 — just verify installed
    except ImportError:
        print("Missing dependencies. Run:")
        print("  pip install google-auth google-auth-httplib2 google-api-python-client")
        sys.exit(1)

    print(f"Connecting to Google Play Developer API for package: {args.package}")
    service = build_service(args.key)

    print("\n── In-app products ───────────────────────────────────────────────")
    for product in IN_APP_PRODUCTS:
        create_or_update_inapp(service, args.package, product)
        time.sleep(0.3)   # be polite to the API

    print("\n── Subscriptions ──────────────────────────────────────────────────")
    for sub in SUBSCRIPTIONS:
        create_or_update_subscription(service, args.package, sub)
        time.sleep(0.3)

    print("\nDone! Check https://play.google.com/console to verify all products are Active.")


if __name__ == "__main__":
    main()
