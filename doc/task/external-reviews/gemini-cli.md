# Báo cáo Đánh giá Độc lập Codebase CPU Info (`com.galaxyjoy.cpuinfo`)

> Đánh giá thực hiện trên nhánh `dev`. Phân tích chuyên sâu kiến trúc, logic hệ thống, native C++ lib, memory leak và tiềm năng sản phẩm. Không lặp lại các hạng mục đã ghi nhận trong `doc/task/feature.md` và `doc/task/quick_win.md`.

---

## 1. Bug / vấn đề cần fix

- **Crash Risk (Android 14+ / API 34+) — Thiếu cờ export khi đăng ký BroadcastReceiver:**
  - `app/src/main/java/com/galaxyjoy/cpuinfo/feat/app/FrmApplications.kt:131`: `requireActivity().registerReceiver(uninstallReceiver, intentFilter)`
  - `app/src/main/java/com/galaxyjoy/cpuinfo/feat/app/FrmNewApplications.kt:167`: `requireActivity().registerReceiver(uninstallReceiver, intentFilter)`
  - **Nguyên nhân & Hậu quả:** App khai báo `targetSdk = 37`. Từ Android 14 (API 34+), việc gọi `registerReceiver` với IntentFilter mà không chỉ định `RECEIVER_EXPORTED` hoặc `RECEIVER_NOT_EXPORTED` sẽ ném ngay lập tức `java.lang.SecurityException`, làm văng app khi người dùng mở màn hình Applications.
  - **Cách khắc phục:** Đổi sang dùng `ContextCompat.registerReceiver(requireActivity(), uninstallReceiver, intentFilter, ContextCompat.RECEIVER_EXPORTED)`.

- **Crash Risk — `ServiceRamTile` gửi Intent rỗng vào `startActivityAndCollapse`:**
  - `app/src/main/java/com/galaxyjoy/cpuinfo/feat/ramtile/ServiceRamTile.kt:116-134`: Tạo `val intent = Intent()` rỗng (không component, không action) rồi bọc vào `PendingIntent.getActivity()` và gọi `startActivityAndCollapse(pendingIntent)`.
  - **Nguyên nhân & Hậu quả:** Trên Android 14+, việc kích hoạt một PendingIntent chứa Intent rỗng sẽ quăng lỗi `ActivityNotFoundException: No Activity found to handle Intent { }`, gây crash TileService khi người dùng bấm vào Quick Settings Tile.
  - **Cách khắc phục:** Nếu muốn mở app thì truyền rõ Intent tới `ActHost::class.java`, nếu chỉ muốn đóng bảng thông báo thì không gọi `startActivityAndCollapse` với Intent rỗng.

- **Crash / Native SIGSEGV Risk — Null Pointer Dereference trong JNI C++:**
  - `app/src/main/cpp/cpuinfo-libs.cpp:26`: `return env->NewStringUTF(cpuinfo_get_package(0)->name);`
  - **Nguyên nhân & Hậu quả:** Nếu chạy trên máy ảo Android (Emulator), kiến trúc x86 lạ hoặc SoC chưa hỗ trợ, `cpuinfo_get_packages_count()` có thể bằng 0 và `cpuinfo_get_package(0)` trả về `nullptr`. Việc truy cập `->name` trực tiếp gây ra lỗi phân đoạn (SIGSEGV) làm sập tiến trình ở tầng Native. Ngoài ra, nếu `name` là null, `NewStringUTF(nullptr)` cũng gây crash JVM.
  - **Cách khắc phục:** Kiểm tra `cpuinfo_get_packages_count() > 0 && cpuinfo_get_package(0) != nullptr && cpuinfo_get_package(0)->name != nullptr` trước khi tạo chuỗi.

- **Crash Risk — `ListLiveData` gọi `setValue` từ background thread:**
  - `app/src/main/java/com/galaxyjoy/cpuinfo/feat/infor/sensor/VMSensorsInfo.kt:38-45` kết hợp `util/lifecycle/ListLiveData.kt:110-116`:
  - **Nguyên nhân & Hậu quả:** Trong `VMSensorsInfo.startProvidingData()`, `sensorManager.registerListener` được gọi trong coroutine `viewModelScope.launch(dispatchersProvider.io)`. Khi không truyền Handler, các sự kiện `onSensorChanged` sẽ bắn về trên chính IO thread. Khi đó `updateSensorInfo` gọi `listLiveData[updatedRowId] = ...`, bên trong `ListLiveData.set()` lại gọi trực tiếp `listStatusChangeNotificator.value = ...` (`MutableLiveData.setValue()`), gây crash `IllegalStateException: Cannot invoke setValue on a background thread`.
  - **Cách khắc phục:** Đảm bảo đăng ký sensor trên Main thread (hoặc chỉ định Main Handler), hoặc sửa `ListLiveData` sử dụng `postValue()` khi không ở main looper.

