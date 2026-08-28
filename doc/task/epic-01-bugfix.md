# Epic 1 — Bugfix & Stability

## ✅ Sprint 1 hoàn thành (2026-08-28)
16 bug đã fix và verify bằng `./gradlew :app:testDevDebugUnitTest` (build + test pass sau mỗi batch): B01, B02, B04, B05, B06, B07, B08, B09, B10, B14, B27, B28, B29, B30, B31, B32.
Còn lại B03/B03b/B11/B12/B13 — để dành Sprint 2 vì gắn liền quyết định kiến trúc (giữ bản Applications nào, xoá `feat/processes`).
B31 hoá ra không phải bug thật (đã verify khi đọc code) — chỉ dọn code chết, không đổi hành vi.

## ✅ Sprint 2 hoàn thành (2026-08-28)
- **B03/B03b**: Đã giữ bản Compose (`FrmNewApplications`), xoá bản cũ (`FrmApplications`/`VMApplications`/`AdtApp`/`ExtendedAppInfo` + layout/menu liên quan). **Phát hiện quan trọng lúc làm**: bản Compose thiếu nút Sắp xếp (không hoạt động) và thiếu 4 action (Rate/More app/Share/Policy) — đặc biệt nút Sort ở bản cũ là 1 trong 3 vị trí quảng cáo Interstitial của app (theo `doc/AD.MD`). Đã hỏi user và **hoàn thiện đầy đủ** trước khi chuyển: thêm nút Sort (kèm quảng cáo giống bản cũ) + 4 action còn thiếu vào `AppScreen.kt`/`FrmNewApplications.kt`. Không mất tính năng, không mất doanh thu quảng cáo. Đồng thời fix B03b (`ContextCompat.registerReceiver` với `RECEIVER_NOT_EXPORTED`).
- **B11/B12/B13**: Đã xoá hẳn `feat/processes/` (5 file Kotlin + 2 layout + entry trong `nav_graph.xml`/`menu_nav.xml`/`ActHost.kt`) — xác nhận tính năng đã bị ẩn khỏi UI từ lâu, không ai dùng được.
- Verify: `./gradlew :app:testDevDebugUnitTest :app:assembleDevDebug` — build APK đầy đủ (gồm cả native C++ 4 kiến trúc CPU) pass.

> Nguồn: đối chiếu chéo 6 review độc lập (4 subagent nội bộ theo vùng code + Codex CLI + Claude CLI, mỗi cái đọc toàn bộ source không thấy bài của nhau). Item nào ≥2 nguồn cùng phát hiện được đánh dấu **[đồng thuận]** — độ tin cậy cao hơn, nên ưu tiên.
>
> Story point: XS (<2h) · S (~0.5d) · M (~1d) · L (2-3d)

## P0 — Crash risk / mất dữ liệu / hiển thị sai nghiêm trọng

