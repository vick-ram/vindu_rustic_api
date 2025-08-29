package org.example.controllers

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.util.toMap
import org.example.domain.models.Category
import org.example.services.CategoryService
import org.example.utils.respondApi

class CategoryController(private val categoryService: CategoryService) {
    fun Route.categoryRoutes() {
        route("/category/") {
            post {
                val categoryRequest = call.receive<Category>().validate()
                val newCategory = categoryService.createCategory(categoryRequest)
                call.respondApi(
                    status = HttpStatusCode.Created,
                    data = newCategory,
                    message = "Category created successfully"
                )
            }

            get("{slug}") {
                val slug = call.parameters["slug"] ?: ""
                val category = categoryService.getCategoryBySlug(slug)
                call.respondApi(
                    data = category,
                    message = "Category data fetched"
                )
            }

            get("{id}") {
                val id = call.parameters["id"] ?: ""
                val category = categoryService.getCategory(id)
                call.respondApi(
                    data = category,
                    message = "Category data fetched"
                )
            }

            get {
                val offset = call.request.queryParameters["offset"]?.toIntOrNull() ?: 0
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 10
                val queryParams = call.request.queryParameters.toMap().mapValues { it.value.firstOrNull() ?: "" }
                    .filterKeys { it != "offset" && it != "limit" }

                val categories = categoryService.getCategories(offset, limit, queryParams)
                call.respondApi(
                    data = categories,
                    message = "Categories data fetched"
                )
            }

            put("{id}") {
                val categoryId = call.parameters["id"] ?: ""
                val categoryRequest = call.receive<Category>()
                val updatedCategory = categoryService.updateCategory(categoryId, categoryRequest)
                call.respondApi(
                    status = HttpStatusCode.Accepted,
                    data = updatedCategory,
                    message = "Category updated successfully"
                )
            }

            delete("{id}") {
                val categoryId = call.parameters["id"] ?: ""
                val deleted = categoryService.deleteCategory(categoryId)
                if (deleted) {
                    call.respondApi<Unit>(
                        status = HttpStatusCode.NoContent,
                        message = "Category deleted successfully"
                    )
                }
            }
        }
    }
}