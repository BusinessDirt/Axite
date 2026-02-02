package github.businessdirt.axite.commands.strings

import kotlin.math.max
import kotlin.math.min

data class StringRange(val start: Int, val end: Int) {

    val isEmpty: Boolean
        get() = start == end

    val length: Int
        get() = end - start

    fun get(reader: ImmutableStringReader): String =
        reader.string.substring(start, end)

    fun get(string: String): String =
        string.substring(start, end)

    companion object {
        @JvmStatic
        fun at(pos: Int) = StringRange(pos, pos)

        @JvmStatic
        fun between(start: Int, end: Int) = StringRange(start, end)

        @JvmStatic
        fun encompassing(a: StringRange, b: StringRange) = StringRange(
            min(a.start, b.start),
            max(a.end, b.end)
        )
    }
}