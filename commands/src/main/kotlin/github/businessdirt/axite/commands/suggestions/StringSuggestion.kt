package github.businessdirt.axite.commands.suggestions

import github.businessdirt.axite.commands.strings.Message
import github.businessdirt.axite.commands.strings.StringRange

/**
 * Represents a single tab completion suggestion.
 */
sealed class Suggestion : Comparable<Suggestion> {
    /** The range in the input string that this suggestion applies to. */
    abstract val range: StringRange
    /** The text to suggest. */
    abstract val text: String
    /** Optional tooltip to display with the suggestion. */
    abstract val tooltip: Message?

    /**
     * Applies this suggestion to the given input string.
     * Replaces the text in [range] with [text].
     *
     * @param input The original input string.
     * @return The new string with the suggestion applied.
     */
    fun apply(input: String): String {
        if (range.start == 0 && range.end == input.length) return text

        return buildString {
            if (range.start > 0) append(input.substring(0, range.start))
            append(text)
            if (range.end < input.length) append(input.substring(range.end))
        }
    }

    /**
     * Expands the suggestion to cover a larger range if necessary.
     *
     * @param command The full command string.
     * @param range The new range to cover.
     * @return A new suggestion instance.
     */
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

    /** Compares suggestions ignoring case. */
    open fun compareToIgnoreCase(other: Suggestion): Int = text.compareTo(other.text, ignoreCase = true)
}

/**
 * A standard string suggestion.
 */
data class StringSuggestion(
    override val range: StringRange,
    override val text: String,
    override val tooltip: Message? = null
) : Suggestion()

/**
 * An integer suggestion.
 * Use [value] for numerical comparisons.
 */
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
