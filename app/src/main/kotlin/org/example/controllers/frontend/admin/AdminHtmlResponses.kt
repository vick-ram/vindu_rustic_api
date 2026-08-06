package org.example.controllers.frontend.admin

import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.RoutingContext
import org.example.utils.respondHtml

internal suspend fun RoutingContext.respondAdminList(
    title: String,
    items: List<*>,
    currentPage: String = "operations",
    message: String? = null,
    status: HttpStatusCode = HttpStatusCode.OK
) {
    call.respondHtml(
        template = "admin/pages/operations/list",
        status = status,
        data = items,
        message = message,
        mapData = mutableMapOf(
            "currentPage" to currentPage,
            "title" to title,
            "items" to items
        )
    )
}

internal suspend fun RoutingContext.respondAdminError(title: String, message: String) {
    respondAdminList(
        title = title,
        items = emptyList<Any>(),
        message = message,
        status = HttpStatusCode.BadRequest
    )
}
