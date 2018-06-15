package com.emoticast.sparktswagger

import com.beerboy.ss.SparkSwagger
import spark.Request
import spark.Response

val root = "home"
val v1 = "v1"
val clips = "clips"

val clip = pathParam(name = "clips",
        condition = nonNegativeInt,
        description = "The clip id")

val name = pathParam(name = "clips",
        condition = nonEmptyString,
        description = "The clip id")

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
                root / v1 / clips / clip with queries(length, offset) isHandledBy ::getClips

        "Run a cute test" GET
                v1 / clips / clip with queries(length, offset) isHandledBy ::getClips

    }
}

fun getClips(request: Request, response: Response): AResponse {
    return AResponse("x", request[offset])
}

data class AResponse(val x: String, val y: Int?)
