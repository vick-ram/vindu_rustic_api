package org.example.controllers

import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.request.receive
import io.ktor.server.request.receiveMultipart
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.util.toMap
import org.example.domain.models.Category
import org.example.plugins.BadRequestException
import org.example.services.CategoryService
import org.example.utils.respondApi
import org.example.utils.saveMedia

class CategoryController(private val categoryService: CategoryService) {
    fun Route.categoryRoutes() {
        route("/categories/") {
            post {
                val multipart = call.receiveMultipart()
                var categoryRequest: Category? = null
                var imageUrl: String? = null

                multipart.forEachPart { part ->
                    when (part) {
                        is PartData.FormItem -> {
                            when (part.name) {
                                "name" -> categoryRequest = (categoryRequest ?: Category()).copy(name = part.value)
                                "slug" -> categoryRequest =
                                    categoryRequest?.copy(slug = part.value) ?: Category(slug = part.value)

                                "description" -> categoryRequest = categoryRequest?.copy(description = part.value)
                                    ?: Category(description = part.value)

                                "displayOrder" -> categoryRequest =
                                    categoryRequest?.copy(displayOrder = part.value.toInt())
                                        ?: Category(displayOrder = part.value.toInt())
                            }
                        }

                        is PartData.FileItem -> {
                            if (part.name == "image") {
                                val fileName = saveMedia("uploads/categories/", part)
                                val url = "/media/categories/$fileName"
                                imageUrl = url
                            }
                        }

                        else -> {}
                    }
                    part.dispose()
                }
                val request = categoryRequest ?: throw BadRequestException("Category data is required")
                try {
                    val newCategory = categoryService.createCategory(request.copy(imageUrl = imageUrl))
                    call.respondApi(
                        status = HttpStatusCode.Created,
                        data = newCategory,
                        message = "Category created successfully"
                    )
                } catch (e: Exception) {
                    throw e
                }
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