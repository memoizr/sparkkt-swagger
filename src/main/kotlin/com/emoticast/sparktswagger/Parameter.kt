package com.emoticast.sparktswagger

sealed class Parameter<T>(
        open val name: String,
        open val pattern: Validator<T>,
        open val description: String,
        open val required: Boolean = false,
        open val emptyAsMissing: Boolean = false,
        open val invalidAsMissing: Boolean = false
)

sealed class HeaderParameter<T>(
        override val name: String,
        override val pattern: Validator<T>,
        override val description: String,
        override val required: Boolean = false,
        override val emptyAsMissing: Boolean = false,
        override val invalidAsMissing: Boolean = false
) : Parameter<T>(name, pattern, description, required, emptyAsMissing)

sealed class QueryParameter<T>(
        override val name: String,
        override val pattern: Validator<T>,
        override val description: String,
        override val required: Boolean = false,
        override val emptyAsMissing: Boolean = false,
        override val invalidAsMissing: Boolean = false
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
                          override val emptyAsMissing: Boolean,
                          override val invalidAsMissing: Boolean) : HeaderParameter<T>(name, pattern, description, true, emptyAsMissing, invalidAsMissing)

data class OptionalHeaderParam<T>(override val name: String,
                                  override val pattern: Validator<T>,
                                  override val description: String,
                                  val default: T,
                                  override val emptyAsMissing: Boolean,
                                  override val invalidAsMissing: Boolean) : HeaderParameter<T>(name, pattern, description, false, emptyAsMissing, invalidAsMissing)

data class OptionalQueryParam<T>(
        override val name: String,
        override val pattern: Validator<T>,
        override val description: String,
        val default: T,
        override val emptyAsMissing: Boolean,
        override val invalidAsMissing: Boolean) : QueryParameter<T>(name, pattern, description, false, emptyAsMissing, invalidAsMissing)

data class QueryParam<T>(
        override val name: String,
        override val pattern: Validator<T>,
        override val description: String,
        override val emptyAsMissing: Boolean,
        override val invalidAsMissing: Boolean
) : QueryParameter<T>(name, pattern, description, true, emptyAsMissing, invalidAsMissing)

fun <T> optionalQueryParam(name: String,
                           description: String,
                           condition: Validator<T>, emptyAsMissing: Boolean = false,
                           invalidAsMissing: Boolean = false) =
        OptionalQueryParam(name, condition.optional(), description, default = null, emptyAsMissing = emptyAsMissing, invalidAsMissing = invalidAsMissing)

fun <T> optionalQueryParam(name: String,
                           description: String,
                           default: T,
                           condition: Validator<T>,
                           emptyAsMissing: Boolean = false,
                           invalidAsMissing: Boolean = false) =
        OptionalQueryParam(name, condition, description, default = default, emptyAsMissing = emptyAsMissing, invalidAsMissing = invalidAsMissing)

fun <T> queryParam(name: String,
                   description: String,
                   condition: Validator<T>,
                   emptyAsMissing: Boolean = false,
                   invalidAsMissing: Boolean = false) =
        QueryParam(name, condition, description, emptyAsMissing = emptyAsMissing, invalidAsMissing = invalidAsMissing)

fun <T> optionalHeaderParam(
        name: String,
        description: String,
        condition: Validator<T>,
        emptyAsMising: Boolean = false,
        invalidAsMissing: Boolean = false) =
        OptionalHeaderParam(name, condition.optional(), description, default = null, emptyAsMissing = emptyAsMising, invalidAsMissing = invalidAsMissing)

fun <T> optionalHeaderParam(
        name: String,
        description: String,
        default: T,
        condition: Validator<T>,
        emptyAsMissing: Boolean = false,
        invalidAsMissing: Boolean = false) =
        OptionalHeaderParam(name, condition, description, default = default, emptyAsMissing = emptyAsMissing, invalidAsMissing = invalidAsMissing)

fun <T> headerParam(
        name: String,
        description: String,
        condition: Validator<T>,
        emptyAsMissing: Boolean = false,
        invalidAsMissing: Boolean = false
) =
        HeaderParam(name, condition, description, emptyAsMissing = emptyAsMissing, invalidAsMissing = invalidAsMissing)

fun <T> pathParam(name: String, description: String, condition: Validator<T>) = PathParam(
        null,
        name,
        condition,
        description)
