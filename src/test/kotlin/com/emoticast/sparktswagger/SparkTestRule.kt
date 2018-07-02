package com.emoticast.sparktswagger

import ch.qos.logback.classic.Level
import com.beerboy.ss.Config
import com.beerboy.ss.DocExpansion
import org.junit.rules.ExternalResource
import org.junit.runner.Description
import org.junit.runners.model.Statement
import java.net.BindException
import java.net.ConnectException

val config = Config(description = "A test",
        basePath = "",
        title = "Test",
        port = 3000,
        logLevel = Level.INFO,
        host = "localhost:3000",
        docPath = "/doc",
        serviceName = "/$root",
        docExpansion = DocExpansion.LIST

)

open class SparkTestRule(val port: Int, val router: Router.() -> Unit = ServerRouter) : ExternalResource() {
    val server = Server(config.copy(port = port))

    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                before()
                fun go() {
                    try {
                        base.evaluate()
                    } catch (b: BindException) {
                        go()
                    } catch (e: ConnectException) {
                        go()
                    } finally {
                        after()
                    }
                }
                go()

            }
        }
    }

    override fun before() {
        server.startWithRoutes(router)
    }
}
