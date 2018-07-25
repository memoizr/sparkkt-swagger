package com.emoticast.sparktswagger

import com.emoticast.sparktswagger.documentation.ContentType
import com.emoticast.sparktswagger.extensions.json
import com.google.gson.Gson
import spark.Request
import spark.Response
import spark.Service
import kotlin.reflect.KClass


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


    inline infix fun <B : Any, reified T : Any> Endpoint<B>.isHandledBy(noinline block: RequestHandler<B>.() -> HttpResponse<T>) {
        endpoints += EndpointBundle(this, T::class) { request, response ->
            val invalidParams = getInvalidParams(request)
            if (invalidParams.isNotEmpty()) {
                response.status(400)
                invalidParams.foldRight(emptyList<String>()) { error, acc -> acc + error }.let { Gson().toJson(HttpResponse.ErrorHttpResponse<T>(400, it)) }
            } else try {
                block(RequestHandler(body, (headerParams + queryParams + pathParams), request, response)).let {
                    response.status(it.code)
                    response.type(ContentType.APPLICATION_JSON.value)
                    when (it) {
                        is HttpResponse.SuccessfulHttpResponse -> it.body.json
                        is HttpResponse.ErrorHttpResponse -> it.json
                    }
                }
            } catch (unregisteredException: UnregisteredParamException) {
                val param = unregisteredException.param

                val type = when (param) {
                    is HeaderParameter -> "header"
                    is QueryParameter -> "query"
                    is PathParam -> "path"
                }
                HttpResponse.ErrorHttpResponse<T>(500, listOf("Attempting to use unregistered $type parameter `${param.name}`")).json
            }
        }
    }
}

data class ParametrizedPath(val path: String, val pathParameters: Set<PathParam<out Any>>) {
    operator fun div(path: String) = copy(path = this.path + "/" + path)
}

val String.leadingSlash get() = if (!startsWith("/")) "/" + this else this
