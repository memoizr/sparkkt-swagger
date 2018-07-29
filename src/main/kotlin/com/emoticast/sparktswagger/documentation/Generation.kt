package com.emoticast.sparktswagger.documentation

import com.emoticast.sparktswagger.Parameter
import com.emoticast.sparktswagger.Router
import com.emoticast.sparktswagger.extensions.json
import java.io.File
import java.io.FileOutputStream
import kotlin.reflect.full.starProjectedType

fun Router.generateDocs(): Spec {
    val openApi = OpenApi(info = Info(config.title, "1"), servers = listOf(Server(config.host)))
    return endpoints
            .groupBy { it.endpoint.url }
            .map {
                it.key to it.value.foldRight(Path()) { bundle: Router.EndpointBundle<*>, path ->
                    path.withOperation(
                            bundle.endpoint.httpMethod,
                            Operation(
                                    tags = bundle.endpoint.url.split("/").drop(2).firstOrNull()?.let { listOf(it) },
                                    summary = bundle.endpoint.summary,
                                    description = bundle.endpoint.description,
                                    responses = emptyMap())
                                    .withResponse(ContentType.APPLICATION_JSON, bundle.response, "200")
                                    .let {
                                        if (bundle.endpoint.body.klass != Nothing::class) {
                                            it.withRequestBody(ContentType.APPLICATION_JSON, bundle.endpoint.body.klass)
                                        } else it
                                    }
                                    .let {
                                        bundle.endpoint.headerParams.fold(it) { acc, p ->
                                            acc.withParameter(Parameters.HeaderParameter(
                                                    name = p.name,
                                                    required = p.required,
                                                    description = getDescription(p),
                                                    schema = toSchema(p.type.kotlin.starProjectedType).withPattern(p.pattern.regex)

                                            ))
                                        }
                                    }
                                    .let {
                                        bundle.endpoint.pathParams.fold(it) { acc, param ->
                                            acc.withParameter(Parameters.PathParameter(
                                                    name = param.name,
                                                    description = getDescription(param),
                                                    schema = toSchema(param.type.kotlin.starProjectedType).withPattern(param.pattern.regex)
                                            ))
                                        }
                                    }
                                    .let {
                                        bundle.endpoint.queryParams.fold(it) { acc, p ->
                                            acc.withParameter(Parameters.QueryParameter(
                                                    name = p.name,
                                                    description = getDescription(p),
                                                    allowEmptyValue = p.emptyAsMissing,
                                                    required = p.required,
                                                    schema = toSchema(p.type.kotlin.starProjectedType).withPattern(p.pattern.regex)

                                            ))
                                        }
                                    }
                    )
                }
            }.fold(openApi) { a, b -> a.withPath(b.first, b.second) }
            .let {  Spec(it.json, this) }
}

data class Spec(val spec: String, val router: Router) {

    fun writeDocsToStaticFolder() {
        val dest = "/tmp/swagger-ui" + "/docs"
        copyResourceToFile("index.html", dest)
        copyResourceToFile("swagger-ui.css", dest)
        copyResourceToFile("swagger-ui.js", dest)
        copyResourceToFile("swagger-ui-bundle.js", dest)
        copyResourceToFile("swagger-ui-standalone-preset.js", dest)
        writeToFile(spec, "$dest/spec.json")
    }
}

private fun getDescription(param: Parameter<*>) =
        "${param.description} - ${param.pattern.description}${if (param.invalidAsMissing) " - Invalid as Missing" else ""}${if (param.emptyAsMissing) " - Empty as Missing" else ""}"

internal fun writeToFile(content: String, destination: String) {
    File(destination.split("/").dropLast(1).joinToString("")).apply { if (!exists()) mkdirs() }
    content.byteInputStream().use { input ->
        FileOutputStream(destination).use { output ->
            input.copyTo(output)
        }
    }
}

internal fun copyResourceToFile(resourceName: String, destination: String) {
    val stream = ClassLoader.getSystemClassLoader().getResourceAsStream(resourceName)
            ?: throw Exception("Cannot get resource \"$resourceName\" from Jar file.")
    val s = "$destination/$resourceName"
    File(destination).apply { if (!exists()) mkdirs() }
    val resStreamOut = FileOutputStream(s)
    stream.use { input ->
        resStreamOut.use { output ->
            input.copyTo(output)
        }
    }
}
