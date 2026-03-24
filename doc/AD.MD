# 📊 AD SYSTEM — cpu-info
> **SDK:** `com.github.royt93:AdmobWrapper:1.1.1` | **Cập nhật:** 2026-03-24 | **Status:** ✅ Đã tích hợp hoàn chỉnh

---

## MỤC LỤC
1. [Cấu hình Ad](#1-cấu-hình-ad)
2. [Touchpoint quảng cáo](#2-touchpoint-quảng-cáo)
3. [Cơ chế bảo vệ AdSafety (tự động)](#3-cơ-chế-bảo-vệ-adsafety-tự-động)
4. [Files đã thay đổi](#4-files-đã-thay-đổi)
5. [Ghi chú kỹ thuật](#5-ghi-chú-kỹ-thuật)

---

## 1. CẤU HÌNH AD

### Provider: AppLovin MAX (`IS_ENABLE_ADMOB = false`)

| Key | Value |
|-----|-------|
| AppLovin SDK Key | `e75FnQfS9XTTqM1Kne69U7PW_MBgAnGQTFvtwVVui6kRPKs5L7ws9twr5IQWwVfzPKZ5pF2IfDa7lguMgGlCyt` |
| Banner ID | `b568752d68ca93f8` |
| Interstitial ID | `5ce404d8a94fa941` |
| App Open ID | `8f218722ddc4ff48` |
| Reward ID | `e460250d026fafa6` (dự phòng, chưa dùng) |

### AdMob IDs (giữ lại để dễ switch provider sau này)

| Loại | Debug (test) | Release (production) |
|------|-------------|---------------------|
| Banner | `ca-app-pub-3940256099942544/6300978111` | `ca-app-pub-3612191981543807/6633406668` |
| Interstitial | `ca-app-pub-3940256099942544/1033173712` | `ca-app-pub-3612191981543807/5493932422` |
| App Open | `ca-app-pub-3940256099942544/9257395921` | `ca-app-pub-3612191981543807/5265851653` |
| AdMob App ID | — | `ca-app-pub-3612191981543807~1355135619` |

---

## 2. TOUCHPOINT QUẢNG CÁO

### Sơ đồ tổng quan

```
SplashActivity
    └── [APP OPEN AD] ← tự động, SDK lo toàn bộ (timeout 8s)

ActHost (Main)
    ├── Bottom bar ──────────── [BANNER AD] ← auto loaded & lifecycle managed
    │                            (onResume/onPause/onDestroy hooks)
    │
    └── Preload Interstitial ── loadInterstitial() gọi sẵn trong onCreate

FrmApplications (tab Apps trong ActHost)
    └── Nút Sort A/Z (action bar) ── [INTERSTITIAL AD] → sort chạy sau callback
```

### Touchpoint 1: App Open Ad — SplashActivity

| Thuộc tính | Giá trị |
|------------|---------|
| File | `SplashActivity.kt` |
| Cách gọi | `AdManager.initSplashScreen(this) { goToMain() }` |
| Auto hiện | Splash khởi động (timeout 8s) & resume từ background |
| Auto SKIP | Cold start, resume < 5s, session < 10s |

### Touchpoint 2: Banner Ad — ActHost

| Thuộc tính | Giá trị |
|------------|---------|
| File | `ActHost.kt` |
| Layout | `@layout/layout_ad_banner` (include trong `act_host_layout.xml`) |
| Cách gọi | `AdManager.loadBanner(context, container, tvLabelAd)` |
| Lifecycle | `bannerResume` / `bannerPause` / `bannerDestroy` — copy đúng 3 override |

### Touchpoint 3: Interstitial Ad — FrmApplications

| Thuộc tính | Giá trị |
|------------|---------|
| File | `FrmApplications.kt` |
| Trigger | Nút Sort A/Z trên action bar |
| Cách gọi | `AdManager.showInterstitial(requireActivity()) { _ -> viewModel.changeAppsSorting() }` |
| Preload | `AdManager.loadInterstitial(this)` gọi trong `ActHost.onCreate` |

> SDK tự throttle: min 60s giữa 2 lần, max 6 lần/session.

### Reward Ad
❌ Không dùng trong app này.

---

## 3. CƠ CHẾ BẢO VỆ ADSAFETY (TỰ ĐỘNG)

SDK tự xử lý, không cần code thêm:

| # | Filter | Giới hạn |
|---|--------|---------|
| 1 | Click bất thường | Phạt 30→60→120→240 phút |
| 2 | Session minimum | ≥ 10 giây trước khi thấy ad |
| 3 | Session Cap | Max 6 Fullscreen Ads/lần mở |
| 4 | Hourly Cap | Max 3 Fullscreen Ads/giờ |
| 5 | Daily Cap | Max 5 Fullscreen Ads/ngày |
| 6 | Throttle | Min 60s giữa 2 Fullscreen |
| 7 | CTR Anomaly | CTR > 10% → bị phạt |

---

## 4. FILES ĐÃ THAY ĐỔI

| File | Thay đổi |
|------|---------|
| `build.gradle.kts` (root) | Thêm JitPack repository |
| `app/build.gradle.kts` | Thêm AppLovin BuildConfig fields + `IS_ENABLE_ADMOB=false` + swap sang `AdmobWrapper:1.1.1` |
| `AndroidManifest.xml` | AdMob App ID + AppLovin SDK key meta-data |
| `GalaxyApp.kt` | Init dùng `AdManager.setConfig → earlyInit → AppLovinSdk.init → AdManager.init` |
| `SplashActivity.kt` | `AdManager.initSplashScreen` cho App Open Ad |
| `ActHost.kt` | Banner (`loadBanner` + lifecycle hooks) + preload interstitial |
| `FrmApplications.kt` | `showInterstitial` tại nút Sort A/Z |
| `Applovin.kt` | Toàn bộ code đã comment out (legacy, không dùng) |
| `sdkadbmob/AdMobManager.kt` | **ĐÃ XÓA** (thay thế bởi AdmobWrapper SDK) |

---

## 5. GHI CHÚ KỸ THUẬT

### ⚠️ VIP GAID Set chưa truyền vào SDK
`GalaxyApp.kt` có hàm `getMyVipGAIDSet()` chứa 21 GAID nhưng **chưa truyền vào `AdSdkConfig`**.
SDK dùng list VIP nội bộ hardcoded (trùng với list trong project), nên hiện tại vẫn hoạt động đúng.

### Luồng khởi tạo
```
GalaxyApp.onCreate()
  → setupAd()
    → AdManager.setConfig(adConfig)       // 1. Gắn config (Main Thread)
    → AdManager.earlyInit(this)           // 2. AdSafety session clock
    → AppLovinSdk.initializeSdk()         // 3. AppLovin SDK init
      → AdManager.init(this, adConfig)    // 4. GAID fetch + VIP check + auto-preload App Open
```
