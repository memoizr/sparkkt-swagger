package com.emoticast.sparktswagger

import com.emoticast.sparktswagger.extensions.parseJson
import com.memoizr.assertk.expect
import org.junit.Test

class ExtensionsTest {
    @Test
    fun `parses sealed classes`() {
        expect that """{"type":"ONE","one":"hello"}""".parseJson<TheSeal>() isEqualTo TheSeal.ONE("hello")
        expect that """{"type":"TWO","two":"hello"}""".parseJson<TheSeal>() isEqualTo TheSeal.TWO("hello")

        expect that """{"type":"ONE","one":"hello"}""".parseJson<TheSeal.ONE>() isEqualTo TheSeal.ONE("hello")
        expect that """{"type":"TWO","two":"hello"}""".parseJson<TheSeal.TWO>() isEqualTo TheSeal.TWO("hello")
    }
}

sealed class TheSeal : Sealed() {
    data class ONE(val one: String) : TheSeal()
    data class TWO(val two: String) : TheSeal()
}