- **Crash Risk (IndexOutOfBoundsException) khi cập nhật Sensor:**
  - `app/src/main/java/com/galaxyjoy/cpuinfo/feat/infor/sensor/VMSensorsInfo.kt:73-74`:
    `val updatedRowId = sensorList.indexOf(event.sensor)`
    `listLiveData[updatedRowId] = Pair(...)`
  - **Nguyên nhân & Hậu quả:** Trên một số thiết bị tùy biến ROM, sensor instance trả về từ `SensorEvent` có thể không khớp `equals` với danh sách ban đầu khiến `indexOf` trả về `-1`. Việc gán `listLiveData[-1]` sẽ ném ra ngoại lệ `IndexOutOfBoundsException`.
  - **Cách khắc phục:** Kiểm tra `if (updatedRowId in 0 until listLiveData.size)`.

- **Logic Bug — Sai lệch nghiêm trọng thuật toán giải mã phiên bản Vulkan:**
  - `app/src/main/java/com/galaxyjoy/cpuinfo/data/provider/DataProviderGpu.kt:38-40`:
    ```kotlin
    val minor = vulkan shl 10 shr 22
    val patch = vulkan shl 20 shr 22
    ```
  - **Nguyên nhân & Hậu quả:** Quy chuẩn Vulkan API quy định: Major = bits 22-31, Minor = bits 12-21 (10 bit), Patch = bits 0-11 (12 bit). Phép toán dịch bit dùng `shr` có dấu (signed shift) và dịch sai số lượng bit (patch 12 bit dịch trái 20 rồi dịch phải 22 bit, làm mất 2 bit dữ liệu và làm sai giá trị). Ví dụ: Vulkan `1.3.250` sẽ bị hiển thị sai thành `1.3.62`.
  - **Cách khắc phục:** Sửa thành chuẩn:
    `val major = (vulkan ushr 22) and 0x3FF`
    `val minor = (vulkan ushr 12) and 0x3FF`
    `val patch = vulkan and 0xFFF`

- **Logic Flaw — Đếm sai số nhân CPU trên hầu hết smartphone Android hiện đại:**
  - `app/src/main/java/com/galaxyjoy/cpuinfo/data/provider/DataProviderCpu.kt:23-28` & `app/src/main/java/com/galaxyjoy/cpuinfo/domain/observable/ObservableCpuData.kt:25, 43`:
  - **Nguyên nhân & Hậu quả:** `DataProviderCpu.getNumberOfCores()` gọi `Runtime.getRuntime().availableProcessors()` trên API >= 17. Trên kiến trúc big.LITTLE / DynamIQ hiện nay, khi máy đang rảnh, các nhân lớn (Big/Prime) thường rơi vào trạng thái ngủ sâu (offline / power-collapsed). Khi đó `availableProcessors()` chỉ trả về số nhân đang online (ví dụ máy 8 nhân nhưng trả về 4 nhân). Vòng lặp lấy xung nhịp CPU chỉ lặp đến 4, UI hiển thị "4 Cores" và các nhân bị biến mất/nhấp nháy bất thường theo tải hệ thống.
  - **Cách khắc phục:** Sử dụng hàm native `cpuinfo_get_processors_count()` từ `libcpuinfo` có sẵn hoặc đọc file `/sys/devices/system/cpu/possible` (ví dụ "0-7" -> 8 nhân).

- **Hardcoded Bug — Tần số tối thiểu (Min Frequency) của CPU bị gán cứng bằng "0":**
  - `app/src/main/java/com/galaxyjoy/cpuinfo/feat/infor/cpu/CpuInfoEpoxyController.kt:59-63`:
    ```kotlin
    val minFreq = if (frequency.min != -1L) {
        context.getString(R.string.cpu_frequency, "0")
    } else { "" }
    ```
  - **Nguyên nhân & Hậu quả:** `DataProviderCpu.getMinMaxFreq()` đã đọc đúng tần số tối thiểu từ kernel và lưu vào `frequency.min`, nhưng Epoxy Controller lại bỏ qua và hardcode chuỗi `"0"`, khiến mọi nhân CPU đều hiển thị mức tối thiểu là "0Mhz".
  - **Cách khắc phục:** Đổi `"0"` thành `frequency.min.toString()`.

