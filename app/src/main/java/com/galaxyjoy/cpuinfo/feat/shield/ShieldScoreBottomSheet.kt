package com.galaxyjoy.cpuinfo.feat.shield

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.galaxyjoy.cpuinfo.R
import com.galaxyjoy.cpuinfo.common.const.AdKeys
import com.galaxyjoy.cpuinfo.feat.achievement.AchievementPrefs
import com.galaxyjoy.cpuinfo.feat.setting.BaseRoundedBottomSheet
import com.galaxyjoy.cpuinfo.feat.vip.streak.CheckInStreak
import com.galaxyjoy.cpuinfo.feat.vip.streak.CheckInStreakPrefs
import com.galaxyjoy.cpuinfo.ui.theme.CpuInfoTheme
import com.roy.sdkadbmob.AdManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.math.roundToInt

/**
 * U10 "Shield Score" + U09 daily streak, combined into one sheet (opened from the toolbar
 * badge). Score is purely informational (no ad gating); the streak claim button is the only
 * ad touchpoint here, and it's optional — base reward grants immediately without watching.
 */
@AndroidEntryPoint
class ShieldScoreBottomSheet : BaseRoundedBottomSheet() {

    @Inject
    lateinit var shieldScoreProvider: ShieldScoreProvider

    @Inject
    lateinit var achievementPrefs: AchievementPrefs

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

        val streakPrefs = CheckInStreakPrefs(requireContext())
        val score = shieldScoreProvider.compute()

        setContent {
            val ctx = LocalContext.current
            val isDark = (ctx.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES

            var hasUnclaimed by remember { mutableStateOf(streakPrefs.hasUnclaimedMilestone()) }
            var justClaimedBase by remember { mutableStateOf(false) }

            CpuInfoTheme(useDarkTheme = isDark) {
                ShieldScoreContent(
                    score = score,
                    recordsBrokenCount = achievementPrefs.getRecordsBrokenCount(),
                    streak = streakPrefs.getStreak(),
                    hasUnclaimedMilestone = hasUnclaimed,
                    justClaimedBase = justClaimedBase,
                    onClaimBaseClicked = {
                        // v1.1.5 has no direct "grant N days" API — activateVipByKey(secret, days)
                        // is the same call VipKeys-based redemption already uses (see VipKeys.kt),
                        // just with days=1 instead of the 30/3-day whitelist amounts.
                        val granted = AdManager.activateVipByKey(requireContext(), AdKeys.VIP_SECRET, 1)
                        if (granted) {
                            streakPrefs.consumeMilestoneClaim()
                            hasUnclaimed = false
                            justClaimedBase = true
                            Toast.makeText(requireContext(), R.string.streak_claimed_toast, Toast.LENGTH_LONG).show()
                        }
                    },
                    onDoubleUpClicked = {
                        val hostActivity = activity ?: return@ShieldScoreContent
                        AdManager.showRewarded(hostActivity) { earned ->
                            if (!isAdded || earned != true) return@showRewarded
                            AdManager.activateVipByKey(requireContext(), AdKeys.VIP_SECRET, 1)
                            Toast.makeText(requireContext(), R.string.streak_claimed_toast, Toast.LENGTH_LONG).show()
                        }
                    },
                )
            }
        }
    }

    companion object {
        const val TAG = "ShieldScoreBottomSheet"
    }
}

@Composable
private fun ShieldScoreContent(
    score: ShieldScoreCalculator.Result,
    recordsBrokenCount: Int,
    streak: Int,
    hasUnclaimedMilestone: Boolean,
    justClaimedBase: Boolean,
    onClaimBaseClicked: () -> Unit,
    onDoubleUpClicked: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.shield_score_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(20.dp))

            ScoreRing(overall = score.overall)
            Spacer(Modifier.height(20.dp))

            ScoreRow(R.drawable.ic_ram_high, stringResource(R.string.shield_score_ram_label), score.ramScore)
            Spacer(Modifier.height(10.dp))
            ScoreRow(R.drawable.ic_storage_disk, stringResource(R.string.shield_score_storage_label), score.storageScore)
            Spacer(Modifier.height(10.dp))
            ScoreRow(R.drawable.ic_battery, stringResource(R.string.shield_score_battery_label), score.batteryScore)
            Spacer(Modifier.height(14.dp))

            Text(
                text = suggestionFor(score),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // U33 — only shown once the user has actually broken a benchmark personal record;
            // "0 records" is noise, not a stat worth surfacing to someone who's never run a
            // benchmark or never beaten their own past result.
            if (recordsBrokenCount > 0) {
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(R.drawable.baseline_star_rate_24),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.height(16.dp),
                    )
                    Text(
                        text = stringResource(R.string.shield_score_records_broken, recordsBrokenCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.streak_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(12.dp))
            StreakDots(streak = streak)
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(
                    R.string.streak_progress,
                    streak,
                    ((streak - 1) / CheckInStreak.MILESTONE_DAYS + 1) * CheckInStreak.MILESTONE_DAYS,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            when {
                hasUnclaimedMilestone -> {
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = onClaimBaseClicked, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.streak_claim_button))
                    }
                }
                justClaimedBase -> {
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(onClick = onDoubleUpClicked, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.streak_claim_button_double))
                    }
                }
                else -> {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.streak_no_milestone_yet),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/**
 * Animated circular ring gauge — sweeps from 0 to [overall]/100 on first composition, with the
 * score number counting up in sync. Track color is a 3-stop gradient (red→amber→green) so the
 * ring's fill color communicates the band at a glance, matching [scoreColor] elsewhere.
 */
