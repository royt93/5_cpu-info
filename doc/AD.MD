# AD migration — `AdmobApplovinWrapper 1.1.5`

> **Cập nhật 2026-08-28**: mục 7 dưới đây ghi "đã xoá field `adView`" đúng tại thời điểm migration, nhưng field này **đã được thêm lại sau đó** trong `ActHost.kt` — cần thiết để có thể `AdManager.bannerDestroy()` thủ công khi user activate VIP giữa session (lib không tự destroy khi VIP toggle vì view đã instantiated trước đó). Không phải leak — có comment giải thích tại chỗ khai báo field. Phần còn lại của tài liệu vẫn đúng lịch sử migration.

Lib reference: `/Users/loitran/AndroidStudioProjects/@mckimquyen/@playstore/@prodution/_AdmobWrapper`
Coordinate: `com.github.royt93:AdmobApplovinWrapper:1.1.5` (JitPack)
Spec gốc của template: `doc/AD_PROMPT_AOS.MD`

## Provider runtime

- **Active**: AppLovin MAX
- **Fallback**: AdMob (flip `BuildConfig.IS_ENABLE_ADMOB=true` để activate)
- **VIP feature**: built-in từ SDK v1.1.4 (Step 9 của template) — không re-implement trong app code.

## 1. Touchpoint table

| Slot          | File hiện tại                                       | Trigger                                               | API mới                                 | API cũ thay thế                                  |
|---------------|-----------------------------------------------------|-------------------------------------------------------|-----------------------------------------|--------------------------------------------------|
| Banner        | `feat/ActHost.kt`                                    | `onCreate` — auto pause/resume/destroy via lib lifecycle | `AdManager.loadBanner(...)`             | `AdManager.bannerResume/Pause/Destroy` (đã xoá) |
| Interstitial  | `feat/app/FrmApplications.kt`                        | Nút Sort A/Z (`menuActionSorting`)                    | `AdManager.showInterstitial(activity)`  | giữ nguyên signature, lib auto throttle          |
| App Open      | `feat/SplashActivity.kt` + `ProcessLifecycleOwner` | warmup ở splash + auto-show khi resume foreground     | `AdManager.awaitSplashComplete` (suspend) | `AdManager.initSplashScreen(...)` (đã xoá)      |
| Rewarded      | `feat/vip/FVipManagement.kt`                         | Nút "Watch ad → 3 days VIP"                           | `AdManager.showRewarded(activity) { earned -> }` | (chưa có trước migrate)                          |

## 2. Migration steps đã thực hiện

### Step 1 — Setup
- `gradle/libs.versions.toml`: bump `admobWrapper = "1.1.1" → "1.1.5"`, đổi artifact `AdmobWrapper → AdmobApplovinWrapper`.
- `app/build.gradle.kts`:
  - Thêm `buildConfigField("String", "PRIVACY_POLICY_URL", "...")` ở `defaultConfig`.
  - Thêm `buildConfigField("String", "ADMOB_REWARDED_ID", "...")` ở cả `debug` + `release` buildType. Test ID (`ca-app-pub-3940256099942544/5224354917`) dùng cho cả 2 — release ID prod chưa có (xem mục 9).

### Step 2 — App init + consent
- `GalaxyApp.setupAd()` (one-shot, lib 1.1.5):
  - `AdManager.setConfig(AdSdkConfig(... vipKeySecret = AdKeys.VIP_SECRET, safety = if (DEBUG) AdSafetyLimits.TEST else AdSafetyLimits.UTILITY))`
  - `AdManager.initialize(this) { success, gaid -> ... }`
- Bỏ `earlyInit` + `AppLovinSdk.initialize` thủ công — lib tự lo nội bộ.

### Step 3 — Splash + UMP consent
- `SplashActivity.onCreate`:
  - `AdManager.requestConsentInfoUpdate(this) { canRequestAds -> runSplashFlow() }`
  - `runSplashFlow()` chạy `lifecycleScope.launch { AdManager.awaitSplashComplete(this@SplashActivity); goToMain() }` (suspend).
- KHÔNG show App Open trên splash thuần — lib `awaitSplashComplete` warmup, App Open hiện khi user resume từ background (lib hook `ProcessLifecycleOwner`).

### Step 4 — Banner (auto-managed)
- `ActHost.onCreate`: chỉ gọi `AdManager.loadBanner(...)` 1 lần.
- Xoá hết `AdManager.bannerResume/Pause/Destroy` trong `onResume/onPause/onDestroy` — lib 1.1.5 hook `ActivityLifecycleCallbacks` tự lo.