- **Kiến trúc sai lệch — Dùng `FragmentResultListener` liên Activity không có tác dụng:**
  - `app/src/main/java/com/galaxyjoy/cpuinfo/feat/ActHost.kt:103-110` và `app/src/main/java/com/galaxyjoy/cpuinfo/feat/vip/FVipManagement.kt:331-334`:
  - **Nguyên nhân & Hậu quả:** `ActHost` lắng nghe sự kiện `KEY_VIP_CHANGED` trên `supportFragmentManager` của `ActHost`. Trong khi đó, `FVipManagement` lại nằm trong một Activity hoàn toàn tách biệt là `ActVip`. Khi `FVipManagement` gọi `parentFragmentManager.setFragmentResult()`, sự kiện chỉ bắn trong `ActVip` chứ không bao giờ tới được `ActHost`. Sở dĩ banner/badge cập nhật được là do `ActHost.onResume()` vô tình chạy lại khi đóng `ActVip`.
  - **Cách khắc phục:** Giao tiếp qua `Activity Result API` (`registerForActivityResult`) khi khởi chạy `ActVip`, hoặc quan sát qua DataStore / Shared Flow singleton.

- **Crash Risk — Thiếu try-catch khi đọc StreamConfigurationMap của Camera:**
  - `app/src/main/java/com/galaxyjoy/cpuinfo/feat/infor/camera/VMCameraInfo.kt:83-85, 102-107`:
  - **Nguyên nhân & Hậu quả:** Gọi `describeStreams(map)` và truy xuất `highSpeedVideoSizes`, `getHighSpeedVideoFpsRangesFor(maxSize)!!` mà không bọc try-catch per-camera. Trên một số thiết bị có driver camera tùy biến (OEM HAL quirks như Samsung/Xiaomi), việc gọi các hàm này khi không hỗ trợ `CONSTRAINED_HIGH_SPEED_VIDEO` có thể quăng `IllegalArgumentException` hoặc `AssertionError`, làm crash app ngay từ khối `init` của ViewModel.
  - **Cách khắc phục:** Bọc toàn bộ quá trình đọc characteristics của từng camera trong khối try-catch độc lập.

- **Ngoại lệ chia cho 0 (Divide by Zero) trong `ServiceCpuTile`:**
  - `app/src/main/java/com/galaxyjoy/cpuinfo/feat/cputile/ServiceCpuTile.kt:45, 98`:
    `Pair(minFreq.sum() / cpuCount, maxFreq.sum() / cpuCount)` và `(sumFreq / cpuCount)`.
  - **Nguyên nhân & Hậu quả:** Nếu `cpuCount == 0`, phép chia ném `ArithmeticException: / by zero`. Đồng thời, khi nhân CPU offline, `getCurrentFreq` trả về `-1`, khiến `sumFreq` bị cộng dồn số âm và tính sai xung trung bình.
  - **Cách khắc phục:** Kiểm tra `if (cpuCount > 0)` và chỉ cộng các giá trị tần số `> 0`.

- **Rò rỉ File Descriptor do không đóng Stream an toàn:**
  - `app/src/main/java/com/galaxyjoy/cpuinfo/feat/infor/storage/StorageInfoViewModel.kt:193, 237`: Mở `FileInputStream("/proc/mounts")` và `dis.close()` thủ công ở cuối hàm không nằm trong `finally` hay `.use { }`. Nếu đọc file gặp lỗi, file descriptor sẽ bị leak.
  - `app/src/main/java/com/galaxyjoy/cpuinfo/util/Utils.kt:79-85`: Tương tự trong hàm `readOneLine()`.
  - `app/src/main/java/com/galaxyjoy/cpuinfo/feat/infor/hardware/VMHardwareInfo.kt:262-270`: Mở `RandomAccessFile("/sys/class/net/wlan0/address")` không có `finally` hoặc `.use { }`.

---

## 2. Cải tiến kỹ thuật (enhance)

