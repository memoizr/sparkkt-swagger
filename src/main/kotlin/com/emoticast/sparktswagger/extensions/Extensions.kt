package com.emoticast.extensions

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.eclipse.jetty.util.UrlEncoded
import kotlin.reflect.KClass

internal fun <T : Any?> T.print(): T = this.apply {
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
internal val String.urlEncoded: String get() = UrlEncoded.encodeString(this)
internal inline fun <reified T : Any> String.parseJson() :T  = Gson().fromJson(this, T::class.java)
internal fun <T : Any> String.parseJson(klass: KClass<T>) :T  = Gson().fromJson(this, klass.java)
