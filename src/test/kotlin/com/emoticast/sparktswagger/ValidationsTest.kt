package com.emoticast.sparktswagger

import com.emoticast.sparktswagger.HttpResponse.ErrorHttpResponse
import com.emoticast.sparktswagger.extensions.json
import org.junit.Rule
import org.junit.Test

class ValidationsTest: SparkTest() {

    val id = pathParam("id", "the id", NonNegativeInt)
    val offset = optionalQueryParam("offset", "offset", condition = NonNegativeInt, default = 20, emptyAsMissing = true)
    val allowInvalidQuery = optionalQueryParam("allowInvalidQuery", "allowInvalid", condition = NonNegativeInt, default = 20, emptyAsMissing = true, invalidAsMissing = true)
    val allowInvalidHeader = optionalHeaderParam("allowInvalidHeader", "allowInvalid", condition = NonNegativeInt, default = 20, emptyAsMissing = true, invalidAsMissing = true)

    @Rule
    @JvmField val rule = SparkTestRule(port) {
        "" GET "foo" / id with queries(offset, allowInvalidQuery) with headers(allowInvalidHeader) isHandledBy {
            request[offset]
            request[allowInvalidHeader]
            request[allowInvalidQuery]
            "ok".ok }
    }

    @Test
    fun `validates routes`() {
        whenPerform GET "/$root/foo/3456" expectCode 200
        whenPerform GET "/$root/foo/hey" expectBody ErrorHttpResponse<Any>(400, listOf("""Path parameter `id` is invalid, expecting non negative integer, got `hey`""")).json expectCode 400
        whenPerform GET "/$root/foo/134?offset=-34" expectBody ErrorHttpResponse<Any>(400, listOf("""Query parameter `offset` is invalid, expecting non negative integer, got `-34`""")).json expectCode 400
        whenPerform GET "/$root/foo/134?offset=a" expectBody ErrorHttpResponse<Any>(400, listOf("""Query parameter `offset` is invalid, expecting non negative integer, got `a`""")).json expectCode 400
        whenPerform GET "/$root/foo/134?allowInvalidQuery=a" expectCode 200
        whenPerform GET "/$root/foo/134" withHeaders mapOf(allowInvalidHeader.name to "boo") expectCode 200
    }
}

