package org.example.config

import io.ktor.server.application.Application
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.PathItem
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.media.IntegerSchema
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.oas.models.media.ObjectSchema
import io.swagger.v3.oas.models.media.StringSchema
import io.swagger.v3.oas.models.parameters.Parameter
import io.swagger.v3.oas.models.parameters.RequestBody
import io.swagger.v3.oas.models.responses.ApiResponse
import io.swagger.v3.oas.models.responses.ApiResponses
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.slf4j.LoggerFactory

class OpenAPIGenerator(private val application: Application, private val factory: DynamicRouteFactory) {
    fun generateOpenApi(): OpenAPI {
        return OpenAPI().apply {
            info = Info()
                .title("Vindu Rustic API")
                .version("1.0.0")
                .description("Automatically generated from DynamicRouteFactory")
                .summary("This document was automatically generated based on registered routes.")

            components = Components().apply {
                addSecuritySchemes("JWT", SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description("JWT bearer authentication"))
                addSecuritySchemes("Session", SecurityScheme()
                    .type(SecurityScheme.Type.APIKEY)
                    .`in`(SecurityScheme.In.COOKIE)
                    .name("SESSION")
                    .description("Session cookie authentication"))
            }

            // Only HTTP routes carry HTTP methods; Websockets configs have an empty
            // methods set and would silently fall through convertPathItem's
            // dispatch, so they're excluded up front rather than processed for nothing
            val paths = factory.getHttpRoutes()
                .groupBy { it.path }
                .mapValues { (_, routeConfigs) -> convertToPathItem(routeConfigs) }

            paths.forEach { (path, pathItem) ->
                path(path, pathItem)
            }

            if (paths.isEmpty()) {
                application.environment.log.warn(
                    "generateOpenApi() produced zero paths. Make sure generateOpenApi() is called " +
                            "on the same DynamicRouteFactory instance that routes were registered on, " +
                            "and after registration has happened."
                )
            }
        }
    }

    private fun convertToPathItem(routeConfigs: List<DynamicRouteConfig>): PathItem {
        return PathItem().apply {
            routeConfigs.forEach { config ->
                val operation = convertToOperation(config)
                // A single route config can carry multiple HTTP methods (e.g. {GET, POST}),
                // and the factory registers every one of them with Ktor. Iterates all of them
                // Here too, instead of matching only the first method in  fixed priority order.
                config.methods.forEach { method ->
                    when (method) {
                        HttpMethodType.GET -> get(operation)
                        HttpMethodType.POST -> post(operation)
                        HttpMethodType.PUT -> put(operation)
                        HttpMethodType.PATCH -> patch(operation)
                        HttpMethodType.DELETE -> delete(operation)
                        HttpMethodType.HEAD -> head(operation)
                        HttpMethodType.OPTIONS -> options(operation)
                    }
                }
            }
        }
    }

    private fun convertToOperation(config: DynamicRouteConfig) : Operation {
        return Operation().apply {
            summary = config.name ?: config.metadata["summary"]?.toString() ?: config.path
            description = "Handler: ${config.handler::class.simpleName}"
            tags = listOfNotNull(config.metadata["tag"]?.toString())

            if (config.requiresAuth) {
                val req = SecurityRequirement()
                when (config.authType) {
                    AuthType.JWT -> req.addList("JWT")
                    AuthType.SESSION -> req.addList("Session")
                    AuthType.ANY -> {
                        req.addList("JWT")
                        req.addList("Session")
                    }
                    AuthType.NONE -> {}
                }
                addSecurityItem(req)
            }
            val pathParams = extractPathParameters(config.path)
            parameters = pathParams.map { paramName ->
                Parameter()
                    .name(paramName)
                    .`in`("path")
                    .required(true)
                    .schema(StringSchema())
            }.toMutableList()

            if (config.methods.contains(HttpMethodType.GET)) {
                parameters?.addAll(extractCommonQueryParameters())
            }

            responses = ApiResponses().apply {
                addApiResponse("200", ApiResponse().description("Success"))
                addApiResponse("400", ApiResponse().description("Bad Request"))
                addApiResponse("401", ApiResponse().description("Unauthorized"))
                addApiResponse("404", ApiResponse().description("Not Found"))
                addApiResponse("500", ApiResponse().description("Internal Server Error"))
            }

            if (config.methods.any { it in setOf(HttpMethodType.POST, HttpMethodType.PUT, HttpMethodType.PATCH) }) {
                requestBody = createRequestBody()
            }
        }
    }

    /**
     * Extracts path parameter names, stripping Ktor's constrained-parameter suffix
     * (e.g. {id:[0-9]+} -> "id") so the OpenAPI parameter name is the actual
     * variable name rather than the full regex constraint.
     */
    private fun extractPathParameters(path: String): List<String> {
        val pattern = Regex("\\{(.*?)\\??}")
        return pattern.findAll(path).map { match ->
            match.groupValues[1].substringBefore(":")
        }.toList()
    }

    private fun extractCommonQueryParameters(): List<Parameter> {
        return listOf(
            Parameter()
                .name("offset")
                .`in`("query")
                .required(false)
                .schema(IntegerSchema().example(0)),
            Parameter()
                .name("limit")
                .`in`("query")
                .required(false)
                .schema(IntegerSchema().example(10))
        )
    }

    private fun createRequestBody() : RequestBody {
        val mediaType = MediaType().schema(ObjectSchema().description("Request body"))
        return RequestBody().apply {
            content = Content().apply {
                addMediaType("application/json", mediaType)
            }
        }
    }
}