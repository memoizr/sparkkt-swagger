package com.emoticast.sparktswagger.documentation

import com.emoticast.extensions.json
import com.emoticast.sparktswagger.Router
import kotlin.reflect.full.starProjectedType

fun Router.generateDocs(): String {
    val openApi = OpenApi(info = Info(config.title, "1"), servers = listOf(Server(config.host)))
    return endpoints
            .groupBy { it.endpoint.url }
            .map {
                it.key to it.value.foldRight(Path()) { a: Router.EndpointBundle<*>, path ->
                    path.withOperation(
                            a.endpoint.httpMethod,
                            Operation(
                                    tags = a.endpoint.url.split("/").drop(2).firstOrNull()?.let { listOf(it) },
                                    summary = a.endpoint.description,
                                    responses = emptyMap())
                                    .withResponse(ContentType.APPLICATION_JSON, a.response, "200")
                                    .let {
                                        if (a.endpoint.body.klass != Nothing::class) {
                                            it.withRequestBody(ContentType.APPLICATION_JSON, a.endpoint.body.klass)
                                        } else it
                                    }
                                    .let {
                                        a.endpoint.headerParams.fold(it) { acc, p ->
                                            it.withParameter(Parameters.HeaderParameter(
                                                    name = p.name,
                                                    required = p.required,
                                                    description = p.description,
                                                    schema = toSchema(p.type.kotlin.starProjectedType)

                                            ))
                                        }
                                    }
                                    .let {
                                        a.endpoint.pathParams.fold(it) { acc, p ->
                                            it.withParameter(Parameters.PathParameter(
                                                    name = p.name,
                                                    description = p.description,
                                                    schema = toSchema(p.type.kotlin.starProjectedType)
                                            ))
                                        }
                                    }
                                    .let {
                                        a.endpoint.queryParams.fold(it) { acc, p ->
                                            it.withParameter(Parameters.QueryParameter(
                                                    name = p.name,
                                                    description = p.description,
                                                    allowEmptyValue = p.emptyAsMissing,
                                                    required = p.required,
                                                    schema = toSchema(p.type.kotlin.starProjectedType)

                                            ))
                                        }
                                    }
                    )
                }
            }.fold(openApi) { a, b -> a.withPath(b.first, b.second) }.apply {
                copyResourceToFile("index.html", destination)
                copyResourceToFile("swagger-ui.css", destination)
                copyResourceToFile("swagger-ui.js", destination)
                copyResourceToFile("swagger-ui-bundle.js", destination)
                copyResourceToFile("swagger-ui-standalone-preset.js", destination)
                writeToFile(this.json, "$destination/${config.docPath}.json")
            }.json
}