package com.emoticast.sparktswagger

sealed class Parameter<T>(
        open val name: String,
        open val pattern: Validator<T>,
        open val description: String,
        open val required: Boolean = false,
        open val default: T? = null,
        open val allowEmptyValues: Boolean = false
)

class HeaderParam<T>(override val name: String,
                     override val pattern: Validator<T>,
                     override val description: String,
                     override val required: Boolean = true,
                     override val default: T? = null,
                     override val allowEmptyValues: Boolean = false) : Parameter<T>(name, pattern, description, required, default, allowEmptyValues)

class PathParam<T>(val path: String? = null,
                   override val name: String,
                   override val pattern: Validator<T>,
                   override val description: String,
                   override val required: Boolean = true,
                   override val default: T? = null,
                   override val allowEmptyValues: Boolean = false) : Parameter<T>(name, pattern, description, required, default, allowEmptyValues) {
    operator fun div(path: String) = this.path + "/" + path
}

data class ParametrizedPath(val path: String, val pathParameters: List<PathParam<out Any>>) {
    operator fun div(path: String) = copy(path = this.path + "/" + path)
}

data class QueryParam<T>(
        override val name: String,
        override val pattern: Validator<T>,
        override val description: String,
        override val required: Boolean = true,
        override val default: T? = null,
        override val allowEmptyValues: Boolean = false) : Parameter<T>(name, pattern, description, required, default, allowEmptyValues)

fun <T> optionalQueryParam(name: String,
                           description: String,
                           condition: Validator<T>) =
        QueryParam(name, condition.optional(), description, required = false)

fun <T> optionalHeaderParam(
        name: String,
        description: String,
        condition: Validator<T>) =
        HeaderParam(name, condition.optional(), description, required = false)

fun <T> optionalHeaderParam(
        name: String,
        description: String,
        default: T,
        condition: Validator<T>) =
        HeaderParam(name, condition, description, default = default, required = false)

fun <T> optionalQueryParam(name: String,
                           description: String,
                           default: T,
                           condition: Validator<T>) =
        QueryParam(name, condition, description, required = false, default = default)

fun <T> queryParam(name: String,
                   description: String,
                   condition: Validator<T>) =
        QueryParam(name, condition, description, true)

fun <T> headerParam(name: String, description: String, condition: Validator<T>, required: Boolean = true, default: T? = null) =
        HeaderParam(name, condition, description, required, default)

fun <T> pathParam(name: String, description: String, condition: Validator<T>) = PathParam(
        null,
        name,
        condition,
        description,
        true,
        null,
        true)
