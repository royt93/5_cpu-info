# 📊 AD SYSTEM — cpu-info
> **SDK:** `com.github.royt93:AdmobWrapper:1.1.1` | **Cập nhật:** 2026-03-23 | **Status:** ✅ Đã tích hợp

---

## MỤC LỤC
1. [Cấu hình Ad](#1-cấu-hình-ad)
2. [Touchpoint quảng cáo](#2-touchpoint-quảng-cáo)
3. [Cơ chế bảo vệ AdSafety (tự động)](#3-cơ-chế-bảo-vệ-adsafety-tự-động)
4. [Files đã thay đổi](#4-files-đã-thay-đổi)

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

```
SplashActivity
    └── [APP OPEN AD] ← tự động, SDK lo toàn bộ

ActHost (Main)
    ├── Bottom bar ──────────── [BANNER AD] ← auto loaded & lifecycle managed
    │
    └── FrmApplications (tab Apps)
            └── Nút Sort A/Z (action bar) ── [INTERSTITIAL AD] → sort chạy sau
```

### Chi tiết Interstitial — FrmApplications

```kotlin
R.id.menuActionSorting -> {
    AdManager.showInterstitial(requireActivity()) { _ ->
        viewModel.changeAppsSorting()   // sort luôn chạy dù có ad hay không
    }
    true
}
```
> SDK tự throttle: min 60s giữa 2 lần, max 6 lần/session.

### App Open Ad — Khi nào hiện
- **Auto hiện:** Splash khởi động (timeout 8s) & resume từ background
- **Auto SKIP:** Cold start, resume < 5s, session < 10s

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
| `AndroidManifest.xml` | Bỏ comment AppLovin SDK key meta-data |
| `GalaxyApp.kt` | Viết lại init dùng `AdManager` + AppLovin path |
| `SplashActivity.kt` | Đổi `AdMobManager` → `AdManager` |
| `ActHost.kt` | Banner dùng `AdManager.loadBanner` + lifecycle hooks + preload inter |
| `FrmRamInfo.kt` | Interstitial touchpoint tại nút RAM Clean |
| `sdkadbmob/AdMobManager.kt` | **ĐÃ XÓA** (thay thế bởi AdmobWrapper SDK) |
