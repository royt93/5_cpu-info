# Code Review — CPU Info (com.galaxyjoy.cpuinfo)

> Review độc lập, chỉ đọc code. Đối chiếu doc/task/feature.md + doc/task/quick_win.md để không lặp ý cũ.

## 1. Bug / vấn đề cần fix

- **Native segfault risk** — `app/src/main/cpp/cpuinfo-libs.cpp:26` `getCpuName()` gọi `cpuinfo_get_package(0)->name` không check `cpuinfo_get_packages_count() > 0`. SoC lạ/exotic → `cpuinfo_get_package(0)` trả `nullptr` → deref crash native, không catch được từ Kotlin (crash cả process).
- **LiveData off-main-thread crash** — `feat/infor/sensor/VMSensorsInfo.kt:38-46` gọi `sensorManager.registerListener()` (overload 3-arg, không Handler) từ `viewModelScope.launch(dispatchersProvider.io)`. Callback `onSensorChanged` chạy thread không xác định → `updateSensorInfo()` set `ListLiveData` (→ `MutableLiveData.value=`) off-main-thread → `IllegalStateException` tiềm ẩn.
- **NPE risk** — `feat/cputile/ServiceCpuTile.kt:63-65` truy cập `qsTile.label/icon/updateTile()` trong loop `while(true)` không safe-call, trong khi `ServiceRamTile.kt` dùng `qsTile?.apply{}` nhất quán. Tile unbind giữa loop → NPE.
- **Duplicate subscription/leak** — `feat/temp/TemperatureVM.kt:55-74` `stopTemperatureRefreshing()` chỉ dispose `refreshingDisposable`, không dispose `temperatureDisposable` (chỉ dispose ở `onCleared`). Tab switch nhanh (onStart/onStop lặp trước khi scan xong) → overwrite field, leak subscription cũ + ghi trùng `prefs.insert(CPU_TEMP_RESULT_KEY,...)`.
- **FD leak + swallowed crash** — `feat/infor/storage/StorageInfoViewModel.kt:193-243` (`getExternalSDMounts`) mở `DataInputStream/FileInputStream` trên `/proc/mounts`, chỉ `close()` cuối try block, không `finally`/`use{}` → leak FD nếu exception giữa chừng. Dòng 222-230: `lineElements[1].lastIndexOf("/")` có thể trả `-1` → `substring(-1,...)` throw `StringIndexOutOfBoundsException`, bị catch ngoài nuốt mất, SD detection âm thầm fail.
- **Div-by-zero / NaN sweep** — `widget/arc/ArcProgress.java:182-187,293` nếu `setMax()` không gọi hoặc XML `arc_max="0"`, `getMax()`=0 → `onDraw` chia `progress/(float)getMax()` → NaN/Infinity truyền vào `canvas.drawArc`.
- **Công thức đảo ngược** — `widget/progress/IconRoundCornerProgressBar.java:116` `ratio = max/progress` (ngược `progress/max`), `progress==0` → Infinity/NaN, progressWidth âm thầm về 0 thay vì đúng logic.
- **Compose stale transition state** — `ui/component/DraggableBox.kt:36-40` `MutableTransitionState` build 1 lần trong `remember{}` không key, không đồng bộ với `isRevealed` live value; `@Suppress("UnusedTransitionTargetStateParameter")` che lint thay vì fix. Dùng trong `AppScreen.kt` swipe-reveal — risk animation stuck/sai trong LazyColumn reuse.
- **Copy-paste bug (preview only)** — `ui/component/CpuSwitchBox.kt:51` `CpuSwitchBoxPreview()` gọi nhầm `CpuCheckbox(...)` thay vì `CpuSwitchBox(...)`.
- **Doc/code drift ad lifecycle** — `doc/AD.MD` §7 nói đã xoá field `adView` + 3 hook thủ công, nhưng `feat/ActHost.kt:72,125,389` vẫn giữ `adView` + gọi `AdManager.bannerDestroy()` thủ công ở `onDestroy` và `applyVipBannerState()`. Không leak (đã null) nhưng doc sai so với thực tế.
- **Consent param bị bỏ qua** — `feat/SplashActivity.kt:39-46` `requestConsentInfoUpdate { canRequestAds -> ... }` không dùng giá trị `canRequestAds`, luôn chạy tiếp `runSplashFlow()` — implicit trust vào SDK tự xử lý, không assert ở app layer.
- **2 implementation trùng lặp** — `feat/app/FrmApplications.kt`+`VMApplications.kt` (View/Rx/Epoxy) và `FrmNewApplications.kt`+`VMNewApplications.kt` (Compose/StateFlow) cùng làm 1 feature "installed apps", cùng đăng ký uninstall receiver riêng. Cần xác nhận cái nào đang wire vào nav, xoá cái stale.

