package com.emoticast.sparktswagger

sealed class HttpResponse<T> {
    abstract val code: Int

    data class SuccessfulHttpResponse<T>(override val code: Int,
                                         val body: T) : HttpResponse<T>()

    data class ErrorHttpResponse<T>(override val code: Int,
                                    val message: List<String>) : HttpResponse<T>()
}

fun <T> T.success(code: Int = 200): HttpResponse<T> = HttpResponse.SuccessfulHttpResponse(code, this)
val <T> T.ok: HttpResponse<T> get() = HttpResponse.SuccessfulHttpResponse(200, this)
val <T> T.created: HttpResponse<T> get() = HttpResponse.SuccessfulHttpResponse(201, this)
fun <T> badRequest(message: String, code: Int = 400) = HttpResponse.ErrorHttpResponse<T>(code, listOf(message))
fun <T> forbidden(message: String) = HttpResponse.ErrorHttpResponse<T>(403, listOf(message))
fun <T> notFound() = HttpResponse.ErrorHttpResponse<T>(404, listOf("not found"))
