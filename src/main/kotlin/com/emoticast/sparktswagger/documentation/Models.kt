package com.emoticast.sparktswagger.documentation

import java.util.*

data class OpenApi(
        val openapi: String = "3.0.0",
        val info: Info,
        val paths: Map<String, Path> = emptyMap(),
        val servers: List<Server>? = null,
        val components: Components? = null,
        val security: Map<String, List<String>>? = null,
        val tags: List<Tag>? = null,
        val externalDocs: ExternalDocumentation? = null
)

data class Components(
        val schemas: Map<String, Schemas>,
        val responses: Map<String, Responses>,
        val parameters: Map<String, Parameters>,
        val examples: Map<String, Examples>,
        val requestBodies: Map<String, RequestBodies>,
        val headers: Map<String, Headers>,
        val securitySchemes: Map<String, SecuritySchemes>,
        val links: Map<String, Links>
)

interface Ref {
    val `$ref`: String
}

sealed class Schemas {
    abstract class BaseSchema<T>(
            open val type: DataType,
            open val format: Format? = null,
            var nullable: Boolean? = null
    ) : Schemas()

    data class ArraySchema(
            val items: Schemas,
            val maxItems: Int? = null,
            val minItems: Int? = null,
            val uniqueItems: Boolean? = null,
            val description: String? = null,
            val default: List<*>? = null
    ) : BaseSchema<List<*>>(type = DataType.array)

    data class StringSchema(
            val description: String? = null,
            val default: String? = null,
            val pattern: String? = null,
            val enum: List<String>? = null
    ) : BaseSchema<String>(type = DataType.string)

    data class IntSchema(
            val description: String? = null,
            val maximum: Int? = null,
            val minimum: Int? = null,
            val default: Int? = null
    ) : BaseSchema<Int>(type = DataType.integer, format = Format.int32)

    data class LongSchema(
            val description: String? = null,
            val default: Long? = null,
            val maximum: Long? = null,
            val minimum: Long? = null
    ) : BaseSchema<Long>(type = DataType.integer, format = Format.int64)

    data class DoubleSchema(
            val description: String? = null,
            val default: Double? = null,
            val maximum: Double? = null,
            val minimum: Double? = null
    ) : BaseSchema<Double>(type = DataType.number, format = Format.double)

    data class FloatSchema(
            val description: String? = null,
            val default: Float? = null,
            val maximum: Double? = null,
            val minimum: Double? = null
    ) : BaseSchema<Float>(type = DataType.number, format = Format.float)

    data class ByteSchema(
            val description: String? = null,
            val default: Byte? = null
    ) : BaseSchema<Byte>(type = DataType.string, format = Format.byte)

    data class BinarySchema(
            val description: String? = null,
            val default: ByteArray? = null
    ) : BaseSchema<ByteArray>(type = DataType.string, format = Format.byte)

    data class BooleanSchema(
            val description: String? = null,
            val default: Boolean? = null
    ) : BaseSchema<Boolean>(type = DataType.boolean)

    data class DateSchema(
            val description: String? = null,
            val default: Date? = null
    ) : BaseSchema<Date>(type = DataType.string, format = Format.date)

    data class DateTimeSchema(
            val description: String? = null,
            val default: Date? = null
    ) : BaseSchema<Date>(type = DataType.string, format = Format.`date-time`)

    data class PasswordSchema(
            val description: String? = null,
            val pattern: String? = null,
            val default: String? = null
    ) : BaseSchema<String>(type = DataType.string, format = Format.password)

    data class ObjectSchema(
            val allOf: List<Schemas>? = null,
            val oneOf: List<Schemas>? = null,
            val anyOf: List<Schemas>? = null,
            val not: List<Schemas>? = null,
            val items: Schemas? = null,
            val properties: Map<String, Schemas>? = null,
            val additionalProperties: Schemas? = null,
            val description: String? = null,
            val default: Any? = null,
            val required: List<String>? = null,
            val example: Any? = null
    ) : BaseSchema<Any>(type = DataType.`object`)

    data class Reference(override val `$ref`: String) : Schemas(), Ref
}

enum class Format { int32, int64, float, double, byte, binary, date, `date-time`, password }
enum class DataType { integer, number, string, boolean, array, `object` }

sealed class Responses {
    data class Reference(override val `$ref`: String) : Responses(), Ref
    data class Response(
            val description: String = "A response",
            val headers: Map<String, Headers>? = null,
            val content: Map<String, MediaType>? = null,
            val links: Map<String, Links>? = null
    ) : Responses()
}

data class MediaType(
        val schema: Schemas? = null,
        val example: Any? = null,
        val examples: Map<String, Examples>? = null,
        val encoding: Map<String, Encoding>? = null
)

