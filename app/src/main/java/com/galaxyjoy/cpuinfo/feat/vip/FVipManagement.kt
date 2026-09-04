package com.galaxyjoy.cpuinfo.feat.vip

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import com.galaxyjoy.cpuinfo.R
import com.galaxyjoy.cpuinfo.common.const.AdKeys
import com.galaxyjoy.cpuinfo.databinding.FVipManagementBinding
import com.galaxyjoy.cpuinfo.feat.vip.gift.VipGiftCode
import com.galaxyjoy.cpuinfo.feat.vip.gift.VipGiftLogic
import com.galaxyjoy.cpuinfo.feat.vip.gift.VipGiftPrefs
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.roy.sdkadbmob.AdManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * VIP management screen — single-entry. Chi tiết spec ở doc/AD_PROMPT_AOS.MD §10.
 */
class FVipManagement : Fragment() {

    private var _binding: FVipManagementBinding? = null
    private val binding get() = _binding!!

    private val vipPrefs by lazy { VipPrefs(requireContext()) }
    private val vipGiftPrefs by lazy { VipGiftPrefs(requireContext()) }

    private var countDownTimer: CountDownTimer? = null
    private var pulseAnimator: ObjectAnimator? = null
    private var shimmerAnimator: ObjectAnimator? = null
    private var glowAnimator: ObjectAnimator? = null
    private var confettiAnimator: ObjectAnimator? = null
    private var confettiAnimatorListener: android.animation.AnimatorListenerAdapter? = null
    private var countUpAnimator: ValueAnimator? = null
    private var lastMinute: Int? = null
    private var scrollToRedeemSectionRunnable: Runnable? = null
    /** True khi confetti đang chạy → bindUi không restart shimmer (tránh race rotate). */
    private var isConfettiRunning: Boolean = false

    private val dateFormatter by lazy {
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FVipManagementBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Slide-in entry animation #2
        val anim = android.view.animation.AnimationUtils.loadAnimation(
            requireContext(),
            R.anim.slide_in_bottom,
        )
        binding.vipRoot.startAnimation(anim)

        // Đảm bảo rewarded sẵn sàng khi user mở VIP screen — lib không tự auto-load.
        Log.d(TAG, "onViewCreated → preload rewarded")
        AdManager.loadRewarded(requireContext())

        setupListeners()
        listenForRevokeResult()
        bindUi()
    }

