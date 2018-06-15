package com.emoticast.sparktswagger

import com.beerboy.ss.SparkSwagger
import com.beerboy.ss.descriptor.EndpointDescriptor.endpointPath
import com.beerboy.ss.descriptor.MethodDescriptor
import com.beerboy.ss.descriptor.ParameterDescriptor
import com.emoticast.extensions.json
import spark.Request
import spark.Response

fun queries(vararg queryParameter: QueryParam<*>) = queryParameter.asList()
fun headers(vararg headerParameter: Parameter<*>) = headerParameter.asList()


data class Endpoint(
        val httpMethod: HTTPMethod,
        val description: String,
        val swagger: SparkSwagger,
        private val paths: String,
        val pathParams: List<PathParam<out Any>>,
        val queryQueryParameters: List<QueryParam<*>>,
        val headerParams: List<Parameter<*>>) {

    val path by lazy { paths.split("/").dropLast(1).joinToString("/") }
    val resourceName by lazy { "/" + paths.split("/").last() }

    infix fun with(queryParameter: QueryParam<*>) = copy(queryQueryParameters = queryQueryParameters + queryParameter)
    infix fun with(queryParameter: List<QueryParam<*>>) = let {
        queryParameter.foldRight(this) { param, endpoint -> endpoint with param }
    }

    inline infix fun <reified T : Any> isHandledBy(noinline block: (Request, Response) -> T) {
        val withResponseType = MethodDescriptor.path(resourceName)
                .withDescription(description)
                .withResponseType(T::class.java)
                .apply {
                    queryQueryParameters.forEach { withQueryParam(it.toParameterDescriptor()) }
                    ParameterDescriptor.ParameterType.QUERY
                    pathParams.forEach { withPathParam(it.toParameterDescriptor()) }
                }

        val endpoint = swagger.endpoint(endpointPath(path), { _, _ -> null })
        when (httpMethod) {
            HTTPMethod.GET -> {
                endpoint.get(withResponseType, { request, response ->
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
                    } + queryQueryParameters.map {
                        val value = request.getQueryParam(it)
                        val query = "Query"
                        when {
                            it.required && value == null -> missingParameterMessage(path, it)
                            !it.required && value == null -> null
                            it.pattern.regex.matches(value!!) -> null
                            else -> invalidParameterMessage(query, it, value)
                        }
                    })
                            .filterNotNull()

                    if (invalidParams.isNotEmpty()) {
                        response.status(400)
                        return@get invalidParams.foldRight(emptyList<String>()) { error, acc -> acc + error }.let {
                            ClientError(400, it).json
                        }
                    }

                    block(request, response).json
                })
            }
            else -> ""
        }
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

inline operator fun <reified T : Any> Request.get(param: PathParam<T>): T? = (params(param.name)
        .let { if (it != null && it.isEmpty()) null else it }
        .let { if (T::class.java.isInstance(0)) it?.toInt() else it } ?: param.default) as T

inline operator fun <reified T : Any> Request.get(param: QueryParam<T>): T = (queryParams(param.name)
        .let { if (it != null && it.isEmpty()) null else it }
        .let { if (T::class.java.isInstance(0)) it?.toInt() else it } ?: param.default) as T
