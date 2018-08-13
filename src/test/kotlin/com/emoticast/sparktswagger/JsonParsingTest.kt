package com.emoticast.sparktswagger

import com.emoticast.sparktswagger.extensions.parseJson
import com.emoticast.sparktswagger.extensions.print
import com.memoizr.assertk.expect
import org.junit.Test


class JsonParsingTest {
    @Test
    fun `it parses valid json`() {
//        expect that """{"test": "value"}""".parseJson<ParsingTest>() isEqualTo ParsingTest("value")
    }

//    val gson = GsonBuilder()
////            .registerSealedClassAdapter()
////            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZZZZZ")
//            .create()
//    val Any.json: String get() = gson.toJson(this)
//    fun Any.toHashMap() = gson.fromJson(json, Map::class.java)
//    inline fun <reified T> String.parseJsons() :T?  = Klaxon().parse<T>(this)

    @Test
    fun `it fails with informative error when json is invalid`() {
        expect thatThrownBy { """{"test": null}""".parseJson<Foo>().print() } hasMessage """Error parsing {"test": null} to Foo(test: kotlin.String?)"""
    }
}
data class Foo(val test: String?)

