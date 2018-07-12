package com.emoticast.sparktswagger

import com.beerboy.ss.SparkSwagger
import com.beerboy.ss.descriptor.EndpointDescriptor.endpointPath
import com.beerboy.ss.descriptor.MethodDescriptor
import com.beerboy.ss.descriptor.ParameterDescriptor
import com.google.gson.Gson
import spark.Request
import spark.Response
import kotlin.reflect.KClass

fun queries(vararg queryParameter: QueryParameter<*>) = queryParameter.asList()
fun headers(vararg headerParameter: HeaderParameter<*>) = headerParameter.asList()

inline fun <reified T : Any> body(gson: Gson = Gson()) = Body(T::class, gson)

data class Body<T : Any>(val klass: KClass<T>, val gson: Gson)


typealias Controller<BODY_TYPE, RESPONSE_TYPE> = Bundle<BODY_TYPE>.() -> HttpResponse<RESPONSE_TYPE>

data class Bundle<T : Any>(
        private val _body: Body<T>?,
        val params: Set<Parameter<*>>,
        val request: Request,
        val response: Response
) {
    val body: T by lazy { _body?.gson?.fromJson(request.body()!!, _body.klass.java)!! }

    inline operator fun <reified T : Any> Request.get(param: PathParam<T>): T =
            checkParamIsRegistered(param)
                    .params(param.name)
                    .let { param.pattern.parse(it) }

    inline operator fun <reified T : Any?> Request.get(param: QueryParam<T>): T =
            checkParamIsRegistered(param)
                    .queryParams(param.name)
                    .let { param.pattern.parse(it) }

    inline operator fun <reified T : Any?> Request.get(param: OptionalQueryParam<T>): T =
            checkParamIsRegistered(param)
                    .queryParams(param.name)
                    .filterValid(param)
                    ?.let { param.pattern.parse(it) } ?: param.default

    inline operator fun <reified T : Any?> Request.get(param: HeaderParam<T>): T =
            checkParamIsRegistered(param)
                    .headers(param.name)
                    .let { param.pattern.parse(it) }

    inline operator fun <reified T : Any?> Request.get(param: OptionalHeaderParam<T>): T =
            checkParamIsRegistered(param)
                    .headers(param.name)
                    .filterValid(param)
                    ?.let { param.pattern.parse(it) } ?: param.default

    fun Request.checkParamIsRegistered(param: Parameter<*>) = if (!params.contains(param)) throw UnregisteredParamException(param) else this
}

data class UnregisteredParamException(val param: Parameter<*>) : Throwable()