- **Loại bỏ hoàn toàn Epoxy để gỡ `kapt` và dọn đường nâng cấp Kotlin 2.x:**
  - `com.airbnb.android:epoxy` đã bị chính Airbnb dừng phát triển và deprecated.
  - Hiện chỉ còn đúng 3 màn hình dùng Epoxy (`FrmCpuInfo`, `FrmGpuInfo`, `FrmRamInfo`), các màn hình khác đều đã dùng `BaseRvFragment` + standard RecyclerView Adapter.
  - Dự án đang phải gánh toàn bộ plugin `kotlin-kapt` cùng `epoxy-processor:5.1.3` chỉ vì 3 màn hình này. Migrate 3 màn hình này sang Compose hoặc standard `ListAdapter` sẽ giúp xoá bỏ `kapt` hoàn toàn khỏi `build.gradle.kts`, tăng tốc build ~35-40%, và tháo bỏ rào cản tương thích phiên bản Kotlin.

- **Dọn dẹp triệt để Dead Code bị bỏ quên trong repo:**
  - `feat/processes/` (`FrmProcesses.kt`, `ProcessesVM.kt`, `PsProvider.kt`, `AdtProcesses.kt`): Tính năng này bị ẩn 100% người dùng trên thực tế vì `ActHost.kt:88` ẩn tab trên Android > M (`minSdk` dự án là 24). Việc đọc `/system/bin/ps` cũng bị chặn bởi SELinux từ Android 7.0+. Cần xóa bỏ toàn bộ thư mục này để giảm size APK.
  - `feat/app/FrmNewApplications.kt`, `VMNewApplications.kt`, `AppScreen.kt`: Cụm Compose rewrite của Applications tab bị bỏ dở và không được cắm vào bất kỳ đâu. Cần quyết định hoàn thiện để thay thế `FrmApplications` cũ hoặc xoá bỏ để tránh phân mảnh.
  - `ext/Activity.kt`: Chứa hơn 350 dòng code comment rác từ năm 2017. Các hàm còn lại (`rateApp`, `moreApp`, `shareApp`) nằm ở default package (thiếu `package com.galaxyjoy...`).
  - `data/provider/DataProviderStorage.kt`: Là một class rỗng được inject vô nghĩa, toàn bộ logic đọc bộ nhớ bị nhồi vào `StorageInfoViewModel`.

- **Thống nhất kiến trúc State & Async (Coroutines Flow vs RxJava3):**
  - Codebase đang dùng song song 2 giải pháp bất đồng bộ: CPU, GPU, RAM dùng Kotlin Flow / Interactor pattern; Storage, Applications, Processes, Temperature lại dùng RxJava3 (`Single`, `Flowable`, `Observable`) lẫn lộn với Coroutines.
  - Migrate toàn bộ các ViewModel còn lại sang Coroutines Flow để xoá bỏ triệt để 2 thư viện nặng: `io.reactivex.rxjava3:rxjava` và `rxandroid`, cùng `InitializerRx`.

- **Tối ưu hóa chu kỳ Polling trong `ObservableCpuData`:**
  - `app/src/main/java/com/galaxyjoy/cpuinfo/domain/observable/ObservableCpuData.kt:21-64`: Vòng lặp `while(true) { delay(1000L) }` mỗi 1 giây lại gọi 7 hàm JNI Native (`getCpuName()`, `getAbi()`, `hasArmNeon()`, 5 loại Cache L1/L2/L3/L4).
  - Đây là các thông số phần cứng tĩnh không bao giờ thay đổi lúc runtime. Cần tách việc query thông số tĩnh ra làm 1 lần duy nhất (cache), trong vòng lặp 1 giây chỉ đọc `currentFreq` để tránh lãng phí CPU và pin.

- **Tối ưu nghẽn cổ chai IPC khi load danh sách Applications:**
  - `app/src/main/java/com/galaxyjoy/cpuinfo/feat/app/VMApplications.kt:73-100`: Duyệt qua toàn bộ ứng dụng cài đặt, với mỗi app gọi đồng bộ: `loadLabel()`, duyệt thư mục `nativeLibraryDir`, và gọi thêm `packageManager.getPackageInfo()` chỉ để lấy icon resource ID.
  - Khi máy cài 200+ app, việc này tạo ra hàng trăm lượt gọi Binder IPC gây đơ giao diện. Cần đọc theo batch, dùng Coroutines Dispatchers.IO và loại bỏ việc gọi lại `getPackageInfo` vì `ApplicationInfo` đã có sẵn.

