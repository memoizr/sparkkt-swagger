package com.emoticast.sparktswagger

import com.emoticast.sparktswagger.documentation.generateDocs

fun main(args: Array<String>) {
    Snitch().setRoutes(ServerRouter).generateDocs().writeDocsToStaticFolder()
}
