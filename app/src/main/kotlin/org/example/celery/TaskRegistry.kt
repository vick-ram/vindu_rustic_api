package org.example.celery

import java.util.concurrent.ConcurrentHashMap

class TaskRegistry {
    private val tasks = ConcurrentHashMap<String, CeleryTask>()

    fun register(task: CeleryTask) {
        tasks[task.name] = task
    }

    fun getTask(taskName: String): CeleryTask? = tasks[taskName]

    fun getRegisteredTasks(): List<String> = tasks.keys().toList()
}