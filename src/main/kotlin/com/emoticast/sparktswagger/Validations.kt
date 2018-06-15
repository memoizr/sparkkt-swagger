package com.emoticast.sparktswagger

object nonNegativeInt : Validator<Int> {
    override val description = "non negative integer"
    override val regex = """^\d+$""".toRegex()
}

object nonEmptyString : Validator<String> {
    override val description = "non empty string"
    override val regex = """^\w+$""".toRegex()
}

interface Validator<T> {
    val regex: Regex
    val description: String
}
