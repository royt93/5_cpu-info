# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Android app **CPU Info** (`com.galaxyjoy.cpuinfo`) — hiển thị thông tin phần cứng / phần mềm thiết bị. Đây là một fork "refactor-in-progress" từ project gốc của KG Soft, có thêm hệ thống quảng cáo và phân phối lên Play Store dưới namespace `galaxyjoy`.

## Build & test commands

Tất cả lệnh chạy ở repo root. Project có hai product flavors (`dev`, `production`) và hai build types (`debug`, `release`) → sinh ra các task variant như `assembleDevDebug`, `assembleProductionRelease`, v.v.

| Việc | Lệnh |
|------|------|
| Unit tests (CI sử dụng) | `./gradlew testDebugUnitTest` |
| Test variant cụ thể | `./gradlew :app:testDevDebugUnitTest` |
| Test một class | `./gradlew :app:testDevDebugUnitTest --tests "com.galaxyjoy.cpuinfo.data.provider.DataProviderRamTest"` |
| Lint | `./gradlew lintDevDebug` (đã set `abortOnError = false`) |
| Build APK debug | `./gradlew assembleDevDebug` |
| Build AAB release (cần ký) | `./gradlew bundleProductionRelease` |
| Install lên thiết bị | `./gradlew installDevDebug` |
| Clean | `./gradlew clean` |

Release signing đọc từ `gradle.properties` (`STORE_FILE`, `STORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`); keystore là `app/keystore.jks`.

## Toolchain (bị pin chặt — đừng tự nâng)

- AGP **8.7.3**, Kotlin **1.9.25**, Java target **11**, NDK **26.3.11579264**, `compileSdk=37`, `minSdk=24`.
- `build.gradle.kts` (root) `force()` các phiên bản: `kotlin-stdlib 1.9.25`, `kotlinx-coroutines 1.9.0`, `play-services-ads 23.6.0`. **Đừng xoá block force này** — Compose BOM và transitive deps kéo artifact compiled với Kotlin 2.x mà compiler 1.9.25 không đọc được metadata (max 2.0.0). Nếu bump Kotlin/coroutines phải bump force tương ứng.
- **KSP** dùng cho Hilt + Glide (`com.google.devtools.ksp:1.9.25-1.0.20`). **kapt** vẫn dùng cho Epoxy (5.1.3 chưa support KSP). Mọi annotation processor mới ưu tiên KSP.
- `GlideApp` class không được generate (Glide KSP 4.16.0 chỉ tạo `GeneratedAppGlideModuleImpl`). Dùng `Glide.with()` trực tiếp; `GlideAppModule` hiện rỗng (chỉ đăng ký với Glide qua `@GlideModule`).

## Cấu trúc cấp cao

### Gradle layout
- Một module ứng dụng duy nhất `:app`.
- **Version catalog**: `gradle/libs.versions.toml` là single source of truth cho mọi version + library + plugin. Khi thêm dependency mới, thêm vào TOML rồi reference qua `libs.xxx` ở `app/build.gradle.kts`.
- `buildSrc/src/main/java/{DependencyUpdates,SigningConfig}.kt` — code helper còn lại sau khi đã migrate sang TOML. `DependencyUpdates.kt` được dùng bởi task `:dependencyUpdates` (filter pre-release versions). `SigningConfig.kt` không được dùng active (signing đọc trực tiếp từ `gradle.properties`).
- `settings.gradle.kts` có `pluginManagement { repositories { ... } }` cho `alias(libs.plugins.xxx)` và `dependencyResolutionManagement.repositoriesMode = PREFER_PROJECT` để cho phép `allprojects { repositories { ... } }` ở root.
- `external/cpuinfo/` — prebuilt static lib `libcpuinfo.a` cho 4 ABI, được CMake link vào shared lib `cpuinfo-libs` (xem `app/src/main/cpp/CMakeLists.txt`). Native lib load qua **ReLinker** (`com.getkeepsafe.relinker`), không phải `System.loadLibrary` thuần.
- Linker flag `-Wl,-z,max-page-size=16384` đã bật để tương thích thiết bị 16KB page size (Android 15+).

### Package map (`com.galaxyjoy.cpuinfo`)

