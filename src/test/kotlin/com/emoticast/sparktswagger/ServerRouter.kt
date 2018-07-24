package com.emoticast.sparktswagger

import com.emoticast.sparktswagger.documentation.TestClass

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
//    val getGreeting: Handler<Nothing, AResponse> = {
//        request[query]
//        AResponse(0, 0, 0, listOf(Query("hey")), FooEnum.A).ok
//    }

    val getPathGreeting: Handler<Nothing, TestClass> = {
        request[query]
        (null!! as TestClass).ok
    }

//    val getGreetingBody: Handler<RequestBody, AResponse> = {
//        request[query]
//        AResponse(0, 0, 0, listOf(Query("hey")), FooEnum.A).ok
//    }

//    "hello" GET "hey" with queries(query) isHandledBy getGreeting

    "List all clips" GET
            v1 / clips / clipId with queries(query, length, offset, query) isHandledBy getPathGreeting

        "Run a cute test" GET
                v1 / clips / clipId with queries(query, length, offset) with headers(name) isHandledBy getPathGreeting

//        "Run a cute test 2" POST
//                v1 / clips / clipId with queries(query, length, offset) with headers(name) with body<RequestBody>() isHandledBy getGreetingBody

        "Run a cute test 2" PUT
                v1 / clips / clipId with queries(query, length, offset) with headers(name) isHandledBy getPathGreeting

        "Run a cute test 2" DELETE
                v1 / clips / clipId with queries(query, length, offset) with headers(name) isHandledBy getPathGreeting

}

enum class FooEnum {
    A, B, C, D
}

//data class Query(val value: String)
//data class AResponse(
//        @Description("a clip id foo")
//        val clipId: Int, val length: Int,
//        @Description("offset yo")
//        val offset: Int, val queries: List<Query>?, val enum: FooEnum)

//data class RequestBody(val hello: String)