### Step 5 — Interstitial
- `FrmApplications.kt`: signature `AdManager.showInterstitial(activity) { _ -> viewModel.changeAppsSorting() }` — tương thích trực tiếp, không sửa logic.
- `AdSafetyLimits.UTILITY` (release): `minTimeBetweenFullscreenAds=90s, maxFullscreenAdsPerSession=3, maxFullscreenAdsPerHour=2, maxFullscreenAdsPerDay=5`.
- `AdSafetyLimits.TEST` (debug): `minTimeBetweenFullscreenAds=5s, caps=~unlimited` — QC test nhanh.

### Step 6 — VIP screen (mandatory, mới)
- `feat/vip/FVipManagement.kt` — Fragment + 13 thành phần UI + 5 animation (pulse / slide-in / shimmer / count-up / confetti+haptic).
- `feat/vip/VipKeys.kt` — whitelist Base64-encoded keys (30d / 3d), `lookupDays(rawInput)`.
- `feat/vip/VipPrefs.kt` — persist `grantedAtMs` + `userRedeemedAtLeastOnce` (lib chưa expose `grantedAt`).
- `common/const/AdKeys.kt` — Base64-decoded VIP secret + privacy URL từ `BuildConfig`.
- Entry: toolbar menu `R.id.menuActionVip` (icon crown) → `navController.navigate(R.id.menuVip)`. BadgeDrawable show khi `AdManager.isVipByKeyActive()`.

### Step 7 — Multi-language
- Default `values/strings.xml` (English) — 29 string VIP mới.
- `values-vi/strings.xml`, `values-cs/strings.xml`, `values-de/strings.xml`, `values-pl/strings.xml` — full dịch.

## 3. Before / after comparison

| Aspect           | Trước (v1.1.1, custom init)                                    | Sau (v1.1.5, one-shot)                                        |
|------------------|---------------------------------------------------------------|---------------------------------------------------------------|
| Provider         | AppLovin MAX (manual init) + AdMob fallback                    | AppLovin MAX + AdMob fallback (cùng setup)                    |
| Architecture     | App-side: 4 lệnh init thủ công                                 | App-side: 2 lệnh (`setConfig` + `initialize`)                  |
| App Open         | Custom `AdManager.initSplashScreen(...)`                       | Lib `awaitSplashComplete` (suspend) + auto-resume foreground   |
| Consent (UMP)    | Không explicit ở app                                           | `requestConsentInfoUpdate` ở `SplashActivity.onCreate`         |
| Memory leak risk | Manual `bannerResume/Pause/Destroy` (3 LOC giữ ref `adView?`) | Auto-manage qua `ActivityLifecycleCallbacks` — 0 ref ở app    |
| Frequency cap    | Default lib                                                    | `UTILITY` preset (release) / `TEST` preset (debug)             |
| VIP              | Không có                                                        | Full VIP screen + 1-day grace built-in từ SDK                  |
| Rewarded         | Không dùng                                                      | Wire ở VIP screen "Watch ad → 3d"                              |
| Lifecycle        | App phải tự wire                                                | Lib hook `ProcessLifecycleOwner` + `ActivityLifecycleCallbacks` |

## 4. Files changed

- **Edit** (≈8): `gradle/libs.versions.toml`, `app/build.gradle.kts`, `app/src/main/java/.../GalaxyApp.kt`, `feat/SplashActivity.kt`, `feat/ActHost.kt`, `res/menu/menu_main.xml`, `res/navigation/nav_graph.xml`, `res/values/colors.xml`, 4 file `values{,-vi,-cs,-de,-pl}/strings.xml`.
- **Create** (≈14): `common/const/AdKeys.kt`, `feat/vip/{FVipManagement,VipKeys,VipPrefs}.kt`, `res/layout/f_vip_management.xml`, 7 drawable + 1 anim.
- **Delete** (0): lib mới drop-in replace lib cũ (cùng package `com.roy.sdkadbmob.*`).

## 5. Lifecycle requirement (Step 4 spec)

| Yêu cầu                                                    | Trạng thái                                                                                                  |
|------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------|
| App Open trên Splash: consent → warmup → navigate          | OK — `SplashActivity.onCreate` → `requestConsentInfoUpdate` → `awaitSplashComplete` → `goToMain`           |
| App Open khi resume từ background                          | OK — lib hook `ProcessLifecycleOwner` qua `registerAppOpenAdLifecycle()` (gọi nội bộ trong `initialize`)    |
| Banner gắn vào container có sẵn                            | OK — `binding.layoutAdBanner.bannerContainer` (act_host_layout.xml)                                         |
| Banner không gọi `pause/resume/destroy` thủ công           | OK — đã xoá (commit này)                                                                                     |
| Interstitial chỉ thay call site cũ                         | OK — giữ trigger nút Sort A/Z, signature tương thích                                                         |
| Rewarded chỉ load+show khi user bấm nút                    | OK — `FVipManagement.btnWatchAd.onClick`                                                                     |

