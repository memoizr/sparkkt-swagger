package com.emoticast.sparktswagger

import com.emoticast.extensions.json
import com.emoticast.extensions.toHashMap
import com.memoizr.assertk.expect
import org.json.JSONObject

object whenPerform {
    infix fun GET(endpoint: String): Expectation {
        return Expectation(HttpMethod.GET, endpoint)
    }

    infix fun POST(endpoint: String): Expectation {
        return Expectation(HttpMethod.POST, endpoint)
    }

    infix fun DELETE(endpoint: String): Expectation {
        return Expectation(HttpMethod.DELETE, endpoint)
    }

    infix fun PUT(endpoint: String): Expectation {
        return Expectation(HttpMethod.PUT, endpoint)
    }

    enum class HttpMethod {
        POST, GET, PUT, DELETE;
    }

    data class Expectation(
            private val method: HttpMethod,
            private val endpoint: String,
            private val headers: Map<String, String> = emptyMap(),
            private val body: Any? = null
    ) {

        private val response by lazy {
            when (method) {
                HttpMethod.GET -> khttp.get("http://localhost:${config.port}$endpoint", headers = headers, json = body?.toHashMap())
                whenPerform.HttpMethod.POST -> khttp.post("http://localhost:${config.port}$endpoint", headers = headers, json = body?.toHashMap())
                whenPerform.HttpMethod.PUT -> khttp.put("http://localhost:${config.port}$endpoint", headers = headers, json = body?.toHashMap())
                whenPerform.HttpMethod.DELETE -> khttp.delete("http://localhost:${config.port}$endpoint", headers = headers, json = body?.toHashMap())
            }
        }

        infix fun withBody(body: Any) = copy(body = body)

        infix fun withHeaders(headers: Map<String, Any?>) = copy(headers = headers.map { it.key to it.value.toString() }.toMap())

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

