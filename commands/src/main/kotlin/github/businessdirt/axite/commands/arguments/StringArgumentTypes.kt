package github.businessdirt.axite.commands.arguments

import github.businessdirt.axite.commands.strings.StringReader

/**
 * Parses a single word (no spaces allowed).
 */
class WordArgumentType : ArgumentType<String> {
    override fun parse(reader: StringReader): String = reader.readUnquotedString()
    override val examples: Collection<String> = listOf("word", "foo_bar", "123")

    override fun equals(other: Any?): Boolean = other is WordArgumentType
    override fun hashCode(): Int = WordArgumentType::class.hashCode()
}

/**
 * Parses a string. If it contains spaces, it must be "quoted like this".
 */
class StringArgumentType : ArgumentType<String> {
    override fun parse(reader: StringReader): String = reader.readString()
    override val examples: Collection<String> = listOf("\"quoted string\"", "word", "\"\"")

    override fun equals(other: Any?): Boolean = other is StringArgumentType
    override fun hashCode(): Int = StringArgumentType::class.hashCode()
}

/**
 * Consumes everything from the current cursor until the end of the input.
 */
class GreedyStringArgumentType : ArgumentType<String> {
    override fun parse(reader: StringReader): String {
        val text = reader.remaining()
        reader.cursor = reader.totalLength()
        return text
    }

    override val examples: Collection<String> = listOf("word", "words with spaces", "anything goes")

    override fun equals(other: Any?): Boolean = other is GreedyStringArgumentType
    override fun hashCode(): Int = GreedyStringArgumentType::class.hashCode()
}

fun String.escapeIfRequired(): String {
    this.toCharArray().forEach {
        if (!StringReader.isAllowedInUnquotedString(it)) return escape(this)
    }

    return this
}

private fun escape(input: String): String {
    val result = StringBuilder("\"")

    for (i in 0..<input.length) {
        val c = input[i]
        if (c == '\\' || c == '"') result.append('\\')
        result.append(c)
    }

    result.append("\"")
    return result.toString()
}