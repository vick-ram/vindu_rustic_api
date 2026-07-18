package org.example.config

import io.ktor.server.application.Application
import io.swagger.v3.core.util.Yaml
import java.io.File
import java.nio.file.Path

/**
 * Generates the OpenAPI spec from the factory instance that routes were actually
 * registered on, and writes it to [outputPath].
 *
 * IMPORTANT: call this with the same DynamicRouteFactory used in Application.module(),
 * and only after factory.registerRoutes(routing) has run -- generateOpenApi() reads
 * getHttpRoutes(), which reflects whatever has been added to the factory so far.
 *
 * The default output path is a source-tree location intended for a local/dev codegen
 * step (e.g. a Gradle task run before packaging), NOT for use at production runtime --
 * "src/main/resources/..." won't exist relative to a deployed jar's working directory.
 * If you need this to run at application startup in production, pass an explicit
 * writable path (e.g. from configuration/environment), or better, serve the spec
 * dynamically from a route instead of writing a file at all.
 */
fun configureOpenAPI(
    application: Application,
    factory: DynamicRouteFactory,
    outputPath: Path = File("src/main/resources/docs/documentation.yaml").toPath()
) {
    val openApiGenerator = OpenAPIGenerator(application, factory)
    val openAPI = openApiGenerator.generateOpenApi()

    val yaml = Yaml.pretty().writeValueAsString(openAPI)

    val file = outputPath.toFile()
    file.parentFile?.let { parent ->
        if (!parent.exists()) parent.mkdirs()
    }
    file.writeText(yaml)
}