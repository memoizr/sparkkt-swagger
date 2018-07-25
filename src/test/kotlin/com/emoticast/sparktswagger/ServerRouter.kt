package com.emoticast.sparktswagger

import com.emoticast.sparktswagger.documentation.Description
import java.util.*

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

private val random = optionalQueryParam(
        name = "random",
        invalidAsMissing = true,
        emptyAsMissing = true,
        description = "randomfoo",
        condition = aFoo)


object aFoo : Validator<Random> {
    override val regex: Regex = "/^hello".toRegex()
    override val description: String  = "a ranom parameter"
    override val parse: (String) -> Random = { Random()}
}

val ServerRouter: Router.() -> Unit = {
//    val getGreeting: Handler<Nothing, AResponse> = {
//        request[query]
//        AResponse(0, 0, 0, listOf(Query("hey")), FooEnum.A).ok
//    }

    val getPathGreeting: Handler<Nothing, AResponse> = {
        request[query]
        AResponse(0, 0, 0, listOf(Query("hey")), FooEnum.A).ok
    }

    val getGreetingBody: Handler<TestClass, AResponse> = {
        request[query]
        AResponse(0, 0, 0, listOf(Query("hey")), FooEnum.A).ok
    }

//    "hello" GET "hey" with queries(query) isHandledBy getGreeting

    "List all clips" GET
            v1 / clips / clipId with queries(random, query, length, offset, query) isHandledBy getPathGreeting

        "Run a cute test" POST
                v1 / clips / clipId with queries(random,query, length, offset) with body<TestClass>() with headers(name) isHandledBy getGreetingBody

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

data class Query(val value: String)
data class AResponse(
        val clipId: Int, val length: Int,
        val offset: Int, val queries: List<Query>?, val enum: FooEnum)

data class RequestBody(val hello: String)

data class TestClass(
        @Description("no way punk", exString = "https://google.com")
        val aString: String,
        @Description("The best int", exInt = 33)
        val aInt: Int,
        val aLong: Long,
        val aFloat: Float,
        val aDouble: Double,
        val aBoolean: Boolean,
        val aBinary: ByteArray,
        val aDate: Date,
        val aListString: List<String>,
        @Description("A great list", exEmptyList = true)
        val aListBoolean: List<Boolean>,
        val anObjectList: List<SimpleObject>,
        val theEnum: ClassicEnum,
        val aSeal: MySeal,
        val moreSeals: List<MySeal>
)

data class SimpleObject(
        val a: String,
        val b: Int,
        val c: Long
)

enum class ClassicEnum {
    foo, bar, baz
}

sealed class MySeal : Sealed() {
    data class Bar(val x: String) : MySeal()
    data class Yo(val y: Int) : MySeal()
    object nope: MySeal()
}
