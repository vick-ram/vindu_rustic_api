package org.example.utils

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
fun LocalDateTime.Companion.now(): LocalDateTime {
    return Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
}

@OptIn(ExperimentalTime::class)
fun LocalDateTime.Companion.currentUtc(): LocalDateTime {
    return Clock.System.now().toLocalDateTime(TimeZone.UTC)
}

fun LocalDateTime.toCustomFormat(): String {
    val day = this.date.day
    val month = this.date.month
    val year = this.date.year

    val dayWithSuffix = "$day${daySuffix(day)}"
    val monthShort = monthAbbreviation(month)

    return "$dayWithSuffix $monthShort $year"
}

fun daySuffix(day: Int): String = when {
    day in 11..13 -> "th"
    day % 10 == 1 -> "st"
    day % 10 == 2 -> "nd"
    day % 10 == 3 -> "rd"
    else -> "th"
}

fun monthAbbreviation(month: Month): String = when (month) {
    Month.JANUARY -> "Jan"
    Month.FEBRUARY -> "Feb"
    Month.MARCH -> "Mar"
    Month.APRIL -> "Apr"
    Month.MAY -> "May"
    Month.JUNE -> "Jun"
    Month.JULY -> "Jul"
    Month.AUGUST -> "Aug"
    Month.SEPTEMBER -> "Sep"
    Month.OCTOBER -> "Oct"
    Month.NOVEMBER -> "Nov"
    Month.DECEMBER -> "Dec"
}