- `feat/` — mỗi feature 1 sub-package (`feat/infor/{cpu,gpu,ram,sensor,storage,screen,hardware,android,camera,drm,media}`, `feat/app`, `feat/temp`, `feat/setting`, `feat/cputile`, `feat/ramtile`). `feat/ActHost.kt` là Activity chính có bottom nav, `feat/SplashActivity.kt` là entry điểm. `feat/processes` đã bị xoá (tab bị ẩn từ lâu, không dùng được — xem `doc/task/epic-01-bugfix.md` B11).
- `data/{provider,local}` — `DataProvider*` cho từng loại thông tin (CPU/GPU/RAM/Storage/Applications) + `RepositoryUserPreferences` (DataStore).
- `domain/{model,observable,action,result}` + `Interactor.kt` — kiến trúc đang chuyển sang Interactor / Observable pattern (TODO trong README), chưa hoàn tất.
- `di/modules/{AppModule,AppModuleBinds}.kt` — Hilt graph. App entry: `GalaxyApp.kt` (`@HiltAndroidApp`).
- `appinitializers/` — pattern: `InitializersApp` orchestrate một list các `AppInitializer` (Timber, Epoxy, Theme, Rx, NativeTools). Khi cần init thứ gì lúc app start, **thêm 1 `AppInitializer` mới**, đừng nhồi vào `GalaxyApp.onCreate`.
- `widget/` — custom view (arc, progress, swipe reveal). `widget/swiperv/` là port của SwipeRevealLayout, code nguyên trạng — không phải nơi nên refactor.
- `ui/{component,theme}` — Compose components và theme (Material3, đang migrate dần).
- `ext/` — extension functions cho Context/Activity.

### Hệ thống quảng cáo
Chi tiết đầy đủ ở `doc/AD.MD`. Tóm tắt:
- Provider hiện tại: **AppLovin MAX** thông qua SDK wrapper `com.github.royt93:AdmobWrapper:1.1.5` (cài qua JitPack — repo đã được thêm vào `allprojects.repositories`). Flag `BuildConfig.IS_ENABLE_ADMOB = false` switch về AppLovin; bật lên là dùng AdMob ID trong cùng file `build.gradle.kts`.
- 3 touchpoint: App Open (`SplashActivity`), Banner (`ActHost` bottom — có lifecycle hooks `bannerResume/Pause/Destroy`), Interstitial (`FrmApplications` — nút Sort A/Z). Preload interstitial gọi trong `ActHost.onCreate`.
- Init flow trong `GalaxyApp.setupAd()`: `AdManager.setConfig(adConfig) → AdManager.initialize(this) { ... }`. Thứ tự này **bắt buộc** — SDK sẽ no-op hoặc crash nếu đảo.
- SDK tự throttle (min 60s giữa fullscreen, max 6/session, 5/ngày) → không cần thêm logic gating ở app layer.
- VIP whitelist GAID cứng (`getMyVipGAIDSet()`) đã được xoá khỏi `GalaxyApp.kt` — SDK dùng danh sách nội bộ + `vipKeySecret` (xem `doc/feature.md` đợt 2 #7).

### Memory leak history
`doc/MEMORY_LEAK.MD` ghi lại 7 leak đã fix (sensor listener, broadcast receiver lifecycle, postDelayed, NavController listener, ReviewManager callback). Khi đụng vào các file đó hoặc thêm pattern tương tự (raw `Thread`, anonymous listener, `postDelayed`, receiver register), check lại doc này trước.

## CI

`.github/workflows/build_and_test.yml` — chạy `./gradlew testDebugUnitTest` trên JDK 17 cho mọi PR và push lên `master`. Branch hiện tại là `dev` (PR target → `master`).

## Test setup hiện tại

- Unit test deps: JUnit 4.13.2, MockK 1.13.13, kotlinx-coroutines-test 1.9.0, kotlin-test (qua `testImplementation(kotlin("test"))`).
- Test target hiện có: `DataProviderRamTest`, `DataProviderApplicationsTest`, `DataProviderGpuTest`, `VMSensorsInfoTest`. `VMSensorsInfoTest.onCleared` là regression cho memory leak #1 (`doc/MEMORY_LEAK.MD`).
- Pattern: dùng MockK mock Android dependencies; `ViewModel.onCleared()` protected → gọi qua reflection. Không dùng Robolectric (giữ test fast). Build script đã set `unitTests.isReturnDefaultValues = true`.

## Notes

- `local.properties` chứa SDK path → không commit.
- `doc/feature.md` track roadmap enhancement (đợt 1 đã ship, đợt 2 đang plan ở mục Deferred).
- `doc/quick_win.md` — danh sách cải tiến nhỏ, dễ làm, chưa gắn vào roadmap chính.
- `doc/AD_PROMPT_AOS.MD` — prompt/spec gốc dùng khi triển khai lại hệ thống quảng cáo, tham khảo khi cần đối chiếu hành vi AppLovin/AdMob mong muốn.