data class Endpoint<B : Any>(
        val httpMethod: HTTPMethod,
        val description: String,
        val swagger: SparkSwagger,
        val url: String,
        val pathParams: Set<PathParam<out Any>>,
        val queryParams: Set<QueryParameter<*>>,
        val headerParams: Set<HeaderParameter<*>>,
        val body: Body<B>) {

    val path by lazy { url.split("/").dropLast(1).joinToString("/") }
    val resourceName by lazy { "/" + url.split("/").last() }

    infix fun with(queryParameter: QueryParameter<*>) = copy(queryParams = queryParams + queryParameter)
    infix fun with(params: HeaderParameter<*>) = copy(headerParams = headerParams + params)
    infix fun <C : Any> with(body: Body<C>) = Endpoint(
            httpMethod,
            description,
            swagger,
            url,
            pathParams,
            queryParams,
            headerParams,
            body
    )

    infix fun with(queryParameter: List<Parameter<*>>) = let {
        queryParameter.foldRight(this) { param, endpoint ->
            when (param) {
                is HeaderParameter -> endpoint with param
                is QueryParameter -> endpoint with param
                else -> throw IllegalArgumentException(param.toString())
            }
        }
    }

    inline infix fun <reified T : Any> isHandledBy(noinline block: Bundle<B>.() -> HttpResponse<T>) {
        val withResponseType = MethodDescriptor.path(resourceName)
                .withSummary(description)
                .withResponseType(T::class)
                .apply {
                    if (body.klass != Nothing::class) {
                        withRequestType(body.klass)
                    }
                    headerParams.forEach { withHeaderParam(it.toParameterDescriptor()) }
                    queryParams.forEach { withQueryParam(it.toParameterDescriptor()) }
                    pathParams.forEach { withPathParam(it.toParameterDescriptor()) }
                }

        val endpoint = swagger.endpoint(endpointPath(path), { _, _ -> null })
        val function: (Request, Response) -> String = { request, response ->
            val invalidParams = getInvalidParams(request)
            if (invalidParams.isNotEmpty()) {
                response.status(400)
                invalidParams.foldRight(emptyList<String>()) { error, acc -> acc + error }.let { Gson().toJson(ClientError(400, it)) }
            } else try {
                block(Bundle(body, (headerParams + queryParams + pathParams), request, response)).let {
                    response.status(it.code)
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
        when (httpMethod) {
            HTTPMethod.GET -> endpoint.get(withResponseType, function)
            HTTPMethod.POST -> endpoint.post(withResponseType, function)
            HTTPMethod.PUT -> endpoint.put(withResponseType, function)
            HTTPMethod.DELETE -> endpoint.delete(withResponseType, function)
            HTTPMethod.OPTIONS -> endpoint.options(withResponseType, function)
        }
    }

    fun getInvalidParams(request: Request): List<String> {
        return (pathParams.map { validateParam(it, request.getPathParam(it), "Path") } +
                queryParams.map { validateParam(it, request.getQueryParam(it), "Query") } +
                headerParams.map { validateParam(it, request.getHeaderParam(it), "Header") })
                .filterNotNull()
    }

    private fun validateParam(it: Parameter<*>, value: String?, path: String): String? {
        return when {
            it.required && value == null -> missingParameterMessage(path, it)
            !it.required && value == null -> null
            it.pattern.regex.matches(value.toString()) -> null
            else -> {
                invalidParameterMessage(path, it, value)
            }
        }
    }

    private fun missingParameterMessage(path: String, it: Parameter<*>) =
            """Required $path parameter `${it.name}` is missing"""

    private fun invalidParameterMessage(query: String, it: Parameter<*>, value: String?) =
            """$query parameter `${it.name}` is invalid, expecting ${it.pattern.description}, got `$value`"""
}

data class ClientError(val code: Int, val message: List<String>)

fun Parameter<*>.toParameterDescriptor(): ParameterDescriptor = ParameterDescriptor.newBuilder()
        .withName(name)
        .withDescription("""$description -- ${pattern.description}${if (emptyAsMissing) " -- Empty as Missing" else ""}${if (invalidAsMissing) " -- Invalid as Missing" else ""}""")
        .withPattern(pattern.regex.toString())
        .withAllowEmptyValue(emptyAsMissing)
        .withRequired(required)
        .apply {
            val parameter = this@toParameterDescriptor
            if (parameter is OptionalHeaderParam<*>) {
                withDefaultValue(parameter.default?.toString())
            } else if (parameter is OptionalQueryParam<*>) {
                withDefaultValue(parameter.default?.toString())
            }
        }
        .build()

fun Request.getPathParam(param: PathParam<*>) = params(param.name)
        .let { if (it != null && param.emptyAsMissing && it.isEmpty()) null else it }

fun Request.getQueryParam(param: QueryParameter<*>) = queryParams(param.name).filterValid(param)
fun Request.getHeaderParam(param: HeaderParameter<*>) = headers(param.name).filterValid(param)

fun String?.filterValid(param: Parameter<*>) = when {
    this == null -> null
    param.emptyAsMissing && this.isEmpty() -> null
    param.invalidAsMissing && !param.pattern.regex.matches(this) -> null
    else -> this
}

sealed class HttpResponse<T> {
    abstract val code: Int
}

fun <T> T.success(code: Int = 200): HttpResponse<T> = SuccessfulHttpResponse(code, this)

val <T> T.ok: HttpResponse<T> get() = SuccessfulHttpResponse(200, this)
val <T> T.created: HttpResponse<T> get() = SuccessfulHttpResponse(201, this)


fun <T> badRequest(message: String, code: Int = 400) = ErrorHttpResponse<T>(code, listOf(message))
fun <T> forbidden(message: String) = ErrorHttpResponse<T>(403, listOf(message))
fun <T> notFound() = ErrorHttpResponse<T>(404, listOf("not found"))

data class SuccessfulHttpResponse<T>(override val code: Int,
                                     val body: T) : HttpResponse<T>()

data class ErrorHttpResponse<T>(override val code: Int,
                                val message: List<String>) : HttpResponse<T>()


