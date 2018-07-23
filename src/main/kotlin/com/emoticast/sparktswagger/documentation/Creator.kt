package com.emoticast.sparktswagger.documentation

import com.emoticast.extensions.json
import com.emoticast.sparktswagger.HTTPMethod
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.full.starProjectedType
import kotlin.reflect.jvm.jvmErasure

object DocCreator {
}

fun OpenApi.withPath(name: String, path: Path) =
        copy(paths = paths + (name to path))

fun Path.withOperation(method: HTTPMethod, operation: Operation) = when (method) {
    HTTPMethod.GET -> copy(get = operation)
    HTTPMethod.POST -> copy(post = operation)
    HTTPMethod.DELETE -> copy(delete = operation)
    HTTPMethod.PUT -> copy(put = operation)
    HTTPMethod.PATCH -> copy(patch = operation)
    HTTPMethod.HEAD -> copy(head = operation)
    HTTPMethod.OPTIONS -> copy(options = operation)
}

fun Operation.withParameter(parameter: Parameters.Parameter) =
        copy(parameters = (parameters ?: emptyList()) + parameter)

enum class ContentType(val value: String) {
    APPLICATION_ATOM_XML("application/atom+xml"),
    APPLICATION_FORM_URLENCODED("application/x-www-form-urlencoded"),
    APPLICATION_JSON("application/json"),
    APPLICATION_OCTET_STREAM("application/octet-stream"),
    APPLICATION_SVG_XML("application/svg+xml"),
    APPLICATION_XHTML_XML("application/xhtml+xml"),
    APPLICATION_XML("application/xml"),
    MULTIPART_FORM_DATA("multipart/form-data"),
    TEXT_HTML("text/html"),
    TEXT_PLAIN("text/plain"),
    TEXT_XML("text/xml"),
    WILDCARD("*/*");
}

fun Operation.withRequestBody(
        contentType: ContentType,
        body: KClass<*>) =
        copy(requestBody = RequestBodies.RequestBody(content =
        mapOf(contentType.value to MediaType(toSchema(body.starProjectedType)))))

fun Operation.withResponse(
        contentType: ContentType,
        body: KClass<*>,
        code: String = "200"
) = copy(responses = responses + (code to Responses.Response(
        content = mapOf(contentType.value to MediaType(
                toSchema(body.starProjectedType)
        )))))

fun main(args: Array<String>) {
    val json = OpenApi(info = Info("hello world", "1.0"))
            .withPath("/foos/{bars}", Path()
                    .withOperation(HTTPMethod.POST, Operation(emptyMap())
                            .withParameter(Parameters.PathParameter("bars", toSchema(String::class.starProjectedType)))
                            .withParameter(Parameters.QueryParameter("query", false, toSchema(String::class.starProjectedType)))
                            .withParameter(Parameters.HeaderParameter("X-Accept", false, toSchema(String::class.starProjectedType)))
                            .withResponse(ContentType.APPLICATION_JSON, TestClass::class)
                            .withRequestBody(ContentType.APPLICATION_JSON, TestClass::class)
                    ))
            .json
}

fun toSchema(type: KType): Schemas {
    val klass = type.jvmErasure
    val schema = when {
        klass == String::class -> Schemas.StringSchema()
        klass == Int::class -> Schemas.IntSchema()
        klass == Long::class -> Schemas.LongSchema()
        klass == Float::class -> Schemas.FloatSchema()
        klass == Double::class -> Schemas.DoubleSchema()
        klass == Boolean::class -> Schemas.BooleanSchema()
        klass == Date::class -> Schemas.DateSchema()
        klass == ByteArray::class -> Schemas.BinarySchema()
        klass == List::class -> Schemas.ArraySchema(items = toSchema(type.arguments.first().type!!))
        klass.java.isEnum -> Schemas.StringSchema(enum = klass.java.enumConstants.map { it.toString() })
        klass.isSealed && Sealed::class.java.isAssignableFrom(klass.java) -> {
            val objects = klass.nestedClasses.filter { it.isFinal && Sealed::class.java.isAssignableFrom(it.java) }.map { it.starProjectedType }
            Schemas.ObjectSchema(anyOf = objects.map { o ->
                toSchema(o).let {
                    when (it) {
                        is Schemas.ObjectSchema -> {
                            it.copy(properties = mapOf(Sealed::type.name to Schemas.StringSchema(description = o.jvmErasure.simpleName)) + (it.properties
                                    ?: emptyMap()),
                                    required = listOf(Sealed::type.name) + (it.required ?: emptyList())
                            )
                        }
                        else -> it
                    }
                }
            }, example = getExample(type))
        }
        else -> {
            val parameters = klass.primaryConstructor!!.parameters
            Schemas.ObjectSchema(
                    properties = parameters.map { param -> param.name!! to (toSchema(param.type).let {
                        when (it) {
                        is Schemas.BaseSchema<*> -> it.apply { description = param.annotations.find { it is Description }?.let { (it as Description).value}
                        }
                        else -> it
                    }
                    }) }.toMap(),
                    required = parameters.filter { !it.type.isMarkedNullable }.map { it.name!! }
            )
        }
    }
    return schema.apply {
        if (type.isMarkedNullable)
            nullable = true
    }
}

fun getExample(type: KType): Any {
    val klass = type.jvmErasure
    val value = when {
        klass == String::class -> "string"
        klass == Int::class -> 0
        klass == Long::class -> 0
        klass == Float::class -> 0.0f
        klass == Double::class -> 0.0
        klass == Boolean::class -> false
        klass == ByteArray::class -> listOf<String>()
        klass == Date::class -> Date().toString()
        klass == List::class -> listOf(getExample(type.arguments.first().type!!))
        klass.java.isEnum -> klass.java.enumConstants.map { it.toString() }.first()
        klass.isSealed && Sealed::class.java.isAssignableFrom(klass.java) -> {
            val subclass = klass.nestedClasses.filter { it.isFinal && Sealed::class.java.isAssignableFrom(it.java) }.first()
            val ex = getExample(subclass.starProjectedType) as Map<Any, Any>
            mapOf(Sealed::type.name to subclass.simpleName) + ex
        }
        else -> {
            val parameters = klass.primaryConstructor!!.parameters
            parameters.map { it.name!! to getExample(it.type) }.toMap()
        }
    }
    return value
}

abstract class Sealed {
    val type: String = this::class.simpleName!!
}

data class TestClass(
        val aString: String,
        val aInt: Int,
        val aLong: Long,
        val aFloat: Float,
        val aDouble: Double,
        val aBoolean: Boolean,
        val aBinary: ByteArray,
        val aDate: Date,
        val aListString: List<String>,
        val aListBoolean: List<String>,
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
