package com.emoticast.sparktswagger

import ch.qos.logback.classic.Level
import org.junit.rules.ExternalResource


open class SparkTestRule : ExternalResource() {
    val server = Server(Level.INFO)
    override fun before() {

        server.start { ServerRouter(it) }

        while (!isConnected()) {
            Thread.sleep(100)
        }
    }

    private fun isConnected(): Boolean {
        return try {
            khttp.get("http://localhost:${Server.port}/")
            true
        } catch (e: Exception) {
            false
        }
    }
}
