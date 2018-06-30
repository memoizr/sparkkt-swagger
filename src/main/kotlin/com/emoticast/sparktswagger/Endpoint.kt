package com.emoticast.sparktswagger

import com.beerboy.ss.SparkSwagger
import com.beerboy.ss.descriptor.EndpointDescriptor.endpointPath
import com.beerboy.ss.descriptor.MethodDescriptor
import com.beerboy.ss.descriptor.ParameterDescriptor
import com.emoticast.extensions.json
import com.emoticast.extensions.parseJson
import spark.Request
import spark.Response
import kotlin.reflect.KClass

fun queries(vararg queryParameter: QueryParam<*>) = queryParameter.asList()
fun headers(vararg headerParameter: HeaderParam<*>) = headerParameter.asList()

inline fun <reified T : Any> body() = Body(T::class)

class Body<T : Any>(val klass: KClass<T>)


typealias SomeBodyBundle<A, B> = Controller<A>.() -> HttpResponse<B>
typealias NoBodyBundle<B> = Controller<Any>.() -> HttpResponse<B>

data class Controller<T : Any>(
        val klass: KClass<T>?,
        val request: Request,
        val response: Response
) {
    val body: T by lazy { request.body()!!.parseJson(klass!!) }
}

data class Endpoint<B : Any>(
        val httpMethod: HTTPMethod,
        val description: String,
        val swagger: SparkSwagger,
        val url: String,
        val pathParams: List<PathParam<out Any>>,
        val queryParams: List<QueryParam<*>>,
        val headerParams: List<HeaderParam<*>>,
        val body: Body<B>? = null) {

    val path by lazy { url.split("/").dropLast(1).joinToString("/") }
    val resourceName by lazy { "/" + url.split("/").last() }

    infix fun with(queryParameter: QueryParam<*>) = copy(queryParams = queryParams + queryParameter)
    infix fun with(params: HeaderParam<*>) = copy(headerParams = headerParams + params)
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
                is HeaderParam -> endpoint with param
                is QueryParam -> endpoint with param
                else -> throw IllegalArgumentException(param.toString())
            }
        }
    }
    inline infix fun <reified T : Any> isHandledBy(noinline block: Controller<B>.() -> HttpResponse<T>) {
        val withResponseType = MethodDescriptor.path(resourceName)
                .withDescription(description)
                .withResponseType(T::class)
                .apply {
                    body?.let { withRequestType(it.klass) }
                    headerParams.forEach { withHeaderParam(it.toParameterDescriptor()) }
                    queryParams.forEach { withQueryParam(it.toParameterDescriptor()) }
                    pathParams.forEach { withPathParam(it.toParameterDescriptor()) }
                }

        val endpoint = swagger.endpoint(endpointPath(path), { _, _ -> null })
        val function: (Request, Response) -> String = { request, response ->
            val invalidParams = getInvalidParams(request)
            if (invalidParams.isNotEmpty()) {
                response.status(400)
                invalidParams.foldRight(emptyList<String>()) { error, acc -> acc + error }.let { ClientError(400, it).json }
            } else block(Controller(body?.klass, request, response)).let {
                response.status(it.code)
                when (it) {
                    is SuccessfulHttpResponse -> it.body.json
                    is ErrorHttpResponse -> it.json
                }
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
        .withDescription("$description -- ${pattern.description}")
        .withPattern(pattern.regex.toString())
        .withAllowEmptyValue(allowEmptyValues)
        .withRequired(required)
        .build()

fun Request.getPathParam(param: PathParam<*>) = params(param.name)
        .let { if (it != null && it.isEmpty()) null else it }

fun Request.getQueryParam(param: QueryParam<*>) = queryParams(param.name)
        .let { if (it != null && it.isEmpty()) null else it }

fun Request.getHeaderParam(param: HeaderParam<*>) = headers(param.name)
        .let { if (it != null && it.isEmpty()) null else it }

inline operator fun <reified T : Any> Request.get(param: PathParam<T>): T = (params(param.name)
        .let { if (it != null && it.isEmpty()) null else it }
        .let { if (T::class.java.isInstance(0)) it?.toInt() else it } ?: param.default) as T

inline operator fun <reified T : Any?> Request.get(param: QueryParam<T>): T = (queryParams(param.name)
        .let { if (it != null && it.isEmpty()) null else it }
        .let { if (T::class.java.isInstance(0)) it?.toInt() else it } ?: param.default) as T

inline operator fun <reified T : Any?> Request.get(param: HeaderParam<T>): T = (headers(param.name)
        .let { if (it != null && it.isEmpty()) null else it }
        .let { if (T::class.java.isInstance(0)) it?.toInt() else it } ?: param.default) as T

sealed class HttpResponse<T> {
    abstract val code: Int
}

fun <T> T.success(code: Int = 200): HttpResponse<T> = SuccessfulHttpResponse(code, this)

val <T> T.ok: HttpResponse<T> get() = SuccessfulHttpResponse(200, this)
val <T> T.created: HttpResponse<T> get() = SuccessfulHttpResponse(201, this)


fun <T> badRequest(message: String, code: Int = 400) = ErrorHttpResponse<T>(code, listOf(message))
fun <T> forbidden(message: String) = ErrorHttpResponse<T>(403, listOf(message))

data class SuccessfulHttpResponse<T>(override val code: Int,
                                val body: T) : HttpResponse<T>()

data class ErrorHttpResponse<T>(override val code: Int,
                           val message: List<String>): HttpResponse<T>()