| # | Bug | File:line | Nguồn | Điểm |
|---|---|---|---|---|
| ✅B01 | `NativeToolsInitializer` gọi `ReLinker.loadLibrary` + `initLibrary()` không try/catch → toàn app crash khi khởi động nếu lib native load fail (thiếu ABI, file corrupt, thiết bị lạ) | `appinitializers/NativeToolsInitializer.kt:13-14` | scan build/test | XS |
| ✅B02 | `cpuinfo-libs.cpp getCpuName()` deref `cpuinfo_get_package(0)->name` không check `cpuinfo_get_packages_count() > 0` / package null / name null → native segfault (SIGSEGV) hoặc `NewStringUTF(nullptr)` crash JVM, không catch được từ Kotlin. Xảy ra thật trên emulator x86/SoC lạ | `app/src/main/cpp/cpuinfo-libs.cpp:26` | **[đồng thuận 3 nguồn]** Claude CLI + Gemini CLI | XS |
| ✅B03 | 2 implementation Applications tồn tại song song: `FrmApplications`/`VMApplications` (RxJava, đang wire vào nav_graph) vs `FrmNewApplications`/`VMNewApplications`/`ObservableApplicationsData` (Coroutine/Flow/Compose, ~700 dòng, hoàn toàn không route tới — dead code). Duplicate logic (`hasNativeLibs`, `getAppIconUri`...) dễ sửa 1 bên quên bên kia. **✅ Đã quyết định**: giữ bản Compose mới, xoá bản cũ (xem Epic 2 T2.1) | `feat/app/**`, `res/navigation/nav_graph.xml:13-17` | **[đồng thuận 3 nguồn]** scan data/domain + Codex + Claude CLI | M |
| ✅B03b | Cả 2 bản Applications đều gọi `requireActivity().registerReceiver(uninstallReceiver, intentFilter)` không chỉ định `RECEIVER_EXPORTED`/`RECEIVER_NOT_EXPORTED` — app `targetSdk=37` (≥ Android 14/API 34) → ném `SecurityException` ngay khi mở tab Applications, crash 100% trên máy Android 14+. **Ưu tiên fix cùng lúc T2.1** vì đụng đúng file đang migrate | `feat/app/FrmApplications.kt:131`, `feat/app/FrmNewApplications.kt:167` | Gemini CLI | XS |
| ✅B04 | Vulkan version bit-shift sai trong `DataProviderGpu`: `minor`/`patch` dùng `shr` (signed shift) thay vì `ushr`, offset patch lệch 2 bit → hiển thị version âm/khổng lồ trên nhiều GPU Adreno/Mali | `data/provider/DataProviderGpu.kt:38-40` | scan data/domain | S |
| ✅B05 | CPU min frequency hardcode chuỗi `"0"` thay vì `frequency.min.toString()` → mọi core luôn hiển thị min freq = 0 | `feat/infor/cpu/CpuInfoEpoxyController.kt:59-63` | scan feat/infor | XS |
| ✅B06 | `ServiceCpuTile` truy cập `qsTile.label/icon/updateTile()` không safe-call trong `while(true)` loop 1s → NPE khi tile bị gỡ khỏi QS panel giữa lúc update. `ServiceRamTile` cùng file loại đã làm đúng bằng `qsTile?.apply{}`. Đồng thời chia `sum/cpuCount` không check `cpuCount==0` → `ArithmeticException` | `feat/cputile/ServiceCpuTile.kt:45,63-65,98` | **[đồng thuận]** scan feat/infor + Claude CLI + Gemini CLI (chia 0) | XS |
| ✅B07 | `ServiceRamTile` tạo `PendingIntent.getActivity()` từ `Intent()` rỗng (không component/action) — trên Android 14+ gọi `startActivityAndCollapse` với Intent rỗng ném `ActivityNotFoundException`, crash TileService khi tap | `feat/ramtile/ServiceRamTile.kt:112-134` | **[đồng thuận 3 nguồn]** Codex + Gemini CLI | S |
| ✅B08 | Storage: mount event handler luôn `listLiveData.add(sdMemory)` không kiểm tra trùng → SD card bị nhân bản trong list mỗi lần hệ thống phát lại sự kiện mount | `feat/infor/storage/StorageInfoViewModel.kt:89-103` | Codex | S |
| ✅B09 | `/proc/mounts` parse: `dis.close()` không nằm trong `finally`/`.use{}` → leak file descriptor khi exception giữa chừng; đồng thời `lineElements[1].lastIndexOf("/")` có thể = -1 → `substring(-1,...)` throw, bị catch ngoài nuốt mất → SD detection âm thầm fail toàn bộ, không log | `feat/infor/storage/StorageInfoViewModel.kt:193-243` | **[đồng thuận]** scan data/domain + Codex + Claude CLI (3 nguồn) | S |
| ✅B10 | `SystemInfoExporter.exportSystemInfo()` tự tạo `CoroutineScope(Dispatchers.Main)` rời rạc không bind lifecycle → nếu user back khỏi `ActHost` khi đang build report (đọc codec/camera/DRM tốn thời gian), coroutine vẫn chạy xong rồi gọi `context.startActivity()` trên Activity đã destroy → crash/leak | `util/SystemInfoExporter.kt:44-60`, `feat/ActHost.kt:142-152` | **[đồng thuận 3 nguồn]** scan ads/infra + Codex + Claude CLI | S |
| ✅B11 | ⚠️ **Xem lại phạm vi**: `PsProvider` gọi `ps -p` không truyền PID cụ thể, giả định tên process luôn ở token cố định — format `ps` khác nhau theo OEM/Android version. Nhưng Gemini CLI phát hiện `feat/processes` **đã bị ẩn hoàn toàn khỏi UI** (`ActHost.kt:88` ẩn tab khi `Build.VERSION.SDK_INT > M`, tức API > 23; đọc `/system/bin/ps` cũng bị SELinux chặn từ Android 7+) → tab này gần như không ai dùng được trên minSdk 24. **Khuyến nghị: xoá hẳn `feat/processes/` thay vì fix bug** (xem T2.10b ở Epic 2) | `feat/processes/PsProvider.kt:42-49,59-89`, `feat/ActHost.kt:88` | scan feat/infor + Codex; correction bởi Gemini CLI | — |

