package com.emoticast.sparktswagger

import com.beerboy.ss.SparkSwagger
import com.beerboy.ss.extensions.json
import com.beerboy.ss.extensions.print
import org.junit.Rule
import org.junit.Test

val stringParam = pathParam(
        name = "stringParam",
        description = "Description",
        condition = nonEmptyString
)

val intparam = pathParam(
        name = "intParam",
        description = "Description",
        condition = nonNegativeInt
)

val q = queryParam(name = "q", description = "description", condition = nonEmptyString)
val int = queryParam(name = "int", description = "description", condition = nonNegativeInt)
private val offset = optionalQueryParam(name = "offset", description = "description", condition = nonNegativeInt, default = 30)
val limit = optionalQueryParam(name = "limit", description = "description", condition = nonNegativeInt)

val qHead = headerParam(name = "q", description = "description", condition = nonEmptyString)
val intHead = headerParam(name = "int", description = "description", condition = nonNegativeInt)
val offsetHead = optionalHeaderParam(name = "offsetHead", description = "description", condition = nonNegativeInt, default = 666)
val limitHead = optionalHeaderParam(name = "limitHead", description = "description", condition = nonNegativeInt)

class ParametersTest {
    @Rule
    @JvmField
    val rule = SparkTestRule { TestRouter(it) }

    class TestRouter(override val http: SparkSwagger) : Router {

        override fun registerRoutes() {
            "" GET "stringpath" / stringParam isHandledBy { TestResult(request[stringParam]).ok }
            "" GET "intpath" / intparam isHandledBy { IntTestResult(request[intparam]).ok }

            "" GET "intpath2" / intparam / "end" isHandledBy {
                request.params(stringParam.name).print()
                IntTestResult(request[intparam]).ok
            }

            "" GET "queriespath" with queries(q) isHandledBy { TestResult(request[q]).ok }
            "" GET "queriespath2" with queries(int) isHandledBy { IntTestResult(request[int]).ok }
            "" GET "queriespath3" with queries(offset) isHandledBy { IntTestResult(request[offset]).ok }
            "" GET "queriespath4" with queries(limit) isHandledBy { NullableIntTestResult(request[limit]).ok }

            "" GET "headerspath" with headers(qHead) isHandledBy { TestResult(request[qHead]).ok }
            "" GET "headerspath2" with headers(intHead) isHandledBy { IntTestResult(request[intHead]).ok }
            "" GET "headerspath3" with headers(offsetHead) isHandledBy { NullableIntTestResult(request[offsetHead]).ok }
            "" GET "headerspath4" with headers(limitHead) isHandledBy { NullableIntTestResult(request[limitHead]).ok }
        }
    }

    @Test
    fun `supports typed path parameters`() {
        whenPerform GET "/$root/stringpath/hellothere" expectBodyJson TestResult("hellothere")
        whenPerform GET "/$root/intpath/300" expectBodyJson IntTestResult(300)
    }

    @Test
    fun `validates path parameters`() {
        whenPerform GET "/$root/intpath2/4545/end" expectBody IntTestResult(4545).json
        whenPerform GET "/$root/intpath2/hello/end" expectBody ErrorHttpResponse<TestResult>(400, listOf("Path parameter `intParam` is invalid, expecting non negative integer, got `hello`")).json
    }

    @Test
    fun `supports query parameters`() {
        whenPerform GET "/$root/queriespath?q=foo" expectBody TestResult("foo").json
        whenPerform GET "/$root/queriespath?q=" expectBody ErrorHttpResponse<TestResult>(400, listOf("Required Query parameter `q` is missing")).json
        whenPerform GET "/$root/queriespath" expectBody ErrorHttpResponse<TestResult>(400, listOf("Required Query parameter `q` is missing")).json

        whenPerform GET "/$root/queriespath2?int=3434" expectBody IntTestResult(3434).json
        whenPerform GET "/$root/queriespath2?int=" expectBody ErrorHttpResponse<TestResult>(400, listOf("Required Query parameter `int` is missing")).json
        whenPerform GET "/$root/queriespath2?int=hello" expectBody ErrorHttpResponse<TestResult>(400, listOf("Query parameter `int` is invalid, expecting non negative integer, got `hello`")).json
        whenPerform GET "/$root/queriespath2?int=-34" expectBody ErrorHttpResponse<TestResult>(400, listOf("Query parameter `int` is invalid, expecting non negative integer, got `-34`")).json
    }

    @Test
    fun `supports default values for query parameters`() {
        whenPerform GET "/$root/queriespath3?offset=42" expectBody IntTestResult(42).json
        whenPerform GET "/$root/queriespath3?offset=" expectBody IntTestResult(30).json
        whenPerform GET "/$root/queriespath3" expectBody IntTestResult(30).json

        whenPerform GET "/$root/queriespath4?limit=42" expectBody NullableIntTestResult(42).json
        whenPerform GET "/$root/queriespath4?limit=" expectBody "{}"
        whenPerform GET "/$root/queriespath4" expectBody "{}"
    }

    @Test
    fun `supports header parameters`() {
        whenPerform GET "/$root/headerspath" withHeaders mapOf(qHead.name to "foo") expectBody TestResult("foo").json
        whenPerform GET "/$root/headerspath" withHeaders mapOf(qHead.name to "")  expectBody ErrorHttpResponse<TestResult>(400, listOf("Required Header parameter `q` is missing")).json
        whenPerform GET "/$root/headerspath" withHeaders mapOf()  expectBody ErrorHttpResponse<TestResult>(400, listOf("Required Header parameter `q` is missing")).json

        whenPerform GET "/$root/headerspath2"  withHeaders mapOf(intHead.name to 3434) expectBody IntTestResult(3434).json
        whenPerform GET "/$root/headerspath2" expectBody ErrorHttpResponse<TestResult>(400, listOf("Required Header parameter `int` is missing")).json
        whenPerform GET "/$root/headerspath2"  withHeaders mapOf(intHead.name to "hello") expectBody ErrorHttpResponse<TestResult>(400, listOf("Header parameter `int` is invalid, expecting non negative integer, got `hello`")).json
        whenPerform GET "/$root/headerspath2" withHeaders mapOf(intHead.name to -34) expectBody ErrorHttpResponse<TestResult>(400, listOf("Header parameter `int` is invalid, expecting non negative integer, got `-34`")).json
    }

    @Test
    fun `supports default values for header parameters`() {
        whenPerform GET "/$root/headerspath3" withHeaders mapOf(offsetHead.name to 42) expectBody IntTestResult(42).json
        whenPerform GET "/$root/headerspath3" expectBody IntTestResult(666).json
        whenPerform GET "/$root/headerspath3" expectBody IntTestResult(666).json

        whenPerform GET "/$root/headerspath4" withHeaders mapOf(limitHead.name to 42) expectBody NullableIntTestResult(42).json
        whenPerform GET "/$root/headerspath4" withHeaders mapOf(limitHead.name to "") expectBody "{}"
        whenPerform GET "/$root/headerspath4" expectBody "{}"
    }

    data class IntTestResult(val result: Int)
    data class NullableIntTestResult(val result: Int?)
}