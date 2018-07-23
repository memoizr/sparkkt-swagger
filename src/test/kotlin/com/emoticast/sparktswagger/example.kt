package com.emoticast.sparktswagger

import com.emoticast.extensions.json

fun main(args: Array<String>) {
    print(Server(config).startWithRoutes(ServerRouter).generateDocs().json)
}
