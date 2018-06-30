package com.emoticast.sparktswagger

import ch.qos.logback.classic.Level
import com.beerboy.ss.Config
import com.beerboy.ss.SparkSwagger
import com.emoticast.sparktswagger.Server.Companion.port
import org.junit.rules.ExternalResource

val config = Config(description = "A test",
        basePath = "",
        title = "Test",
        host = "localhost:$port",
        docPath = "/doc",
        serviceName = "/$root")

open class SparkTestRule(val router: (SparkSwagger) -> Router = { ServerRouter(it) }) : ExternalResource() {
    val server = Server(Level.INFO)
    override fun before() {

        server.start(config, router)

        while (!isConnected()) {
            Thread.sleep(100)
        }
    }

    override fun after() {
        server.stop()
        super.after()
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
