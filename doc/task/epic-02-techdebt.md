# Epic 2 — Tech Debt & Kiến trúc

> README.md gốc liệt kê TODO: "Unify architecture", "Replace layouts with Compose", "Replace RxJava with coroutines", "Tests", "Add benchmarks", "Migrate Material 3". Epic này track tiến độ thật của các TODO đó dựa trên review, không lặp việc đã Implemented trong `doc/feature.md`.

## Story 1 — Hoàn tất hoặc dứt điểm migration Interactor/Observable

**Trạng thái thật**: chỉ **3/12 vùng feature** (`cpu`, `gpu`, `ram` trong `feat/infor`, cộng Applications ở dạng chưa wire) dùng pattern mới (`Frm → VM → Observable*Data(Interactor) → DataProvider`) end-to-end. Còn lại `storage`, `hardware`, `sensor`, `camera`, `screen`, `android`, `drm`, `media`, `processes`, `temp` vẫn 100% pattern cũ (ViewModel gọi thẳng Android API/RxJava).

**Rủi ro để dang dở lâu**: chi phí duplicate tăng dần (case Applications ở B03), dev mới dễ sửa nhầm chỗ chết (`DataProviderStorage.kt` rỗng nhưng tưởng có tác dụng), 2 mô hình threading (Rx + Flow) tăng rủi ro sửa nhầm.

**Task**:
- T2.1 — ✅ **Đã xong (2026-08-28)**: xoá `FrmApplications`/`VMApplications`/`AdtApp`/`ExtendedAppInfo` (cũ), wire `FrmNewApplications` vào `nav_graph.xml` thay thế. Đã fix chung B03b. **Phát hiện lúc làm**: bản Compose ban đầu thiếu nút Sort (không hoạt động) + 4 action (Rate/More/Share/Policy) + mất 1 vị trí quảng cáo Interstitial — đã hỏi user, chọn hoàn thiện đầy đủ trước khi chuyển thay vì chấp nhận mất tính năng. Verify bằng `assembleDevDebug` full build — L
- T2.2 — Migrate `storage` sang Interactor/Observable pattern, xoá `DataProviderStorage.kt` stub rỗng hoặc implement thật — M
- T2.3 — Migrate `hardware` (`VMHardwareInfo.kt` 432 dòng, đang gộp 5-6 domain concern: battery/camera/audio/wireless/usb — tách domain layer) — L
- T2.4 — Migrate `temp` (đang RxJava) sang Coroutine/Flow — M
- T2.10b — ✅ **Đã xong (2026-08-28)** Xoá hẳn `feat/processes/` (`FrmProcesses`, `ProcessesVM`, `PsProvider`, `AdtProcesses`) — tab này đã bị ẩn 100% người dùng thật (`ActHost.kt:88` ẩn khi `SDK_INT > M`/API 23, trong khi `minSdk=24`) và đọc `/system/bin/ps` bị SELinux chặn từ Android 7+ nên dữ liệu vốn không đáng tin ngay cả khi hiện. Xoá thay vì fix B11-B13 — giảm size APK, giảm nợ kỹ thuật thay vì tăng — S

## Story 2 — Dọn 3 mô hình async song song

RxJava3 (`Disposable`) + Coroutine/Flow + custom `ListLiveData` cùng tồn tại. Ưu tiên coroutine/Flow cho mọi polling/one-shot query để cancellation/loading/error/test thống nhất.

- T2.5 — Xoá RxJava khỏi `VMApplications`, `StorageInfoViewModel`, `ProcessesVM`, `PsProvider`, `TemperatureProvider`, `TemperatureVM`, `InitializerRx` (đi kèm Story 1) — gộp chung, không tách task riêng.

## Story 3 — Test coverage cho domain layer mới (ưu tiên hơn provider cũ)

Zero coverage hiện tại cho đúng lớp được kỳ vọng dễ test nhất:

