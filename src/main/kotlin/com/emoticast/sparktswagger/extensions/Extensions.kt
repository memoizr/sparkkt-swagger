package com.emoticast.sparktswagger.extensions

import com.emoticast.sparktswagger.Sealed
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializer
import kotlin.reflect.full.isSubclassOf

fun <T : Any?> T.print(): T = this.apply {
    val stackFrame = Thread.currentThread().stackTrace[2]
    val className = stackFrame.className
    val methodName = stackFrame.methodName
    val fileName = stackFrame.fileName
    val lineNumber = stackFrame.lineNumber
    println("$this at $className.$methodName($fileName:$lineNumber)")
}

val gson = GsonBuilder().registerSealedClassAdapter().setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZZZZZ").create()
val Any.json: String get() = gson.toJson(this)
fun Any.toHashMap() = gson.fromJson(json, Map::class.java)
inline fun <reified T : Any> String.parseJson() :T  = gson.fromJson(this, T::class.java)

fun GsonBuilder.registerSealedClassAdapter() = registerTypeHierarchyAdapter(Sealed::class.java, JsonDeserializer<Sealed> { json, typeOfT, context ->
   val clazz = typeOfT as Class<*>
   val nestedClasses = clazz.kotlin.nestedClasses
   val subclasses = nestedClasses.filter { it.isFinal && it.isSubclassOf(clazz.kotlin) }
   val type = json.asJsonObject.get(Sealed::type.name).asString
   val subtype = subclasses.find { it.simpleName == type }
   Gson().fromJson<Sealed>(json, subtype!!.java)
})
