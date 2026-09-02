# Quick Wins — Feature Backlog

> Đề xuất feature mới cho cpu-info. **Sắp xếp theo khả thi tăng dần** — items ở **đầu** là phức tạp nhất (least feasible), items ở **cuối** là dễ nhất (most feasible / quick win).
>
> Mỗi mục đều **tách bạch** với feature đã có, không chồng lấn `feat/infor/{cpu,gpu,ram,sensor,storage,screen,hardware,android}`, `feat/app`, `feat/processes`, `feat/temp`, `feat/cputile`, `feat/ramtile`.

---

## 🎯 User picks (2026-05-16)

**Picked** (9/10, đề xuất execution order từ nhỏ → lớn):

| Order | # | Feature | Effort | Status |
|---|---|---|---|---|
| 1 | #10 | DRM/Widevine level | 2h | ✅ Implemented — `feat/infor/drm/` |
| 2 | #8 | Media codec capabilities | 0.5d | ✅ Implemented — `feat/infor/media/` |
| 3 | #7 | Display detail & refresh rate | 0.5d | ✅ Implemented — `feat/infor/screen/VMScreenInfo.kt` (HDR, refresh rate, wide color gamut) |
| 4 | #6 | Camera capabilities | 0.5d | ✅ Implemented — `feat/infor/camera/` |
| 5 | #9 | Export hardware report | 0.5d | ✅ Implemented — `util/SystemInfoExporter.kt` (JSON/Text via `ACTION_SEND`) |
| 6 | #5 | Network info screen | 1d | ✅ Implemented (2026-08-30, Sprint 15) — `feat/infor/network/` |
| 7 | #4 | Battery health & analytics | 1d | ✅ Implemented (2026-08-30, Sprint 17) — `feat/infor/battery/` |
| 8 | #1 | Floating system monitor overlay | 3–4d | ❌ Skipped (2026-08-30, user decision) — see rationale below |

> **2026-08-29 status sweep**: this doc predates the F/U-coded feature sprints (Sprint 3–14: VIP streak, Device Truth Score, Cluster Topology, Throttle Fingerprint, thermal status, AI Readiness, Hardware Snapshot, Sensor Test Suite, App Permission Inventory, Vulkan/GLES Detail, USB/BT Inspector, Fleet Compare — see git log). Verified against current codebase rather than assumed; #5 and #1 were the only items from this doc's original bundle still genuinely open at that point.

**Skipped**: #3 CPU stress test / benchmark. **#2 Widget v2 đã làm xong (2026-09-01)** — xem chi tiết bên dưới.

**❌ #1 Floating overlay — skipped, 2026-08-30**: no code was written for this item (research/exploration only, via read-only `grep`/`find` — nothing to revert). Reason: this app is a live, published Play Store app (per root `CLAUDE.md`). The feature needs two "special" surfaces Play Store reviews strictly —
> - `SYSTEM_ALERT_WINDOW` ("draw over other apps"), and
> - a foreground service that (from Android 14/API 34) must declare `foregroundServiceType="specialUse"` with a `PROPERTY_SPECIAL_USE_FGS_SUBTYPE` justification in the manifest, which Play Console then requires a written justification for at submission time.
>
> Presented this tradeoff to the user (build-and-accept-review-risk vs. skip) — user chose to skip rather than take on Play Console review/rejection risk for a differentiator feature. Revisit if the product decides the signature-feature value is worth that review overhead; the tech plan below is still valid, just not started.

