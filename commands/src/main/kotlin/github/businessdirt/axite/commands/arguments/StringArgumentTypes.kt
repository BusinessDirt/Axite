package github.businessdirt.axite.commands.arguments

import github.businessdirt.axite.commands.strings.StringReader

sealed class StringArgumentType : ArgumentType<String> {

    data object Word : StringArgumentType() {
        override val examples: Collection<String> = listOf("word", "foo_bar", "123")
        override fun parse(reader: StringReader): String = reader.readUnquotedString()
    }

    data object Greedy : StringArgumentType() {
        override val examples: Collection<String> = listOf("word", "words with spaces", "anything goes")
        override fun parse(reader: StringReader): String {
            val text = reader.remaining()
            reader.cursor = reader.totalLength()
            return text
        }
    }

    data object Quotable : StringArgumentType() {
        override val examples: Collection<String> = listOf("\"quoted string\"", "word", "\"\"")
        override fun parse(reader: StringReader): String = reader.readString()
    }
}

fun String.escapeIfRequired(): String = when {
    any { !StringReader.isAllowedInUnquotedString(it) } -> escape()
    else -> this
}

private fun String.escape(): String = buildString {
    append('"')
    for (char in this@escape) {
        if (char == '\\' || char == '"') append('\\')
        append(char)
    }
    append('"')
}