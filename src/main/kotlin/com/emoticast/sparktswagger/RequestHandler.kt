package com.emoticast.sparktswagger

import com.beust.klaxon.Klaxon
import com.emoticast.sparktswagger.extensions.klaxon
import com.emoticast.sparktswagger.extensions.parse
import spark.Request
import spark.Response
import kotlin.reflect.KClass

data class RequestHandler<T : Any>(
        private val _body: Body<T>?,
        val params: Set<Parameter<*>>,
        val request: Request,
        val response: Response) {
    val body: T by lazy { _body?.customGson?.parse(request.body()!!, _body.klass)!! }


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
fun description(description: String) = OpDescription(description)
inline fun <reified T : Any> body(customGson: Klaxon = klaxon) = Body(T::class, customGson)

fun String?.filterValid(param: Parameter<*>) = when {
    this == null -> null
    param.emptyAsMissing && this.isEmpty() -> null
    param.invalidAsMissing && !param.pattern.regex.matches(this) -> null
    else -> this
}

data class Body<T : Any>(val klass: KClass<T>, val customGson: Klaxon = klaxon)

data class UnregisteredParamException(val param: Parameter<*>) : Throwable()

typealias Handler<BODY_TYPE, RESPONSE_TYPE> = RequestHandler<BODY_TYPE>.() -> HttpResponse<RESPONSE_TYPE>
