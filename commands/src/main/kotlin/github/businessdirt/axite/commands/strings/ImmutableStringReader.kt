package github.businessdirt.axite.commands.strings

interface ImmutableStringReader {
    val string: String
    var cursor: Int

    fun remainingLength(): Int
    fun totalLength(): Int

    fun consumed(): String
    fun remaining(): String

    fun canRead(length: Int = 1): Boolean
    fun peek(offset: Int = 0): Char
}