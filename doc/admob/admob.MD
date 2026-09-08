package name: com.galaxyjoy.cpuinfo
policy: https://loitp.notion.site/Term-Privacy-Policy-Disclaimer-319b1cd8783942fa8923d2a3c9bce60f

admob app id: ca-app-pub-3004713799155145~7714695218
banner: ca-app-pub-3004713799155145/6401613541
inter: ca-app-pub-3004713799155145/2681860387
open app: ca-app-pub-3004713799155145/9271319465
native: ca-app-pub-3004713799155145/1368778714
rewarded: ca-app-pub-3004713799155145/1616190729

chú ý:

- dùng admob provider
- audit kỹ các flow tích hợp các loại ad để tìm rủi ro vi phạm admob l
- có vẻ như app native chưa được tích hợp, bạn có gợi ý gì?
- debug mode: dùng bộ admob id test
- release mode: dùng bộ admob id real ở file này
- có thể dùng vip key 30 days: 9fA0q7eN!27cLx04@21993Y2u0I7#Q0
  3 days: eQ7@93L0f!2Y2707xN04021993u0I#2aKMD
- chú ý thông tin nhạy cảm cần phải sync
  lên https://github.com/royt93/myKeyStore/ và local path có ở macbook này (
  đã có sẵn), nếu bạn phát hiện thông tin nhạy cảm nào chưa sync, báo tôi ngay lập tức
- nên tập trung toàn bộ thông tin nhạy cảm vào 1 file duy nhất, vd như app.properties và sync lên
- lưu ý bổ sung unit test + widget test + integration test cho mọi case
- lưu ý follow đúng guide AD_PROMPT_AOS.MD, không hiểu gì báo tôi ngay
- lưu ý thêm id test hash device khi init sdk, tham khảo https://github.com/royt93/myKeyStore/blob/main/README.md