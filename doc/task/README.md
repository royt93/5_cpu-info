# Task Backlog — cpu-info

## Chú giải trạng thái (mọi file trong `doc/task/`)

Mỗi task/item trong 4 file epic dưới đây luôn ở đúng 1 trong 3 trạng thái:

| Emoji | Trạng thái | Ý nghĩa |
|---|---|---|
| ✅ | **done** | Đã xong, đã verify (build/test/smoke test) |
| 🟡 | **inprogress** | Đã làm 1 phần, còn việc chưa xong (ghi rõ phần còn thiếu trong mô tả) |
| 📋 / ⏸️ / ❌ | **todo** | Chưa làm — 📋 = đã lên plan chi tiết sẵn sàng làm; ⏸️ = có ý định làm nhưng hoãn ưu tiên thấp hơn việc khác; ❌ = cân nhắc rồi quyết định không làm ở thời điểm hiện tại (ghi rõ lý do), vẫn tính là "todo" theo nghĩa có thể làm lại nếu quyết định đổi |

Task mới (feature ý tưởng, bugfix, tech debt...) luôn thêm vào đúng 1 trong 4 file epic dưới đây theo đúng loại việc — không tạo file rời rạc ở nơi khác trong `doc/`.

> Tạo ngày 2026-08-28. Nguồn: đọc toàn bộ source (151 file Kotlin) qua **4 subagent nội bộ** quét theo vùng (feat/infor + tiles/processes, data/domain/di, ads/infra/widget/ui, tests/CI/build) + **3 AI agent CLI độc lập** (Codex CLI, Claude CLI, Gemini CLI qua `agy`) mỗi agent đọc code từ đầu không thấy bài của nhau — dùng để đối chiếu chéo, item nào nhiều nguồn cùng phát hiện được đánh dấu **[đồng thuận]**, độ tin cậy cao hơn.
>
> Raw output của 3 CLI ngoài lưu ở `external-reviews/` (`codex.md`, `claude-cli.md`, `gemini-cli.md`) để tra cứu lại khi cần.
>
> **Không lặp lại** nội dung đã có ở [feature.md](feature.md) (đã Implemented đợt 1-2) và [quick_win.md](quick_win.md) (feature đã Picked đợt 3) — 2 file đó vẫn là nguồn sự thật cho phần đã quyết định trước đây. (Cả 2 file này chuyển từ `doc/` gốc vào `doc/task/` ngày 2026-09-02 theo yêu cầu gom mọi task vào 1 chỗ.)

## ✅ Quyết định đã chốt (2026-08-28)

1. **Thứ tự sprint**: (1) Bugfix crash-risk nhanh → (2) Tech debt/kiến trúc (Applications migration trước) → (3) Feature mới. Làm tuần tự, không xen kẽ trừ U09/U10 (xem dưới).
2. **Applications duplicate (B03)**: giữ bản Compose mới (`FrmNewApplications`), xoá bản View+RxJava cũ (`FrmApplications`). Làm ở Sprint 2, kèm fix bug crash Android 14+ mới phát hiện (B03b — thiếu `RECEIVER_EXPORTED` flag).
3. **Flagship USP (Epic 4)**: làm cả 3 — U09+U10 (VIP streak/Shield Score, effort thấp, có thể xen kẽ ngay) → U02 (Throttling Fingerprint) → U01 (Device Truth Score). Chi tiết thứ tự và lý do ở `epic-04-unique-ideas.md`.

## Cấu trúc backlog

| File | Nội dung | Số item |
|---|---|---|
| [epic-01-bugfix.md](epic-01-bugfix.md) | Bug cần fix, chia P0 (crash/sai dữ liệu nghiêm trọng) → P2 (cosmetic) | 32 |
| [epic-02-techdebt.md](epic-02-techdebt.md) | Refactor, migration kiến trúc dang dở, test gap, CI/build hygiene | 30 task, 10 story |
| [epic-03-new-features.md](epic-03-new-features.md) | Feature mới, gap vs CPU-Z/AIDA64/Device Info HW | 12 |
| [epic-04-unique-ideas.md](epic-04-unique-ideas.md) | Ý tưởng độc quyền (USP) — U01/U02 được **3 AI review độc lập** cùng tự đề xuất | 16 |
| [feature.md](feature.md) | Roadmap enhancement đợt 1-2 (đã ship) + Ideas | — |
| [quick_win.md](quick_win.md) | Cải tiến nhỏ, dễ làm — #1-#10 | 10 |

## Đánh giá tổng quan tình trạng repo

- **Đã rất sạch ở phần vừa refactor** (`doc/task/feature.md` đợt 1-2): TOML, KSP, dead code cũ đã dọn — review lần này không tìm lại bug ở những phần đó.
- **Điểm nóng thật sự**: migration Interactor/Observable pattern mới hoàn thành 3/12 vùng feature, để lại đúng 1 dead-code hoàn chỉnh (Applications Compose version chưa wire) — đây là rủi ro kỹ thuật lớn nhất hiện tại (Epic 2, Story 1, **đã quyết định** giữ bản Compose).
- **17 bug P0** phát hiện (tăng từ 11 sau khi Gemini CLI review xong) — đáng chú ý nhất: `registerReceiver` thiếu export flag làm crash 100% trên Android 14+ khi mở tab Applications (B03b), và `availableProcessors()` đếm sai số core trên chip big.LITTLE hiện đại (B27, ảnh hưởng gần như mọi flagship 2023+).
- **`feat/processes` hoá ra là dead code thật** — tab đã bị ẩn với API > 23 nên minSdk 24 hiện tại không ai thấy được; khuyến nghị xoá hẳn thay vì fix 3 bug đã tìm thấy trong đó (Epic 2, T2.10b).
- **doc/CLAUDE.md và doc/AD.MD đã lệch thực tế** (compileSdk, version AdmobWrapper, mô tả ad init flow, VIP GAID) — nên update cùng lúc dọn Epic 2.
- **Tín hiệu USP mạnh**: "Device Truth Score" (phát hiện chip giả/refurb) và "Throttling Fingerprint" (đường cong xung-nhiệt) mỗi ý tưởng được cả 3 AI review độc lập tự đề xuất mà không thấy bài nhau — hiếm khi có sự hội tụ rõ như vậy, đáng tin là gap thị trường thật.
