package com.emoticast.sparktswagger

import com.emoticast.sparktswagger.documentation.generateDocs

fun main(args: Array<String>) {
    Snitch(config).setRoutes(ServerRouter).generateDocs().writeToFile()
}