- T2.6 — Test `domain/observable/*` (ObservableCpuData, ObservableRamData, ObservableGpuData, ObservableApplicationsData) — M
- T2.7 — Test `domain/action/RamCleanupAction`, `domain/result/InteractorGetPackageName` — S
- T2.8 — Test `DataProviderCpu` (đã note trong `doc/feature.md` là target nhưng bị rớt khỏi scope thực thi đợt 1) — S
- T2.9 — Test regression cho các bug vừa fix ở Epic 1: SD-card upsert, malformed `/proc/mounts` line, nhiều format output `ps`, tile với `qsTile == null` — M

## Story 4 — Dọn dead code

- T2.10 — Xoá `InfoItemsEpoxyController` (class rỗng, đã `@Suppress("unused")`) — XS
- T2.11 — Gộp `VMHardwareInfo.getCameraInfo()` (Camera API cũ, deprecated) vào `VMCameraInfo` (Camera2, đã đủ) — xoá duplicate — S
- T2.12 — Xoá `VMAndroidInfo.getVmVersion()` Dalvik-check chết (minSdk 24 → luôn ART) — XS
- T2.13 — Fix typo `glesVersio` → `glesVersion` lan ra 4 file (`GpuData.kt`, `ObservableGpuData.kt`, `GpuInfoEpoxyController.kt`, `SystemInfoExporter.kt`) — XS

## Story 5 — Ad system & init convention

- T2.14 — Tách `GalaxyApp.setupAd()` thành `AdInitializer` theo đúng convention `AppInitializer` đã đặt ra cho mọi init khác — đồng thời fix thứ tự (hiện `setupAd()` chạy TRƯỚC `initializers.init()` → Timber chưa plant khi Ad SDK log) — S
- T2.15 — `AppModuleBinds` dùng `Set<AppInitializer>` (Hilt multibinding) không đảm bảo thứ tự — thêm priority/dependency rõ ràng nếu có initializer phụ thuộc nhau — S
- T2.16 — Cập nhật `CLAUDE.md` mục "Hệ thống quảng cáo": doc ghi flow cũ (`setConfig → earlyInit → ...`) và `getMyVipGAIDSet()` "chưa wire" nhưng thực tế đã xoá hẳn từ đợt 2; `compileSdk` ghi 36 nhưng thực tế 37; `AdmobWrapper` ghi 1.1.1 nhưng TOML thực tế 1.1.5 — XS
- T2.17 — Cập nhật `doc/AD.MD` §7: ghi đã xoá field `adView` + 3 hook thủ công nhưng `ActHost.kt` vẫn giữ và gọi `AdManager.bannerDestroy()` thủ công — XS

## Story 6 — Performance & cleanup nhỏ

- T2.18 — `ObservableCpuData` poll lại cả min/max freq mỗi 1s dù cố định theo boot — cache 1 lần, chỉ poll current freq — XS
- T2.19 — `ArcProgress`/`BaseRoundCornerProgressBar` override `invalidate()` rebuild `Paint`/`GradientDrawable` mỗi lần gọi (mỗi setter) — cache, chỉ rebuild khi input thật sự đổi — S
- T2.20 — `TopAppBarView.kt` còn dùng Material2 `androidx.compose.material.TopAppBar` giữa codebase M3 — migrate — XS
- T2.21 — Deprecated API cleanup: EGL10 legacy (`FrmGpuInfo`), `windowManager.defaultDisplay`, `Build.SERIAL`, `FrmGpuInfo` thiếu `onDestroyView` cleanup handler — S
- T2.22 — `multiDexEnabled=true` dư thừa (minSdk 24, D8/R8 native multidex từ API 21) — xoá cấu hình + dependency — XS
- T2.23 — Dọn `app/proguard-rules.pro` (265 dòng, phần lớn cho lib không còn dùng: butterknife/retrofit/realm/eventbus/facebook/dexter/ucrop/jsoup...) — thêm rule cho RxJava3 thật (`io.reactivex.rxjava3.**`) và AdmobWrapper nếu SDK không tự bundle consumer rules — S
- T2.24 — Camera/DRM/Media ViewModel init đọc metadata đồng bộ trong constructor (Camera2, MediaDrm, MediaCodecList) — có thể gây jank/ANR trên OEM chậm — chuyển IO dispatcher + `StateFlow<UiState>` — M

