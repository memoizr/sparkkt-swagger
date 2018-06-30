package com.emoticast.sparktswagger

object nonNegativeInt : Validator<Int> {
    override val description = "non negative integer"
    override val regex = """^\d+$""".toRegex()
    override val parse: (String) -> Int = { it.toInt() }
}

object nonEmptyString : Validator<String> {
    override val description = "non empty string"
    override val regex = """^\w+$""".toRegex()
    override val parse: (String) -> String = { it }
}

//object optionalNonEmptyString : Validator<String?> {
//    override val description = "non empty string"
//    override val regex = """^\w+$""".toRegex()
//}
//
//object nonEmptyStringList : Validator<List<String>> {
//    override val description = "non empty string list"
//    override val regex = """^(\w+,?)*\w+$""".toRegex()
//}

interface Validator<T> {
    val regex: Regex
    val description: String

    val parse: (String) -> T

    fun optional(): Validator<T?> = this as Validator<T?>
}
