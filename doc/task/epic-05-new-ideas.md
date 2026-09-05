# Epic 5 — Ý tưởng mới (brainstorm sau khi U01-U35/F01-F12/quick_win/tech-debt đều đã done)

> Tạo ngày 2026-09-05. Backlog cũ (epic-03, epic-04, quick_win) đã cạn hoàn toàn. Nguồn: 1 subagent
> map lại chính xác toàn bộ feature hiện có (tránh đề xuất trùng) + 3 subagent brainstorm độc lập,
> mỗi agent 1 lăng kính riêng (gap vs competitor / signature differentiator / privacy-security-power-user),
> không thấy bài nhau. Khác epic-04 (không có hội tụ 3 nguồn trùng ý tưởng lần này), nhưng cụm
> "Truth series" (#1-#4) tự hội tụ nội tại — cùng 1 agent nhận ra 4 ý nối tiếp đúng mạch sản phẩm
> đã có (U01 Device Truth Score, U02 Throttling Fingerprint).

## 🏆 Top pick — "Truth Series" mở rộng (nối mạch U01/U02 đã có)

### E01 — Storage Truth / Fake Capacity Detector
Sparse-pattern write/read/verify trên toàn dải dung lượng khai báo (kiểu H2testw, không cần fill hết) để phát hiện flash giả báo sai dung lượng (128GB thật ra chỉ 8GB) — lừa đảo phổ biến trên máy xách tay/refurb. Quét nhanh free, quét sâu (lưới dày hơn) VIP-gated + thẻ chia sẻ "Genuine/Fake Storage".
**Giá trị**: viral cao vì bắt được gian lận thật, tiền thật. **Effort**: M.

### E02 — Sensor Truth Audit
Đo tốc độ lấy mẫu/jitter/noise floor thật của từng sensor (accelerometer/gyro/proximity/light) qua timestamp `SensorEventListener`, so với thông số driver khai — phát hiện máy giá rẻ giả gyro (suy ra từ accelerometer) hoặc không đạt sample rate quảng cáo. Không cần permission mới.
**Giá trị**: mở rộng đúng mạch "phát hiện giả mạo phần cứng" của U01 sang domain sensor. **Effort**: M.

### E03 — RAM Truth / Virtual-Swap RAM Detector
Test độ trễ dạng "latency-cliff" (truy cập tuần tự vượt quá RAM vật lý) để tìm điểm gián đoạn băng thông/độ trễ đánh dấu ranh giới RAM thật vs RAM ảo (swap trên storage) — bắt đúng chiêu marketing "8GB+8GB mở rộng" phổ biến ở máy giá rẻ 2025-2026. Tái dùng nguyên primitive đọc/ghi của `rambench` (U16).
**Giá trị**: timing tốt, đúng scam đang phổ biến. **Effort**: M.

### ✅ E04 — Silicon Lottery / Per-Core Binning Certificate — Đã xong (2026-09-05)
Mở rộng infra `sched_setaffinity` đã xây cho U31: pin từng core riêng lẻ trong 1 cụm, so sánh tần số/throughput bền vững — các core "giống hệt" cùng die vẫn có thể khác biệt do binning. Ra thẻ chia sẻ "core khoẻ nhất/yếu nhất máy bạn".
**Giá trị**: rẻ vì tái dùng gần hết infra U31. **Effort**: S.

**Đã làm**: package mới `feat/siliconlottery/{SiliconLotteryBenchmark,SiliconLotteryRunner,VMSiliconLottery,SiliconLotteryScreen,FrmSiliconLottery}.kt`, tab mới `SILICON_LOTTERY_POS=21` ngay sau Cluster Bench. Đúng khuôn `ClusterBenchmarkRunner`: mỗi core đo tuần tự (không song song, tránh nhiễu nhiệt/scheduling), dedicated single-thread executor pin đúng 1 core, tạo/huỷ trong đúng phạm vi đo 1 core (tránh affinity leak sang coroutine khác dùng chung thread pool). Số core lấy từ `DataProviderCpu.getNumberOfCores()` (nguồn đã fix bug B27, không dùng `Runtime.availableProcessors()` đếm thiếu core big bị power-collapse). Model thuần `SiliconLotteryBenchmark` có `strongest()`/`weakest()`/`spreadPercent()` (% chênh lệch core mạnh nhất/yếu nhất) — tái dùng `BenchmarkSafety` (từ sweep tech-debt cùng ngày) cho ngưỡng an toàn nhiệt.

**Tận dụng ngay hạ tầng test vừa mở khoá cùng ngày**: `DataNativeProviderCpu`/`setThreadAffinity()` vừa được đổi thành `open` (để vá gap test cho `ClusterBenchmarkRunner`) — `SiliconLotteryRunnerTest` (5 case) dùng lại đúng kỹ thuật fake-subclass đó, là JVM unit test thật ngay từ ngày đầu, không cần đợi thêm 1 sweep sau như `ClusterBenchmarkRunner` đã từng phải chờ. `SiliconLotteryBenchmarkTest` (8 case) phủ đúng `strongest`/`weakest`/`spreadPercent` mọi edge case (rỗng, 1 core, core mạnh nhất đo 0 ops, các core bằng nhau).

**Bug thật phát hiện lúc smoke test tay trên TECNO KJ7**: `SiliconLotteryScreen`'s `DoneContent` copy nguyên cấu trúc `Column` không cuộn từ `ClusterBenchScreen` — với Cluster Bench chỉ 2-3 cụm nên vừa màn hình, nhưng Silicon Lottery có tới 8 hàng (1/core) khiến nút "Chia sẻ"/"Xong" bị đẩy khỏi màn hình, không bấm được. Sửa bằng `Modifier.verticalScroll(rememberScrollState())` trên `Column` ngoài cùng — chỉ sửa trong `SiliconLotteryScreen.kt`, không đụng `ClusterBenchScreen.kt` vì không có cùng bug (chưa đủ nội dung để tràn).

**Test**: `testDevDebugUnitTest` 449/449 pass (13 test mới: `SiliconLotteryBenchmarkTest` x8, `SiliconLotteryRunnerTest` x5). Widget test `SiliconLotteryScreenTest` (7 test, gồm case affinity warning + ẩn dòng spread khi chỉ 1 core) — 7/7 pass trên TECNO KJ7 (không chạy được trên Pixel 7 Pro do lỗi harness `InputManager.getInstance` đã biết từ trước, không liên quan feature này). Integration `SiliconLotteryRunnerInstrumentedTest` (real device, real core count, real `sched_setaffinity`) — 1/1 pass trên TECNO KJ7, log thật cho thấy đúng hiện tượng "silicon lottery": core 0-5 (cụm Tiết kiệm điện + phần cụm Hiệu năng) ~42K-53K ops/s, core 6-7 ~194K-205K ops/s. **Smoke test tay thật trên TECNO KJ7** (Pixel 7 Pro bị ngắt kết nối giữa phiên — theo yêu cầu user chỉ dùng TECNO cho phần còn lại): chạy full "Bắt đầu test" 8 core (~12s) → Done hiện đúng "Chênh lệch nhân mạnh nhất/yếu nhất: 93,9%", "Nhân 0 ▼ yếu nhất: 69.281 ops/s" (đỏ), "Nhân 7 ★ mạnh nhất: 1.140.377 ops/s" (xanh lá) — khớp chính xác phân cụm Tiết kiệm điện (nhân 0-1, chậm hẳn) vs Hiệu năng (nhân 2-7); bấm "Chia sẻ" mở đúng share sheet, nội dung khớp; không crash, không quảng cáo che UI trong suốt quá trình (đã dừng đúng 1 lần theo R4 khi thấy App Open test ad lúc khởi động app, đợi xác nhận rồi tiếp tục).

## Ý tưởng khác — gap vs competitor (CPU-Z/AIDA64/Device Info HW)

| # | Ý tưởng | Mô tả ngắn | API/nguồn | Permission | Effort |
|---|---|---|---|---|---|
| E05 | Haptics/Vibrator Hardware Profile | `hasAmplitudeControl()`, primitive support (CLICK/TICK/THUD), resonant frequency/Q-factor (API 33+) | `Vibrator`/`VibratorManager` | Không | S |
| E06 | Biometric Hardware Inventory | Class biometric (STRONG/WEAK/DEVICE_CREDENTIAL) theo `BiometricManager`, cờ FEATURE_FINGERPRINT/FACE/IRIS — không đụng dữ liệu sinh trắc thật | `BiometricManager`/`PackageManager` | Không | S |
| E07 | Thermal Zone Full Sensor Explorer | Glob toàn bộ `/sys/class/thermal/thermal_zone*` thay vì 4 đường dẫn cứng hiện tại — 15-30 zone thật trên SoC hiện đại (skin/GPU/modem/camera/VR/battery) | sysfs | Không | S |
| E08 | Foldable / Multi-Display & Hinge Posture | `FoldingFeature` (góc bản lề/hướng/che khuất) qua `androidx.window`, `DisplayManager.getDisplays()` cho màn hình phụ (USB-C DP, Chromecast) | Jetpack WindowManager (dep mới) | Không | M |
| E09 | GNSS Satellite Signal Diagnostic | Vệ tinh nhìn thấy/dùng để định vị, C/N0 từng vệ tinh, breakdown chòm sao (GPS/GLONASS/Galileo/BeiDou/QZSS/NavIC), dual-frequency capability | `GnssStatus.Callback`, `GnssCapabilities` (API 13+) | **ACCESS_FINE_LOCATION** (đã có tiền lệ consent flow ở tab Network) | L |
| E10 | Storage Filesystem & Volume Deep Detail | Loại filesystem qua `/proc/mounts`/`StatFs`, liệt kê toàn bộ volume (`StorageManager.getStorageVolumes()`) thay vì chỉ 1 đường dẫn SD card đoán sẵn | `StorageManager`/sysfs | Không (stat-only) | S-M |
| E11 | Interactive Touchscreen Diagnostic | Lưới đa điểm chạm sống + áp lực/kích thước từng điểm, tốc độ lấy mẫu từ `getHistoricalEventTime()`, số điểm chạm tối đa qua `InputDevice.getMotionRanges()` | Input API thuần | Không | M |

## Ý tưởng khác — privacy/security/power-user

| # | Ý tưởng | Mô tả ngắn | API/nguồn | Permission | Effort |
|---|---|---|---|---|---|
| E12 | Notification Listener & Accessibility Service Audit | App nào đang giữ quyền `BIND_NOTIFICATION_LISTENER_SERVICE`/`BIND_ACCESSIBILITY_SERVICE` — vector spyware/stalkerware phổ biến, ít ai tự kiểm tra | `NotificationManager.getEnabledListenerPackages()`, `Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES` | Không | S |
| E13 | Default App Role Inventory | App nào đang giữ role nhạy cảm (SMS/dialer/assistant/browser/launcher/autofill mặc định) — phát hiện default bị chiếm sau malware | `RoleManager` (API 29+) | Không | S |
| E14 | Device Admin / MDM Policy Transparency | Có Device Admin/Owner/Profile Owner đang active không, chính sách gì (camera/screen-capture/USB-debug bị khoá) — quan trọng cho máy cũ nghi có MDM ẩn | `DevicePolicyManager`, `UserManager.getUserRestrictions()` | Không | S/M |
| E15 | Screen Lock & Biometric Enrollment Strength Report | Loại khoá màn hình đang cấu hình (none/pattern/PIN/password/biometric), độ mạnh biometric đã đăng ký — khác F04 (chỉ có phía hardware keystore, chưa có phía posture user đã cấu hình) | `KeyguardManager`, `BiometricManager` | Không | S |
| E16 | Input Method (IME) Inventory | Danh sách bàn phím đang bật/đang chọn, bàn phím bên thứ 3 nào xin quyền Internet — rủi ro keylogger kinh điển | `InputMethodManager` | Không | S |
| E17 | VPN/Proxy Active-Connection Indicator | Traffic đang qua VPN/proxy HTTP không, transport mạng mặc định là gì | `ConnectivityManager.getNetworkCapabilities()`, `getDefaultProxy()` | ACCESS_NETWORK_STATE (thường đã có) | S |
| E18 | Cleartext/Network Security Config Self-Audit | App này (và app khác qua flag legacy) có cho phép cleartext traffic không — thẻ minh bạch nhỏ | `ApplicationInfo.FLAG_USES_CLEARTEXT_TRAFFIC` | Không | S |
| E19 | Exported Content-Provider/Receiver Audit | Đếm/liệt kê app cài sẵn có provider/receiver `exported=true` không có permission guard — bề mặt tấn công khác F05 (F05 chỉ nhìn theo tên SDK) | `PackageManager` (GET_PROVIDERS/GET_RECEIVERS) | ⚠️ cần `QUERY_ALL_PACKAGES` — Play Store hạn chế mạnh, nên scope theo `<queries>` khai báo sẵn | M |

## Ý tưởng khác — Shield Score / gamification mở rộng

| # | Ý tưởng | Mô tả ngắn | Nguồn dữ liệu | Effort |
|---|---|---|---|---|
| E20 | Battery Replacement Forecast | Ngoại suy tuyến tính trên dữ liệu VIP Diagnostic History (U07) đã thu thập sẵn (cycle count theo thời gian) → dự đoán ngày/chu kỳ health tụt dưới 80%. Không thu thêm dữ liệu gì, thuần hồi quy. VIP-gated tự nhiên vì cần lịch sử U07. | `VipDiagnosticReportRepository` có sẵn | S |
| E21 | Reboot Stability Log | Mỗi lần mở app so `uptimeMillis`/boot time với lần ghi trước trong DataStore — đổi nghĩa là vừa reboot, ghi lại theo thời gian. Theo dõi "N lần reboot bất thường/tháng" — tín hiệu Shield Score chưa có (hiện chỉ tĩnh RAM/Storage/Battery). | `SystemClock`/`System.currentTimeMillis()` | S |

## Tổng kết & khuyến nghị

**Quick-win bundle** (S effort, 0 permission mới, rẻ nhất): E04 (Silicon Lottery, tái dùng gần hết infra U31) + E20 (Battery Forecast, thuần toán trên data có sẵn) + E21 (Reboot Log) + E05/E06/E07/E15/E16 (đọc info thuần, không permission).

**Truth Series bundle** (giá trị/viral cao nhất, đúng mạch sản phẩm): E01 + E02 + E03 — cùng 1 chủ đề "phát hiện gian lận phần cứng", có thể làm nối tiếp nhau như cách U01→U02 đã làm.

**Privacy/security bundle** (giá trị người dùng thật, phù hợp positioning "diagnostics"): E12 + E13 + E15 + E16 + E17 — toàn bộ 0 permission mới, phát hiện thật (stalkerware, default app hijack, keylogger).

**Cân nhắc kỹ trước khi làm**: E09 (cần ACCESS_FINE_LOCATION — permission mới đầu tiên kể từ Network tab), E19 (cần QUERY_ALL_PACKAGES — rủi ro Play Store review giống lý do Floating Overlay bị skip 2 lần, nên khả năng cao sẽ bị từ chối nếu đề xuất).
