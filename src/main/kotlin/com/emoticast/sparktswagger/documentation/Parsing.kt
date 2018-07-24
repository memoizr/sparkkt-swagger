package com.emoticast.sparktswagger.documentation

import com.emoticast.sparktswagger.Sealed
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.full.starProjectedType
import kotlin.reflect.jvm.jvmErasure

internal fun toSchema(type: KType): Schemas {
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
        klass.isSealed && Sealed::class.java.isAssignableFrom(klass.java) -> sealedSchema(klass, type)
        else -> objectSchema(klass)
    }
    return schema.apply {
        if (type.isMarkedNullable) nullable = true
    }
}

private fun objectSchema(klass: KClass<*>): Schemas.ObjectSchema {
    val parameters = klass.primaryConstructor!!.parameters
    return Schemas.ObjectSchema(
            properties = parameters.map { param ->
                param.name!! to (toSchema(param.type)
                        .let { schema ->
                            val desc = param.annotations.find { it is Description }?.let { (it as Description) }
                            when (schema) {
                                is Schemas.ArraySchema -> schema.apply {
                                    description = desc?.description
                                    example = if (desc?.exEmptyList != null && desc.exEmptyList) listOf<Any>() else null
                                }
                                is Schemas.StringSchema -> schema.apply {
                                    description = desc?.description
                                    example = desc?.exString?.nullIfEmpty()
                                }
                                is Schemas.FloatSchema -> schema.apply {
                                    description = desc?.description
                                    example = desc?.exFloat?.nullIfZero()
                                }
                                is Schemas.DoubleSchema -> schema.apply {
                                    description = desc?.description
                                    example = desc?.exDouble?.nullIfZero()
                                }
                                is Schemas.IntSchema -> schema.apply {
                                    description = desc?.description
                                    example = desc?.exInt?.nullIfZero()
                                }
                                is Schemas.LongSchema -> schema.apply {
                                    description = desc?.description
                                    example = desc?.exLong?.nullIfZero()
                                }
                                is Schemas.BaseSchema<*> -> schema.apply {
                                    description = desc?.description
                                }
                                else -> schema
                            }
                        }
                        )
            }
                    .toMap()
            ,
            required = parameters.filter { !it.type.isMarkedNullable }.map { it.name!! }
    )
}

private fun sealedSchema(klass: KClass<*>, type: KType): Schemas.ObjectSchema {
    val subtypes = klass.nestedClasses.filter { it.isFinal && Sealed::class.java.isAssignableFrom(it.java) }.map { it.starProjectedType }
    return Schemas.ObjectSchema(anyOf = subtypes.map { o ->
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

private fun String.nullIfEmpty() = if (isEmpty()) null else this
private fun Int.nullIfZero() = if (this == 0) null else this
private fun Long.nullIfZero() = if (this == 0L) null else this
private fun Double.nullIfZero() = if (this == 0.0) null else this
private fun Float.nullIfZero() = if (this == 0f) null else this

@Suppress("IMPLICIT_CAST_TO_ANY")
private fun getExample(type: KType): Any {
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