@Composable
private fun ScoreRing(overall: Int) {
    val animatedProgress = remember { Animatable(0f) }
    LaunchedEffect(overall) {
        animatedProgress.animateTo(
            targetValue = overall / 100f,
            animationSpec = tween(durationMillis = 900, easing = LinearEasing),
        )
    }
    val displayedNumber = (animatedProgress.value * 100).roundToInt()
    val ringColor = scoreColor(overall)
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(140.dp)) {
        Canvas(modifier = Modifier.size(140.dp)) {
            val stroke = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
            val inset = stroke.width / 2
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                size = Size(size.width - stroke.width, size.height - stroke.width),
                style = stroke,
            )
            drawArc(
                color = ringColor,
                startAngle = -90f,
                sweepAngle = 360f * animatedProgress.value,
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                size = Size(size.width - stroke.width, size.height - stroke.width),
                style = stroke,
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$displayedNumber",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = ringColor,
            )
            Text(
                text = scoreBandLabel(overall),
                style = MaterialTheme.typography.bodySmall,
                color = ringColor,
            )
        }
    }
}

@Composable
private fun ScoreRow(iconRes: Int, label: String, value: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            tint = scoreColor(value),
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(end = 12.dp),
        )
        val animatedValue by animateFloatAsState(
            targetValue = value / 100f,
            animationSpec = tween(durationMillis = 700),
            label = "scoreRowProgress",
        )
        // Colors must be resolved here (composable context) — DrawScope inside Canvas{} below
        // cannot call @Composable functions like MaterialTheme.colorScheme.*.
        val trackColor = MaterialTheme.colorScheme.surfaceVariant
        val barColor = scoreColor(value)
        Box(
            modifier = Modifier
                .weight(1f)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
        ) {
            Canvas(modifier = Modifier.fillMaxWidth().height(6.dp)) {
                drawRoundRect(
                    color = trackColor,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()),
                )
                drawRoundRect(
                    color = barColor,
                    size = Size(size.width * animatedValue, size.height),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()),
                )
            }
        }
        Text(
            text = "$value",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}

/**
 * 7 dots representing the current cycle toward [CheckInStreak.MILESTONE_DAYS]. Filled dots
 * scale-in sequentially on appear for a light "reward tracker" feel.
 */
@Composable
private fun StreakDots(streak: Int) {
    val dayInCycle = ((streak - 1) % CheckInStreak.MILESTONE_DAYS) + 1
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        for (day in 1..CheckInStreak.MILESTONE_DAYS) {
            val filled = day <= dayInCycle
            AnimatedVisibility(
                visible = true,
                enter = scaleIn(animationSpec = tween(durationMillis = 250, delayMillis = day * 60)) +
                    fadeIn(animationSpec = tween(durationMillis = 250, delayMillis = day * 60)),
            ) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(
                            if (filled) {
                                Brush.linearGradient(listOf(Color(0xFF66BB6A), Color(0xFF4CAF50)))
                            } else {
                                Brush.linearGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.surfaceVariant,
                                        MaterialTheme.colorScheme.surfaceVariant,
                                    ),
                                )
                            },
                        ),
                )
            }
        }
    }
}

private fun scoreColor(score: Int): Color = when {
    score >= 80 -> Color(0xFF4CAF50)
    score >= 50 -> Color(0xFFFFA726)
    else -> Color(0xFFE53935)
}

@Composable
private fun scoreBandLabel(score: Int): String = when {
    score >= 80 -> stringResource(R.string.shield_score_band_excellent)
    score >= 60 -> stringResource(R.string.shield_score_band_good)
    score >= 40 -> stringResource(R.string.shield_score_band_fair)
    else -> stringResource(R.string.shield_score_band_poor)
}

@Composable
private fun suggestionFor(score: ShieldScoreCalculator.Result): String {
    val lowest = minOf(score.ramScore, score.storageScore, score.batteryScore)
    return when {
        lowest >= 70 -> stringResource(R.string.shield_score_suggestion_none)
        score.ramScore == lowest -> stringResource(R.string.shield_score_suggestion_ram)
        score.storageScore == lowest -> stringResource(R.string.shield_score_suggestion_storage)
        else -> stringResource(R.string.shield_score_suggestion_battery)
    }
}
