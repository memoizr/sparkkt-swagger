package com.emoticast.sparktswagger

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import com.beerboy.ss.SparkSwagger
import org.slf4j.LoggerFactory
import spark.Service

interface Router {
    val http: SparkSwagger

    fun registerRoutes()

    infix fun String.GET(path: String) = Endpoint(HTTPMethod.GET, this, http, path, emptyList(), emptyList(), emptyList())
    infix fun String.GET(path: ParametrizedPath) = Endpoint(HTTPMethod.GET, this, http, path.path, path.pathParameters, emptyList(), emptyList())

    operator fun String.div(path: String) = this + "/" + path
    operator fun String.div(path: PathParam<out Any>) = ParametrizedPath(this + "/:" + path.name, listOf(path))
}

class Server(val level: Level) {
    companion object {
        val port: Int = 3000
    }

    fun start(router: (SparkSwagger)-> Router) {
        val root = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger
        root.level = level

        val http = Service.ignite().port(port)
        val swagger = SparkSwagger.of(http)
        router(swagger).registerRoutes()
        swagger.generateDoc()
    }
}
