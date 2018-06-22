package com.emoticast.sparktswagger

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import com.beerboy.ss.Config
import com.beerboy.ss.SparkSwagger
import org.slf4j.LoggerFactory
import spark.Service

interface Router {
    val http: SparkSwagger

    fun registerRoutes()

    infix fun String.GET(path: String) = Endpoint<Any>(HTTPMethod.GET, this, http, path, emptyList(), emptyList(), emptyList())
    infix fun String.GET(path: ParametrizedPath) = Endpoint<Any>(HTTPMethod.GET, this, http, path.path, path.pathParameters, emptyList(), emptyList())

    infix fun String.POST(path: String) = Endpoint<Any>(HTTPMethod.POST, this, http, path, emptyList(), emptyList(), emptyList())
    infix fun String.POST(path: ParametrizedPath) = Endpoint<Any>(HTTPMethod.POST, this, http, path.path, path.pathParameters, emptyList(), emptyList())

    infix fun String.PUT(path: String) = Endpoint<Any>(HTTPMethod.PUT, this, http, path, emptyList(), emptyList(), emptyList())
    infix fun String.PUT(path: ParametrizedPath) = Endpoint<Any>(HTTPMethod.PUT, this, http, path.path, path.pathParameters, emptyList(), emptyList())

    infix fun String.DELETE(path: String) = Endpoint<Any>(HTTPMethod.DELETE, this, http, path, emptyList(), emptyList(), emptyList())
    infix fun String.DELETE(path: ParametrizedPath) = Endpoint<Any>(HTTPMethod.DELETE, this, http, path.path, path.pathParameters, emptyList(), emptyList())

    operator fun String.div(path: String) = this + "/" + path
    operator fun String.div(path: PathParam<out Any>) = ParametrizedPath(this + "/:" + path.name, listOf(path))
}

class Server(val level: Level) {
    companion object {
        const val port: Int = 3000
    }

    fun start(config: Config, router: (SparkSwagger)-> Router) {
        val logger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger
        logger.level = level

        val http = Service.ignite().port(port)
        val swagger = SparkSwagger.of(http, config)
        router(swagger).registerRoutes()
        swagger.generateDoc()
    }
}