## 2. Cải tiến kỹ thuật (enhance)

- **Interactor/domain pattern dở dang** — `data/provider/DataProviderStorage.kt` là stub rỗng, không dùng ở đâu. `feat/infor/storage/StorageInfoViewModel.kt` bypass hoàn toàn domain layer, đọc trực tiếp `/proc/mounts`/`Environment`, vẫn dùng RxJava3 (Single/Schedulers) trong khi Cpu/Ram/Gpu/Applications đã migrate Coroutines+Interactor. 2 concurrency model coexist — Storage là gap rõ nhất, nên migrate hoặc xoá stub.
- **Ad init không theo AppInitializer pattern** — `GalaxyApp.kt` `setupAd()` (~27 dòng) inline thẳng trong `onCreate`, vi phạm convention `InitializersApp`/`AppInitializer` mà chính project đặt ra cho mọi init khác (Timber, Epoxy, Theme, Rx, NativeTools). Nên tách `AdInitializer`.
- **Ordering fragile** — `GalaxyApp.kt:26-27` `setupAd()` chạy trước `initializers.init(this)` (Timber chưa plant) — nếu AdManager sau này log qua Timber, log bị mất âm thầm.
- **Polling dư thừa** — `domain/observable/ObservableCpuData.kt:21-63` loop `while(true)` 1s refetch cả min/max freq (`DataProviderCpu.getCurrentFreq`/`getMinMaxFreq`) dù min/max cố định theo boot — nên cache 1 lần, chỉ poll current freq.
- **Custom view invalidate() override tốn kém** — `widget/arc/ArcProgress.java:133-137` và `widget/progress/BaseRoundCornerProgressBar.java:437-441`: override `invalidate()` để rebuild `Paint`/`GradientDrawable` mỗi lần gọi (tức mỗi setter: setProgress, setStrokeWidth,...) thay vì chỉ khi cần — gauge CPU/RAM live update tốn allocation liên tục.
- **Inconsistent cleanup** — `feat/infor/gpu/FrmGpuInfo.kt` không có `onDestroyView` override để null `glSurfaceView`/cancel `handler` như `BaseFrm.onDestroyView()` pattern chuẩn của project (khả năng benign vì GLSurfaceView tự stop, nhưng không nhất quán).
- **Material2 leftover** — `ui/component/TopAppBarView.kt` vẫn import `androidx.compose.material.TopAppBar` (M2) giữa codebase đang migrate M3.
- **multidex dư thừa** — `app/build.gradle.kts` `multiDexEnabled=true` + androidx.multidex dependency không cần thiết vì `minSdk=24` (D8/R8 native multidex từ API 21).
- **Compose BOM cũ** — `composeBom=2024.06.00` lệch khá xa so với `compileSdk/targetSdk=37` hiện tại (CLAUDE.md ghi compileSdk=36 — cũng lệch so với thực tế, nên đồng bộ doc).
- **buildSrc dead code** — `SigningConfig.kt` gần như không dùng (đã ghi trong CLAUDE.md, nhưng có thể xoá hẳn thay vì giữ làm cảnh).
- **Test coverage gap** — chưa có test cho: `DataProviderCpu`, native JNI layer (`cpuinfo-libs.cpp`), `UserPreferencesRepository`, toàn bộ `domain/observable` + `domain/action` + `domain/result` (ObservableCpuData/RamData/GpuData/ApplicationsData, RamCleanupAction, InteractorGetPackageName) — đây là layer mới migrate, đáng lẽ ưu tiên test hơn provider cũ.
- **Doc drift khác** — CLAUDE.md ghi `AdmobWrapper:1.1.1` nhưng TOML thực tế `1.1.5`; ghi compileSdk 36 nhưng thực tế 37. Nên cập nhật CLAUDE.md.

