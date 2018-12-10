package com.emoticast.sparktswagger

import com.memoizr.assertk.expect
import org.junit.Rule
import org.junit.Test

class FormatTest : SparkTest() {

    @Rule
    @JvmField val rule = SparkTestRule(port) {
        "" GET "json" isHandledBy { "ok".ok }
        "" GET "bytearray" isHandledBy { "ok".ok.format(Format.VideoMP4) }
    }

    @Test
    fun `returns correct format`() {
        whenPerform GET "/$root/json" expect {
            expect that it.headers["Content-Type"] isEqualTo "application/json"
        }

        whenPerform GET "/$root/bytearray" expect {
            expect that it.headers["Content-Type"] isEqualTo "video/mp4"
        }
    }
}
