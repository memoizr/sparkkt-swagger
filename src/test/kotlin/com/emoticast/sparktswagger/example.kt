package com.emoticast.sparktswagger

import ch.qos.logback.classic.Level

fun main(args: Array<String>) {
    Server(Level.INFO).start { ServerRouter(it) }
}
