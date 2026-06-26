package org.example.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import java.io.File

fun configureOpenAPI() {
    val factory = DynamicRouteFactory()
    val openApiGenerator = OpenAPIGenerator(factory)
    val objectMapper = ObjectMapper(YAMLFactory())

    val openAPI = openApiGenerator.generateOpenApi()
    val yaml = objectMapper.writeValueAsString(openAPI)

    val folder = File("src/main/resources/docs")
    if (!folder.exists()) {
        folder.mkdirs()
    }
    val file = File(folder, "documentation.yaml")
    file.writeText(yaml)
}