package com.emoticast.sparktswagger

import com.emoticast.extensions.json
import org.junit.Rule
import org.junit.Test

class FooTest {

    @Rule
    @JvmField val rule = SparkTestRule()

    @Test
    fun yyoo() {
        whenPerform GET "/" expectCode 200
        whenPerform GET "/home/v1/clips/3456" expectCode 200
        whenPerform GET "/home/v1/clips/hey" expectCode 400 expectBody ClientError(400, listOf("""path parameter "clips" is invalid""")).json
        whenPerform GET "/v1/clips/134?offset=-34" expectCode 400 expectBody ClientError(400, listOf("""query parameter `offset` is invalid""")).json
    }
}