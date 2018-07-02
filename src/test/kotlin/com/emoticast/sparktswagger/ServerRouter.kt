package com.emoticast.sparktswagger

val root = "home"
val v1 = "/v1"
val clips = "clips"

val clipId = pathParam(
        name = "clipId",
        condition = NonNegativeInt,
        description = "The clip id")

val name = headerParam(name = "clips",
        condition = NonEmptyString,
        description = "The clip id")

private val query = optionalQueryParam(
        name = "query",
        default = "978",
        description = "The query",
        condition = NonEmptyString)

private val length = optionalQueryParam(
        name = "length",
        description = "The number of items returned in the page",
        default = 20,
        condition = NonNegativeInt)

private val offset = optionalQueryParam(
        name = "offset",
        description = "The offset from the first item",
        default = 0,
        condition = NonNegativeInt)

val ServerRouter: Router.() -> Unit = {
    val getParametrizedGreeting: SomeBodyBundle<RequestBody, String> = { body.hello.plus("").ok }

    val getGreeting: NoBodyBundle<String> = {
        request[query]
        "hello".ok
    }

    "hello" GET "hey" isHandledBy getGreeting

    "List all clips" GET
            v1 / clips / clipId with queries(length, offset, query) isHandledBy getGreeting

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

}

//fun getClips(request: Request, response: Response): AResponse {
//    return AResponse(request[clipId], request[length], request[offset], listOf(Query(request[query])), FooEnum.A)
//}

enum class FooEnum {
    A, B, C, D
}

data class Query(val value: String)
data class AResponse(val clipId: Int, val length: Int, val offset: Int, val queries: List<Query>?, val enum: FooEnum)

data class RequestBody(val hello: String)
