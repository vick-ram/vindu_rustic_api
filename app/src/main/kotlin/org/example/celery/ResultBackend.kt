package org.example.celery

interface ResultBackend {
    suspend fun storeResult(taskId: String, result: TaskResult, expirySeconds: Long = 3600)
    suspend fun getResult(taskId: String): TaskResult?
    suspend fun close()
}