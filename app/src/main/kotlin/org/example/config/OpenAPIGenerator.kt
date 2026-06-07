package org.example.config

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

class OpenAPIGenerator(private val factory: DynamicRouteFactory) {
    fun generateOpenApi(): OpenAPI {
        return OpenAPI().apply {
            info = Info()
                .title("Vindu Rustic Lights API")
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

            val paths = factory.getAllRoutes()
                .groupBy { it.path }
                .mapValues { (_, routeConfig) ->
                    convertToPathItem(routeConfig)
                }

            paths.forEach { (path, pathItem) ->
                path(path, pathItem)
            }
        }
    }

    private fun convertToPathItem(routeConfigs: List<DynamicRouteConfig>): PathItem {
        return PathItem().apply {
            routeConfigs.forEach { config ->
                val operation = convertToOperation(config)
                when {
                    config.methods.contains(HttpMethodType.GET) -> get(operation)
                    config.methods.contains(HttpMethodType.POST) -> post(operation)
                    config.methods.contains(HttpMethodType.PUT) -> put(operation)
                    config.methods.contains(HttpMethodType.PATCH) -> patch(operation)
                    config.methods.contains(HttpMethodType.DELETE) -> delete(operation)
                    config.methods.contains(HttpMethodType.HEAD) -> head(operation)
                    config.methods.contains(HttpMethodType.OPTIONS) -> options(operation)
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
                    else -> {}
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
            }

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

    private fun extractPathParameters(path: String): List<String> {
        val pattern = Regex("\\{(.*?)}")
        return pattern.findAll(path).map { it.groupValues[1] }.toList()
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