## P0 (bổ sung sau khi Gemini CLI review xong)

| # | Bug | File:line | Nguồn | Điểm |
|---|---|---|---|---|
| ✅B27 | `DataProviderCpu.getNumberOfCores()` dùng `Runtime.getRuntime().availableProcessors()` — trên chip big.LITTLE/DynamIQ hiện đại, core lớn thường offline khi máy rảnh (power-collapsed) → API này chỉ đếm core đang online, ví dụ máy 8 core hiển thị "4 Cores", số core nhấp nháy theo tải. Fix: dùng `cpuinfo_get_processors_count()` (native, đã có sẵn) hoặc đọc `/sys/devices/system/cpu/possible` | `data/provider/DataProviderCpu.kt:23-28`, `domain/observable/ObservableCpuData.kt:25,43` | Gemini CLI | S |
| ✅B28 | `VMSensorsInfo`: `sensorList.indexOf(event.sensor)` có thể trả `-1` trên ROM tuỳ biến (sensor instance không khớp `equals`) → `listLiveData[-1]` ném `IndexOutOfBoundsException`. Thêm bounds check trước khi gán | `feat/infor/sensor/VMSensorsInfo.kt:73-74` | Gemini CLI | XS |
| ✅B29 | `ListLiveData.set()` gọi thẳng `MutableLiveData.setValue()` — khi `VMSensorsInfo` đăng ký sensor không truyền Handler (chạy trên IO thread), `onSensorChanged` bắn về background thread → `setValue` từ background thread ném `IllegalStateException`. Fix: đăng ký sensor kèm Main Handler, hoặc sửa `ListLiveData` dùng `postValue()` khi không ở main looper | `feat/infor/sensor/VMSensorsInfo.kt:38-45`, `util/lifecycle/ListLiveData.kt:110-116` | Gemini CLI | S |
| ✅B30 | `VMCameraInfo` đọc `StreamConfigurationMap`/`getHighSpeedVideoFpsRangesFor(maxSize)!!` không try-catch per-camera — OEM HAL quirks (Samsung/Xiaomi) có thể ném `IllegalArgumentException`/`AssertionError` ngay trong `init` của ViewModel → crash khi mở tab Camera | `feat/infor/camera/VMCameraInfo.kt:83-85,102-107` | Gemini CLI | S |
| ✅B31 | `FragmentResultListener` xuyên Activity không có tác dụng thật: `ActHost` lắng nghe `KEY_VIP_CHANGED` trên `supportFragmentManager` của chính nó, nhưng `FVipManagement` nằm trong `ActVip` (Activity khác) — event không bao giờ tới `ActHost`. **Xác minh khi fix**: không phải bug thật — `ActHost.onResume()` đã unconditionally gọi lại `applyVipBannerState()`+`refreshVipBadgeAndPulse()` mỗi lần quay lại từ `ActVip` (đảm bảo bởi vòng đời Activity chuẩn), nên UI luôn đúng. Đã xoá đoạn `FragmentResultListener` chết (không phải fix hành vi, chỉ dọn code gây hiểu nhầm) | `feat/ActHost.kt:103-110`, `feat/vip/FVipManagement.kt:331-334` | Gemini CLI | M |

