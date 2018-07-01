package com.emoticast.sparktswagger

import ch.qos.logback.classic.Level
import com.beerboy.ss.Config
import org.junit.rules.ExternalResource
import org.junit.runner.Description
import org.junit.runners.model.Statement
import java.net.BindException
import java.net.ConnectException
import java.util.*

val config = Config(description = "A test",
        basePath = "",
        title = "Test",
        port = 3000,
        logLevel = Level.INFO,
        host = "localhost:3000",
        docPath = "/doc",
        serviceName = "/$root")

open class SparkTestRule(val router: Router.() -> Unit = ServerRouter) : ExternalResource() {
    val server = Server(config.copy(port = Random().nextInt(5000) + 2000))

    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                before()
                try {
                    base.evaluate()
                } catch (b: BindException) {
                    apply(base, description)
                } catch (e: ConnectException) {
                    apply(base, description)
                } finally {
                    after()
                }

            }
        }
    }

    override fun before() {
        server.startWithRoutes(router)
    }
}
