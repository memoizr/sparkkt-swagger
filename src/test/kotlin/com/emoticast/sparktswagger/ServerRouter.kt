package com.emoticast.sparktswagger

import com.beerboy.ss.SparkSwagger
import spark.Request
import spark.Response

val root = "home"
val v1 = "v1"
val clips = "clips"

val clipId = pathParam(
        name = "clipId",
        condition = nonNegativeInt,
        description = "The clip id")

val name = pathParam(name = "clips",
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

        "List all clips" GET
                root / v1 / clips / clipId with queries(length, offset) isHandledBy ::getClips

        "Run a cute test" GET
                v1 / clips / clipId with queries(length, offset) isHandledBy ::getClips

    }
}

fun getClips(request: Request, response: Response): AResponse {
    return AResponse(request[clipId]!!, request[length], request[offset], request[query])
}

data class AResponse(val clipId: Int, val length: Int, val offset: Int, val query: String)