## P1 — Bug thật nhưng ít nghiêm trọng hơn / edge case

| # | Bug | File:line | Nguồn | Điểm |
|---|---|---|---|---|
| ✅B12 | ⚠️ Cùng nhóm B11 (`feat/processes` dead code) — `ProcessBuilder` không bao giờ `destroy()`/đóng stream, leak Process + FD mỗi 5s refresh. Không cần fix riêng nếu xoá cả module | `feat/processes/PsProvider.kt:46-49` | scan feat/infor | — |
| ✅B13 | ⚠️ Cùng nhóm B11 — `AdtProcesses.rss/vsize.toLong()` crash `NumberFormatException` nếu dòng `ps` thiếu cột. Không cần fix riêng nếu xoá cả module | `feat/processes/AdtProcesses.kt:37-38` | scan feat/infor | — |
| ✅B14 | `VMSensorsInfo`: `registerListener`/`unregisterListener` chạy trên coroutine io riêng biệt không đồng bộ thứ tự → xoay màn hình/chuyển tab nhanh có thể để sensor đăng ký ngoài ý muốn; callback `onSensorChanged` không chạy main thread nhưng set `LiveData.value=` trực tiếp → `IllegalStateException` tiềm ẩn | `feat/infor/sensor/VMSensorsInfo.kt:38-52` | **[đồng thuận]** scan feat/infor + Claude CLI | S |
| B15 | `TemperatureVM.stopTemperatureRefreshing()` chỉ dispose `refreshingDisposable`, bỏ quên `temperatureDisposable` (chỉ dispose ở `onCleared`) → tab switch nhanh leak subscription cũ + ghi trùng kết quả vào prefs | `feat/temp/TemperatureVM.kt:55-74` | Claude CLI | S |
| B16 | `FrmInfoContainer`: `TabLayoutMediator.attach()` không lưu reference để `.detach()` ở `onDestroyView()` — cùng dạng leak #7 (NavController listener) đã fix trước đây nhưng chưa áp dụng lại ở đây | `feat/infor/FrmInfoContainer.kt:25-27` | scan feat/infor | XS |
| B17 | `RamCleanupAction` tăng `killedCount` ngay sau khi gọi `killBackgroundProcesses` bất kể có thực sự kill được gì không (Android modern hạn chế kill cross-UID) — UI báo "Cleaned!" sai sự thật | `domain/action/RamCleanupAction.kt:26-45`, `feat/ramtile/ServiceRamTile.kt:81-104` | Codex | S |
| B18 | Applications tab thiếu `QUERY_ALL_PACKAGES`/`<queries>` đầy đủ → `getInstalledApplications()` chỉ trả app "nhìn thấy được" trên Android 11+, không phải toàn bộ — cần đổi UX "launchable apps" hoặc xin Play policy | `AndroidManifest.xml:17-26`, `data/provider/DataProviderApplications.kt:15-27` | Codex | M |
| B19 | `ExtendedAppInfo.appSize` khởi tạo 0, không nơi nào gán lại trên API < 26 → UI hiển thị "Calculating..." vĩnh viễn, tính năng hỏng ngầm | `feat/app/ExtendedAppInfo.kt:17`, `feat/app/AdtApp.kt:73-76` | scan feat/infor | S |
| B20 | Widget custom view: `ArcProgress`/`IconRoundCornerProgressBar` chia `progress/getMax()` (hoặc công thức đảo `max/progress`) không check `max==0`/`progress==0` → NaN/Infinity truyền vào `canvas.drawArc`, progress width sai âm thầm | `widget/arc/ArcProgress.java:182-187,293`, `widget/progress/IconRoundCornerProgressBar.java:116` | Claude CLI | S |
| B21 | `FVipManagement.scrollToRedeemSection()` dùng `postDelayed` lambda không bị huỷ ở `onDestroyView()` — cùng pattern leak #4 (SplashActivity) đã fix nhưng chưa áp dụng lại | `feat/vip/FVipManagement.kt:103-113` | scan ads/infra | XS |
| B22 | `AppScreen.kt`: `LaunchedEffect(snackbarMessage) { scope.launch { ... } }` dùng `rememberCoroutineScope()` thay vì chạy trực tiếp trong `LaunchedEffect` → job cũ không cancel theo key đổi, snackbar có thể chồng/trễ | `feat/app/AppScreen.kt:83-92` | scan ads/infra | XS |
| B23 | `DraggableBox`: `MutableTransitionState` build 1 lần trong `remember{}` không key, không đồng bộ `isRevealed`; `@Suppress("UnusedTransitionTargetStateParameter")` che lint thay vì fix — risk animation stuck trong LazyColumn reuse (dùng ở swipe-reveal Applications) | `ui/component/DraggableBox.kt:36-40` | Claude CLI | S |

