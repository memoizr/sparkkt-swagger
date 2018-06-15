package com.emoticast.sparktswagger

import com.emoticast.extensions.json
import com.memoizr.assertk.expect
import org.json.JSONObject

object whenPerform {
    infix fun GET(endpoint: String): Expectation {
        return Expectation(endpoint)
    }

    class Expectation(
            private val endpoint: String,
            headers: Map<String, String> = emptyMap()) {

        private val response = khttp.get("http://localhost:${Server.port}$endpoint", headers = headers)

        infix fun withHeaders(headers: Map<String, String>) = Expectation(endpoint, headers)

        infix fun expectBody(body: String) = apply {
            expect that response.text isEqualTo body
        }

        infix fun expectCode(code: Int) = apply {
            expect that response.statusCode isEqualTo code
        }

        infix fun expectBodyJson(body: Any) = apply {
            expect that response.jsonObject.toString() isEqualTo JSONObject(body.json).toString()
        }
    }
}