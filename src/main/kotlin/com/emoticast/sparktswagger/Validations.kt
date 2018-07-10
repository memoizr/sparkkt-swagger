package com.emoticast.sparktswagger

object NonNegativeInt : Validator<Int> {
    override val description = "non negative integer"
    override val regex = """^\d+$""".toRegex()
    override val parse: (String) -> Int = { it.toInt() }
}

object NonEmptyString : Validator<String> {
    override val description = "non empty string"
    override val regex = """^.+$""".toRegex()
    override val parse: (String) -> String = { it }
}

object NonEmptyStringSet : Validator<Set<String>> {
    override val description = "non empty string set"
    override val regex = """^(.+,?)*\w+$""".toRegex()
    override val parse: (String) -> Set<String> = { it.split(",").toSet() }
}

object StringSet : Validator<Set<String>> {
    override val description = "string set"
    override val regex = """^\.*$""".toRegex()
    override val parse: (String) -> Set<String> = { it.split(",").toSet() }
}

interface Validator<T> {
    val regex: Regex
    val description: String

    val parse: (String) -> T

    fun optional(): Validator<T?> = this as Validator<T?>
}
