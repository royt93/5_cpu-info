# Epic 2 — Tech Debt & Kiến trúc

> README.md gốc liệt kê TODO: "Unify architecture", "Replace layouts with Compose", "Replace RxJava with coroutines", "Tests", "Add benchmarks", "Migrate Material 3". Epic này track tiến độ thật của các TODO đó dựa trên review, không lặp việc đã Implemented trong `doc/feature.md`.

## Story 1 — Hoàn tất hoặc dứt điểm migration Interactor/Observable

**Trạng thái thật**: **7/12 vùng feature** (`cpu`, `gpu`, `ram`, `hardware`, `temp`, `storage`, `media` trong `feat/infor`, cộng Applications ở dạng chưa wire) dùng pattern mới (`Frm → VM → Observable*Data(Interactor) → DataProvider`) end-to-end. Còn lại `sensor`, `camera`, `screen`, `android`, `drm` vẫn 100% pattern cũ (ViewModel gọi thẳng Android API). `processes` đã xoá hẳn, xem T2.10b. Không vùng nào còn lại dùng RxJava (đã xác nhận qua khảo sát Sprint 22 — RxJava chỉ còn ở `sensor` nếu tính `SensorEventListener` là "API cũ", không phải Rx thật).

**Rủi ro để dang dở lâu**: chi phí duplicate tăng dần (case Applications ở B03) — còn 5 vùng dùng API Android trực tiếp không qua `DataProvider`/`Interactor` (`sensor`, `camera`, `screen`, `android`, `drm`). `sensor` là vùng khó nhất còn lại: `VMSensorsInfo` dùng `SensorEventListener` sống (không phải one-shot như 5 vùng vừa xong) + lifecycle `onStart()/onStop()` + có test ràng buộc chặt vào hành vi đăng ký/huỷ đăng ký — sẽ cần shape mới (`callbackFlow{}`) chưa dùng ở vùng nào trong 7 vùng đã migrate.

