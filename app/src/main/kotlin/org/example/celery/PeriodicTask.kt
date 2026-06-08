package org.example.celery

data class PeriodicTask(
    val name: String,
    val task: TaskSignature,
    val schedule: Schedule,
)

sealed class Schedule {
    data class Timedelta(val seconds: Long) : Schedule()
    data class Crontab(val expression: String) : Schedule()

    companion object {
        fun every(seconds: Long) = Timedelta(seconds)
        fun crontab(expression: String) = Crontab(expression)
    }
}
