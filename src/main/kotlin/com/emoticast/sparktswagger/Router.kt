package com.emoticast.sparktswagger

import ch.qos.logback.classic.Logger
import com.beerboy.ss.Config
import com.beerboy.ss.SparkSwagger
import org.slf4j.LoggerFactory
import spark.Service

class Router(val http: SparkSwagger) {

    infix fun String.GET(path: String) = Endpoint<Any>(HTTPMethod.GET, this, http, path.leadingSlash, emptySet(), emptySet(), emptySet())
    infix fun String.GET(path: ParametrizedPath) = Endpoint<Any>(HTTPMethod.GET, this, http, path.path.leadingSlash, path.pathParameters, emptySet(), emptySet())

    infix fun String.POST(path: String) = Endpoint<Any>(HTTPMethod.POST, this, http, path.leadingSlash, emptySet(), emptySet(), emptySet())
    infix fun String.POST(path: ParametrizedPath) = Endpoint<Any>(HTTPMethod.POST, this, http, path.path.leadingSlash, path.pathParameters, emptySet(), emptySet())

    infix fun String.PUT(path: String) = Endpoint<Any>(HTTPMethod.PUT, this, http, path.leadingSlash, emptySet(), emptySet(), emptySet())
    infix fun String.PUT(path: ParametrizedPath) = Endpoint<Any>(HTTPMethod.PUT, this, http, path.path.leadingSlash, path.pathParameters, emptySet(), emptySet())

    infix fun String.DELETE(path: String) = Endpoint<Any>(HTTPMethod.DELETE, this, http, path.leadingSlash, emptySet(), emptySet(), emptySet())
    infix fun String.DELETE(path: ParametrizedPath) = Endpoint<Any>(HTTPMethod.DELETE, this, http, path.path.leadingSlash, path.pathParameters, emptySet(), emptySet())

    operator fun String.div(path: String) = this.leadingSlash + "/" + path
    operator fun String.div(path: PathParam<out Any>) = ParametrizedPath(this + "/:" + path.name, setOf(path))
}

val String.leadingSlash get() = if (!startsWith("/")) "/" + this else this

class Server(val config: Config) {
    val http by lazy {   Service.ignite().port(config.port) }


    fun startWithRoutes(router: Router.()-> Unit): SparkSwagger {
        val logger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger
        logger.level = config.logLevel

        val swagger = SparkSwagger.of(http, config)
        router(Router(swagger))
        return swagger
    }

    fun stop() {
        http.stop()
    }
}
