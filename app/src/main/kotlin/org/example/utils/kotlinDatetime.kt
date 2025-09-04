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

    val hour = this.hour
    val minute = this.minute

    val (hour12, amPm) = when {
        hour == 0 -> 12 to "AM"
        hour < 12 -> hour to "PM"
        hour == 12 -> 12 to "AM"
        else -> (hour - 12) to "PM"
    }

    val formattedTime = minute.toString().padStart(2, '0')
    val time = "$hour12:$formattedTime $amPm"

    val dayWithSuffix = "$day${daySuffix(day)}"
    val monthShort = monthAbbreviation(month)

    return "$dayWithSuffix $monthShort $year, $time"
}

fun daySuffix(day: Int): String = when {
    day in 11..13 -> "TH"
    day % 10 == 1 -> "ST"
    day % 10 == 2 -> "ND"
    day % 10 == 3 -> "RD"
    else -> "TH"
}

fun monthAbbreviation(month: Month): String = when (month) {
    Month.JANUARY -> "JAN"
    Month.FEBRUARY -> "FEB"
    Month.MARCH -> "MAR"
    Month.APRIL -> "APR"
    Month.MAY -> "MAY"
    Month.JUNE -> "JUN"
    Month.JULY -> "JUL"
    Month.AUGUST -> "AUG"
    Month.SEPTEMBER -> "SEP"
    Month.OCTOBER -> "OCT"
    Month.NOVEMBER -> "NOV"
    Month.DECEMBER -> "DEC"
}
