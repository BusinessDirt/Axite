package github.businessdirt.axite.commands.suggestions

import github.businessdirt.axite.commands.strings.Message
import github.businessdirt.axite.commands.strings.StringRange

sealed class Suggestion : Comparable<Suggestion> {
    abstract val range: StringRange
    abstract val text: String
    abstract val tooltip: Message?

    fun apply(input: String): String {
        if (range.start == 0 && range.end == input.length) return text

        return buildString {
            if (range.start > 0) append(input.substring(0, range.start))
            append(text)
            if (range.end < input.length) append(input.substring(range.end))
        }
    }

    fun expand(command: String, range: StringRange): Suggestion {
        if (range == this.range) return this

        val newText = buildString {
            if (range.start < this@Suggestion.range.start) {
                append(command.substring(range.start, this@Suggestion.range.start))
            }
            append(text)
            if (range.end > this@Suggestion.range.end) {
                append(command.substring(this@Suggestion.range.end, range.end))
            }
        }

        return StringSuggestion(range, newText, tooltip)
    }

    override fun compareTo(other: Suggestion): Int = text.compareTo(other.text)

    open fun compareToIgnoreCase(other: Suggestion): Int = text.compareTo(other.text, ignoreCase = true)
}

data class StringSuggestion(
    override val range: StringRange,
    override val text: String,
    override val tooltip: Message? = null
) : Suggestion()

data class IntegerSuggestion(
    override val range: StringRange,
    val value: Int,
    override val tooltip: Message? = null
) : Suggestion() {

    override val text: String = value.toString()

    override fun compareTo(other: Suggestion): Int = when (other) {
        is IntegerSuggestion -> value.compareTo(other.value)
        else -> super.compareTo(other)
    }

    override fun compareToIgnoreCase(other: Suggestion): Int = compareTo(other)
}