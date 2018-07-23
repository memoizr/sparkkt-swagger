package com.emoticast.extensions

import com.google.gson.Gson
import com.google.gson.GsonBuilder

 fun <T : Any?> T.print(): T = this.apply {
    val stackFrame = Thread.currentThread().stackTrace[2]
    val className = stackFrame.className
    val methodName = stackFrame.methodName
    val fileName = stackFrame.fileName
    val lineNumber = stackFrame.lineNumber
    println("$this at $className.$methodName($fileName:$lineNumber)")
}

internal val gson = GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZZZZZ").create()
internal val Any.json: String get() = Gson().toJson(this)
internal fun Any.toHashMap() = gson.fromJson(json, Map::class.java)
