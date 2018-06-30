package com.emoticast.sparktswagger

sealed class Parameter<T>(
        open val name: String,
        open val pattern: Validator<T>,
        open val description: String,
        open val required: Boolean = false,
        open val emptyAsMissing: Boolean = false
)

sealed class HeaderParameter<T>(
        override val name: String,
        override val pattern: Validator<T>,
        override val description: String,
        override val required: Boolean = false,
        override val emptyAsMissing: Boolean = false
) : Parameter<T>(name, pattern, description, required, emptyAsMissing)

sealed class QueryParameter<T>(
        override val name: String,
        override val pattern: Validator<T>,
        override val description: String,
        override val required: Boolean = false,
        override val emptyAsMissing: Boolean = false
) : Parameter<T>(name, pattern, description, required, emptyAsMissing)

data class PathParam<T>(
        val path: String? = null,
        override val name: String,
        override val pattern: Validator<T>,
        override val description: String) : Parameter<T>(name, pattern, description, true, false) {
    operator fun div(path: String) = this.path + "/" + path
}

data class ParametrizedPath(val path: String, val pathParameters: List<PathParam<out Any>>) {
    operator fun div(path: String) = copy(path = this.path + "/" + path)
}

data class HeaderParam<T>(override val name: String,
                          override val pattern: Validator<T>,
                          override val description: String,
                          override val emptyAsMissing: Boolean) : HeaderParameter<T>(name, pattern, description, true, emptyAsMissing)

data class OptionalHeaderParam<T>(override val name: String,
                                  override val pattern: Validator<T>,
                                  override val description: String,
                                  val default: T,
                                  override val emptyAsMissing: Boolean) : HeaderParameter<T>(name, pattern, description, false, emptyAsMissing)

data class OptionalQueryParam<T>(
        override val name: String,
        override val pattern: Validator<T>,
        override val description: String,
        val default: T,
        override val emptyAsMissing: Boolean) : QueryParameter<T>(name, pattern, description, false, emptyAsMissing)

data class QueryParam<T>(
        override val name: String,
        override val pattern: Validator<T>,
        override val description: String,
        override val emptyAsMissing: Boolean) : QueryParameter<T>(name, pattern, description, true, emptyAsMissing)

fun <T> optionalQueryParam(name: String,
                           description: String,
                           condition: Validator<T>, allowEmptyValues: Boolean = false) =
        OptionalQueryParam(name, condition.optional(), description, default = null, emptyAsMissing = allowEmptyValues)

fun <T> optionalQueryParam(name: String,
                           description: String,
                           default: T,
                           condition: Validator<T>, allowEmptyValues: Boolean = false) =
        OptionalQueryParam(name, condition, description, default = default, emptyAsMissing = allowEmptyValues)

fun <T> queryParam(name: String,
                   description: String,
                   condition: Validator<T>, emptyAsMissing: Boolean = false) =
        QueryParam(name, condition, description, emptyAsMissing = emptyAsMissing)

fun <T> optionalHeaderParam(
        name: String,
        description: String,
        condition: Validator<T>, emptyAsMising: Boolean = false) =
        OptionalHeaderParam(name, condition.optional(), description, default = null, emptyAsMissing = emptyAsMising)

fun <T> optionalHeaderParam(
        name: String,
        description: String,
        default: T,
        condition: Validator<T>, emptyAsMissing: Boolean =false) =
        OptionalHeaderParam(name, condition, description, default = default, emptyAsMissing = emptyAsMissing)

fun <T> headerParam(name: String, description: String, condition: Validator<T>, allowEmptyValues: Boolean = false) =
        HeaderParam(name, condition, description, emptyAsMissing = allowEmptyValues)

fun <T> pathParam(name: String, description: String, condition: Validator<T>) = PathParam(
        null,
        name,
        condition,
        description)
