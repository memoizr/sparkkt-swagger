package com.emoticast.sparktswagger.documentation

import com.emoticast.sparktswagger.Sealed
import java.io.File
import java.io.FileOutputStream
import java.util.*

internal fun writeToFile(content: String, destination: String) {
    File(destination.split("/").dropLast(1).joinToString("")).apply { if (!exists()) mkdirs() }
    content.byteInputStream().use { input ->
        FileOutputStream(destination).use { output ->
            input.copyTo(output)
        }
    }
}

internal fun copyResourceToFile(resourceName: String, destination: String) {
    val stream = ClassLoader.getSystemClassLoader().getResourceAsStream(resourceName)
            ?: throw Exception("Cannot get resource \"$resourceName\" from Jar file.")
    val s = "$destination/$resourceName"
    File(destination).apply { if (!exists()) mkdirs() }
    val resStreamOut = FileOutputStream(s)
    stream.use { input ->
        resStreamOut.use { output ->
            input.copyTo(output)
        }
    }
}

data class TestClass(
        @Description("no way punk", exString = "https://google.com")
        val aString: String,
        @Description("The best int", exInt = 33)
        val aInt: Int,
        val aLong: Long,
        val aFloat: Float,
        val aDouble: Double,
        val aBoolean: Boolean,
        val aBinary: ByteArray,
        val aDate: Date,
        val aListString: List<String>,
        @Description("A great list", exEmptyList = true)
        val aListBoolean: List<Boolean>,
        val anObjectList: List<SimpleObject>,
        val theEnum: ClassicEnum,
        val aSeal: MySeal
)

data class SimpleObject(
        val a: String,
        val b: Int,
        val c: Long
)

enum class ClassicEnum {
    foo, bar, baz
}

sealed class MySeal : Sealed() {
    data class Bar(val x: String) : MySeal()
    data class Yo(val y: Int) : MySeal()
}