data class Encoding(
        val contentType: String? = null,
        val headers: Map<String, Headers>? = null,
        val style: String? = null,
        val explode: Boolean? = null,
        val allowReserved: Boolean? = null
)

sealed class Headers {
    data class Reference(override val `$ref`: String) : Headers(), Ref
    data class Header(
            val name: String,
            val description: String? = null,
            val externalDocs: ExternalDocumentation? = null
    ) : Headers()
}

sealed class Links {
    data class Reference(override val `$ref`: String) : Links(), Ref
    data class Link(
            val operationRef: String? = null,
            val operationId: String? = null,
            val parameters: Map<String, Any>? = null,
            val requestBody: Any? = null,
            val description: String? = null,
            val server: Server?
    ) : Links()
}

enum class ParameterType { query, header, path, cookie }
sealed class Parameters {
    data class Reference(override val `$ref`: String) : Parameters(), Ref

    abstract class Parameter() : Parameters()

    data class QueryParameter(
            val name: String,
            val required: Boolean,
            val schema: Schemas,
            val description: String? = null,
            val deprecated: Boolean? = false,
            val allowEmptyValue: Boolean? = false
    ) : Parameter() {
        val `in`: ParameterType = ParameterType.query
    }

    data class HeaderParameter(
            val name: String,
            val required: Boolean,
            val schema: Schemas,
            val description: String? = null,
            val deprecated: Boolean? = false
    ) : Parameter() {
        val `in`: ParameterType = ParameterType.header
    }

    data class PathParameter(
            val name: String,
            val schema: Schemas,
            val description: String? = null,
            val deprecated: Boolean? = false
    ) : Parameter() {
        val `in`: ParameterType = ParameterType.path
        val required = true
    }

    data class CookieParameter(
            val name: String,
            val required: Boolean,
            val schema: Schemas,
            val description: String? = null,
            val deprecated: Boolean? = false
    ) : Parameter() {
        val `in`: ParameterType = ParameterType.cookie
    }
}

sealed class Examples {
    data class Reference(override val `$ref`: String) : Examples(), Ref
    data class Example(
            val summary: String? = null,
            val description: String? = null,
            val value: Any? = null,
            val externalValue: String? = null
    )
}

sealed class RequestBodies {
    data class Reference(override val `$ref`: String) : RequestBodies(), Ref
    data class RequestBody(
            val description: String? = null,
            val content: Map<String, MediaType>,
            val required: Boolean? = null
    ) : RequestBodies()
}

data class Path(
        val `$ref`: String? = null,
        val summary: String? = null,
        val description: String? = null,
        val get: Operation? = null,
        val put: Operation? = null,
        val post: Operation? = null,
        val delete: Operation? = null,
        val options: Operation? = null,
        val head: Operation? = null,
        val patch: Operation? = null,
        val trace: Operation? = null,
        val servers: List<Server>? = null,
        val parameters: Parameters? = null
)


data class Operation(
        val responses: Map<String, Responses>,
        val tags: List<String>? = null,
        val summary: String? = null,
        val description: String? = null,
        val externalDocs: ExternalDocumentation? = null,
        val operationId: String? = null,
        val parameters: List<Parameters>? = null,
        val requestBody: RequestBodies? = null,
        val deprecated: Boolean? = null,
        val security: Map<String, List<String>>? = null,
        val servers: List<Server>? = null
)


data class Contact(val name: String? = null, val url: String? = null, val email: String? = null)
data class License(val name: String, val url: String? = null)
data class Server(val url: String, val description: String? = null, val variables: Map<String, ServerVariable>? = null)
data class ServerVariable(val default: String, val enum: List<String>? = null, val description: String? = null)
data class Tag(val name: String, val description: String? = null, val externalDocs: ExternalDocumentation? = null)
data class ExternalDocumentation(val url: String, val description: String? = null)

data class OAuthFlows(
        val implicit: OAuthFlow? = null,
        val password: OAuthFlow? = null,
        val clientCredentials: OAuthFlow? = null,
        val authorizationCode: OAuthFlow? = null
)

data class OAuthFlow(
        val authorizationUrl: String,
        val tokenUrl: String,
        val refreshUrl: String? = null,
        val scopes: Map<String, String>
)

sealed class SecuritySchemes {
    enum class In { query, `header`, cookie }
    enum class Type { apiKey, http, oauth2, openIdConnect }

    data class Reference(override val `$ref`: String) : SecuritySchemes(), Ref
    data class SecurityScheme(
            val type: Type,
            val description: String? = null,
            val name: String,
            val `in`: In,
            val scheme: String,
            val bearerFormat: String?,
            val flows: OAuthFlows,
            val openIdConnectUrl: String
    )
}

data class Info(
        val title: String,
        val version: String,
        val description: String? = null,
        val termsOfService: String? = null,
        val contact: Contact? = null,
        val license: License? = null
)
