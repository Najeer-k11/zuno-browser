# Zuno Browser: Play Store kit

Everything needed to fill in Google Play Console for the first (internal testing) release.

```
playstore/
├── listing/en-US/            title, short and full description (paste as-is)
├── graphics/
│   ├── icon-512.png          512×512 store icon (32-bit PNG)
│   └── feature-graphic-1024x500.png
├── screenshots/
│   ├── raw/                  plain device captures
│   └── phone/                framed, captioned store screenshots (upload these)
└── release/
    ├── zuno-0.1.1-internal.aab   signed app bundle (git-ignored)
    └── release-notes-0.1.1.txt
```

| Item | Value |
|---|---|
| Package name | `com.zuno.browser` |
| Version | 0.1.1 (versionCode 2) |
| Upload key | `~/keystores/zuno-release.jks`, alias `zuno`, SHA-256 `58:D2:42:18:80:AD:AA:B0:15:74:00:DE:E7:BA:6D:47:5E:91:3A:C4:A9:FA:A9:5F:F1:8F:3C:8E:17:EE:36:4F` |
| Target SDK | 36 · min SDK 26 (Android 8.0) |
| 16 KB page size | Supported (all native libs 0x4000-aligned) |
| Privacy policy URL | https://najeer-k11.github.io/zuno-browser/privacy/ |

---

## 1. Create the app

- App name: **Zuno Browser** · Default language: English (United States)
- App or game: **App** · Free
- Play App Signing: **enroll**. Google keeps the app signing key; your keystore above becomes the **upload key**. Back the keystore and its password up somewhere safe.

## 2. Internal testing release

1. Testing → Internal testing → Create release → upload `release/zuno-0.1.1-internal.aab`.
2. Release name: `0.1.1`. Release notes: `release/release-notes-0.1.1.txt`.
3. Testers: create an email list (up to 100) → share the opt-in link.

## 3. Store listing (Grow → Store presence → Main store listing)

| Field | Source |
|---|---|
| App name (30) | `listing/en-US/title.txt` |
| Short description (80) | `listing/en-US/short-description.txt` |
| Full description (4000) | `listing/en-US/full-description.txt` |
| App icon | `graphics/icon-512.png` |
| Feature graphic | `graphics/feature-graphic-1024x500.png` |
| Phone screenshots (2–8) | `screenshots/phone/*.png` |
| Category | **Communication** (where Play lists web browsers) |
| Tags | Browser, Privacy, Ad blocker |
| Contact email | kvenkatanajeer@gmail.com |

## 4. App content (Policy → App content)

**Privacy policy**: the GitHub Pages URL above.

**Ads**: *No, my app does not contain ads.* (Zuno shows no ads of its own; websites are third-party content.)

**App access**: *All functionality is available without special access* (no login).

**Content rating** (IARC questionnaire)
- Category: *All other app types*
- Users can access unrestricted web content through a browser: **Yes**. This is expected for browsers, and the rating will usually come out as **Mature 17+ / PEGI 18 / 18+**. That's normal for any browser.
- Violence, sex, language, gambling **inside the app itself**: No
- Users interact or exchange content with each other: No · Shares location: No · Digital purchases: No

**Target audience**: **18 and over** (keeps the app out of Families policy requirements, which an unrestricted browser can't meet).

**News app**: No · **COVID-19 apps**: No · **Government app**: No · **Financial features**: none · **Health**: none.

**Data safety**
- Does the app collect or share any of the required user data types? **No.**
  - Tabs, history and settings stay on the device and are never sent to the developer → not "collected" under Play's definition.
  - Requests to websites, the chosen search engine and Mozilla's update/safe-browsing services are part of the user's browsing, not data collection by the developer.
- Is all user data encrypted in transit? **Yes** (HTTPS for Mozilla services; HTTPS-only available for websites).
- Can users request their data be deleted? There is no account or server-side data. Users delete local data in Settings → Data or by uninstalling.

**Foreground service declaration** (asked because the manifest declares `FOREGROUND_SERVICE_MEDIA_PLAYBACK`)
- Type: **Media playback**
- Description: *"Keeps audio and video from web pages (for example music sites) playing when the user leaves the app or locks the screen, with play/pause controls in the notification and lock screen. Starts only when the user plays media; can be turned off in Settings → Browsing → Background play."*
- Video: record ~20 s showing: play a song on a site → press Home → the notification controls appear → pause from the notification. Upload it to YouTube (unlisted) and paste the link.

**Permissions to explain if asked**: `POST_NOTIFICATIONS` (media controls), `INTERNET`, `ACCESS_NETWORK_STATE`, `WAKE_LOCK` (playback).

## 5. Privacy policy on GitHub Pages

The site lives in `/docs` (`docs/index.html` and `docs/privacy/index.html`).

1. Push the repo to GitHub.
2. Repo → Settings → Pages → Source: *Deploy from a branch* → Branch `main`, folder **`/docs`** → Save.
3. Live at https://najeer-k11.github.io/zuno-browser/privacy/ (Pages enabled from `main` → `/docs`).

## 6. Before moving past internal testing

- New personal developer accounts must run a **closed test with at least 12 testers for 14 days** before production access.
- Re-check the listing copy against what's shipped whenever features change (the description only claims what the app does today).
