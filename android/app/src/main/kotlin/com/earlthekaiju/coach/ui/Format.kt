package com.earlthekaiju.coach.ui

import androidx.compose.runtime.staticCompositionLocalOf
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.IsoFields
import java.util.Locale

/** True when the device uses a 24-hour clock. Provided once by the app shell. */
val LocalUse24Hour = staticCompositionLocalOf { false }

object Format {
    private val locale: Locale get() = Locale.getDefault()

    fun time(t: LocalTime, use24h: Boolean): String =
        if (use24h) {
            String.format(Locale.ROOT, "%02d:%02d", t.hour, t.minute)
        } else {
            val h = if (t.hour % 12 == 0) 12 else t.hour % 12
            String.format(Locale.ROOT, "%d:%02d %s", h, t.minute, if (t.hour < 12) "AM" else "PM")
        }

    /** Short time without the AM/PM suffix, for big numerals that carry the suffix separately. */
    fun timeNumerals(t: LocalTime, use24h: Boolean): String =
        if (use24h) String.format(Locale.ROOT, "%02d:%02d", t.hour, t.minute)
        else String.format(Locale.ROOT, "%d:%02d", if (t.hour % 12 == 0) 12 else t.hour % 12, t.minute)

    fun meridiem(t: LocalTime, use24h: Boolean): String = if (use24h) "" else if (t.hour < 12) "AM" else "PM"

    fun minutes(total: Int): String = when {
        total < 60 -> "$total min"
        total % 60 == 0 -> "${total / 60} h"
        else -> "${total / 60} h ${total % 60} min"
    }

    /** Countdown text: 300 → "5:00", 3725 → "1:02:05". Never negative. */
    fun clock(seconds: Long): String {
        val s = seconds.coerceAtLeast(0)
        val h = s / 3600
        val m = (s % 3600) / 60
        val sec = s % 60
        return if (h > 0) String.format(Locale.ROOT, "%d:%02d:%02d", h, m, sec) else String.format(Locale.ROOT, "%d:%02d", m, sec)
    }

    /** Seconds as a compact duration label: 300 → "5:00", 45 → "0:45". */
    fun secondsLabel(seconds: Int): String = clock(seconds.toLong())

    fun dayEyebrow(date: LocalDate): String =
        "${date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale)} · ${date.month.getDisplayName(TextStyle.SHORT, locale)} ${date.dayOfMonth}"

    fun longDate(date: LocalDate): String =
        "${date.dayOfWeek.getDisplayName(TextStyle.FULL, locale)}, ${date.month.getDisplayName(TextStyle.FULL, locale)} ${date.dayOfMonth}"

    fun shortDate(date: LocalDate): String =
        "${date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale)}, ${date.month.getDisplayName(TextStyle.SHORT, locale)} ${date.dayOfMonth}"

    fun relativeDay(date: LocalDate, today: LocalDate): String = when (date) {
        today -> "Today"
        today.plusDays(1) -> "Tomorrow"
        today.minusDays(1) -> "Yesterday"
        else -> shortDate(date)
    }

    fun weekRange(start: LocalDate): String {
        val end = start.plusDays(6)
        val m1 = start.month.getDisplayName(TextStyle.SHORT, locale)
        val m2 = end.month.getDisplayName(TextStyle.SHORT, locale)
        return if (start.month == end.month) "$m1 ${start.dayOfMonth}–${end.dayOfMonth}" else "$m1 ${start.dayOfMonth} – $m2 ${end.dayOfMonth}"
    }

    fun weekNumber(date: LocalDate): Int = date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)

    fun dayLetter(date: LocalDate): String = date.dayOfWeek.getDisplayName(TextStyle.NARROW, locale)

    fun dayName(date: LocalDate): String = date.dayOfWeek.getDisplayName(TextStyle.FULL, locale)

    fun count(n: Int, one: String, many: String = one + "s"): String = if (n == 1) "1 $one" else "$n $many"

    val isoDate: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
}