- **Hợp nhất các hệ thống lưu trữ Preferences:**
  - Codebase đang phân mảnh 4 hệ thống lưu trữ cấu hình: `UserPreferencesRepository` (DataStore), `Prefs.kt` (Gson SharedPrefs), `VipPrefs.kt`, và `defaultSharedPreferences` trong `FrmSettings`.
  - `UserPreferencesRepository.setApplicationsSortingOrder` bị bỏ hoang không dùng, trong khi `VMApplications` lại đọc từ `Prefs.kt`. Cần quy tụ toàn bộ về Jetpack DataStore duy nhất.

- **Khắc phục lỗ hổng Unit Test coverage:**
  - Toàn bộ repo chỉ có 4 file unit test đơn giản, trong đó `DataProviderApplicationsTest` lại đi test một class không được dùng trong production code.
  - Cần bổ sung test cho: Logic tính toán xung nhịp CPU, `TemperatureProvider`, giải mã DRM/Widevine, Camera fallback, và logic whitelist VIP keys.

---

## 3. Feature mới nên làm
*(So sánh khoảng trống tính năng với CPU-Z, AIDA64, Device Info HW, CPU Monitor — loại trừ các tính năng đã nằm trong roadmap `quick_win.md`)*

- **Kiến trúc phân cụm CPU Topology & Cluster Layout (Prime / Performance / Efficiency):**
  - **Gap:** CPU-Z và Device Info HW phân chia cực kỳ rõ ràng cấu trúc vi xử lý (ví dụ Snapdragon 8 Gen 3: 1x Cortex-X4 @ 3.3GHz + 3x Cortex-A720 @ 3.15GHz + 2x Cortex-A720 @ 2.96GHz + 2x Cortex-A520 @ 2.27GHz).
  - **Giải pháp:** App hiện tại chỉ liệt kê phẳng từ Core 0 đến Core 7 mà không phân biệt được nhân nào là nhân tiết kiệm điện, nhân nào là nhân hiệu năng cao. Sử dụng `libcpuinfo` để nhóm các nhân theo cluster, hiển thị nhãn microarchitecture (Cortex-X, Cortex-A) và mức xung min/max của từng cụm.

- **Đồ thị cảm biến thời gian thực (Sensor Live Realtime Plotter):**
  - **Gap:** AIDA64 và Device Info HW có biểu đồ dạng sóng trực quan cho các cảm biến chuyển động và môi trường.
  - **Giải pháp:** Tab Sensors hiện tại chỉ hiển thị chuỗi văn bản tĩnh dạng `X=... Y=... Z=...` nhảy số liên tục gây khó theo dõi. Tích hợp biểu đồ waveform nhẹ (50 điểm mẫu gần nhất) cho Accelerometer, Gyroscope và Barometer.

- **Chi tiết phân tích ứng dụng chuyên sâu (App Inspector & Security Audit):**
  - **Gap:** Tab Applications của app hiện tại chỉ liệt kê tên và danh sách file `.so`. Trong khi Device Info HW cho phép mổ xẻ chi tiết từng app.
  - **Giải pháp:** Hiển thị thêm: Target SDK / Min SDK (phát hiện app cũ có nguy cơ bảo mật), ngày cài đặt & cập nhật lần cuối, danh sách Permissions nhạy cảm (Camera, Location, Microphone, Background Data), và cấu trúc Split APKs (Base + Config splits).

- **Nhận diện phần cứng AI & NPU / Deep Learning Capabilities:**
  - **Gap:** Xu hướng chip 2025-2026 tập trung mạnh vào NPU và xử lý AI cục bộ (Qualcomm Hexagon, MediaTek APU, Google Tensor TPU, Samsung NPU), nhưng các app info hiện nay đều bỏ trống phần này.
  - **Giải pháp:** Phát hiện sự hiện diện của NPU/TPU, kiểm tra phiên bản Android Neural Networks API (NNAPI) HAL, và các tập lệnh tăng tốc ma trận (như `ARM i8mm`, `ARM Dot Product`).

- **Thông số phần cứng Âm thanh (Audio Hardware & Hi-Res Output Capabilities):**
  - **Gap:** Tab Hardware hiện tại dùng code ALSA `/proc/asound` bị SELinux chặn hoàn toàn.
  - **Giải pháp:** Dùng Android `AudioManager` & `AudioDeviceInfo` hiện đại để liệt kê: Thiết bị xuất âm thanh (USB DAC, Bluetooth Codecs như LDAC, aptX HD, LHDC, AAC), hỗ trợ Hi-Res Audio (24-bit/192kHz), Spatial Audio, và Dolby Atmos capabilities.

