package com.galaxyjoy.cpuinfo.feat.vip.streak

/**
 * Pure daily-streak logic (U09) — no Android deps, evaluated once per app open against the
 * epoch-day stored in [CheckInStreakPrefs]. Every [MILESTONE_DAYS] consecutive days unlocks a
 * VIP reward claim (see [CheckInStreakPrefs.getUnclaimedMilestones]).
 */
internal object CheckInStreak {

    const val MILESTONE_DAYS = 7

    data class Evaluation(
        val streak: Int,
        val isNewCheckIn: Boolean,
        val milestoneReached: Boolean,
    )

    /**
     * @param lastCheckInEpochDay epoch-day (see [java.time.LocalDate.toEpochDay]) of the last
     * recorded check-in, or 0 if never checked in.
     * @param currentStreak streak count as of [lastCheckInEpochDay].
     * @param todayEpochDay today's epoch-day.
     */
    fun evaluate(lastCheckInEpochDay: Long, currentStreak: Int, todayEpochDay: Long): Evaluation {
        if (lastCheckInEpochDay == todayEpochDay) {
            // Already recorded today (e.g. second onResume same day) — idempotent no-op.
            return Evaluation(streak = currentStreak, isNewCheckIn = false, milestoneReached = false)
        }
        val newStreak = if (lastCheckInEpochDay == todayEpochDay - 1) currentStreak + 1 else 1
        return Evaluation(
            streak = newStreak,
            isNewCheckIn = true,
            milestoneReached = newStreak % MILESTONE_DAYS == 0,
        )
    }
}