## P2 — Cosmetic / preview-only / rủi ro thấp

| # | Bug | File:line | Nguồn | Điểm |
|---|---|---|---|---|
| B24 | `CpuSwitchBoxPreview()` gọi nhầm `CpuCheckbox(...)` thay vì `CpuSwitchBox(...)` — chỉ ảnh hưởng preview Compose, không runtime | `ui/component/CpuSwitchBox.kt:51` | Claude CLI | XS |
| B25 | Consent callback `requestConsentInfoUpdate { canRequestAds -> ... }` không dùng giá trị `canRequestAds`, luôn chạy tiếp splash flow — implicit trust SDK, không assert app layer | `feat/SplashActivity.kt:39-46` | Claude CLI | XS |
| B26 | Format số/chuỗi phụ thuộc locale mặc định (`uppercase()`, `"%.2f".format()`) làm sort app và export JSON/TEXT không ổn định giữa các locale (dấu phẩy vs dấu chấm thập phân) | `feat/app/VMApplications.kt:95-98`, `util/SystemInfoExporter.kt:85-96` | Codex | S |
| ✅B32 | `Utils.readOneLine()` mở stream không `.use{}`/`finally` — leak FD nếu exception (thêm 1 instance của pattern B09) | `util/Utils.kt:79-85` | Gemini CLI | XS |

---

## ✅ Quyết định đã chốt (2026-08-28)
Sprint order: **1) crash-fix nhanh → 2) tech debt (Applications migration...) → 3) feature mới**, làm tuần tự — xem `README.md`.
B03: giữ bản Compose (`FrmNewApplications`), xoá bản cũ (`FrmApplications`) — làm chung với B03b (registerReceiver crash) vì cùng file đang đụng tới.

## Tổng story point
**P0**: 17 item (11 gốc + 6 bổ sung sau Gemini CLI review), ~2 tuần nếu làm tuần tự 1 dev — nhưng phần lớn là XS/S, gộp commit hợp lý còn ~1.5 tuần thực tế.
**P1**: 12 item, ~1 tuần.
**P2**: 4 item, gộp vào bất kỳ sprint nào làm cleanup.
**B11-B13**: không tính điểm riêng — gộp vào quyết định "xoá `feat/processes/`" ở Epic 2 (T2.10b), không phải bugfix.

**Khuyến nghị gộp commit cho Sprint 1 (crash-fix)**:
- Batch "crash risk core" (B01, B02, B06, B10, B29) — 1 buổi, chỉ thêm try/catch/safe-call/bounds-check, không đổi logic.
- Batch "storage subsystem" (B08, B09, B32) — cùng nhóm file `/proc/mounts` + `Utils.readOneLine`, sửa cùng lúc.
- Batch "Android 14+ receiver" (B03b) — làm cùng lúc T2.1 (Applications migration) vì đụng đúng 2 file đang xoá/giữ.
- Batch "sensor" (B14, B28, B29) — cùng file `VMSensorsInfo.kt`, sửa 1 lần, thêm test luôn.
- B31 (FragmentResultListener) nên làm riêng, đụng tới luồng VIP — cần test kỹ regression banner/badge VIP.
- B27 (core count) cần đối chiếu kỹ với native `cpuinfo_get_processors_count()` trước khi đổi — không phải 1-dòng, nên tách task riêng dù effort nhỏ.
