package org.example.celery

import java.time.Instant
import java.time.ZoneId

class CrontabSchedule(
    private val minute: String = "*",
    private val hour: String = "*",
    private val dayOfWeek: String = "*",
    private val dayOfMonth: String = "*",
    private val monthOfYear: String = "*"
) {
    fun isDue(now: Instant): Boolean {
        val zonedNow = now.atZone(ZoneId.systemDefault())

        return matchesField(zonedNow.minute, minute) &&
        matchesField(zonedNow.hour, hour) &&
                matchesField(zonedNow.dayOfWeek.value, dayOfWeek) &&
                matchesField(zonedNow.dayOfMonth, dayOfMonth) &&
                matchesField(zonedNow.monthValue, monthOfYear)
    }

    private fun matchesField(value: Int, field: String): Boolean {
        return when {
            field == "*" -> true
            field.contains(",") -> field.split(",").any { matchesField(value, it) }
            field.contains("/") ->{
                val (range, step) = field.split("/")
                val stepVal =step.toInt()
                when (range) {
                    "*" -> value % stepVal == 0
                    else -> {
                        val (start, end) = if (range.contains("-")) {
                            range.split("-").let { it[0].toInt() to it[1].toInt() }
                        } else {
                            range.toInt() to range.toInt()
                        }
                        value in start..end && (value - start) % stepVal == 0
                    }
                }
            }
            field.contains("-") -> {
                val (start, end) = field.split("-").map { it.toInt() }
                value in start..end
            }
            else -> value == field.toInt()
        }
    }
}