package com.galaxyjoy.cpuinfo

import android.app.Application
import com.galaxyjoy.cpuinfo.appinitializers.InitializersApp
import com.galaxyjoy.cpuinfo.common.const.AdKeys
import com.galaxyjoy.cpuinfo.feat.SplashActivity
import com.galaxyjoy.cpuinfo.feat.vip.ActVip
import com.galaxyjoy.cpuinfo.feat.vip.VipKeys
import com.roy.sdkadbmob.AdManager
import com.roy.sdkadbmob.AdSafetyLimits
import com.roy.sdkadbmob.AdSdkConfig
import com.roy.sdkadbmob.ErrorReporter
import com.roy.sdkadbmob.PaidEventListener
import com.roy.sdkadbmob.SafeLogger
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class GalaxyApp : Application() {

    private companion object {
        private const val TAG = "roy93~GalaxyApp"

        // QA test-device hash cho AdMob (RequestConfiguration hash, KHÔNG PHẢI GAID) — bắt buộc
        // trước khi QA click ad thật (Bước 3b, AD_PROMPT_AOS.MD), tránh invalid traffic tự khoá
        // tài khoản AdMob. Nguồn: https://github.com/royt93/myKeyStore/blob/main/README.md
        private val QA_ADMOB_TEST_DEVICE_HASHES = arrayOf(
            "813DCF48B3E486F15A60676D49A2AB09", // Samsung SM-A507FN (A50s)
            "E165942547A491D06E43E24870B990B2", // OPPO CPH1989 (Reno2)
            "C3632968623F0B44E87CE401A06AC8F9", // TCL 9032X
            "4A2AA8832A7FE9D7805081AD03C9CE68", // Xiaomi 23028RN4DG
            "DEE1D0C6AEA4CA5C94FA4D709087A3AC", // vivo V2352A
            "5D2E85389997C743F9CC33DF5F70D736", // ZTE Blade A52
            "EB7B6504801B5E518C4CE6D519ED325C", // samsung SM-A115F
            "FED3CA82141FF6113F2D069F8395B966", // samsung SM-A507FN (unit 2)
            "96E61CBFCE6BC0BDCA1612F1BACB56BE", // OPPO CPH1989 (unit 2)
            "5B409111AF01C6BB9F9FF77AEEB44275", // TECNO BG6
            "D1B50484E250B064A9BF6F7CAE29A941", // samsung SM-S928B
            "322285166ACB542864828826D2D92491", // Google Pixel 7 Pro
            // TECNO KJ7 — nhiều hash lịch sử, CỐ Ý giữ tất cả (không xoá cái nào — user chỉ thị
            // 2026-09-10: thêm càng nhiều càng an toàn, hash cũ không khớp thì cũng vô hại, chỉ có
            // hash KHỚP mới có tác dụng). Xác nhận thực nghiệm 2026-09-10: hash phụ thuộc signing
            // cert (debug vs release keystore) — cài debug rồi production trên CÙNG máy/ANDROID_ID,
            // debug build luôn báo 7FA023DF...F0B92, production-signed build luôn báo
            // B1EF014D...C9A9. Google không document điều này ở đâu. Sự cố thật 2026-09-10: cài đè
            // production lên debug, hash debug không khớp nữa → phát ad thật, user kill app ngay vì
            // sợ invalid traffic. Từ nay đăng ký ĐỦ hash debug + release cho MỌI máy trong bảng.
            // Chi tiết: myKeyStore/README.md.
            "E422A3A7DF4E2B2C686ACF87E0BD87CC", // TECNO KJ7 (2026-08-31, hash cũ nhất còn ghi nhận được)
            "B1EF014DD6D4DC54A4D160ECAA04C9A9", // TECNO KJ7 — production-signed build (2026-09-09/10)
            "7FA023DF89A8F446A4D9C665CEBF0B92", // TECNO KJ7 — debug build (2026-09-10)
        )

        // Hash RIÊNG cho UMP ConsentDebugSettings (umpDebugGeography) — KHÁC hệ với
        // QA_ADMOB_TEST_DEVICE_HASHES ở trên (AdMob RequestConfiguration.setTestDeviceIds), dù cùng
        // là "test-device hash" và cùng đọc từ logcat. Xác nhận thực nghiệm 2026-09-09 trên TECNO
        // KJ7: 2 hash KHÁC NHAU cho cùng 1 máy vật lý cùng thời điểm (7FA023DF... vs B1EF014D...).
        // Đọc từ logcat: "Use new ConsentDebugSettings.Builder().addTestDeviceHashedId(\"<HASH>\")".
        private val QA_UMP_DEBUG_GEOGRAPHY_TEST_DEVICE_HASHES = arrayOf(
            "7FA023DF89A8F446A4D9C665CEBF0B92", // TECNO KJ7 (2026-09-09)
        )
    }

    @Inject
    lateinit var initializers: InitializersApp

    override fun onCreate() {
        super.onCreate()
        SafeLogger.d(TAG, "onCreate → initializers + setupAd")
        initializers.init(this)
        setupAd()
    }

    private fun setupAd() {
        val safety = if (BuildConfig.DEBUG) AdSafetyLimits.TEST else AdSafetyLimits.UTILITY
        SafeLogger.d(
            TAG,
            "setupAd: isEnableAdmob=${BuildConfig.IS_ENABLE_ADMOB}, isDebug=${BuildConfig.DEBUG}, safetyPreset=${if (BuildConfig.DEBUG) "TEST" else "UTILITY"}",
        )
        val adConfig = AdSdkConfig(
            isEnableAdmob          = BuildConfig.IS_ENABLE_ADMOB,
            isDebug                = BuildConfig.DEBUG,
            admobAppOpenId         = BuildConfig.ADMOB_APP_OPEN_ID,
            admobInterstitialId    = BuildConfig.ADMOB_INTERSTITIAL_ID,
            admobBannerId          = BuildConfig.ADMOB_BANNER_ID,
            admobRewardedId        = BuildConfig.ADMOB_REWARDED_ID,
            applovinAppOpenId      = BuildConfig.APPLOVIN_APP_OPEN_ID,
            applovinInterstitialId = BuildConfig.APPLOVIN_INTER_ID,
            applovinBannerId       = BuildConfig.APPLOVIN_BANNER_ID,
            applovinRewardedId     = BuildConfig.APPLOVIN_REWARD_ID,
            applovinSdkKey         = BuildConfig.APPLOVIN_SDK_KEY,
            // Secret RIÊNG, chỉ chống-tamper SharedPreferences — KHÔNG còn trùng mã VIP user gõ
            // (xem VIP_ANTI_TAMPER_SECRET KDoc, đây là fix của lỗi cũ).
            vipKeySecret           = AdKeys.VIP_ANTI_TAMPER_SECRET,
            safety                 = safety,
            // Class reference (an toàn với R8) — đừng dựa vào default deprecated
            // appOpenExcludedActivityNames (xem Security Checklist, AD_PROMPT_AOS.MD). ActVip: phát
            // hiện lúc audit 2026-09-08 là Activity RIÊNG (không phải Fragment như tưởng ban đầu) —
            // dùng đúng cơ chế này thay vì suppressAppOpenTemporarily thủ công trong FVipManagement
            // (đã xoá, có bug thật đã verify trên máy: unsuppress nhầm ở onPause khi app background).
            appOpenExcludedActivities = listOf(SplashActivity::class.java, ActVip::class.java),
            // "Thẻ cào" đúng chuẩn SDK (non-deprecated) — thay cho activateVipByKey(secret, days)
            // cũ. onRedeemClick giờ pass THẲNG key user gõ, SDK tự tra map này + tự cấp đúng số
            // ngày. Không cần allowLegacyPlaintextVipKey nữa (giữ mặc định false = an toàn hơn) —
            // grantViaRewarded/gift-code/streak-claim đã chuyển sang AdManager.grantVipDays(...).
            vipRedeemCodes = VipKeys.keyToDays,
            // Public key ECDSA thật (sinh qua AdManager.generateVipKeyPair(), KHÔNG phải cặp mẫu
            // của SDK) — activateVipByKey tự thử verify token TRƯỚC redeem code, nên onRedeemClick
            // không cần sửa gì thêm để chấp nhận token; xem feat/vip/mint/ (debug-only) để đúc.
            vipTokenPublicKey = BuildConfig.VIP_TOKEN_PUBLIC_KEY,
            // Debug-only QA override — ép UMP báo geography EEA/NOT_EEA để xem form consent GDPR
            // thật trên máy VN mà không cần VPN (AD_PROMPT_AOS.MD audit 2026-09-09: SDK hỗ trợ sẵn
            // cơ chế này nhưng app chưa từng wire). Bật bằng `-PdebugGeo=EEA`; rỗng = tắt (mặc định),
            // release luôn rỗng (build.gradle.kts). Chỉ có tác dụng trên thiết bị đã khai ở
            // QA_ADMOB_TEST_DEVICE_HASHES — UMP debug geography yêu cầu test-device hash tương ứng.
            umpDebugGeography = if (BuildConfig.DEBUG) BuildConfig.UMP_DEBUG_GEOGRAPHY.ifBlank { null } else null,
            umpTestDeviceHashedIds = if (BuildConfig.DEBUG) QA_UMP_DEBUG_GEOGRAPHY_TEST_DEVICE_HASHES.toList() else emptyList(),
        )

        // Revenue tracking — SDK Demo mẫu (MyApplication.kt) set 2 listener này trong
        // Application.onCreate TRƯỚC setConfig/initialize, KHÔNG set từ Activity (SDK gắn "chủ sở
        // hữu" listener = Activity foreground lúc set → tự xoá khi Activity đó destroy, mất
        // tracking âm thầm). Trước đó app chưa wire — đây là gap thật so với sample, không phải
        // optional bỏ qua được nếu muốn đo doanh thu/lỗi ad.
        AdManager.paidEventListener = PaidEventListener { adType, valueMicros, currency, precision, adSource ->
            SafeLogger.d(TAG, "💰 paid: $adType \$${valueMicros / 1_000_000.0} $currency (precision=$precision, source=$adSource)")
        }
        AdManager.errorReporter = ErrorReporter { throwable, context ->
            SafeLogger.w(TAG, "⚠️ ad error [$context]: ${throwable.message}", throwable)
        }

        AdManager.setConfig(adConfig)
        // Bắt buộc trước khi QA click ad thật (Bước 3b) — không khai = invalid traffic risk.
        AdManager.setTestDeviceIds(*QA_ADMOB_TEST_DEVICE_HASHES)
        SafeLogger.d(TAG, "AdManager.setConfig done → initialize")
        AdManager.initialize(this) { success, gaid ->
            SafeLogger.d(TAG, "AdManager.initialize callback: success=$success, gaid=$gaid, isVipNow=${AdManager.isVipByKeyActive()}")
        }
    }
}
