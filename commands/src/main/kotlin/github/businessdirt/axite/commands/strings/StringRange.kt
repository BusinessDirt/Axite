package github.businessdirt.axite.commands.strings

import kotlin.math.max
import kotlin.math.min

/**
 * Represents a range of characters in a string.
 *
 * @property start The start index (inclusive).
 * @property end The end index (exclusive).
 */
data class StringRange(val start: Int, val end: Int) {

    /** Checks if the range is empty (start == end). */
    val isEmpty: Boolean
        get() = start == end

    /** Returns the length of the range. */
    val length: Int
        get() = end - start

    /**
     * Extracts the substring corresponding to this range from the reader.
     *
     * @param reader The reader containing the string.
     * @return The substring.
     */
    fun get(reader: ImmutableStringReader): String =
        reader.string.substring(start, end)

    /**
     * Extracts the substring corresponding to this range from the string.
     *
     * @param string The string.
     * @return The substring.
     */
    fun get(string: String): String =
        string.substring(start, end)

    companion object {
        /** Creates a range at a specific position (length 0). */
        @JvmStatic
        fun at(pos: Int) = StringRange(pos, pos)

        /** Creates a range between two positions. */
        @JvmStatic
        fun between(start: Int, end: Int) = StringRange(start, end)

        /** Creates a range encompassing both given ranges. */
        @JvmStatic
        fun encompassing(a: StringRange, b: StringRange) = StringRange(
            min(a.start, b.start),
            max(a.end, b.end)
        )
    }
}