    private fun setupListeners() {
        binding.btnRedeemKey.setOnClickListener { onRedeemClick() }
        binding.btnWatchAd.setOnClickListener { onWatchAdClick() }
        binding.btnGiftVip.setOnClickListener { onGiftVipClick() }
        binding.btnRevoke.setOnClickListener { onRevokeClick() }
        binding.tvPrivacyPolicy.setOnClickListener {
            openPrivacyPolicy()
        }
        binding.etRedeemKey.doAfterTextChanged {
            binding.tilRedeemKey.error = null
        }
        // Khi keyboard show → scroll để cả EditText + button Activate đều visible.
        binding.etRedeemKey.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) scrollToRedeemSection()
        }
    }

    /**
     * Scroll để cả EditText + button Activate vào view khi keyboard show.
     *
     * Dùng `requestRectangleOnScreen` — built-in API walks up parent chain và scroll
     * mọi ScrollView ancestors → đảm bảo chính xác bất kể nested depth.
     */
    private fun scrollToRedeemSection() {
        // Post delayed để keyboard có thời gian show + ScrollView resize trước khi scroll.
        scrollToRedeemSectionRunnable?.let { binding.btnRedeemKey.removeCallbacks(it) }
        val runnable = Runnable {
            if (_binding == null) return@Runnable
            val extraPaddingPx = (16 * resources.displayMetrics.density).toInt()
            val rect = android.graphics.Rect(
                0,
                0,
                binding.btnRedeemKey.width,
                binding.btnRedeemKey.height + extraPaddingPx,
            )
            binding.btnRedeemKey.requestRectangleOnScreen(rect, false)
        }
        scrollToRedeemSectionRunnable = runnable
        binding.btnRedeemKey.postDelayed(runnable, 300L)
    }

    private fun onRedeemClick() {
        val raw = binding.etRedeemKey.text?.toString().orEmpty()
        Log.d(TAG, "onRedeemClick: input length=${raw.length}")
        if (raw.isBlank()) {
            binding.tilRedeemKey.error = getString(R.string.vip_redeem_invalid)
            return
        }
        val days = VipKeys.lookupDays(raw)
        if (days == null) {
            if (tryRedeemGiftCode(raw)) return
            Log.d(TAG, "onRedeemClick: invalid key (not in whitelist, not a valid gift code)")
            binding.tilRedeemKey.error = getString(R.string.vip_redeem_invalid)
            return
        }
        // Lib v1.1.5 single-key: phải pass AdKeys.VIP_SECRET (= vipKeySecret) cho dù
        // app-side detect key 30d hay 3d. App-side đã verify whitelist qua lookupDays().
        val ok = AdManager.activateVipByKey(requireContext(), AdKeys.VIP_SECRET, days)
        Log.d(TAG, "onRedeemClick: activate result=$ok, days=$days")
        if (ok) {
            vipPrefs.saveGrantedAtMs(System.currentTimeMillis())
            vipPrefs.markUserRedeemed()
            vipPrefs.addTotalDaysActivated(days)
            binding.etRedeemKey.setText("")
            triggerConfettiAndHaptic()
            Toast.makeText(
                requireContext(),
                getString(R.string.vip_redeem_success, days),
                Toast.LENGTH_SHORT,
            ).show()
            bindUi()
            notifyVipChanged()
        } else {
            binding.tilRedeemKey.error = getString(R.string.vip_redeem_invalid)
        }
    }

    /**
     * U11 — the `etRedeemKey` field's OTHER accepted input shape: a [VipGiftCode] instead of a
     * [VipKeys] whitelist entry. Returns `true` if [raw] was recognized and handled (whether the
     * redemption itself succeeded or failed) so the caller stops before showing the generic
     * "invalid key" error meant for neither-of-the-two-formats input.
     */
    private fun tryRedeemGiftCode(raw: String): Boolean {
        val issuedEpochDay = VipGiftCode.decode(raw) ?: return false
        val today = VipGiftPrefs.todayEpochDay()
        if (!VipGiftLogic.isCodeFresh(issuedEpochDay, today)) {
            Log.d(TAG, "tryRedeemGiftCode: code expired (issued=$issuedEpochDay, today=$today)")
            return false
        }
        if (!VipGiftLogic.canRedeemToday(vipGiftPrefs.getLastRedeemedEpochDay(), today)) {
            Log.d(TAG, "tryRedeemGiftCode: already redeemed a gift today")
            binding.tilRedeemKey.error = getString(R.string.vip_gift_already_redeemed_today)
            return true
        }
        val days = VipGiftLogic.daysToGrantForAccumulate(AdManager.getVipByKeyExpiry(), System.currentTimeMillis())
        val ok = AdManager.activateVipByKey(requireContext(), AdKeys.VIP_SECRET, days)
        Log.d(TAG, "tryRedeemGiftCode: activate result=$ok, accumulateDays=$days")
        if (!ok) return false

        vipGiftPrefs.saveLastRedeemedEpochDay(today)
        vipPrefs.saveGrantedAtMs(System.currentTimeMillis())
        vipPrefs.markUserRedeemed()
        // Total-activated stat should reflect the real gift amount, not the internal
        // accumulate-adjusted `days` sent to the SDK to avoid overwriting existing VIP time.
        vipPrefs.addTotalDaysActivated(VipGiftLogic.GIFT_DAYS)
        binding.etRedeemKey.setText("")
        triggerConfettiAndHaptic()
        Toast.makeText(
            requireContext(),
            getString(R.string.vip_redeem_success, VipGiftLogic.GIFT_DAYS),
            Toast.LENGTH_SHORT,
        ).show()
        bindUi()
        notifyVipChanged()
        return true
    }

    /** U11 — VIP user shares a 1-day gift code for a friend, limited to once/day (generation
     * side; redemption side has its own once/day limit in [tryRedeemGiftCode]). */
    private fun onGiftVipClick() {
        if (!AdManager.isVipByKeyActive()) return // defensive — bindUi already hides/disables the button
        val today = VipGiftPrefs.todayEpochDay()
        if (!VipGiftLogic.canGenerateToday(vipGiftPrefs.getLastGeneratedEpochDay(), today)) {
            Toast.makeText(requireContext(), getString(R.string.vip_gift_already_generated_today), Toast.LENGTH_SHORT).show()
            return
        }
        val code = VipGiftCode.encode(today)
        vipGiftPrefs.saveLastGeneratedEpochDay(today)
        Log.d(TAG, "onGiftVipClick: generated gift code for epochDay=$today")

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, getString(R.string.vip_gift_share_message, code))
        }
        startActivity(Intent.createChooser(shareIntent, getString(R.string.vip_gift_share_chooser_title)))
        bindUi()
    }

    private fun onWatchAdClick() {
        val activity = activity ?: return
        // Nếu user đã VIP → lib sẽ skip rewarded với earned=false. KHÔNG cho click.
        // bindUi() đã hide btn khi active, đây là defensive check.
        if (AdManager.isVipByKeyActive()) {
            Log.d(TAG, "onWatchAdClick blocked — user already VIP")
            return
        }
        Log.d(TAG, "onWatchAdClick → showRewarded")
        AdManager.showRewarded(activity) { earned ->
            Log.d(TAG, "showRewarded callback: earned=$earned")
            if (!isAdded || _binding == null) return@showRewarded
            if (earned) {
                // Rewarded contract: chỉ grant khi user xem hết ad. KHÔNG fallback
                // interstitial nếu user skip — đó là cheat reward, vi phạm policy.
                grantViaRewarded()
            } else {
                // Reload để lần sau có ad sẵn (lib không tự auto-reload sau show).
                AdManager.loadRewarded(requireContext())
                Toast.makeText(
                    requireContext(),
                    getString(R.string.vip_watch_ad_failed),
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }
    }

    private fun grantViaRewarded() {
        // Lib single-key: pass AdKeys.VIP_SECRET. App-side overrides days = 3.
        val ok = AdManager.activateVipByKey(
            requireContext(),
            AdKeys.VIP_SECRET,
            VipKeys.VIP_3D_DAYS,
        )
        Log.d(TAG, "grantViaRewarded: activate result=$ok, days=${VipKeys.VIP_3D_DAYS}")
        if (ok) {
            vipPrefs.saveGrantedAtMs(System.currentTimeMillis())
            vipPrefs.markUserRedeemed()
            vipPrefs.addTotalDaysActivated(VipKeys.VIP_3D_DAYS)
            triggerConfettiAndHaptic()
            Toast.makeText(
                requireContext(),
                getString(R.string.vip_redeem_success, VipKeys.VIP_3D_DAYS),
                Toast.LENGTH_SHORT,
            ).show()
            bindUi()
            notifyVipChanged()
        } else {
            Toast.makeText(
                requireContext(),
                getString(R.string.vip_watch_ad_failed),
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    private fun onRevokeClick() {
        Log.d(TAG, "onRevokeClick → show revoke BottomSheet")
        val fm = parentFragmentManager
        if (fm.isStateSaved) return
        if (fm.findFragmentByTag(VipRevokeConfirmBottomSheet.TAG) != null) return
        VipRevokeConfirmBottomSheet()
            .show(fm, VipRevokeConfirmBottomSheet.TAG)
    }

    private fun listenForRevokeResult() {
        parentFragmentManager.setFragmentResultListener(
            VipRevokeConfirmBottomSheet.REQUEST_KEY,
            viewLifecycleOwner,
        ) { _, bundle ->
            val confirmed = bundle.getBoolean(VipRevokeConfirmBottomSheet.ARG_CONFIRMED, false)
            Log.d(TAG, "revoke result: confirmed=$confirmed")
            if (confirmed) {
                AdManager.clearVipByKey()
                vipPrefs.clearGrantedAtMs()
                bindUi()
                notifyVipChanged()
            }
        }
    }

    private fun openPrivacyPolicy() {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(AdKeys.PRIVACY_POLICY_URL))
            startActivity(intent)
        } catch (_: Exception) {
            // ignore — user can read about/policy in browser via main app menu
        }
    }

    private fun bindUi() {
        val active = AdManager.isVipByKeyActive()
        val now = System.currentTimeMillis()
        val expiresAt = AdManager.getVipByKeyExpiry()
        val grantedAtRaw = vipPrefs.getGrantedAtMs()
        // Grace VIP do SDK grant không qua app code → grantedAt = 0. Fallback ước lượng
        // = expiresAt - 24h (grace mặc định 1-day) để progress + activated row render được.
        val grantedAt = when {
            grantedAtRaw > 0L -> grantedAtRaw
            active           -> expiresAt - DAY_MS
            else             -> 0L
        }

        // 1. Status header
        if (active) {
            binding.statusHeaderInner.setBackgroundResource(R.drawable.bg_vip_status_header_active)
            binding.tvStatusTitle.setText(R.string.vip_active)
            binding.tvStatusSubtitle.text = getString(R.string.vip_until, dateFormatter.format(Date(expiresAt)))
            binding.tvStatusSubtitle.visibility = View.VISIBLE
            binding.viewCrownGlow.visibility = View.VISIBLE
            startCrownGlow()
            // Khi confetti đang chạy → để listener trong triggerConfettiAndHaptic restart
            // shimmer sau khi confetti end. Tránh 2 animator cùng rotate property cùng lúc.
            if (!isConfettiRunning) startCrownShimmer()
        } else {
            binding.statusHeaderInner.setBackgroundResource(R.drawable.bg_vip_status_header_free)
            binding.tvStatusTitle.setText(R.string.vip_free_user)
            binding.tvStatusSubtitle.visibility = View.GONE
            binding.viewCrownGlow.visibility = View.GONE
            stopCrownGlow()
            stopCrownShimmer()
        }
        // Pulse luôn chạy (active hay free đều show nút "Watch ad")
        startWatchAdPulse()

        // 2-3. Activation / Expiry rows
        binding.tvActivatedAt.apply {
            if (active && grantedAt > 0L) {
                text = getString(R.string.vip_activated_at, dateFormatter.format(Date(grantedAt)))
                visibility = View.VISIBLE
            } else {
                visibility = View.GONE
            }
        }
        binding.tvExpiresAt.apply {
            if (active) {
                text = getString(R.string.vip_expires_at, dateFormatter.format(Date(expiresAt)))
                visibility = View.VISIBLE
            } else {
                visibility = View.GONE
            }
        }

        // 12. Active VIP card single-entry
        bindActiveEntryCard(active, expiresAt, grantedAt)

        // Watch ad btn: hide khi đã VIP (lib sẽ skip rewarded anyway → tránh confuse user).
        binding.btnWatchAd.visibility = if (active) View.GONE else View.VISIBLE

        // U11 — gift btn: chỉ VIP mới có gì để tặng; disable (không ẩn) khi đã tạo mã hôm nay,
        // để user còn thấy nút thay vì tưởng tính năng biến mất.
        binding.btnGiftVip.visibility = if (active) View.VISIBLE else View.GONE
        binding.btnGiftVip.isEnabled = active &&
            VipGiftLogic.canGenerateToday(vipGiftPrefs.getLastGeneratedEpochDay(), VipGiftPrefs.todayEpochDay())

        // Revoke btn: hide khi free user (không có VIP để revoke). UI sạch hơn disabled.
        // Layout XML default enabled=true (mặc định MaterialButton), nhưng explicit set để
        // chắc chắn — tránh state lệch giữa visibility + enabled.
        binding.btnRevoke.visibility = if (active) View.VISIBLE else View.GONE
        binding.btnRevoke.isEnabled = active

        // Stats card
        bindStats(active, expiresAt, now)

        // 4-5. Progress + countdown
        countDownTimer?.cancel()
        countDownTimer = null
        if (active) {
            val remaining = (expiresAt - now).coerceAtLeast(0L)
            binding.progressVip.visibility = View.VISIBLE
            binding.tvCountdown.visibility = View.VISIBLE
            startCountdown(grantedAt, expiresAt, remaining)
        } else {
            binding.progressVip.visibility = View.GONE
            binding.tvCountdown.visibility = View.GONE
        }
    }

    companion object {
        private const val TAG = "roy93~Vip"
        private const val DAY_MS = 24L * 60L * 60L * 1000L
        /** FragmentResult key — ActHost listen để refresh banner/badge khi VIP toggle. */
        const val KEY_VIP_CHANGED = "vip_changed"
    }

    private fun notifyVipChanged() {
        Log.d(TAG, "notifyVipChanged → broadcasting to ActHost")
        parentFragmentManager.setFragmentResult(KEY_VIP_CHANGED, Bundle.EMPTY)
    }

    /** Days remaining (live, updated in countdown) + total activated (cộng dồn). */
    private fun bindStats(active: Boolean, expiresAtMs: Long, nowMs: Long) {
        val daysRemaining = if (active) {
            val diff = (expiresAtMs - nowMs).coerceAtLeast(0L)
            // Round UP để "còn 5h" = 1 ngày, không phải 0 ngày.
            ((diff + DAY_MS - 1) / DAY_MS).toInt()
        } else 0
        binding.tvStatsDaysRemaining.text = daysRemaining.toString()
        binding.tvStatsTotalActivated.text = vipPrefs.getTotalDaysActivated().toString()
    }

    private fun bindActiveEntryCard(active: Boolean, expiresAt: Long, grantedAt: Long) {
        if (!active) {
            binding.cardActiveEntry.visibility = View.GONE
            return
        }
        binding.cardActiveEntry.visibility = View.VISIBLE

        val redeemed = vipPrefs.userRedeemedAtLeastOnce()
        if (!redeemed) {
            // Grace entry (first-install) — chưa redeem bao giờ
            binding.tvActiveEntryLabel.setText(R.string.vip_entry_first_install)
        } else {
            // Estimate days from (grantedAt → expiresAt) — fallback 30 nếu không có
            val total = (expiresAt - grantedAt).coerceAtLeast(0L)
            val days = (total / (24L * 60L * 60L * 1000L)).toInt().coerceAtLeast(1)
            binding.tvActiveEntryLabel.text = getString(R.string.vip_entry_redeemed, days)
        }
        binding.tvActiveEntryExpiry.text =
            getString(R.string.vip_expires_at, dateFormatter.format(Date(expiresAt)))
    }

    private fun startCountdown(grantedAtMs: Long, expiresAtMs: Long, remainingMs: Long) {
        countDownTimer = object : CountDownTimer(remainingMs, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                if (_binding == null) return
                val days = (millisUntilFinished / 86_400_000L).toInt()
                val hours = ((millisUntilFinished / 3_600_000L) % 24L).toInt()
                val minutes = ((millisUntilFinished / 60_000L) % 60L).toInt()
                val seconds = ((millisUntilFinished / 1_000L) % 60L).toInt()
                binding.tvCountdown.text =
                    getString(R.string.vip_remaining, days, hours, minutes, seconds)

                // Animation #4: count-up khi minute đổi (không mỗi giây)
                if (lastMinute != null && lastMinute != minutes) {
                    countUpAnimator?.cancel()
                    countUpAnimator = ValueAnimator.ofFloat(0.95f, 1.0f).apply {
                        duration = 400L
                        addUpdateListener { anim ->
                            if (_binding == null) return@addUpdateListener
                            val scale = anim.animatedValue as Float
                            binding.tvCountdown.scaleX = scale
                            binding.tvCountdown.scaleY = scale
                        }
                        start()
                    }
                }
                lastMinute = minutes

                val nowMs = System.currentTimeMillis()
                binding.progressVip.setProgressCompat(
                    computeElapsedProgress(grantedAtMs, expiresAtMs, nowMs),
                    /* animated = */ true,
                )
            }

            override fun onFinish() {
                if (_binding == null) return
                bindUi()
            }
        }.start()
    }

    private fun computeElapsedProgress(grantedAtMs: Long, expiresAtMs: Long, nowMs: Long): Int {
        val total = expiresAtMs - grantedAtMs
        if (total <= 0L) return 100
        val elapsed = nowMs - grantedAtMs
        return ((elapsed.toDouble() / total.toDouble()) * 100.0)
            .toInt()
            .coerceIn(0, 100)
    }

    private fun startWatchAdPulse() {
        pulseAnimator?.cancel()
        pulseAnimator = ObjectAnimator.ofPropertyValuesHolder(
            binding.btnWatchAd,
            android.animation.PropertyValuesHolder.ofFloat(View.SCALE_X, 1.0f, 1.05f),
            android.animation.PropertyValuesHolder.ofFloat(View.SCALE_Y, 1.0f, 1.05f),
        ).apply {
            duration = 1600L
            repeatMode = ObjectAnimator.REVERSE
            repeatCount = ObjectAnimator.INFINITE
            start()
        }
    }

    /** Pulse halo glow around crown — alpha + scale, infinite reverse. */
    private fun startCrownGlow() {
        if (glowAnimator?.isRunning == true) return
        glowAnimator?.cancel()
        glowAnimator = ObjectAnimator.ofPropertyValuesHolder(
            binding.viewCrownGlow,
            android.animation.PropertyValuesHolder.ofFloat(View.ALPHA, 0.35f, 0.85f),
            android.animation.PropertyValuesHolder.ofFloat(View.SCALE_X, 0.95f, 1.15f),
            android.animation.PropertyValuesHolder.ofFloat(View.SCALE_Y, 0.95f, 1.15f),
        ).apply {
            duration = 1800L
            repeatMode = ObjectAnimator.REVERSE
            repeatCount = ObjectAnimator.INFINITE
            start()
        }
    }

    private fun stopCrownGlow() {
        glowAnimator?.cancel()
        glowAnimator?.removeAllUpdateListeners()
        glowAnimator?.removeAllListeners()
        glowAnimator = null
        if (_binding != null) {
            binding.viewCrownGlow.alpha = 0f
            binding.viewCrownGlow.scaleX = 1f
            binding.viewCrownGlow.scaleY = 1f
        }
    }

    private fun startCrownShimmer() {
        shimmerAnimator?.cancel()
        shimmerAnimator = ObjectAnimator.ofFloat(
            binding.imgCrown, View.ROTATION, -5f, 5f,
        ).apply {
            duration = 3000L
            repeatMode = ObjectAnimator.REVERSE
            repeatCount = ObjectAnimator.INFINITE
            start()
        }
    }

    private fun stopCrownShimmer() {
        shimmerAnimator?.cancel()
        shimmerAnimator = null
        if (_binding != null) {
            binding.imgCrown.rotation = 0f
        }
    }

    /** Animation #5: Lottie confetti overlay + crown burst + haptic feedback. */
    private fun triggerConfettiAndHaptic() {
        // Pause shimmer rotation để không conflict với confetti rotation
        shimmerAnimator?.cancel()
        confettiAnimator?.cancel()
        confettiAnimator?.removeAllListeners()
        isConfettiRunning = true

        // 1. Lottie confetti overlay
        binding.lottieConfetti.apply {
            visibility = View.VISIBLE
            cancelAnimation()
            progress = 0f
            removeAllAnimatorListeners()
            addAnimatorListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    if (_binding != null) binding.lottieConfetti.visibility = View.GONE
                }
            })
            playAnimation()
        }

        // 2. Crown burst
        val listener = object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                isConfettiRunning = false
                if (_binding != null && AdManager.isVipByKeyActive()) {
                    startCrownShimmer()
                }
            }

            override fun onAnimationCancel(animation: android.animation.Animator) {
                isConfettiRunning = false
            }
        }
        confettiAnimatorListener = listener
        confettiAnimator = ObjectAnimator.ofPropertyValuesHolder(
            binding.imgCrown,
            android.animation.PropertyValuesHolder.ofFloat(View.SCALE_X, 1.0f, 1.5f, 1.0f),
            android.animation.PropertyValuesHolder.ofFloat(View.SCALE_Y, 1.0f, 1.5f, 1.0f),
            android.animation.PropertyValuesHolder.ofFloat(View.ROTATION, 0f, 360f),
        ).apply {
            duration = 800L
            addListener(listener)
            start()
        }
        // Haptic feedback
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                binding.imgCrown.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            } else {
                @Suppress("DEPRECATION")
                binding.imgCrown.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            }
        } catch (_: Exception) {
            // ignore — haptic không khả dụng trên 1 số device
        }
    }

    override fun onPause() {
        super.onPause()
        pulseAnimator?.cancel()
        shimmerAnimator?.cancel()
        glowAnimator?.cancel()
    }

    override fun onResume() {
        super.onResume()
        // Restart animators in case fragment returns from background
        if (_binding != null) {
            startWatchAdPulse()
            if (AdManager.isVipByKeyActive()) {
                startCrownShimmer()
                startCrownGlow()
            }
        }
    }

    override fun onDestroyView() {
        // Cleanup Lottie
        _binding?.lottieConfetti?.apply {
            removeAllAnimatorListeners()
            cancelAnimation()
        }
        scrollToRedeemSectionRunnable?.let { binding.btnRedeemKey.removeCallbacks(it) }
        scrollToRedeemSectionRunnable = null
        countDownTimer?.cancel(); countDownTimer = null
        pulseAnimator?.cancel()
        pulseAnimator?.removeAllUpdateListeners()
        pulseAnimator?.removeAllListeners()
        pulseAnimator = null
        shimmerAnimator?.cancel()
        shimmerAnimator?.removeAllUpdateListeners()
        shimmerAnimator?.removeAllListeners()
        shimmerAnimator = null
        glowAnimator?.cancel()
        glowAnimator?.removeAllUpdateListeners()
        glowAnimator?.removeAllListeners()
        glowAnimator = null
        confettiAnimator?.cancel()
        confettiAnimator?.removeAllUpdateListeners()
        confettiAnimatorListener?.let { confettiAnimator?.removeListener(it) }
        confettiAnimator?.removeAllListeners()
        confettiAnimator = null
        confettiAnimatorListener = null
        isConfettiRunning = false
        countUpAnimator?.cancel()
        countUpAnimator?.removeAllUpdateListeners()
        countUpAnimator?.removeAllListeners()
        countUpAnimator = null
        _binding = null
        super.onDestroyView()
    }
}
