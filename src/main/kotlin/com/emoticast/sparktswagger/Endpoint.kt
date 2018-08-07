package com.emoticast.sparktswagger

import com.emoticast.sparktswagger.documentation.Visibility
import spark.Request

data class OpDescription(val description: String)

data class Endpoint<B : Any>(
        val httpMethod: HTTPMethod,
        val summary: String?,
        val description: String?,
        val url: String,
        val pathParams: Set<PathParam<out Any>>,
        val queryParams: Set<QueryParameter<*>>,
        val headerParams: Set<HeaderParameter<*>>,
        val body: Body<B>,
        val visibility: Visibility = Visibility.PUBLIC) {

    infix fun withQuery(queryParameter: QueryParameter<*>) = copy(queryParams = queryParams + queryParameter)
    infix fun withHeader(params: HeaderParameter<*>) = copy(headerParams = headerParams + params)
    infix fun <C : Any> with(body: Body<C>) = Endpoint(
            httpMethod,
            summary,
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

    infix fun inSummary(summary: String) = copy(summary = summary)
    infix fun isDescribedAs(description: String) = copy(description = description)

    infix fun with(visibility: Visibility) = copy(visibility = visibility)

    infix fun with(queryParameter: List<Parameter<*>>) = let {
        queryParameter.foldRight(this) { param, endpoint ->
            when (param) {
                is HeaderParameter -> endpoint withHeader param
                is QueryParameter -> endpoint withQuery param
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