## Story 7 — Loại bỏ Epoxy (Gemini CLI)

Epoxy đã bị Airbnb ngừng phát triển, chỉ còn 3 màn hình dùng (`FrmCpuInfo`, `FrmGpuInfo`, `FrmRamInfo` — các màn khác đã dùng `BaseRvFragment`/standard Adapter). Toàn bộ `kotlin-kapt` plugin đang phải giữ lại chỉ vì 3 màn này (CLAUDE.md đã note Epoxy là annotation processor "legacy" duy nhất chưa qua KSP).

- T2.28 — Migrate `FrmCpuInfo`/`FrmGpuInfo`/`FrmRamInfo` từ Epoxy sang standard `ListAdapter`/`RecyclerView.Adapter` hoặc Compose (nhất quán hướng migration Story 1) — sau đó xoá `kapt` + `epoxy-processor` khỏi build script hoàn toàn. Lợi ích: tăng tốc build (ước tính Gemini CLI ~35-40%), gỡ rào cản khi nâng Kotlin > 1.9.25 — L

## Story 8 — Hợp nhất hệ thống Preferences (Gemini CLI)

Đang phân mảnh 4 hệ thống lưu cấu hình song song: `UserPreferencesRepository` (DataStore), `Prefs.kt` (Gson SharedPrefs), `VipPrefs.kt`, `defaultSharedPreferences` trong `FrmSettings`. Phát hiện cụ thể: `UserPreferencesRepository.setApplicationsSortingOrder` bị bỏ hoang không dùng, trong khi `VMApplications` lại đọc sort order từ `Prefs.kt` — 2 nguồn sự thật cho cùng 1 setting.

- T2.29 — Quy tụ về Jetpack DataStore duy nhất, xoá `Prefs.kt` (Gson SharedPrefs) — làm sau/song song Story 1 vì đụng `VMApplications`/`VMNewApplications` — M

## Story 9 — Performance khác (Gemini CLI)

- T2.30 — `VMApplications`/`ObservableApplicationsData` load danh sách app: với mỗi app gọi thêm `packageManager.getPackageInfo()` chỉ để lấy icon resource ID dù `ApplicationInfo` đã có sẵn field cần thiết — với máy 200+ app tạo hàng trăm Binder IPC call dư thừa, có thể gây đơ UI khi mở tab. Loại bỏ call thừa + batch trên `Dispatchers.IO` — M (làm chung T2.1)

## Story 10 — CI/Build hygiene

- T2.25 — Thêm step lint vào CI (hiện `abortOnError=false` khiến lint chạy cũng không fail — cân nhắc bật lại cho warning nghiêm trọng) — S
- T2.26 — Thêm step `assembleDevDebug` vào CI để bắt lỗi compile resource/manifest/CMake mà unit test không phủ — S
- T2.27 — Audit permission thừa trong Manifest: `POST_NOTIFICATIONS` (không thấy dùng NotificationManager trong code app), `REQUEST_DELETE_PACKAGES` (code chỉ dùng `ACTION_UNINSTALL_PACKAGE`, không cần permission silent-uninstall này) — S

---

## ✅ Quyết định đã chốt (2026-08-28)
T2.1 (Applications): giữ bản Compose, xoá bản cũ — **đã khoá**, làm ở Sprint 2 (sau khi xong batch crash-fix Epic 1 Sprint 1).
Thứ tự tổng thể: Sprint 1 = Epic 1 (bugfix) → Sprint 2 = Epic 2 (tech debt, T2.1 trước tiên) → Sprint 3 = Epic 3 (feature mới).

## Ước lượng tổng
Story 1+2 (migration + async cleanup, gồm T2.1 đã khoá): ~2-3 tuần, rủi ro cao nhất trong epic này.
Story 3 (test): ~1 tuần.
Story 4-6, 8-10 (dọn dẹp/perf/CI): ~1 tuần, có thể làm xen kẽ, rủi ro thấp.
Story 7 (xoá Epoxy): ~3-4 ngày riêng, nên làm cuối cùng sau khi Story 1 xong (đỡ conflict vì CPU/GPU/RAM đã ổn định pattern mới).