## 6. Policy check (Step 7 — 5 rule)

| # | Rule                                                                | PASS/FAIL | Dẫn chứng                              |
|---|---------------------------------------------------------------------|-----------|----------------------------------------|
| 1 | Banner không che nội dung tương tác (dock bottom)                   | PASS      | `act_host_layout.xml:36-48`           |
| 2 | App Open không show trên splash thuần                               | PASS      | `SplashActivity.kt:32-48`             |
| 3 | Interstitial không show liên tiếp (frequency cap)                   | PASS      | `GalaxyApp.kt` `safety = UTILITY/TEST` |
| 4 | Rewarded chỉ trigger khi user bấm nút                               | PASS      | `FVipManagement.kt` `btnWatchAd.onClick` |
| 5 | Không tự click ad / không UI giả mạo close button                   | PASS      | layout không có overlay                |

## 7. Memory review (3 điểm)

| Điểm review                                          | Kết luận | Dẫn chứng                                                                                                     |
|------------------------------------------------------|----------|---------------------------------------------------------------------------------------------------------------|
| ActHost giữ reference banner sau `onDestroy`         | OK       | Xoá field `adView: View?` + 3 lifecycle hook thủ công — lib auto cleanup.                                     |
| Fragment VIP giữ reference rewarded callback         | OK       | Guard `if (!isAdded || _binding == null) return@showRewarded` trong `FVipManagement.kt`.                       |
| Application leak Activity qua `AdManager` callback   | OK       | `AdManager.initialize(this) { ... }` — `this` = Application, không phải Activity.                              |
| VIP screen: animator/timer/binding cleanup           | OK       | `onDestroyView` cancel all `CountDownTimer` + 5 animator + `_binding = null`.                                  |

## 8. Self-review checklist (Step 11)

- [x] Slot ad đã thay (grep `bannerResume/Pause/Destroy` còn 0 match ở app, `initSplashScreen` còn 0 match ở app)
- [x] Build `assembleDevDebug` PASS (29s, no warning từ migration code)
- [⚠️] Build `assembleProductionRelease` — `compileProductionReleaseKotlin` + `compileProductionReleaseJavaWithJavac` PASS, **nhưng `lintVitalAnalyzeProductionRelease` crash** do toolchain bug (`IncompatibleClassChangeError` trong `NonNullableMutableLiveDataDetector` — Kotlin 1.9.25 + AGP 8.7.3 + lifecycle-lint mismatch). **KHÔNG do code migration**. Workaround khi CI: `-x lintVitalAnalyzeProductionRelease`, hoặc thêm `lint { disable += "NullSafeMutableLiveData" }` vào `app/build.gradle.kts`.
- [x] 5 policy rule PASS
- [x] Consent đã wire ở `SplashActivity.onCreate`
- [x] VIP 1-day auto-trial dùng SDK built-in — không re-implement
- [x] VIP screen 13/13 components + 5/5 animation
- [x] File structure đúng: `FVipManagement.kt`, `VipKeys.kt`, `VipPrefs.kt`, layout `f_vip_management.xml`
- [x] VIP key Base64-encoded trong `AdKeys.kt` — grep plain key trong source `.kt` = 0 match (chỉ Base64 string)
- [x] Translations vi/cs/de/pl/en đầy đủ
- [x] Toolbar badge "VIP" theo `AdManager.isVipByKeyActive()`
- [x] Privacy Policy footer + URL từ `AdKeys.PRIVACY_POLICY_URL` (`BuildConfig`)
- [x] Bảng so sánh trước/sau viết ở §3
- [x] List info user cần cấp viết ở §9

## 9. Info user cần cấp

| Item                              | Trạng thái        | Ghi chú                                                                                          |
|-----------------------------------|-------------------|--------------------------------------------------------------------------------------------------|
| AdMob Rewarded ID prod (release)  | **CHƯA CÓ**        | Đang dùng test ID `ca-app-pub-3940256099942544/5224354917` cho cả debug+release. Test ID **không kiếm tiền** — thay khi có ID prod. |
| AppLovin Rewarded ID              | OK                | `APPLOVIN_REWARD_ID = "e460250d026fafa6"` (đã trong BuildConfig).                                |
| AppLovin SDK key                  | OK                | `APPLOVIN_SDK_KEY` trong `defaultConfig`.                                                         |
| Privacy Policy URL                | OK                | `https://loitp.notion.site/.../Privacy-Policy-...` — migrate sang `BuildConfig.PRIVACY_POLICY_URL`. |
| Test device ID (EEA test)         | Optional          | Để test UMP consent dialog ở EEA region. Add sau qua `AdManager.setTestDeviceIds(...)` nếu cần.    |
| Banner placement layout ID        | OK                | `R.id.layoutAdBanner.bannerContainer` (act_host_layout.xml).                                      |
