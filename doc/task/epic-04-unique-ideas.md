# Epic 4 — Tính năng độc quyền (USP / signature differentiator)

> Mục tiêu: tính năng chưa app nào trong phân khúc "system info" làm, tận dụng lợi thế kỹ thuật riêng (native `cpuinfo` lib đang under-used, hệ VIP có sẵn, `SystemInfoExporter` có sẵn khung).

## 🏆 Top pick (3 nguồn AI độc lập cùng tự đề xuất — tín hiệu rất mạnh)

### U01 — "Device Truth Score" / Chip authenticity check / "Silicon Detective"
Đối chiếu thông tin SoC thật từ native `cpuinfo` (microarchitecture, ISA, core topology, và sâu hơn: thanh ghi phần cứng `MIDR`/`MPIDR`/`REVIDR`/CPU Part number — đọc trực tiếp tầng Native, bỏ qua hoàn toàn chuỗi giả mạo trong `Build.MODEL`/`/proc/cpuinfo`) với `Build` fields, ABI, GPU/Vulkan, camera metadata → phát hiện firmware giả spec, chip dựng/re-marked, máy refurb/xách tay khai sai model, xác định silicon stepping/revision. Xuất bằng chứng từng điểm mismatch cụ thể, không chỉ 1 điểm số mơ hồ.
**Giá trị**: cực cao cho thị trường mua bán máy cũ — không competitor nào (CPU-Z, AIDA64) làm sâu tới mức "phát hiện giả mạo bằng thanh ghi phần cứng", họ chỉ hiển thị info bề mặt.
**Nguồn**: Codex (P0, "Device Truth Score") + Claude CLI ("Chip authenticity check") + Gemini CLI ("Silicon Detective — SoC Binning Verification") — **3 nguồn độc lập, không thấy bài nhau, cùng hội tụ về đúng 1 ý tưởng cốt lõi**.
**Effort**: L (cần nghiên cứu kỹ database chipset thật vs khai báo + đọc thanh ghi native).

### U02 — Throttling Fingerprint / Freq-vs-temp curve / "Hardware Degradation Audit"
Chạy workload ngắn có kiểm soát (Gemini CLI đề xuất cụ thể: stress test 60s đa luồng native), ghi xung từng cluster + thermal status + pin theo thời gian thực → "đường cong chịu tải" so sánh được trước/sau khi thay pin, update ROM, thay keo tản nhiệt. So sánh tỷ lệ suy giảm hiệu năng Cold state vs Throttled state, xuất "Hardware Health Card" đồ hoạ có thể chia sẻ (watermark VIP). Tận dụng infra `ObservableCpuData` polling đã có sẵn (Epic 2 Story 6, T2.18) + `feat/temp`.
**Giá trị**: chưa app nào trong phân khúc trực quan hoá tương quan freq-temp tốt, và "chứng nhận sức khỏe phần cứng" chia sẻ được là hook viral tự nhiên.
**Nguồn**: Codex ("Throttling Fingerprint") + Claude CLI ("Freq-vs-temp correlation live graph") + Gemini CLI ("VIP Hardware Degradation & Thermal Throttling Audit") — **3 nguồn độc lập cùng ý tưởng cốt lõi**.
**Effort**: M-L. Lịch sử dài hạn có thể là tính năng VIP-only (monetization hook tự nhiên).

### U12 — "AI Readiness Score" (mới, Gemini CLI)
Tổng hợp các cờ tập lệnh AI mà `libcpuinfo` đã có sẵn nhưng chưa khai thác (`cpuinfo_has_arm_i8mm`, `cpuinfo_has_arm_bf16`, `cpuinfo_has_arm_neon_dot`, `cpuinfo_has_arm_sve/sve2`) cùng RAM khả dụng + số core hiệu năng cao → chấm điểm "máy chạy được on-device LLM cỡ nào" (vd "Đạt chuẩn chạy LLM 3B" / "Đạt chuẩn xử lý ảnh GenAI"). Dùng chung nền tảng dữ liệu với F10 (Epic 3, NPU/AI Capability Detection).
**Giá trị**: đúng xu hướng on-device AI 2025-2026 (Gemini Nano và tương tự), chưa app system-info nào trên Play Store có chỉ số này — timing tốt.
**Nguồn**: Gemini CLI (1 nguồn, nhưng dữ liệu nền `libcpuinfo` cực kỳ cụ thể và đã sẵn có, effort MVP thấp vì F10 đã build data layer).
**Effort**: S-M nếu làm sau F10 (tái dùng data layer).

## Ý tưởng khác

