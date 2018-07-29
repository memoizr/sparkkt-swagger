package com.emoticast.sparktswagger

import com.emoticast.sparktswagger.extensions.json
import com.emoticast.sparktswagger.extensions.print
import org.junit.Rule
import org.junit.Test

class ValidationsTest: SparkTest() {

    val id = path("id", "the id", NonNegativeInt)
    val offset = optionalQuery("offset", "offset", condition = NonNegativeInt, default = 20, emptyAsMissing = true)
    val allowInvalidQuery = optionalQuery("allowInvalidQuery", "allowInvalid", condition = NonNegativeInt, default = 20, emptyAsMissing = true, invalidAsMissing = true)
    val allowInvalidHeader = optionalHeader("allowInvalidHeader", "allowInvalid", condition = NonNegativeInt, default = 20, emptyAsMissing = true, invalidAsMissing = true)
    val stringSet = optionalQuery("stringset", "stringset", condition = StringSet)

    @Rule
    @JvmField val rule = SparkTestRule(port) {
        "" GET "foo" / id with queries(offset, allowInvalidQuery, stringSet) with headers(allowInvalidHeader) isHandledBy {
            request[offset]
            request[allowInvalidHeader]
            request[allowInvalidQuery]
            request[stringSet].print()
            "ok".ok }
    }

    @Test
    fun `validates routes`() {
        whenPerform GET "/$root/foo/3456" expectCode 200
        whenPerform GET "/$root/foo/hey" expectBody HttpResponse.ErrorHttpResponse<Any>(400, listOf("""Path parameter `id` is invalid, expecting non negative integer, got `hey`""")).json expectCode 400
        whenPerform GET "/$root/foo/134?offset=-34" expectBody HttpResponse.ErrorHttpResponse<Any>(400, listOf("""Query parameter `offset` is invalid, expecting non negative integer, got `-34`""")).json expectCode 400
        whenPerform GET "/$root/foo/134?offset=a" expectBody HttpResponse.ErrorHttpResponse<Any>(400, listOf("""Query parameter `offset` is invalid, expecting non negative integer, got `a`""")).json expectCode 400
        whenPerform GET "/$root/foo/134?allowInvalidQuery=a" expectCode 200
        whenPerform GET "/$root/foo/134" withHeaders mapOf(allowInvalidHeader.name to "boo") expectCode 200
        whenPerform GET "/$root/foo/134" withHeaders mapOf(allowInvalidHeader.name to "boo") expectCode 200

        whenPerform GET "/$root/foo/11" withHeaders mapOf(stringSet.name to "foo") expectBody """"ok""""
        whenPerform GET "/$root/foo/11" withHeaders mapOf(stringSet.name to "foo,bar") expectBody """"ok""""
        whenPerform GET "/$root/foo/11?stringset=foo"  expectBody """"ok""""
        whenPerform GET "/$root/foo/11?stringset=foo%20bar"  expectBody """"ok""""
        whenPerform GET "/$root/foo/11?stringset=foo,bar"  expectBody """"ok""""
    }
}

