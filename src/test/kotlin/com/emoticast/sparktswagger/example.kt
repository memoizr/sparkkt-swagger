package com.emoticast.sparktswagger

fun main(args: Array<String>) {
    Server(config).startWithRoutes(ServerRouter).generateDoc()
}