**Đợt 3a hoàn tất** (#10 + #8 + #7 + #6 + #9) — đã ship, không rõ commit nào cụ thể (không track riêng lúc đó), xác nhận qua code hiện tại.
**#5 hoàn tất** (Sprint 15, 2026-08-30) — Network Info tab, permission consent flow đầu tiên của app.
**#4 hoàn tất** (Sprint 17, 2026-08-30) — tab Battery riêng, thay hẳn phần battery basic cũ trong Hardware tab.
**Còn lại thật sự mở**: #1 (đã skip, xem lý do trên).

---

## 🔴 #1 — Floating system monitor overlay (3-4 ngày)

**Mô tả**: Bubble nổi hiển thị real-time CPU/RAM/temp/network speed trên top mọi app.

**Tech**:
- `SYSTEM_ALERT_WINDOW` permission (Android 8+ cần user grant qua Settings)
- Foreground service với notification persistent
- Toggle qua Quick Settings Tile (đã có infrastructure `ServiceCpuTile`/`ServiceRamTile`)
- UX: drag-move, resize, hide gesture, snap-to-edge

**Risk**: ANR nếu update quá tần suất; battery drain; quirks per OEM (Xiaomi, Vivo có blocking).

**Value**: 🌟🌟🌟 signature differentiator — competitors như CPU-Z Android không có. **Lý do đặt ở top**: scope lớn nhất, không phải pure-API, UX phức tạp.

---

## ✅ #2 — RAM widget rewrite cho Android O+ (2 ngày) — Đã xong (2026-09-01)

**Mô tả**: Thay thế widget legacy vừa xoá (chỉ chạy Android < O) bằng implementation modern, minSdk=24 trở lên (bỏ qua ràng buộc "O+" gốc — không còn cần thiết khi dùng WorkManager thay vì JobScheduler/foreground service trực tiếp).

**Đã làm**: `feat/ramwidget/RamWidgetProvider.kt` (AppWidgetProvider) + `RamWidgetUpdateWorker.kt` (CoroutineWorker, WorkManager periodic 15 phút — floor của WorkManager). Nút "Dọn RAM" tái dùng `RamCleanupAction`/`DataProviderRam` có sẵn (không qua Hilt — 2 class này constructor thuần framework, không đáng thêm `HiltWorkerFactory` vào `GalaxyApp`). Layout `widget_ram.xml` (RemoteViews chuẩn, không DataBinding/custom View), theme sáng/tối tự động qua `@color/surface`/`onSurface`/`accent` có sẵn.

**Đã bỏ khỏi scope gốc** (đơn giản hoá có chủ đích): "Configurable update interval qua widget config activity" — cố định 15 phút (floor WorkManager), không thêm activity cấu hình riêng — giá trị thấp so với effort, có thể thêm sau nếu có yêu cầu thật. "Foreground service cho realtime" — không cần, WorkManager đủ.

**Risk đã note đúng**: chỉ ảnh hưởng periodic refresh (15p), không ảnh hưởng nút "Dọn RAM" (chạy ngay khi tap, không phụ thuộc battery optimization).

**Verify**: unit test (WorkManager schedule/cancel + cleanup-rồi-refresh) + instrumented test (RemoteViews render đúng số liệu thật + edge case percentage>100) + smoke test thật trên TECNO — đặt widget lên home screen thật qua launcher picker, hiển thị đúng RAM sống (khớp tab RAM trong app), tap "Dọn RAM" xác nhận qua logcat GC thật chạy, xoá widget sạch không crash.

---

## 🟠 #3 — CPU stress test / mini benchmark (1.5 ngày)

**Mô tả**: Burn CPU N giây, score float ops/sec hoặc Coremark-style, compare với baseline.

**Tech**:
- Native C++ workload (đã có CMake setup, có thể nhúng vào `cpuinfo-libs`)
- Foreground service để không bị system kill mid-test
- Live freq graph trong khi chạy (qua `DataProviderCpu.getCurrentFreq`)
- Hardcoded baseline scores per chipset family

**Risk**: Battery drain warning từ Play Store; cần disclaimer; thermal throttling skew kết quả.

**Value**: 🌟🌟 differentiator vs read-only info app, gamify thiết bị.

---

## ✅ #4 — Battery health & analytics (1 ngày) — Đã xong

**Đã làm (Sprint 17, 2026-08-30)**: tab riêng `feat/infor/battery` (`FrmBatteryInfo`/`VMBatteryInfo`), thay hẳn phần battery basic cũ trong `feat/infor/hardware` (đã xoá `getBatteryStatus()`/`getBatteryHealthStatus()` khỏi `VMHardwareInfo`, cùng broadcast receiver `ACTION_POWER_CONNECTED/DISCONNECTED` không còn cần nữa).

**Tech đã dùng**:
- `BatteryManager.BATTERY_PROPERTY_*` (CHARGE_COUNTER, ENERGY_COUNTER, CURRENT_NOW) qua `getLongProperty`
- Designed capacity qua reflection `PowerProfile` có sẵn (`BatteryStatusProvider`, không phải `BatteryStats` như dự kiến ban đầu)
- Cycle count (Android 14+) — **sửa lại so với plan**: đây là extra `BatteryManager.EXTRA_CYCLE_COUNT` trên intent `ACTION_BATTERY_CHANGED`, không phải `BATTERY_PROPERTY_CYCLE_COUNT` (constant đó không tồn tại — phát hiện lúc build, compile lỗi "Unresolved reference")
- Poll mỗi 3s qua `viewModelScope` + coroutine (thay cho broadcast receiver) → charging state/current now/cycle count đều live, tự cancel khi ViewModel cleared, không leak
- Session min/max current: **scope hẹp hơn plan** — chỉ text (không phải graph vẽ đường) vì app chưa có chart component nào để tái dùng (xem F01), tự dựng 1 chart riêng cho 1 dòng dữ liệu không đáng effort
- Health status enum (good/cold/overheat/dead/over_voltage/unspecified_failure) — giữ nguyên logic cũ từ `VMHardwareInfo`
- Thêm luôn charging type "Wireless" (`BATTERY_PLUGGED_WIRELESS`) — code cũ chỉ nhận diện USB/AC

**Risk đã gặp**: không có OEM crash trong lúc test; charge counter/energy counter tự ẩn nếu thiết bị trả `Long.MIN_VALUE` (không hỗ trợ). **Bug thật phát hiện lúc smoke test trên máy thật (Galaxy S24 Ultra)**: `BATTERY_PROPERTY_CURRENT_NOW`/`CURRENT_AVERAGE` theo tài liệu AOSP trả về µA, nhưng máy này trả thẳng mA (đối chiếu `adb shell dumpsys battery` → `current now: 1039` khớp raw value app đọc được) — code ban đầu chia /1000 theo chuẩn µA nên hiển thị sai lệch 1000 lần (~1.0 mA thay vì ~1000 mA thật). Đã vá bằng `microAmpsToMa()` — raw value có biên độ dưới `ALREADY_MA_THRESHOLD` (20,000) được coi là đã ở đơn vị mA (dòng điện máy thật không bao giờ dưới ~10mA khi đang polling), ngược lại mới chia /1000. Verify lại trên máy thật: Current now đã hiển thị đúng 1021 mA (khớp `dumpsys`).

**Value**: 🌟🌟🌟 mọi user đều care về battery health.

---

## ✅ #5 — Network info screen (1 ngày) — Đã xong (Sprint 15, 2026-08-30)

**Mô tả**: Tab mới `feat/infor/network`. Hoàn toàn không có hiện tại (chỉ có permission internet/wifi/network state).

**Tech**:
- WiFi: SSID, BSSID, IP v4/v6, gateway, DNS, signal strength (dBm), link speed, frequency (2.4/5/6 GHz), security (WPA2/WPA3), MAC randomization status — qua `WifiManager`
- Mobile: carrier, network type (5G NR/SA, LTE-A, etc.), signal strength, CellID, MCC/MNC — qua `TelephonyManager`
- VPN status, proxy — qua `ConnectivityManager.getNetworkCapabilities()`
- Live signal strength update qua `TelephonyCallback` (API 31+)

**Risk**: Một số info cần permission `READ_PHONE_STATE` / `ACCESS_FINE_LOCATION` (cho WiFi SSID Android 10+); cần UX consent.

**Value**: 🌟🌟🌟 gap rõ ràng nhất so với feature hiện có.

---

## ✅ #6 — Camera hardware capabilities (0.5 ngày) — Đã xong

**Mô tả**: Tab mới `feat/infor/camera`. Read-only metadata, không cần CAMERA permission.

**Tech**:
- `CameraManager.getCameraIdList()` → loop
- `getCameraCharacteristics()` → focal length, sensor size, max resolution, supported video frame rates (60/120/240/960 fps), RAW capture, manual control, OIS, HDR support, lens facing

**Risk**: Minimal — read-only metadata API.

**Value**: 🌟🌟 user check trước khi mua phim/dùng tính năng video.

---

## ✅ #7 — Display detail & refresh rate (0.5 ngày) — Đã xong

**Mô tả**: Mở rộng `feat/infor/screen` thành tab riêng hoặc nested section. Bổ sung thông tin thiếu mà user 2026 quan tâm nhất.

**Tech**:
- Refresh rate hiện tại (live, có thể `Choreographer` callback) + max + supported modes (`Display.getSupportedModes()`)
- HDR capabilities (`Display.getHdrCapabilities()` → HDR10/HLG/Dolby Vision)
- Color gamut, color space (`Display.getColorMode()`)
- Pixel density độc lập per axis (xdpi, ydpi)
- Notch/cutout info (`DisplayCutout.getBoundingRects()`)
- Always-on display support

**Risk**: Minimal.

**Value**: 🌟🌟 user gaming/multimedia care nhiều.

---

## ✅ #8 — Media codec capabilities (0.5 ngày) — Đã xong

**Mô tả**: Tab mới `feat/infor/media`. Zero info codec hiện tại.

**Tech**:
- `MediaCodecList.REGULAR_CODECS` → list codec (HEVC, AV1, H.264, VP9, AAC, Opus)
- Max resolution/bitrate cho mỗi codec
- HW vs SW decoder (`MediaCodecInfo.isHardwareAccelerated()`)
- HDR profile support (HDR10, HDR10+, Dolby Vision)
- Encoder vs decoder

**Risk**: Minimal — pure API.

**Value**: 🌟🌟 user check trước khi mua phim/game.

---

## 🟡 #9 — Export hardware report (0.5 ngày) — Đã xong một phần

**Đã làm**: `SystemInfoExporter.kt` implement 2/3 format dự kiến — `Format.TEXT` và `Format.JSON`, chia sẻ qua `ACTION_SEND` (đúng plan), picker format qua `ExportFormatBottomSheet.kt`. **Còn thiếu so với mô tả gốc**: format HTML (printable) chưa có — enum `Format` chỉ có `TEXT`/`JSON`, chưa `HTML`.

**Mô tả gốc**: Generate JSON/HTML/Text từ tất cả `DataProvider*`, share intent.

**Tech**:
- `SystemInfoExporter.kt` đã có khung (utility class) — chỉ cần extend
- Pull data từ tất cả existing providers
- Format: JSON (machine-readable), HTML (printable), plain text
- `Intent.ACTION_SEND` → email/Telegram/save file (đã có `FileProvider` trong manifest)

**Risk**: Minimal — chỉ là format + share.

**Value**: 🌟🌟 user utility, hiện phải screenshot từng tab.

---

## ✅ #10 — Widevine DRM level (~2 giờ) — Đã xong (vượt scope gốc: security level + HDCP level + max HDCP)

**Mô tả**: Single-screen feature. Bottom-line item dễ làm nhất.

**Tech**:
- `MediaDrm.getPropertyString("securityLevel")` → L1/L2/L3
- HDCP version supported (qua `Display.getDeviceProductInfo()` Android 13+)
- Cho biết device có chơi Netflix HD/Disney+ HD được không (L1 = HD streaming OK)
- Có thể nest vào tab Media (#8) nếu làm cả 2

**Risk**: Zero — single API call.

**Value**: 🌟🌟 user streaming care; ROI cao nhất / effort.

---

## Tổng kết

| # | Feature | Effort | Distinct | Risk | Value |
|---|---|---|---|---|---|
| 1 | Floating overlay | 3–4d | ✅✅✅ | High (UX, OEM quirks) | 🌟🌟🌟 |
| 2 | Widget v2 (Android O+) | 2d | ✅ | Med (battery opt kill) | 🌟🌟 |
| 3 | CPU stress test | 1.5d | ✅✅ | Med (battery, thermal) | 🌟🌟 |
| 4 | Battery health | 1d | 🟡 (extend) | Low (reflection OEM) | 🌟🌟🌟 |
| 5 | Network info | 1d | ✅✅✅ | Low (permission UX) | 🌟🌟🌟 |
| 6 | Camera capabilities | 0.5d | ✅ | Zero | 🌟🌟 |
| 7 | Display detail | 0.5d | 🟡 (extend) | Zero | 🌟🌟 |
| 8 | Media codec | 0.5d | ✅ | Zero | 🌟🌟 |
| 9 | Export report | 0.5d | ✅ | Zero | 🌟🌟 |
| 10 | DRM/Widevine | 2h | ✅ | Zero | 🌟🌟 |

**Recommend bundle quick-wins** (#6 + #7 + #8 + #9 + #10): ≈ 2.5 ngày, 5 tab/feature mới hiển thị ngay, 0 permission rủi ro, 0 backend, fit hoàn hảo "CPU Info" positioning read-only.

**Recommend big bet**: #1 (floating overlay) — signature feature competitor không có, ROI dài hạn cao nhất.