**Task**:
- T2.1 — ✅ **Đã xong (2026-08-28)**: xoá `FrmApplications`/`VMApplications`/`AdtApp`/`ExtendedAppInfo` (cũ), wire `FrmNewApplications` vào `nav_graph.xml` thay thế. Đã fix chung B03b. **Phát hiện lúc làm**: bản Compose ban đầu thiếu nút Sort (không hoạt động) + 4 action (Rate/More/Share/Policy) + mất 1 vị trí quảng cáo Interstitial — đã hỏi user, chọn hoàn thiện đầy đủ trước khi chuyển thay vì chấp nhận mất tính năng. Verify bằng `assembleDevDebug` full build — L
- T2.2 — ✅ **Đã xong (Sprint 21, 2026-08-30)**: migrate `storage` sang pattern mới — `DataProviderStorage.kt` (data/provider, hết rỗng, chứa toàn bộ logic đọc dung lượng + scan `/proc/mounts` chuyển từ VM sang) + `StorageData`/`StorageVolume` (domain/model) + `ObservableStorageData` (domain/observable). **Vùng đầu tiên dùng `MutableInteractor`** (không phải `ImmutableInteractor` như 5 vùng trước) — storage không tự poll, chỉ đổi khi có sự kiện mount SD card, nên interactor chờ trigger `invoke(Unit)` thay vì tự chạy loop; `FrmStorageInfo` vẫn tự đăng ký `BroadcastReceiver` + debounce 2s (giữ nguyên, đây là mối quan tâm Android/lifecycle thuộc về Fragment) rồi gọi `viewModel.refreshSdCard()` để trigger. **Đơn giản hoá có chủ đích**: bản cũ chỉ "upsert" riêng dòng SD card (giữ Internal/External cũ nguyên) mỗi lần trigger; bản mới đọc lại **toàn bộ** snapshot (Internal+External+SD) mỗi lần trigger — dung lượng trống vốn thay đổi theo thời gian nên đọc lại hết đơn giản hơn vá từng phần, và cấu trúc "1 emission = 1 snapshot đầy đủ" khớp pattern 5 vùng trước. Nhờ vậy **toàn bộ lớp bug B08 (duplicate SD row) biến mất về mặt cấu trúc** — không còn logic "add nếu khác" nào để tái phát bug, danh sách hiển thị luôn rebuild toàn bộ từ snapshot mới nhất. Xoá hẳn `upsertSdCard()` (và 3 test tương ứng — không còn code path nào gọi tới), giữ nguyên `candidateMountPoint()` (đổi chỗ sang `DataProviderStorage`, còn nguyên 5 test cũ + thêm test cho `findSdCardVolume()`). Verify: unit test + build xanh, smoke test thật trên Pixel 7 Pro thật — 3 dòng Internal/External/SD hiển thị đúng số liệu thật (kể cả 1 match dương tính giả nhỏ 62MB từ heuristic `/proc/mounts` — hành vi đã có từ code gốc, không phải regression), re-render đúng sau rời/quay lại tab (chu kỳ đăng ký/huỷ đăng ký receiver), không crash, logcat sạch — M
- T2.3 — ✅ **Đã xong (Sprint 19, 2026-08-30)**: migrate `hardware` sang pattern mới — `DataProviderHardware` (data/provider) + `HardwareData` (domain/model) + `ObservableHardwareData` (domain/observable, emit 1 lần vì toàn field tĩnh, không cần polling loop như CPU/RAM) + `VMHardwareInfo` giờ chỉ còn wiring (`observe().map{}.asLiveData()`), format hiển thị chuyển xuống `FrmHardwareInfo`. **Sizing L trong doc đã lỗi thời**: `VMHardwareInfo.kt` thực tế chỉ 157 dòng lúc bắt đầu sprint này (không phải 432) — battery đã tách ra `VMBatteryInfo` (Sprint 17), camera đã tách ra `VMCameraInfo` từ trước, nên phần còn lại chỉ có wireless + USB, không cần polling, không có RxJava — effort thực tế gần S/M hơn L. Nhân tiện fix `RandomAccessFile` đọc Wi-Fi MAC leak FD nếu `readLine()` throw (dùng `.use{}`). Verify: unit test + build xanh, smoke test thật trên emulator (API cao, Pixel 10 Pro XL AVD) — data hiển thị đúng y hệt bản cũ, re-render đúng sau khi rời tab và quay lại, không crash, logcat sạch — S/M
- T2.4 — ✅ **Đã xong (Sprint 20, 2026-08-30)**: migrate `temp` sang pattern mới — `DataProviderTemperature` (data/provider, thay `TemperatureProvider` cũ cho CPU-temp discovery) + `TemperatureData` sealed interface (domain/model — `Probing`/`Available`/`Unavailable`, cần vì đây là vùng đầu tiên có state discovery 2 giai đoạn thật, không chỉ 1 shape tĩnh như CPU/RAM/Hardware) + `ObservableTemperatureData` (domain/observable, port `Observable.interval(0,3,SECONDS)` RxJava → `flow{ while+delay }`, giữ nguyên probe-once-cache-path-rồi-poll). `TemperatureVM` giữ `isLoading`/`isError` (data binding XML `frm_temperature.xml` bind thẳng vào 2 field này, không đổi layout) nhưng bỏ hẳn RxJava/`Prefs` trực tiếp; format hiển thị (icon + string resource) chuyển xuống `FrmTemperature`. **Khác CPU/RAM/GPU/Hardware**: `temp` cần start/stop theo `Fragment.onStart()/onStop()` (không phải luôn chạy từ `init` như các vùng kia) — giữ nguyên 2 public method `startTemperatureRefreshing()/stopTemperatureRefreshing()`, giờ quản lý 1 `Job` thay vì 2 RxJava `Disposable`. **Phát hiện lúc làm**: `TemperatureProvider` (class cũ) vẫn bị `ThrottleTestRunner` và `VMBatteryInfo` dùng chung cho `getBatteryTemperature()` — không xoá cả class, chỉ bóc RxJava (`getCpuTemperatureFinder()`/`CpuTemperatureResult`, không còn ai gọi ngoài code cũ) ra khỏi nó, giữ nguyên method + signature cho 2 consumer kia (tránh mở rộng phạm vi sang 2 feature không liên quan). Cache key Prefs đổi tên (`temp_cpu_path_key` thay vì `temp_result_key` cũ) vì kiểu dữ liệu cache đổi từ object Gson-serialize sang String path thuần — dùng chung key cũ sẽ đọc nhầm JSON blob thành file path. Thêm `DataProviderTemperatureTest`. Verify: unit test + build xanh, smoke test thật trên emulator — CPU hiện đúng fallback "not supported" (emulator không có sysfs), Battery hiện đúng 25°C sống động qua nhiều chu kỳ poll, re-render đúng sau rời/quay lại tab, màn Stress Test (dùng chung `TemperatureProvider`) vẫn load được, không crash, logcat sạch — M
- T2.31 — ✅ **Đã xong (Sprint 22, 2026-08-31)**: migrate `media` sang pattern mới — `DataProviderMedia` (data/provider, đọc + sort `MediaCodecList`) + `MediaData`/`MediaCodecData` (domain/model) + `ObservableMediaData` (domain/observable, emit 1 lần — danh sách codec tĩnh cho vòng đời process, cùng shape với Hardware/Media). `VMMediaInfo` chỉ còn wiring; format hiển thị (string resource) chuyển xuống `FrmMediaInfo`. **Vùng dễ nhất trong 6 vùng còn lại** (khảo sát trước khi chọn: 63 dòng VM, 0 test, không RxJava/listener/lifecycle đặc biệt) — chọn đúng như dự kiến, không phát sinh bất ngờ. Verify: unit test + build xanh, smoke test thật trên Galaxy S24 Ultra — 106 codec hiển thị đúng (70 decoder/36 encoder), re-render đúng sau rời/quay lại tab, không crash, logcat sạch — S
- T2.10b — ✅ **Đã xong (2026-08-28)** Xoá hẳn `feat/processes/` (`FrmProcesses`, `ProcessesVM`, `PsProvider`, `AdtProcesses`) — tab này đã bị ẩn 100% người dùng thật (`ActHost.kt:88` ẩn khi `SDK_INT > M`/API 23, trong khi `minSdk=24`) và đọc `/system/bin/ps` bị SELinux chặn từ Android 7+ nên dữ liệu vốn không đáng tin ngay cả khi hiện. Xoá thay vì fix B11-B13 — giảm size APK, giảm nợ kỹ thuật thay vì tăng — S

## Story 2 — Dọn 3 mô hình async song song

RxJava3 (`Disposable`) + Coroutine/Flow + custom `ListLiveData` cùng tồn tại. Ưu tiên coroutine/Flow cho mọi polling/one-shot query để cancellation/loading/error/test thống nhất.

- T2.5 — Xoá RxJava khỏi `VMApplications`, `StorageInfoViewModel`, `ProcessesVM`, `PsProvider`, ~~`TemperatureProvider`~~, ~~`TemperatureVM`~~, `InitializerRx` (đi kèm Story 1) — gộp chung, không tách task riêng. **`TemperatureVM` xong hoàn toàn, `TemperatureProvider` xong phần CPU-temp scan (Sprint 20) — vẫn còn RxJava-free `getBatteryTemperature()` dùng chung với Throttle/Battery, không phải nợ kỹ thuật, chỉ là 1 method nhỏ đã hết Rx.** `ProcessesVM`/`PsProvider` đã xoá hẳn cùng `feat/processes/` (T2.10b), không cần làm riêng nữa.

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
