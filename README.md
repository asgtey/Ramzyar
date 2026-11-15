# Ramzyar · رمزیار

**Stop hunting for OTP codes in your notifications.** Ramzyar is a small, fully offline Android app that reads an incoming one-time-password SMS, pulls out the code, copies it to the clipboard, and shows it in a slim floating bar — right on top of whatever app you are using.

[فارسی](README.fa.md) · English

[![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Platform](https://img.shields.io/badge/Android-7.0%2B%20(API%2024)-3DDC84?logo=android&logoColor=white)](#requirements)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![No network permission](https://img.shields.io/badge/network-NONE-success)](#privacy)
[![Release APK](https://github.com/asgtey/Ramzyar/actions/workflows/release.yml/badge.svg)](https://github.com/asgtey/Ramzyar/actions/workflows/release.yml)

<!--
  Add real screenshots here — reviewers and users both look for them:
  ![Overlay bar](docs/screenshots/overlay.png)
  ![Settings](docs/screenshots/settings.png)
-->

## The problem

On Android, an OTP arrives as an ordinary SMS. Everything the platform offers is either intrusive or manual:

- **Autofill suggestions** only appear for apps that opted in, and only for the focused field.
- **Notification popups** get covered by the keyboard exactly when you need to read them.
- **Copy-by-hand** means switching apps, opening Messages, finding the SMS, selecting the digits.
- Persian/Arabic OTP messages are worse: codes are often sent as **Persian digits** (`۴۸۲۹۱۳`), with wording that differs bank by bank (رمز پویا، کد تایید، کد فعال‌سازی…), so naive "first 6-digit number" parsers grab the wrong number.

Ramzyar solves this with a single, zero-network pipeline that stays out of your way.

## Features

- **On-device OTP extraction** — no server, no parser model, no account.
- **Slim floating bar** instead of a screen-blocking popup; drag it anywhere and the position is remembered.
- **Smart code selection** — normalizes Persian/Arabic-Indic digits to ASCII, finds every numeric sequence, and picks the one closest to the *last* OTP keyword in the message (so `رمز قبلی 9999 باطل شد. رمز جدید شما 12345` yields `12345`).
- **Automatic clipboard copy** on arrival, with tap-to-copy again on the bar.
- **Custom keywords** — add the wording your bank or service uses (`شناسه بازیابی`, `کد ورود`, …).
- **Blacklist words / numbers and a sender whitelist** — never react to marketing SMS or unknown senders.
- **Optional notification cleanup** — dismisses the SMS notification that carried the code (via Notification Listener access), so the tray does not fill with dead OTPs.
- **History of the last 30 codes** with sender and timestamp.
- **Auto-dismiss countdown** (10–600 s, default 110 s) drawn as a thin progress line.
- **Haptic feedback** on arrival, RTL-first UI, Vazirmatn font, light and dark theme.

## How it works

```
SMS arrives
   │
   ▼
SmsReceiver (BroadcastReceiver, goAsync)
   │  • app enabled?
   │  • sender in blacklist?  /  whitelist enabled and sender not allowed?
   ▼
CodeExtractor.extractSmartWithLabel(message, customKeywords, blacklistWords)
   │  • normalize ۰-۹ and ٠-٩ to 0-9
   │  • find numeric runs of 4–10 digits
   │  • keyword hits = built-in list + user keywords
   │  • anchor = last keyword hit, pick nearest run (runs after the anchor win)
   ▼
   ├─ copy to clipboard (optional)
   ├─ vibrate (optional)
   ├─ append to history (max 30)
   ├─ dismiss the matching SMS notification (optional)
   └─ start SmsOverlayService ──▶ floating bar + countdown ← tap to copy / drag / close
```

Everything happens in the receiver and a foreground-free `Service`; there is no background polling and no scheduled work.

## Privacy

- The app declares **no `INTERNET` permission** — check `app/src/main/AndroidManifest.xml`. It physically cannot send your SMS anywhere.
- SMS content is parsed in memory; only the extracted code, its label and the sender are stored locally (DataStore), capped at 30 entries and clearable from the UI.
- No analytics, no crash reporting SDK, no ads, no third-party network libraries.

## Requirements

- Android 7.0 (API 24) or newer, **arm64-v8a** device — the release build ships a single ARM64 APK.
- Permissions the app asks for, and why:
  | Permission | Needed for | Optional? |
  |---|---|---|
  | `RECEIVE_SMS` | reading incoming OTP SMS | required for the core feature |
  | `SYSTEM_ALERT_WINDOW` | the floating bar | required for the overlay |
  | `READ_SMS` | listing senders so you can pick them for the whitelist/blacklist | optional |
  | `VIBRATE` | haptic feedback | optional |
  | Notification Listener access | dismissing the SMS notification | optional |

## Install

1. Download the latest APK from [Releases](https://github.com/asgtey/Ramzyar/releases).
2. Install it (you may need to allow installation from unknown sources).
3. Open the app and grant the SMS and overlay permissions.

> The release workflow signs APKs with an ephemeral CI key unless a keystore is provided, so a newer build may need the previous one to be uninstalled first. See *Building* below if you want a stable upgrade path.

## Building from source

```bash
git clone https://github.com/asgtey/Ramzyar.git
cd Ramzyar

# unit tests for the OTP parser (JVM, no emulator needed)
./gradlew testDebugUnitTest

# debug build
./gradlew assembleDebug
```

Release builds are produced by [`.github/workflows/release.yml`](.github/workflows/release.yml) on every push to `master` and create a GitHub Release with an `arm64-v8a` APK. To sign with your own key, add these repository secrets:

| Secret | Value |
|---|---|
| `RELEASE_KEYSTORE_BASE64` | `base64 release.jks` |
| `RELEASE_STORE_PASSWORD` | keystore password |
| `RELEASE_KEY_ALIAS` | key alias |
| `RELEASE_KEY_PASSWORD` | key password |

For local release builds, copy `gradle.properties.example` values for the four `RELEASE_*` keys into `gradle.properties` (git-ignored) or export them as environment variables.

**Toolchain:** Kotlin 2.0.21, AGP 8.13.2, Gradle 8.13, compileSdk 36, minSdk 24, JDK 17+.

## Configuration

| Setting | Default | What it does |
|---|---|---|
| رمزیار فعال باشد | on | master switch for the whole pipeline |
| کپی خودکار پس از دریافت | on | copy the code to the clipboard immediately |
| لرزش هنگام رسیدن رمز | on | short vibration on detection |
| اعلان پیامک رمز خودکار بسته شود | on | dismiss the SMS notification (needs Notification Listener access) |
| مدت نمایش نوار | 110 s | overlay lifetime, 10–600 s |
| کلمات کلیدی سفارشی | empty | extra OTP keywords |
| لیست سیاه کلمات | empty | messages containing these words are ignored entirely |
| لیست سیاه شماره‌ها | empty | senders that are never processed |
| فعال‌سازی لیست سفید | off | process only the listed senders |
| تاریخچه رمزها | last 30 | clearable from the app |

## Troubleshooting

- **The bar does not appear** — check the overlay permission, and disable battery optimisation for the app on aggressive OEM ROMs (MIUI, EMUI, ColorOS).
- **A code is missed or the wrong number is picked** — add the exact wording to *custom keywords*; the parser anchors on the last keyword in the message.
- **The SMS notification is not dismissed** — grant *Notification access* in system settings; without it Android only lets the default SMS app change a message's read state.
- **Nothing happens at all** — make sure another OTP app (or a system "verification code" service) has not taken over SMS handling.

## Roadmap

- [ ] Extract `CodeExtractor` into a standalone, published library so other Android apps can rely on the parser.
- [ ] Unit-test corpus of real-world Persian/English OTP templates (banks, e-commerce, government services).
- [ ] Publish to F-Droid / IzzyOnDroid.
- [ ] Optional "code detected, fill into focused field" accessibility service.

## Contributing

Issues and pull requests are welcome — especially **real OTP message templates** that the parser gets wrong (redact the actual code). Please keep changes small and include a unit test for parser changes; details in [CONTRIBUTING.md](CONTRIBUTING.md).

## License

[MIT](LICENSE) © asgtey
