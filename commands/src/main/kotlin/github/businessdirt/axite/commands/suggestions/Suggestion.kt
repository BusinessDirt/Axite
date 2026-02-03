package github.businessdirt.axite.commands.suggestions

import github.businessdirt.axite.commands.strings.Message
import github.businessdirt.axite.commands.strings.StringRange
import java.util.*

open class Suggestion(
    val range: StringRange,
    val text: String,
    val tooltip: Message? = null
) : Comparable<Suggestion> {

    fun apply(input: String): String {
        if (range.start == 0 && range.end == input.length) return text

        return buildString {
            if (range.start > 0) append(input.substring(0, range.start))
            append(text)
            if (range.end < input.length) append(input.substring(range.end))
        }
    }

    override fun compareTo(other: Suggestion): Int =
        text.compareTo(other.text)

    open fun compareToIgnoreCase(other: Suggestion): Int =
        text.compareTo(other.text, ignoreCase = true)

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

        return Suggestion(range, newText, tooltip)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Suggestion) return false
        return range == other.range && text == other.text && tooltip == other.tooltip
    }

    override fun hashCode(): Int = Objects.hash(range, text, tooltip)
    override fun toString(): String = "Suggestion(range=$range, text='$text', tooltip='$tooltip')"
}

class IntegerSuggestion(
    range: StringRange,
    val value: Int,
    tooltip: Message? = null
) : Suggestion(range, value.toString(), tooltip) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is IntegerSuggestion) return false
        if (!super.equals(other)) return false
        return value == other.value
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + value
        return result
    }

    override fun toString(): String =
        "IntegerSuggestion(value=$value, range=$range, text='$text', tooltip='$tooltip')"

    override fun compareTo(other: Suggestion): Int = when (other) {
        is IntegerSuggestion -> value.compareTo(other.value)
        else -> super.compareTo(other)
    }

    override fun compareToIgnoreCase(other: Suggestion): Int = compareTo(other)
}