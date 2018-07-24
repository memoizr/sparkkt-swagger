package com.emoticast.sparktswagger

abstract class Sealed {
    val type: String = this::class.simpleName!!
}
