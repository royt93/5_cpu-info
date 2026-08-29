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
| 6 | #5 | Network info screen | 1d | 📋 Picked — genuinely not started, no `feat/infor/network/` package exists |
| 7 | #4 | Battery health & analytics | 1d | 🟡 Partial — health status enum + basic battery fields already in `feat/infor/hardware/VMHardwareInfo.kt`; cycle count / charging-speed graph / dedicated tab not done |
| 8 | #1 | Floating system monitor overlay | 3–4d | 📋 Picked — genuinely not started, no `SYSTEM_ALERT_WINDOW`/overlay code anywhere |

> **2026-08-29 status sweep**: this doc predates the F/U-coded feature sprints (Sprint 3–14: VIP streak, Device Truth Score, Cluster Topology, Throttle Fingerprint, thermal status, AI Readiness, Hardware Snapshot, Sensor Test Suite, App Permission Inventory, Vulkan/GLES Detail, USB/BT Inspector, Fleet Compare — see git log). Verified against current codebase rather than assumed; #5 and #1 are the only items from this doc's original bundle still genuinely open.

**Skipped**: #2 Widget v2 (Android O+), #3 CPU stress test / benchmark.

**Đợt 3a hoàn tất** (#10 + #8 + #7 + #6 + #9) — đã ship, không rõ commit nào cụ thể (không track riêng lúc đó), xác nhận qua code hiện tại.
**Còn lại**: #5 (Network info, 1d, cần permission consent flow) và #1 (Floating overlay, 3-4d, signature feature rủi ro cao nhất).

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

## 🔴 #2 — RAM widget rewrite cho Android O+ (2 ngày)

**Mô tả**: Thay thế widget legacy vừa xoá (chỉ chạy Android < O) bằng implementation modern hỗ trợ Android 8+.

**Tech**:
- `AppWidgetProvider` + `WorkManager` periodic (min 15 phút) hoặc Foreground service cho realtime
- `JobScheduler` cho background refresh
- Quick action button "kill background apps" (permission `KILL_BACKGROUND_PROCESSES` đã có)
- Configurable update interval qua widget config activity

**Risk**: Android battery optimization aggresively kill widget services trên OEM khác nhau.

**Value**: 🌟🌟 user request phổ biến nhưng đã có alternative (third-party widget apps).

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

## 🟠 #4 — Battery health & analytics (1 ngày)

**Mô tả**: Mở rộng `feat/infor/hardware` (hiện chỉ có battery basic) thành tab riêng `feat/infor/battery`.

**Tech**:
- `BatteryManager.BATTERY_PROPERTY_*` (CHARGE_COUNTER, CAPACITY, ENERGY_COUNTER, CURRENT_NOW)
- Designed capacity vs current qua reflection `BatteryStats`
- Cycle count (Android 14+, `BatteryManager.BATTERY_PROPERTY_CYCLE_COUNT`)
- Charging speed graph 5–10 phút gần nhất (session-only, không persist)
- Health status enum (good/cold/overheat/dead/over_voltage/unspecified_failure)

**Risk**: Reflection vào `BatteryStats` có thể fail trên một số OEM; cycle count chỉ available Android 14+.

**Value**: 🌟🌟🌟 mọi user đều care về battery health.

---

## 🟠 #5 — Network info screen (1 ngày)

**Mô tả**: Tab mới `feat/infor/network`. Hoàn toàn không có hiện tại (chỉ có permission internet/wifi/network state).

**Tech**:
- WiFi: SSID, BSSID, IP v4/v6, gateway, DNS, signal strength (dBm), link speed, frequency (2.4/5/6 GHz), security (WPA2/WPA3), MAC randomization status — qua `WifiManager`
- Mobile: carrier, network type (5G NR/SA, LTE-A, etc.), signal strength, CellID, MCC/MNC — qua `TelephonyManager`
- VPN status, proxy — qua `ConnectivityManager.getNetworkCapabilities()`
- Live signal strength update qua `TelephonyCallback` (API 31+)

**Risk**: Một số info cần permission `READ_PHONE_STATE` / `ACCESS_FINE_LOCATION` (cho WiFi SSID Android 10+); cần UX consent.

**Value**: 🌟🌟🌟 gap rõ ràng nhất so với feature hiện có.

---

## 🟡 #6 — Camera hardware capabilities (0.5 ngày)

**Mô tả**: Tab mới `feat/infor/camera`. Read-only metadata, không cần CAMERA permission.

**Tech**:
- `CameraManager.getCameraIdList()` → loop
- `getCameraCharacteristics()` → focal length, sensor size, max resolution, supported video frame rates (60/120/240/960 fps), RAW capture, manual control, OIS, HDR support, lens facing

**Risk**: Minimal — read-only metadata API.

**Value**: 🌟🌟 user check trước khi mua phim/dùng tính năng video.

---

## 🟡 #7 — Display detail & refresh rate (0.5 ngày)

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

## 🟡 #8 — Media codec capabilities (0.5 ngày)

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

## 🟡 #9 — Export hardware report (0.5 ngày)

**Mô tả**: Generate JSON/HTML/Text từ tất cả `DataProvider*`, share intent.

**Tech**:
- `SystemInfoExporter.kt` đã có khung (utility class) — chỉ cần extend
- Pull data từ tất cả existing providers
- Format: JSON (machine-readable), HTML (printable), plain text
- `Intent.ACTION_SEND` → email/Telegram/save file (đã có `FileProvider` trong manifest)

**Risk**: Minimal — chỉ là format + share.

**Value**: 🌟🌟 user utility, hiện phải screenshot từng tab.

---

## 🟢 #10 — Widevine DRM level (~2 giờ)

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
