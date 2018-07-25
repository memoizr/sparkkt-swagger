package com.emoticast.sparktswagger

import com.emoticast.sparktswagger.extensions.gson
import com.google.gson.Gson
import spark.Request
import spark.Response
import kotlin.reflect.KClass

data class RequestHandler<T : Any>(
        private val _body: Body<T>?,
        val params: Set<Parameter<*>>,
        val request: Request,
        val response: Response) {
    val body: T by lazy { _body?.customGson?.fromJson(request.body()!!, _body.klass.java)!! }


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

fun queries(vararg queryParameter: QueryParameter<*>) = queryParameter.asList()
fun headers(vararg headerParameter: HeaderParameter<*>) = headerParameter.asList()
inline fun <reified T : Any> body(customGson: Gson = gson) = Body(T::class, gson)

fun String?.filterValid(param: Parameter<*>) = when {
    this == null -> null
    param.emptyAsMissing && this.isEmpty() -> null
    param.invalidAsMissing && !param.pattern.regex.matches(this) -> null
    else -> this
}

data class Body<T : Any>(val klass: KClass<T>, val customGson: Gson = gson)

data class UnregisteredParamException(val param: Parameter<*>) : Throwable()

typealias Handler<BODY_TYPE, RESPONSE_TYPE> = RequestHandler<BODY_TYPE>.() -> HttpResponse<RESPONSE_TYPE>