## 3. Feature mới nên làm

*(đã có trong quick_win.md: Network/Camera/Media/Display/Battery/Export/DRM — không lặp lại)*

- Storage read/write speed benchmark (sequential/random I/O) — phổ biến ở Device Info HW, CPU-Z.
- Thermal throttling monitor có lịch sử — theo dõi CPU freq drop theo nhiệt độ theo thời gian, khác `feat/temp` hiện tại (chỉ tức thời). Tận dụng sẵn `ObservableCpuData` polling infra.
- Sensor test suite tương tác (graph accelerometer/gyroscope real-time, proximity trigger, compass) thay vì chỉ list tĩnh như `feat/infor/sensor` hiện có.
- OpenGL ES/Vulkan capability chi tiết hơn (full extension list, Vulkan API level, compute shader support).
- Bootloader/security patch/SELinux status — phổ biến ở Device Info HW.
- Root detection / Play Integrity status hiển thị cho user (không chặn app).
- Bluetooth/NFC/USB-C capability chi tiết (BT version, codec aptX/LDAC, USB-C PD/DisplayPort Alt Mode).
- Benchmark result history/trend lưu theo thời gian (build trên #3 CPU stress test đã Skip trong quick_win — nếu làm lại thì nên có history ngay từ đầu).

## 4. Ý tưởng tính năng độc quyền (unique selling point)

- **CPU topology visualizer** — dùng native cpuinfo lib (đã link `libcpuinfo.a`, hiện chỉ lấy name) để show cache hierarchy (L1/L2/L3 size, shared/private), core cluster mapping (big.LITTLE/DynamIQ), ISA extension list (NEON/SVE/dotprod...). Native lib đã có data này, chưa khai thác — ít competitor show sâu tới mức topology graphic.
- **"Chip authenticity check"** — đối chiếu thông tin native cpuinfo (microarchitecture, ISA) với tên marketing claim để phát hiện device fake-spec/refurbished mislabeled — giá trị cao cho thị trường mua bán máy cũ/xách tay.
- **Freq-vs-temp correlation live graph** — kết hợp `feat/temp` + `ObservableCpuData` polling đã có sẵn, hiển thị throttling curve real-time — chưa app nào trong phân khúc làm tốt việc tương quan 2 chỉ số này trực quan.
- **Floating overlay (đã pick #1 quick_win)** — củng cố: đây vẫn là differentiator lớn nhất, kết hợp thêm freq-vs-temp graph ở trên sẽ mạnh hơn nữa.
- **VIP-tier "diagnostic report lịch sử"** — tận dụng VIP system sẵn có + Export feature (#9 quick_win): lưu report theo mốc thời gian (session-only hiện tại), cho VIP user xem trend hao mòn pin/hiệu năng qua nhiều tháng — monetization hook tự nhiên, chưa competitor free-tier nào làm.
- **QuickSettings tile family mở rộng** — đã có `cputile`/`ramtile`, có thể thêm tile riêng cho battery health/network signal, tận dụng infra tile sẵn có, chi phí implement thấp so với giá trị tiện dụng.
