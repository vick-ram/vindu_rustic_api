package org.example.celery

data class PeriodicTask(
    val name: String,
    val task: TaskSignature,
    val schedule: Schedule,
    val enabled: Boolean = true
)

sealed class Schedule {
    data class Timedelta(val seconds: Long) : Schedule()
    data class Crontab(val crontab: CrontabSchedule) : Schedule()

    companion object {
        fun every(seconds: Long) = Timedelta(seconds)
        fun crontab(
            minute: String = "*",
            hour: String = "*",
            dayOfWeek: String = "*",
            dayOfMonth: String = "*",
            monthOfYear: String = "*"
        ) = Crontab(CrontabSchedule(minute, hour, dayOfWeek, dayOfMonth, monthOfYear))
    }
}
