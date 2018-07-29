package com.emoticast.sparktswagger

import ch.qos.logback.classic.Logger
import org.slf4j.LoggerFactory
import spark.Service

class Snitch(val config: Config = Config()) {
    val http by lazy { Service.ignite().port(config.port) }


    fun setRoutes(routerConfiguration: Router.() -> Unit): Router {
        http.externalStaticFileLocation("/tmp/swagger-ui/docs")
        val logger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger
        logger.level = config.logLevel

        val router = Router(config, http)
        routerConfiguration(router)
        router.startListening()
        return router
    }

    fun Router.startListening() {

        endpoints.forEach {
            val path = config.basePath + it.endpoint.url.replace("/{", "/:").replace("}", "")
            when (it.endpoint.httpMethod) {
                HTTPMethod.GET -> service.get(path, it.function)
                HTTPMethod.POST -> service.post(path, it.function)
                HTTPMethod.PUT -> service.put(path, it.function)
                HTTPMethod.PATCH -> service.patch(path, it.function)
                HTTPMethod.HEAD -> service.head(path, it.function)
                HTTPMethod.DELETE -> service.delete(path, it.function)
                HTTPMethod.OPTIONS -> service.options(path, it.function)
            }
        }
    }

    fun stop() {
        http.stop()
    }
}