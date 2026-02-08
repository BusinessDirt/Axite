package github.businessdirt.axite.commands.arguments

import github.businessdirt.axite.commands.strings.StringReader

/**
 * Sealed class for string argument types.
 */
sealed class StringArgumentType : ArgumentType<String> {

    /**
     * Reads a single word (unquoted string).
     * Stops at whitespace.
     */
    data object Word : StringArgumentType() {
        override val examples: Collection<String> = listOf("word", "foo_bar", "123")
        override fun parse(reader: StringReader): String = reader.readUnquotedString()
    }

    /**
     * Reads the entire remaining input.
     * This is useful for chat messages or descriptions at the end of a command.
     */
    data object Greedy : StringArgumentType() {
        override val examples: Collection<String> = listOf("word", "words with spaces", "anything goes")
        override fun parse(reader: StringReader): String {
            val text = reader.remaining()
            reader.cursor = reader.totalLength()
            return text
        }
    }

    /**
     * Reads a string that can be quoted.
     * If the first character is a quote, it reads until the closing quote, handling escapes.
     * Otherwise, it behaves like [Word].
     */
    data object Quotable : StringArgumentType() {
        override val examples: Collection<String> = listOf("\"quoted string\"", "word", "\"\"")
        override fun parse(reader: StringReader): String = reader.readString()
    }
}

/**
 * Escapes the string if it contains characters that require quoting.
 */
fun String.escapeIfRequired(): String = when {
    any { !StringReader.isAllowedInUnquotedString(it) } -> escape()
    else -> this
}

/**
 * Wraps the string in quotes and escapes internal quotes and backslashes.
 */
fun String.escape(): String = buildString {
    append('"')
    for (char in this@escape) {
        if (char == '\\' || char == '"') append('\\')
        append(char)
    }
    append('"')
}
