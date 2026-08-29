package com.balandman.liftlog.data

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * A "gym day" runs 4am to 4am rather than midnight to midnight, so a late-night
 * session still counts as that evening's workout and the tiles do not reset out
 * from under you partway through.
 */
object GymDay {

    const val RESET_HOUR = 4

    fun dayOf(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault()): LocalDate {
        val moment = Instant.ofEpochMilli(epochMillis).atZone(zone)
        return if (moment.hour < RESET_HOUR) {
            moment.toLocalDate().minusDays(1)
        } else {
            moment.toLocalDate()
        }
    }

    fun today(zone: ZoneId = ZoneId.systemDefault()): LocalDate =
        dayOf(System.currentTimeMillis(), zone)

    /** True when [epochMillis] falls inside the current gym day. */
    fun isToday(epochMillis: Long?, zone: ZoneId = ZoneId.systemDefault()): Boolean {
        if (epochMillis == null) return false
        return dayOf(epochMillis, zone) == today(zone)
    }
}
