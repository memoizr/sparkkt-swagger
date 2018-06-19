package com.emoticast.sparktswagger

import ch.qos.logback.classic.Level
import com.beerboy.ss.Config
import com.emoticast.sparktswagger.Server.Companion.port
import org.junit.rules.ExternalResource

val config = Config(description = "A test",
                basePath = "/$root",
                title = "Test",
                host = "localhost:$port",
                docPath = "/doc")

open class SparkTestRule : ExternalResource() {
    val server = Server(Level.INFO)
    override fun before() {

        server.start(config) { ServerRouter(it) }

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
