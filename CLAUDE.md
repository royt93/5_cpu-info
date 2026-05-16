# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Android app **CPU Info** (`com.galaxyjoy.cpuinfo`) — hiển thị thông tin phần cứng / phần mềm thiết bị. Đây là một fork "refactor-in-progress" từ project gốc của KG Soft, có thêm hệ thống quảng cáo và phân phối lên Play Store dưới namespace `galaxyjoy`.

## Build & test commands

Tất cả lệnh chạy ở repo root. Project có hai product flavors (`dev`, `production`) và hai build types (`debug`, `release`) → sinh ra các task variant như `assembleDevDebug`, `assembleProductionRelease`, v.v.

| Việc | Lệnh |
|------|------|
| Unit tests (CI sử dụng) | `./gradlew testDebugUnitTest` |
| Test một class | `./gradlew testDevDebugUnitTest --tests "com.galaxyjoy.cpuinfo.SomeTest"` |
| Lint | `./gradlew lintDevDebug` (đã set `abortOnError = false`) |
| Build APK debug | `./gradlew assembleDevDebug` |
| Build AAB release (cần ký) | `./gradlew bundleProductionRelease` |
| Install lên thiết bị | `./gradlew installDevDebug` |
| Clean | `./gradlew clean` |

Release signing đọc từ `gradle.properties` (`STORE_FILE`, `STORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`); keystore là `app/keystore.jks`.

## Toolchain (bị pin chặt — đừng tự nâng)

- AGP **8.7.3**, Kotlin **1.9.25**, Java target **11**, NDK **26.3.11579264**, `compileSdk=36`, `minSdk=24`.
- `build.gradle.kts` (root) `force()` các phiên bản: `kotlin-stdlib 1.9.20`, `kotlinx-coroutines 1.7.3`, `play-services-ads 23.6.0`. Sửa version ở `app/build.gradle.kts` mà không đụng tới block `resolutionStrategy.force(...)` sẽ bị override âm thầm.
- `kapt` đang dùng cho Hilt, Epoxy, Glide — KSP chưa bật.

## Cấu trúc cấp cao

### Gradle layout
- Một module ứng dụng duy nhất `:app`.
- `buildSrc/src/main/java/{Libs,Versions,DependencyUpdates,SigningConfig}.kt` — version catalog tự viết. Phần lớn entries trong `Libs.kt` đã bị comment out; dependency thật được khai báo trực tiếp ở `app/build.gradle.kts`. Khi thêm dependency mới, theo convention hiện tại là **hardcode version vào `app/build.gradle.kts`**, không thêm vào `Libs.kt`.
- `external/cpuinfo/` — prebuilt static lib `libcpuinfo.a` cho 4 ABI, được CMake link vào shared lib `cpuinfo-libs` (xem `app/src/main/cpp/CMakeLists.txt`). Native lib load qua **ReLinker** (`com.getkeepsafe.relinker`), không phải `System.loadLibrary` thuần.
- Linker flag `-Wl,-z,max-page-size=16384` đã bật để tương thích thiết bị 16KB page size (Android 15+).

### Package map (`com.galaxyjoy.cpuinfo`)
Source code đang ở giai đoạn refactor → tồn tại **song song hai cách tổ chức**:

- `feat/` — code mới hơn, mỗi feature 1 sub-package (`feat/infor/{cpu,gpu,ram,sensor,storage,screen,hardware,android}`, `feat/app`, `feat/processes`, `feat/temp`, `feat/setting`, `feat/cputile`, `feat/ramtile`, `feat/ramwidget`). `feat/ActHost.kt` là Activity chính có bottom nav, `feat/SplashActivity.kt` là entry điểm.
- `features/information/` — code cũ còn sót lại (chỉ còn một sub-package). Khi thêm feature mới đặt vào `feat/`, không tạo thêm dưới `features/`.
- `data/{provider,local}` — `DataProvider*` cho từng loại thông tin (CPU/GPU/RAM/Storage/Applications) + `RepositoryUserPreferences` (DataStore).
- `domain/{model,observable,action,result}` + `Interactor.kt` — kiến trúc đang chuyển sang Interactor / Observable pattern (TODO trong README), chưa hoàn tất.
- `di/modules/{AppModule,AppModuleBinds}.kt` — Hilt graph. App entry: `GalaxyApp.kt` (`@HiltAndroidApp`).
- `appinitializers/` — pattern: `InitializersApp` orchestrate một list các `AppInitializer` (Timber, Epoxy, Theme, Rx, NativeTools, RamWidget). Khi cần init thứ gì lúc app start, **thêm 1 `AppInitializer` mới**, đừng nhồi vào `GalaxyApp.onCreate`.
- `widget/` — custom view (arc, progress, swipe reveal). `widget/swiperv/` là port của SwipeRevealLayout, code nguyên trạng — không phải nơi nên refactor.
- `ui/{component,theme}` — Compose components và theme (Material3, đang migrate dần).
- `sdkadbmob/` — wrapper cũ quanh AdMob, hiện không dùng (xem mục Ads bên dưới).
- `ext/` — extension functions cho Context/Activity. `ext/Applovin.kt` đã comment toàn bộ (legacy).

### Hệ thống quảng cáo
Chi tiết đầy đủ ở `doc/AD.MD`. Tóm tắt:
- Provider hiện tại: **AppLovin MAX** thông qua SDK wrapper `com.github.royt93:AdmobWrapper:1.1.1` (cài qua JitPack — repo đã được thêm vào `allprojects.repositories`). Flag `BuildConfig.IS_ENABLE_ADMOB = false` switch về AppLovin; bật lên là dùng AdMob ID trong cùng file `build.gradle.kts`.
- 3 touchpoint: App Open (`SplashActivity`), Banner (`ActHost` bottom — có lifecycle hooks `bannerResume/Pause/Destroy`), Interstitial (`FrmApplications` — nút Sort A/Z). Preload interstitial gọi trong `ActHost.onCreate`.
- Init flow trong `GalaxyApp.setupAd()`: `setConfig → earlyInit → AppLovinSdk.initializeSdk → AdManager.init`. Thứ tự này **bắt buộc** — SDK sẽ no-op hoặc crash nếu đảo.
- SDK tự throttle (min 60s giữa fullscreen, max 6/session, 5/ngày) → không cần thêm logic gating ở app layer.
- `getMyVipGAIDSet()` trong `GalaxyApp.kt` chứa danh sách VIP nhưng **chưa được truyền vào `AdSdkConfig`** — SDK hiện dùng list nội bộ trùng khớp. Nếu cần thêm GAID, phải sửa cả 2 chỗ hoặc wire qua config.

### Memory leak history
`doc/MEMORY_LEAK.MD` ghi lại 7 leak đã fix (sensor listener, broadcast receiver lifecycle, postDelayed, NavController listener, ReviewManager callback). Khi đụng vào các file đó hoặc thêm pattern tương tự (raw `Thread`, anonymous listener, `postDelayed`, receiver register), check lại doc này trước.

## CI

`.github/workflows/build_and_test.yml` — chạy `./gradlew testDebugUnitTest` trên JDK 17 cho mọi PR và push lên `master`. Branch hiện tại là `dev` (PR target → `master`).

## Notes

- Mặc dù `app/build.gradle.kts` còn nhiều khối `testImplementation` bị comment, **không xóa** — đó là chỗ đánh dấu để khôi phục khi bật lại test (theo README TODO).
- `compile_info.txt` / `compile_output.txt` ở root là build log debug, không phải source.
- `local.properties` chứa SDK path → không commit.