---

## 4. Ý tưởng tính năng độc quyền (unique selling point)
*(Tính năng tạo sự khác biệt vượt trội, khai thác tối đa thư viện native `libcpuinfo` và hệ thống VIP có sẵn)*

- **"On-Device AI Readiness & Silicon Capability Score" (Đo lường năng lực chạy AI cục bộ):**
  - **Ý tưởng:** Năm 2025-2026 là kỷ nguyên AI trên thiết bị (Gemini Nano, on-device LLM). Người dùng rất tò mò liệu điện thoại của mình có đủ sức chạy các mô hình AI trực tiếp không cần mạng hay không.
  - **Lợi thế kỹ thuật riêng:** Thư viện native `libcpuinfo` (`cpuinfo.h`) đã có sẵn hàng loạt cờ tập lệnh AI chuyên sâu mà app chưa khai thác: `cpuinfo_has_arm_i8mm` (Int8 matrix multiplication), `cpuinfo_has_arm_bf16` (Bfloat16), `cpuinfo_has_arm_neon_dot` (Dot product), `cpuinfo_has_arm_sve / sve2` (Scalable Vector Extension).
  - **Cách triển khai:** Tổng hợp các tập lệnh phần cứng này cùng dung lượng RAM khả dụng và số nhân hiệu năng cao để chấm một điểm số trực quan: "AI Readiness Rating" (ví dụ: *Đạt chuẩn chạy On-Device LLM 3B / Đạt chuẩn xử lý ảnh GenAI*). Chưa có bất kỳ ứng dụng System Info nào trên Play Store cung cấp chỉ số này.

- **"VIP Hardware Degradation & Thermal Throttling Audit" (Đặc quyền VIP — Kiểm tra mức độ chai linh kiện & bóp xung nhiệt độ):**
  - **Ý tưởng:** Gia tăng giá trị thực cho người dùng VIP thay vì chỉ có quyền lợi tắt quảng cáo.
  - **Cách triển khai:** Một bài stress test ngắn 60 giây chạy workload native đa luồng được tối ưu riêng kết hợp đọc liên tục tần số CPU và nhiệt độ thermal zone:
    - Xác định sau bao nhiêu giây thì chip bị quá nhiệt và bóp xung (Thermal Throttling Rate).
    - So sánh tỷ lệ suy giảm hiệu năng giữa lúc máy mát (Cold state) và lúc nóng (Throttled state).
    - Xuất ra "Chứng nhận sức khỏe phần cứng" (Hardware Health Card) đồ họa đẹp mắt có watermark VIP để chia sẻ mạng xã hội.

- **"Silicon Detective — Phát hiện chip nhái / SoC Binning Verification":**
  - **Ý tưởng:** Vấn nạn máy dựng, chip giá rẻ đổi tên (re-marked SoC) hoặc máy xách tay bị can thiệp kernel build.prop rất phổ biến.
  - **Lợi thế kỹ thuật riêng:** `libcpuinfo` đọc trực tiếp thanh ghi vi xử lý phần cứng (`MIDR`, `MPIDR`, `REVIDR`, CPU Part number, Implementer ID) ở tầng Native C, hoàn toàn bỏ qua các chuỗi giả mạo trong `Build.MODEL` hay `/proc/cpuinfo`.
  - **Cách triển khai:** Đối chiếu thông số thanh ghi thực tế với cơ sở dữ liệu mẫu để xác thực: Chip này có đúng là Snapdragon / Dimensity chính hãng hay là chip dựng, chip thuộc phiên bản stepping nào (Silicon revision).

- **"Dynamic Quick Settings Tile & Adaptive Monitoring" (Nâng cấp hạ tầng Tile sẵn có):**
  - **Ý tưởng:** App đã có sẵn hạ tầng 2 Tile (`ServiceCpuTile` và `ServiceRamTile`).
  - **Cách triển khai:** Gộp và nâng cấp thành **Smart Combo Tile**: Cho phép người dùng tùy chọn hiển thị chỉ số quan tâm nhất ngay trên thanh cài đặt nhanh (CPU load, RAM trống, hoặc nhiệt độ Pin) với màu sắc tự động thích ứng Material You (Dynamic Color) theo ngữ cảnh hệ thống, hỗ trợ click nhanh để dọn RAM hoặc xem chi tiết.
