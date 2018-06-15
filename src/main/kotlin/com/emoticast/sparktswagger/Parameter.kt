package com.emoticast.sparktswagger

sealed class Parameter<T>(
        open val name: String,
        open val pattern: Validator<T>,
        open val description: String,
        open val default: T? = null,
        open val required: Boolean = false,
        open val allowEmptyValues: Boolean = false
)

class PathParam<T>(val path: String? = null,
                override val name: String,
                override val pattern: Validator<T>,
                override val description: String,
                override val default: T? = null,
                override val required: Boolean = false,
                override val allowEmptyValues: Boolean = false) : Parameter<T>(name, pattern, description, default, required, allowEmptyValues) {
    operator fun div(path: String) = this.path + "/" + path
}

data class ParametrizedPath(val path: String, val pathParameters: List<PathParam<out Any>>) {
    operator fun div(path: String) = copy(path = this.path + "/" + path)
}

data class QueryParam<T>(
        override val name: String,
        override val pattern: Validator<T>,
        override val description: String,
        override val default: T? = null,
        override val required: Boolean = false,
        override val allowEmptyValues: Boolean = false) : Parameter<T>(name, pattern, description, default, required, allowEmptyValues)

fun <T> queryParam(name: String,
                   description: String,
                   condition: Validator<T>,
                   default: T? = null,
                   required: Boolean = false
) = QueryParam(
        name,
        condition,
        description,
        default,
        required,
        required
)

fun <T> pathParam(name: String, description: String, condition: Validator<T>) = PathParam<T>(
        null,
        name,
        condition,
        description,
        null,
        true,
        true
)
