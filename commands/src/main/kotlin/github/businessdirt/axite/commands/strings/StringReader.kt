package github.businessdirt.axite.commands.strings

import github.businessdirt.axite.commands.exceptions.*

/**
 * A mutable string reader used for parsing commands.
 *
 * @property string The source string.
 */
class StringReader(override val string: String) : ImmutableStringReader {

    override var cursor: Int = 0

    constructor(other: StringReader) : this(other.string) {
        this.cursor = other.cursor
    }

    override fun remainingLength(): Int = string.length - cursor
    override fun totalLength(): Int = string.length

    override fun consumed(): String = string.substring(0, cursor)
    override fun remaining(): String = string.substring(cursor)

    override fun canRead(length: Int): Boolean = cursor + length <= string.length
    override fun peek(offset: Int): Char = string[cursor + offset]

    /** Reads the next character and advances the cursor. */
    fun read(): Char = string[cursor++]
    /** Skips the next character and advances the cursor. */
    fun skip(): Int = cursor++

    /** Skips any whitespace characters at the current cursor. */
    fun skipWhitespace() {
        while (canRead() && Character.isWhitespace(peek())) skip()
    }

    @Throws(CommandSyntaxException::class)
    private inline fun <reified T : Number> StringReader.readNumber(parser: (String) -> T?): T {
        val start = cursor
        while (canRead() && isAllowedNumber(peek())) skip()

        val text = string.substring(start, cursor)
        if (text.isEmpty()) throw error(CommandError.ExpectedType(T::class), start)

        return parser(text) ?: run {
            cursor = start
            throw error(CommandError.InvalidValue(T::class, text), start)
        }
    }

    /** Reads an integer. */
    fun readInt(): Int = readNumber { it.toIntOrNull() }
    /** Reads a double. */
    fun readDouble(): Double = readNumber { it.toDoubleOrNull() }
    /** Reads a long. */
    fun readLong(): Long = readNumber { it.toLongOrNull() }
    /** Reads a float. */
    fun readFloat(): Float = readNumber { it.toFloatOrNull() }

    /** Reads an unquoted string (alphanumeric, underscore, dash, dot, plus). */
    fun readUnquotedString(): String {
        val start = cursor
        while (canRead() && isAllowedInUnquotedString(peek())) skip()
        return string.substring(start, cursor)
    }

    /**
     * Reads a quoted string.
     * Expects the next character to be a quote.
     */
    @Throws(CommandSyntaxException::class)
    fun readQuotedString(): String {
        if (!canRead()) return ""

        val next = peek()
        expect(CommandError.ExpectedStartOfQuote) { isQuotedStringStart(next) }

        skip()
        return readStringUntil(next)
    }

    /**
     * Reads characters until a terminator is found.
     * Handles escape sequences.
     */
    @Throws(CommandSyntaxException::class)
    fun readStringUntil(terminator: Char): String {
        val result = StringBuilder()
        var escaped = false

        while (canRead()) {
            val c = read()
            if (escaped) {
                // Check if the character after the escape is valid
                if (c == terminator || c == SYNTAX_ESCAPE) {
                    result.append(c)
                    escaped = false
                } else {
                    // Point the cursor at the invalid character for better error reporting
                    cursor--
                    throw error(CommandError.InvalidEscape(c))
                }
            } else when (c) {
                SYNTAX_ESCAPE -> escaped = true
                terminator -> return result.toString()
                else -> result.append(c)
            }
        }

        // If the loop finishes, we ran out of string before finding the terminator
        throw error(CommandError.ExpectedEndOfQuote)
    }

    /**
     * Reads a string.
     * Can be either quoted or unquoted.
     */
    @Throws(CommandSyntaxException::class)
    fun readString(): String {
        if (!canRead()) return ""

        val next = peek()
        if (isQuotedStringStart(next)) {
            skip()
            return readStringUntil(next)
        }

        return readUnquotedString()
    }

    /**
     * Reads a boolean value ("true" or "false").
     */
    @Throws(CommandSyntaxException::class)
    fun readBoolean(): Boolean {
        val start = cursor
        val value = readString()

        expect(CommandError.ExpectedType(Boolean::class), start, value::isNotEmpty)
        return when (value) {
            "true" -> true
            "false" -> false
            else -> {
                cursor = start
                throw error(CommandError.InvalidValue(Boolean::class, value))
            }
        }
    }

    /**
     * Expects the next character to be [c].
     * Advances the cursor if successful.
     */
    @Throws(CommandSyntaxException::class)
    fun expect(c: Char) {
        expect(CommandError.ExpectedSymbol(c)) { canRead() && peek() == c }
        skip()
    }

    companion object {
        const val SYNTAX_ESCAPE: Char = '\\'
        const val SYNTAX_DOUBLE_QUOTE: Char = '"'
        const val SYNTAX_SINGLE_QUOTE: Char = '\''

        fun isAllowedNumber(c: Char): Boolean =
            c in '0'..'9' || c == '.' || c == '-'

        fun isQuotedStringStart(c: Char): Boolean =
            c == SYNTAX_DOUBLE_QUOTE || c == SYNTAX_SINGLE_QUOTE

        fun isAllowedInUnquotedString(c: Char): Boolean =
            c in '0'..'9' || c in 'A'..'Z'
                    || c in 'a'..'z' || c == '_'
                    || c == '-' || c == '.' || c == '+'
    }
}
