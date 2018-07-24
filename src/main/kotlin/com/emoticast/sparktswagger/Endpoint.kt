package com.emoticast.sparktswagger

import spark.Request


data class Endpoint<B : Any>(
        val httpMethod: HTTPMethod,
        val description: String,
        val url: String,
        val pathParams: Set<PathParam<out Any>>,
        val queryParams: Set<QueryParameter<*>>,
        val headerParams: Set<HeaderParameter<*>>,
        val body: Body<B>) {

    infix fun with(queryParameter: QueryParameter<*>) = copy(queryParams = queryParams + queryParameter)
    infix fun with(params: HeaderParameter<*>) = copy(headerParams = headerParams + params)
    infix fun <C : Any> with(body: Body<C>) = Endpoint(
            httpMethod,
            description,
            url,
            pathParams,
            queryParams,
            headerParams,
            body
    )

    private fun Request.getPathParam(param: PathParam<*>) = params(param.name)
            .let { if (it != null && param.emptyAsMissing && it.isEmpty()) null else it }
    private fun Request.getQueryParam(param: QueryParameter<*>) = queryParams(param.name).filterValid(param)
    private fun Request.getHeaderParam(param: HeaderParameter<*>) = headers(param.name).filterValid(param)

    infix fun with(queryParameter: List<Parameter<*>>) = let {
        queryParameter.foldRight(this) { param, endpoint ->
            when (param) {
                is HeaderParameter -> endpoint with param
                is QueryParameter -> endpoint with param
                else -> throw IllegalArgumentException(param.toString())
            }
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