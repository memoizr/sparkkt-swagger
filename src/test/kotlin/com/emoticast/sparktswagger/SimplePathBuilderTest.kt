package com.emoticast.sparktswagger

import org.junit.Rule
import org.junit.Test

class SimplePathBuilderTest {

    @Rule
    @JvmField
    val rule = SparkTestRule {
        "foo" GET "/foo" isHandledBy { TestResult("get value").ok }
        "foo" PUT "/foo" isHandledBy { TestResult("put value").created }
        "foo" POST "/foo" isHandledBy { TestResult("post value").created }
        "foo" DELETE "/foo" isHandledBy { TestResult("delete value").ok }

        "foo" GET "/error" isHandledBy { if (false) TestResult("never happens").ok else badRequest("Something went wrong") }
        "foo" GET "/forbidden" isHandledBy { if (false) TestResult("never happens").ok else forbidden("Forbidden") }

        "foo" GET "noslash/bar" isHandledBy { TestResult("success").ok }
        "foo" PUT "noslash/bar" isHandledBy { TestResult("success").ok }
        "foo" POST "noslash/bar" isHandledBy { TestResult("success").ok }
        "foo" DELETE "noslash/bar" isHandledBy { TestResult("success").ok }

        "foo" GET "infixslash" / "bar" isHandledBy { TestResult("success").ok }
        "foo" PUT "infixslash" / "bar" isHandledBy { TestResult("success").ok }
        "foo" POST "infixslash" / "bar" isHandledBy { TestResult("success").ok }
        "foo" DELETE "infixslash" / "bar" isHandledBy { TestResult("success").ok }
    }


    @Test
    fun `returns successful status codes`() {
        whenPerform GET "/$root/foo" expectBodyJson TestResult("get value") expectCode 200
        whenPerform PUT "/$root/foo" expectBodyJson TestResult("put value") expectCode 201
        whenPerform POST "/$root/foo" expectBodyJson TestResult("post value") expectCode 201
        whenPerform DELETE "/$root/foo" expectBodyJson TestResult("delete value") expectCode 200
    }

    @Test
    fun `returns error responses`() {
        whenPerform GET "/$root/error" expectBodyJson badRequest<TestResult>("Something went wrong") expectCode 400
        whenPerform GET "/$root/forbidden" expectBodyJson badRequest<TestResult>("Forbidden", 403) expectCode 403
    }

    @Test
    fun `when there's no leading slash, it adds it`() {
        whenPerform GET "/$root/noslash/bar" expectBodyJson TestResult("success") expectCode 200
        whenPerform PUT "/$root/noslash/bar" expectBodyJson TestResult("success") expectCode 200
        whenPerform POST "/$root/noslash/bar" expectBodyJson TestResult("success") expectCode 200
        whenPerform DELETE "/$root/noslash/bar" expectBodyJson TestResult("success") expectCode 200
    }

    @Test
    fun `supports infix slash`() {
        whenPerform GET "/$root/infixslash/bar" expectBodyJson TestResult("success") expectCode 200
        whenPerform PUT "/$root/infixslash/bar" expectBodyJson TestResult("success") expectCode 200
        whenPerform POST "/$root/infixslash/bar" expectBodyJson TestResult("success") expectCode 200
        whenPerform DELETE "/$root/infixslash/bar" expectBodyJson TestResult("success") expectCode 201
    }
}

data class TestResult(val value: String)