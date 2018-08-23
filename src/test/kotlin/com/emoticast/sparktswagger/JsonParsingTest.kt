package com.emoticast.sparktswagger

import com.emoticast.sparktswagger.extensions.parseJson
import com.emoticast.sparktswagger.extensions.print
import com.memoizr.assertk.expect
import org.junit.Test


class JsonParsingTest {

    @Test
    fun `it fails with informative error when json is invalid`() {
        expect thatThrownBy { """{"test": null}""".parseJson<Foo>().print() } hasMessage """Error parsing {"test": null} to Foo(test: kotlin.String?)"""
    }
}
data class Foo(val test: String?)

