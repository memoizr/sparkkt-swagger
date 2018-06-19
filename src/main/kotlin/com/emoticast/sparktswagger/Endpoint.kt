package com.emoticast.sparktswagger

import com.beerboy.ss.SparkSwagger
import com.beerboy.ss.descriptor.EndpointDescriptor.endpointPath
import com.beerboy.ss.descriptor.MethodDescriptor
import com.beerboy.ss.descriptor.ParameterDescriptor
import com.emoticast.extensions.json
import spark.Request
import spark.Response
import kotlin.reflect.KClass

fun queries(vararg queryParameter: QueryParam<*>) = queryParameter.asList()
fun headers(vararg headerParameter: HeaderParam<*>) = headerParameter.asList()

inline fun <reified T : Any> body() = Body(T::class)

class Body<T : Any>(val klass: KClass<T>)

data class Endpoint(
        val httpMethod: HTTPMethod,
        val description: String,
        val swagger: SparkSwagger,
        private val paths: String,
        val pathParams: List<PathParam<out Any>>,
        val queryQueryParameters: List<QueryParam<*>>,
        val headerParams: List<HeaderParam<*>>,
        val body: Body<*>? = null) {

    val path by lazy { paths.split("/").dropLast(1).joinToString("/") }
    val resourceName by lazy { "/" + paths.split("/").last() }

    infix fun with(queryParameter: QueryParam<*>) = copy(queryQueryParameters = queryQueryParameters + queryParameter)
    infix fun with(params: HeaderParam<*>) = copy(headerParams = headerParams + params)
    infix fun with(body: Body<*>) = copy(body = body)

    infix fun with(queryParameter: List<Parameter<*>>) = let {
        queryParameter.foldRight(this) { param, endpoint ->
            when (param) {
                is HeaderParam -> endpoint with param
                is QueryParam -> endpoint with param
                else -> throw IllegalArgumentException(param.toString())
            }
        }
    }

    inline infix fun <reified T : Any> isHandledBy(noinline block: (Request, Response) -> T) {
        val withResponseType = MethodDescriptor.path(resourceName)
                .withDescription(description)
                .withResponseType(T::class)
                .apply {
                    body?.let { withRequestType(it.klass) }
                    headerParams.forEach { withHeaderParam(it.toParameterDescriptor()) }
                    queryQueryParameters.forEach { withQueryParam(it.toParameterDescriptor()) }
                    pathParams.forEach { withPathParam(it.toParameterDescriptor()) }
                }

        val endpoint = swagger.endpoint(endpointPath(path), { _, _ -> null })
        val function: (Request, Response) -> String = { request, response ->
            val invalidParams = getInvalidParams(request)
            if (invalidParams.isNotEmpty()) {
                response.status(400)
                invalidParams.foldRight(emptyList<String>()) { error, acc -> acc + error }.let { ClientError(400, it).json }
            } else block(request, response).json
        }
        when (httpMethod) {
            HTTPMethod.GET -> endpoint.get(withResponseType, function)
            HTTPMethod.POST -> endpoint.post(withResponseType, function)
            HTTPMethod.PUT -> endpoint.put(withResponseType, function)
            HTTPMethod.DELETE -> endpoint.delete(withResponseType, function)
            else -> ""
        }
    }

    fun getInvalidParams(request: Request): List<String> {
        val invalidParams = (pathParams.map {
            val value = request.getPathParam(it)
            val path = "Path"
            when {
                it.required && value == null -> missingParameterMessage(path, it)
                !it.required && value == null -> null
                it.pattern.regex.matches(value.toString()) -> null
                else -> {
                    invalidParameterMessage(path, it, value)
                }
            }
        }
                +
                queryQueryParameters.map {
                    val value = request.getQueryParam(it)
                    val query = "Query"
                    when {
                        it.required && value == null -> missingParameterMessage(path, it)
                        !it.required && value == null -> null
                        it.pattern.regex.matches(value!!) -> null
                        else -> invalidParameterMessage(query, it, value)
                    }
                }
                +
                headerParams.map {
                    val value = request.getHeaderParam(it)
                    val query = "Header"
                    when {
                        it.required && value == null -> missingParameterMessage(path, it)
                        !it.required && value == null -> null
                        it.pattern.regex.matches(value!!) -> null
                        else -> invalidParameterMessage(query, it, value)
                    }
                }
                )
                .filterNotNull()
        return invalidParams
    }

    fun missingParameterMessage(path: String, it: Parameter<*>) =
            """Required $path parameter `${it.name}` is missing"""

    fun invalidParameterMessage(query: String, it: Parameter<*>, value: String?) =
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

inline operator fun <reified T : Any> Request.get(param: PathParam<T>): T? = (params(param.name)
        .let { if (it != null && it.isEmpty()) null else it }
        .let { if (T::class.java.isInstance(0)) it?.toInt() else it } ?: param.default) as T

inline operator fun <reified T : Any> Request.get(param: QueryParam<T>): T = (queryParams(param.name)
        .let { if (it != null && it.isEmpty()) null else it }
        .let { if (T::class.java.isInstance(0)) it?.toInt() else it } ?: param.default) as T
