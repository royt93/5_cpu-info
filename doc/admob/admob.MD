package name: com.galaxyjoy.cpuinfo
policy: https://loitp.notion.site/Term-Privacy-Policy-Disclaimer-319b1cd8783942fa8923d2a3c9bce60f

Giá trị thật (App ID, ad-unit ID, VIP key) đã chuyển sang
`../../../../myKeyStore/com.galaxyjoy.cpuinfo/ads.properties` (private repo royt93/myKeyStore) —
KHÔNG lưu plaintext ở đây nữa. File này từng bị commit + push kèm plaintext lên repo public
`royt93/5_cpu-info` (commit `9bf8daa`, `d4ab8ca`, `da1ce2d`, branch `dev`) — đã redact ở HEAD
nhưng giá trị cũ vẫn còn trong lịch sử git cho tới khi rewrite history (xem báo cáo audit
2026-09-08).

chú ý (giữ lại cho lần audit sau):

- dùng admob provider
- audit kỹ các flow tích hợp các loại ad để tìm rủi ro vi phạm admob
- native ad chưa tích hợp được — AdmobApplovinWrapper (kể cả tag mới nhất 1.8.5) không có API
  Native Ad, xem báo cáo audit 2026-09-08
- debug mode: dùng bộ admob id test
- release mode: dùng bộ admob id real trong ads.properties
- thông tin nhạy cảm tập trung vào ads.properties (myKeyStore), đã sync lên
  https://github.com/royt93/myKeyStore/
- đã bổ sung id test hash device khi init sdk (GalaxyApp.setupAd), tham khảo
  https://github.com/royt93/myKeyStore/blob/main/README.md