| # | Ý tưởng | Mô tả ngắn | Nguồn |
|---|---|---|---|
| U03 | Hardware Diff / Before-After Snapshot | Ký + lưu snapshot cấu hình, sau OTA/repair tự highlight thay đổi (security patch, camera ID, sensor vendor, DRM level, governor). Share qua exporter sẵn có | Codex |
| U04 | Privacy-preserving Fleet Compare | Fingerprint làm mờ identifier, so với cùng model/SoC → percentile nhiệt/xung/RAM khả dụng. Opt-in rõ ràng, không gửi GAID/serial | Codex |
| U05 | "Can My Device?" rule engine | Trả lời câu cụ thể: "phát Netflix HD?", "quay RAW?", "chạy game Vulkan?" — dựa capability thực tế (codec/Widevine/HDCP/HDR/Vulkan). Rule pack update từ xa, bản nâng cao cho VIP | Codex |
| U06 | CPU topology visualizer | Cache hierarchy (L1/L2/L3, shared/private), core cluster mapping (big.LITTLE/DynamIQ), ISA extension list (NEON/SVE/dotprod) — native `cpuinfo` lib đã có data này, hiện chỉ lấy tên chip, chưa khai thác | Claude CLI |
| U07 | VIP diagnostic report lịch sử | Lưu report theo mốc thời gian (hiện `SystemInfoExporter` chỉ session-only) — VIP xem trend hao mòn pin/hiệu năng qua nhiều tháng. Monetization hook tận dụng VIP system có sẵn | Claude CLI |
| U08 | QS tile family mở rộng / "Smart Combo Tile" | Thêm tile battery health / network signal, tận dụng infra `cputile`/`ramtile` sẵn có. Gemini CLI đề xuất thêm: gộp thành 1 tile cho chọn chỉ số ưu tiên (CPU/RAM/nhiệt độ) + màu Material You động — chi phí thấp, giá trị tiện dụng tức thì | **[đồng thuận 2 nguồn]** Claude CLI + Gemini CLI |
| U09 | VIP streak — daily check-in | Mở app N ngày liên tiếp → +giờ VIP miễn phí (rewarded ad tuỳ chọn nhân đôi). Dùng lại `VipPrefs`/`AdManager.activateVipByKey` sẵn có. Đánh vào D1/D7 retention — hiện app chưa có cơ chế nào | scan ads/infra |
| U10 | "Shield Score" — Device Health Score badge | Gộp RAM/Storage/Battery hiện có thành 1 điểm 0-100 hiển thị badge cạnh icon VIP toolbar; bấm mở bottom sheet gợi ý optimize + rewarded-ad CTA hợp lệ (chỉ trigger khi user chủ động bấm) | scan ads/infra |
| U11 | VIP "gift a day" share loop | VIP user generate mã 1 lần tặng bạn +1 ngày VIP (giới hạn 1 mã/ngày) → chia sẻ qua Intent.ACTION_SEND có sẵn. Tạo viral loop nhẹ cho app read-only vốn không có social feature | scan ads/infra |

---

## ✅ Quyết định đã chốt (2026-08-28)

**Làm cả 3 hướng, kết hợp**: U01 + U02 + (U09+U10). Thứ tự đề xuất theo effort tăng dần, tận dụng lại infra của bước trước cho bước sau:

1. **U09 + U10 trước** (VIP streak + Shield Score) — effort thấp nhất, tái dùng VIP infra + data provider có sẵn 100%, ship được trong 1 sprint nhỏ ngay sau khi Epic 1+2 ổn định. Tạo baseline retention/monetization trước khi đầu tư feature nặng.
2. **U02 tiếp theo** (Throttling Fingerprint) — effort trung bình, tái dùng `ObservableCpuData` polling infra đã có (sau khi tối ưu ở T2.18). Có thể xuất "Hardware Health Card" chia sẻ được — vòng lặp viral nhẹ giống U11.
3. **U01 sau cùng** (Device Truth Score / Silicon Detective) — effort lớn nhất, cần nghiên cứu database chipset kỹ, nhưng giá trị kinh doanh dài hạn cao nhất và có thể tái dùng phần data thu thập được từ U02 (đọc thanh ghi/topology).
4. **U12 (AI Readiness Score) cân nhắc chèn song song U01** vì dùng chung nền `libcpuinfo` flags chưa khai thác — nếu build F10 (Epic 3) trước, U12 gần như free add-on.

**Không phải đợi hết Epic 1+2 mới bắt đầu U09/U10** — 2 ý tưởng này không đụng vùng code đang tech-debt (Applications/Storage), có thể xen kẽ ngay khi rảnh 1-2 ngày giữa các sprint bugfix.
