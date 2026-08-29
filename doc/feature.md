# Enhancement Plan — cpu-info

> Plan dựa trên đánh giá codebase (2026-05-16). Mỗi mục là 1 commit độc lập, có thể revert riêng.

---

## ✅ Implemented — đợt 1 (2026-05-16)

| # | Mục | Outcome |
|---|---|---|
| 1 | Xóa code chết | Đã xóa `ext/Applovin.kt` (toàn bộ comment), `sdkadbmob/` (package rỗng), `compile_info.txt`, `compile_output.txt`, block `testImplementation` comment-out trong `app/build.gradle.kts`. Build vẫn pass. |
| 2 | Bump `resolutionStrategy.force()` versions | Điều chỉnh từ "bỏ" → "bump cho khớp" sau khi phát hiện force là load-bearing (transitive deps kéo Kotlin 2.x). kotlin-stdlib 1.9.20 → 1.9.25, coroutines 1.7.3 → 1.9.0. Thêm comment giải thích. |
| 3 | Migrate sang `gradle/libs.versions.toml` | Tạo `gradle/libs.versions.toml`. Root + app build script chuyển sang `alias(libs.plugins.xxx)` + `libs.xxx`. Xóa `Libs.kt` + `Versions.kt`. Thêm `pluginManagement` + `dependencyResolutionManagement(PREFER_PROJECT)` vào `settings.gradle.kts`. |
| 4 | Hilt + Glide → KSP | Thêm plugin `com.google.devtools.ksp:1.9.25-1.0.20`. `kapt(hilt-android-compiler)` → `ksp(...)`. `kapt(glide:compiler)` → `ksp(glide:ksp)`. Giữ `kapt(epoxy-processor)`. Side effect: `GlideApp` không còn được generate bởi Glide KSP → thay bằng `Glide.with()` trực tiếp trong `AdtApp.kt` (`GlideAppModule` rỗng nên tương đương). |
| 5 | Smoke test critical path | 10 unit tests pass: 4 `DataProviderRamTest`, 3 `DataProviderApplicationsTest`, 2 `DataProviderGpuTest`, 1 `VMSensorsInfoTest` (regression cho memory leak #1). Test deps: JUnit 4.13.2, MockK 1.13.13, coroutines-test 1.9.0. Chạy bằng `./gradlew :app:testDevDebugUnitTest`. |

## ✅ Implemented — đợt 2 (2026-05-16)

| # | Mục | Outcome |
|---|---|---|
| 6 | Xóa `features/information/` rỗng | Folder chỉ chứa `.DS_Store`, đã xóa hoàn toàn. Concern "feat/ vs features/" duplication chính thức đóng. |
| 7 | Xóa `getMyVipGAIDSet()` dead code | Xóa function + 21 GAID hardcoded khỏi `GalaxyApp.kt`. SDK đã có internal VIP list tương đương (doc/AD.MD đã ghi). |
| 8 | Fix `!!` NPE risks | `VMHardwareInfo.kt:300` + `DataProviderCpu.kt:75` đổi `listFiles()!!.size` → `listFiles()?.size ?: 1`. Idiomatic Kotlin, không dựa vào exception flow. |
| 9 | Fix 5 deprecated API warnings | `Divider` → `HorizontalDivider` (AppScreen.kt). `updateTransition` → `rememberTransition` (DraggableBox.kt). `overridePendingTransition` → `overrideActivityTransition` (SplashActivity.kt, có API gate cho < UDC). AppLovin `mediationProvider` setter + `initializeSdk(listener)` → `AppLovinSdkInitializationConfiguration.builder()` (GalaxyApp.kt). 3 unused Compose params suppress bằng `@Suppress("UNUSED_PARAMETER")` để giữ plumbing cho future swipe-reveal implementation. **Runtime crash fix**: API mới yêu cầu xóa `<meta-data applovin.sdk.key>` khỏi `AndroidManifest.xml` (mutex với key trong config object). |
| 10 | Xóa ServiceStorageUsage + RamUsageWidgetProvider | Xóa 10 file (`RamUsageWidgetProvider.kt`, `ServiceRefresh.kt`, `ServiceStorageUsage.kt`, `InitializerRamWidget.kt`, `widget_ram_provider.xml`, `vi_widget.xml`, `ic_ram_preview.png`, `bools.xml` x2, `values-v14/dimens.xml`). Xóa 3 manifest entries. Xóa `KEY_RAM_REFRESHING` + `KEY_RAM_CATEGORIES` + RAM PreferenceCategory. Xóa `_shouldStartStorageServiceEvent` + `onUpdatePackageSizeEvent` trong VMApplications, observer trong FrmApplications. **Xóa hẳn EventBus dependency** (TOML + build.gradle.kts) — không còn subscriber nào. Lý do gate: cả 2 feature đều có API gate < O nên ~99% user 2026 không bao giờ chạy. |

## ✅ Implemented — F/U-coded feature sprints (2026-08-29, Sprint 3–14)

Separate loop from the refactor plan above — new user-facing features, tracked via commit
messages (`feat: ... (Sprint N, <code>)`) rather than this table. See `git log --oneline dev` for
the full list. Summary: VIP daily check-in streak (U09/U10), Device Truth Score (U01), CPU Cluster
Topology (F09/U06), Throttling Fingerprint stress test (U02), thermal status + security checklist
(F02/F04), AI Readiness Score (F10/U12), Hardware Diff/Snapshot (U03), Interactive Sensor Test
Suite (F07), App Permission & SDK Inventory (F05), Vulkan/GLES Detail (F08), USB/BT Inspector
(F03), Privacy-preserving Fleet Compare (U04). Also see `doc/quick_win.md` — most of its "đợt 3a"
bundle (#10/#8/#7/#6/#9) turned out to already be implemented as part of this loop.

## 🟡 In progress
*(none)*

## 📋 Picked — đợt 1 (đã thực thi xong)

### 1. Xóa code chết
- `app/src/main/java/com/galaxyjoy/cpuinfo/ext/Applovin.kt` — toàn bộ file đã comment, legacy AppLovin trực tiếp (đã thay bằng AdmobWrapper SDK)
- `app/src/main/java/com/galaxyjoy/cpuinfo/sdkadbmob/` — package rỗng (xem `doc/AD.MD` mục 4)
- `compile_info.txt`, `compile_output.txt` ở root — build log debug
- Block `testImplementation` comment-out trong `app/build.gradle.kts` — sẽ thêm lại đúng ở task #5

### 2. Bump `resolutionStrategy.force()` versions
**Điều chỉnh khi thực thi**: build vỡ khi xóa force — transitive deps (Compose BOM, ...) kéo `kotlin-stdlib:2.1.0` và `kotlinx-coroutines:1.10.1` (cả hai compiled với Kotlin 2.x), nhưng compiler 1.9.25 chỉ đọc metadata ≤ 2.0.0.

→ Thay vì xóa, **bump versions** cho khớp app declares:
- `kotlin-stdlib` 1.9.20 → 1.9.25 (match Kotlin compiler)
- `kotlinx-coroutines` 1.7.3 → 1.9.0 (match dependency declaration; trước đó là downgrade vô nghĩa)
- Thêm comment giải thích lý do tồn tại của block force.

### 3. Migrate sang `gradle/libs.versions.toml`
- Tạo `gradle/libs.versions.toml` chứa toàn bộ version + library declarations
- Sửa `app/build.gradle.kts` dùng `libs.xxx` references
- Xóa `buildSrc/src/main/java/Libs.kt` + `Versions.kt`
- Giữ `DependencyUpdates.kt` (vẫn có giá trị cho task `dependencyUpdates`) và `SigningConfig.kt`

### 4. Hilt + Glide → KSP (giữ kapt cho Epoxy)
- Thêm plugin `com.google.devtools.ksp` ở root (version map với Kotlin 1.9.25 → KSP `1.9.25-1.0.20`)
- `kapt("com.google.dagger:hilt-android-compiler:2.51.1")` → `ksp(...)`
- `kapt("com.github.bumptech.glide:compiler:4.16.0")` → `ksp("com.github.bumptech.glide:ksp:4.16.0")`
- **Giữ** `kapt("com.airbnb.android:epoxy-processor:5.1.3")` — Epoxy chưa support KSP đầy đủ
- Verify: `./gradlew clean assembleDevDebug`

### 5. Smoke test critical path
Target ~10–15 unit tests cho:
- `data/provider/DataProviderCpu` — parse `/proc/cpuinfo` lines
- `data/provider/DataProviderGpu` — GL extension parsing
- `data/provider/DataProviderRam` — `/proc/meminfo` parsing
- `data/provider/DataProviderApplications` — package filter logic
- `feat/infor/sensor/VMSensorsInfo.onCleared()` — verify `unregisterListener` được gọi (regression cho memory leak #1 trong `doc/MEMORY_LEAK.MD`)

Test deps cần uncomment trong `app/build.gradle.kts`:
```kotlin
testImplementation("junit:junit:4.13.2")
testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
testImplementation("io.mockk:mockk:1.13.x")
testImplementation(kotlin("test"))
```

---

## ⏸️ Deferred

- **Security cleanup** (gỡ password/keystore khỏi repo) — user pick: skip, repo private
- **Merge `feat/` ↔ `features/`** — scope lớn, để đợt sau
- **Tech stack consolidation** (bỏ EventBus, RxJava→Coroutines) — scope rất lớn, cần plan riêng
- **Bỏ Epoxy** — Airbnb đã deprecated, nhưng scope migration sang RecyclerView/LazyColumn lớn, defer

## ❌ Skipped
- Full Hilt test runner + Espresso UI test (chọn smoke test thay thế)

## 💭 Ideas
- Migrate Material 2 → Material 3 dynamic colors (TODO trong README)
- Bench native cpuinfo lib khởi tạo time
- Tách `widget/swiperv/` thành module riêng (port code, không nên đụng)
