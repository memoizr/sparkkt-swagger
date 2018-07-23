package com.emoticast.sparktswagger

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import com.emoticast.extensions.json
import com.emoticast.extensions.print
import com.emoticast.sparktswagger.documentation.*
import com.emoticast.sparktswagger.documentation.Server
import com.google.gson.Gson
import org.slf4j.LoggerFactory
import spark.Request
import spark.Response
import spark.Service
import kotlin.reflect.KClass
import kotlin.reflect.full.starProjectedType

class Router(val config: Config, val service: Service) {
    data class EndpointBundle<T : Any>(val endpoint: Endpoint<T>, val response: KClass<*>, val function: (Request, Response) -> String)

    val endpoints = mutableListOf<EndpointBundle<*>>()
    val destination = "/tmp/swagger-ui/foobar"

    init {
        service.externalStaticFileLocation(destination)
    }

    infix fun String.GET(path: String) = Endpoint(HTTPMethod.GET, this, path.leadingSlash, emptySet(), emptySet(), emptySet(), Body(Nothing::class))
    infix fun String.GET(path: ParametrizedPath) = Endpoint(HTTPMethod.GET, this, path.path.leadingSlash, path.pathParameters, emptySet(), emptySet(), Body(Nothing::class))

    infix fun String.POST(path: String) = Endpoint(HTTPMethod.POST, this, path.leadingSlash, emptySet(), emptySet(), emptySet(), Body(Nothing::class))
    infix fun String.POST(path: ParametrizedPath) = Endpoint(HTTPMethod.POST, this, path.path.leadingSlash, path.pathParameters, emptySet(), emptySet(), Body(Nothing::class))

    infix fun String.PUT(path: String) = Endpoint(HTTPMethod.PUT, this, path.leadingSlash, emptySet(), emptySet(), emptySet(), Body(Nothing::class))
    infix fun String.PUT(path: ParametrizedPath) = Endpoint(HTTPMethod.PUT, this, path.path.leadingSlash, path.pathParameters, emptySet(), emptySet(), Body(Nothing::class))

    infix fun String.DELETE(path: String) = Endpoint(HTTPMethod.DELETE, this, path.leadingSlash, emptySet(), emptySet(), emptySet(), Body(Nothing::class))
    infix fun String.DELETE(path: ParametrizedPath) = Endpoint(HTTPMethod.DELETE, this, path.path.leadingSlash, path.pathParameters, emptySet(), emptySet(), Body(Nothing::class))

    operator fun String.div(path: String) = this.leadingSlash + "/" + path
    operator fun String.div(path: PathParam<out Any>) = ParametrizedPath(this + "/{${path.name}}", setOf(path))


    inline infix fun <B : Any, reified T : Any> Endpoint<B>.isHandledBy(noinline block: Bundle<B>.() -> HttpResponse<T>) {

        val function: (Request, Response) -> String = { request, response ->
            val invalidParams = getInvalidParams(request)
            if (invalidParams.isNotEmpty()) {
                response.status(400)
                invalidParams.foldRight(emptyList<String>()) { error, acc -> acc + error }.let { Gson().toJson(ClientError(400, it)) }
            } else try {
                block(Bundle(body, (headerParams + queryParams + pathParams), request, response)).let {
                    response.status(it.code)
                    response.type(ContentType.APPLICATION_JSON.value)
                    when (it) {
                        is SuccessfulHttpResponse -> Gson().toJson(it.body)
                        is ErrorHttpResponse -> Gson().toJson(it)
                    }
                }
            } catch (unregisteredException: UnregisteredParamException) {
                val param = unregisteredException.param

                val type = when (param) {
                    is HeaderParameter -> "header"
                    is QueryParameter -> "query"
                    is PathParam -> "path"
                }
                Gson().toJson(ErrorHttpResponse<T>(500, listOf("Attempting to use unregistered $type parameter `${param.name}`")))
            }
        }

        endpoints.add(EndpointBundle(this, T::class, function))
        val path = config.basePath + url.replace("/{", "/:").replace("}", "")
        when (httpMethod) {
            HTTPMethod.GET -> service.get(path, function)
            HTTPMethod.POST -> service.post(path, function)
            HTTPMethod.PUT -> service.put(path, function)
            HTTPMethod.PATCH -> service.patch(path, function)
            HTTPMethod.HEAD -> service.head(path, function)
            HTTPMethod.DELETE -> service.delete(path, function)
            HTTPMethod.OPTIONS -> service.options(path, function)
        }
    }

    fun generateDocs(): OpenApi {
        val openApi = OpenApi(info = Info(config.title, "1"), servers = listOf(Server(config.host)))
        return endpoints
                .groupBy { it.endpoint.url }
                .map {
                    it.key to it.value.foldRight(Path()) { a: EndpointBundle<*>, path ->
                        path.withOperation(
                                a.endpoint.httpMethod,
                                Operation(
                                        tags = a.endpoint.url.split("/").print().drop(2).firstOrNull()?.let { listOf(it) },
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
                }
    }

    fun OpenApi.serveDocs() {
    }
}

val String.leadingSlash get() = if (!startsWith("/")) "/" + this else this

class Server(val config: Config) {
    val http by lazy { Service.ignite().port(config.port) }


    fun startWithRoutes(router: Router.() -> Unit): Router {
        val logger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger
        logger.level = config.logLevel

        val router = Router(config, http)
        router(router)
        return router
    }

    fun stop() {
        http.stop()
    }
}

data class Config(
        val description: String,
        val title: String,
        val host: String,
        val port: Int = 3000,
        val logLevel: Level,
        val basePath: String,
        val docPath: String,
        val termsOfService: String? = null,
        val contact: ConfigContact? = null,
        val license: ConfigLicense? = null,
        val project: Project? = null,
        val externalDoc: ExternalDoc? = null,
        val schemes: List<Scheme> = listOf(Scheme.HTTP),
        val theme: Theme = Theme.MATERIAL,
        val deepLinking: Boolean = false,
        val displayOperationId: Boolean = false,
        val defaultModelsExpandDepth: Int = 1,
        val defaultModelExpandDepth: Int = 1,
        val defaultModelRendering: ModelRendering = ModelRendering.model,
        val displayRequestDuration: Boolean = false,
        val docExpansion: DocExpansion = DocExpansion.FULL,
        val filter: Boolean = false,
        val showExtensions: Boolean = true,
        val showCommonExtensions: Boolean = true,
        val operationsSorter: String = "alpha",
        val tagsSorter: String = "alpha"
)

data class ConfigContact(
        val name: String,
        val email: String,
        val url: String
)

data class ConfigLicense(
        val name: String,
        val url: String)

data class Project(
        val groupId: String,
        val artifactId: String
)


data class ExternalDoc(
        val description: String,
        val url: String
)

enum class DocExpansion { LIST, FULL, NONE }
enum class Theme { OUTLINE, FEELING_BLUE, FLATTOP, MATERIAL, MONOKAI, MUTED, NEWSPAPER }
enum class ModelRendering { example, model }

enum class Scheme(val value: String) {
    HTTP("http"),
    HTTPS("https"),
    WS("ws"),
    WSS("wss");
}
