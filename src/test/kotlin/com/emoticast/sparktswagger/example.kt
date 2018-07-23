package com.emoticast.sparktswagger

fun main(args: Array<String>) {
    print(Server(config).startWithRoutes(ServerRouter).generateDocs())
}
