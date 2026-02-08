package github.businessdirt.axite.commands.strings

/**
 * An immutable view of a string reader.
 * Allows inspecting the state of reading without providing methods to modify it directly
 * (though implementations like [StringReader] are mutable).
 */
interface ImmutableStringReader {
    /** The source string. */
    val string: String
    /** The current cursor position. */
    var cursor: Int

    /** Returns the number of characters remaining. */
    fun remainingLength(): Int
    /** Returns the total length of the string. */
    fun totalLength(): Int

    /** Returns the string consumed so far. */
    fun consumed(): String
    /** Returns the string remaining to be read. */
    fun remaining(): String

    /**
     * Checks if at least [length] characters can be read.
     * @param length The number of characters to check.
     */
    fun canRead(length: Int = 1): Boolean
    /**
     * Peeks at the character at the specified offset from the current cursor.
     * @param offset The offset from the cursor.
     */
    fun peek(offset: Int = 0): Char
}
