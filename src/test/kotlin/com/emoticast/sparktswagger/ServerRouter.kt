package com.emoticast.sparktswagger

import com.beerboy.ss.SparkSwagger
import com.emoticast.extensions.print
import spark.Request
import spark.Response

val root = "home"
val v1 = "v1"
val clips = "clips"

val clipId = pathParam(
        name = "clipId",
        condition = nonNegativeInt,
        description = "The clip id")

val name = headerParam(name = "clips",
        condition = nonEmptyString,
        description = "The clip id")

val query = queryParam(
        name = "query",
        description = "The query",
        default = "",
        condition = nonEmptyString)

val length = queryParam(
        name = "length",
        description = "The number of items returned in the page",
        default = 20,
        condition = nonNegativeInt)

val offset = queryParam(
        name = "offset",
        description = "The offset from the first item",
        default = 0,
        condition = nonNegativeInt)

class ServerRouter(override val http: SparkSwagger) : Router {

    override fun registerRoutes() {


        val getParametrizedGreeting: SomeBodyBundle<RequestBody, String> = {  body.hello.plus("") }

        val getGreeting: NoBodyBundle<String> = {
            request[offset]
            "hello"
        }

        "List all clips" GET
                root / v1 / clips / clipId with body<RequestBody>() with queries(length, offset) isHandledBy getParametrizedGreeting

//        "Run a cute test" GET
//                v1 / clips / clipId with queries(length, offset) with headers(name) isHandledBy ::getClips
//
//        "Run a cute test 2" POST
//                v1 / clips / clipId with queries(length, offset) with headers(name) with body<RequestBody>() isHandledBy ::getClips
//
//        "Run a cute test 2" PUT
//                v1 / clips / clipId with queries(length, offset) with headers(name) isHandledBy ::getClips
//
//        "Run a cute test 2" DELETE
//                v1 / clips / clipId with queries(length, offset) with headers(name) isHandledBy ::getClips

        http.spark.get("/$root/docs") { request, response ->
            khttp.get("http://localhost:3000").text.print()}

        http.spark.get("/$root/:star") { request, response ->
            val p = request.params(":star") ?: ""
            khttp.get("http://localhost:3000/$p").text.print()}

    }
}

fun getClips(request: Request, response: Response): AResponse {
    return AResponse(request[clipId]!!, request[length], request[offset], listOf(Query(request[query])), FooEnum.A)
}

enum class FooEnum{
    A, B, C, D
}

data class Query(val value: String)
data class AResponse(val clipId: Int, val length: Int, val offset: Int, val queries: List<Query>?, val enum: FooEnum)

data class RequestBody(val hello: String)
