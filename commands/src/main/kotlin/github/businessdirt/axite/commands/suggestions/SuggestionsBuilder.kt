package github.businessdirt.axite.commands.suggestions

import github.businessdirt.axite.commands.CommandDsl
import github.businessdirt.axite.commands.strings.Message
import github.businessdirt.axite.commands.strings.StringRange
import java.util.*

/**
 * Builder for creating suggestions.
 *
 * @property input The full input string.
 * @property inputLowerCase The full input string in lower case.
 * @property start The start index for suggestions.
 */
@CommandDsl
class SuggestionsBuilder(
    val input: String,
    val inputLowerCase: String = input.lowercase(Locale.ROOT),
    val start: Int
) {
    // Internal list to hold the suggestions
    private val result = mutableListOf<Suggestion>()

    /** The remaining input string starting from [start]. */
    val remaining: String = input.substring(start)
    /** The remaining input string in lower case. */
    val remainingLowerCase: String = inputLowerCase.substring(start)

    /**
     * Adds a string suggestion.
     *
     * @param text The text to suggest.
     * @param tooltip Optional tooltip.
     */
    fun suggest(text: String, tooltip: Message? = null) {
        if (text != remaining) {
            result.add(StringSuggestion(StringRange.between(start, input.length), text, tooltip))
        }
    }

    /**
     * Adds an integer suggestion.
     *
     * @param value The value to suggest.
     * @param tooltip Optional tooltip.
     */
    fun suggest(value: Int, tooltip: Message? = null) {
        result.add(IntegerSuggestion(StringRange.between(start, input.length), value, tooltip))
    }

    /**
     * Adds suggestions from another builder.
     *
     * @param other The other builder.
     */
    fun add(other: SuggestionsBuilder) {
        result.addAll(other.result)
    }

    /**
     * Forks the builder to a new start position.
     *
     * @param newStart The new start position.
     * @param block logic to populate the sub-builder.
     */
    inline fun fork(newStart: Int, block: SuggestionsBuilder.() -> Unit) {
        val subBuilder = SuggestionsBuilder(input, inputLowerCase, newStart)
        subBuilder.block()
        this.add(subBuilder)
    }

    /** Builds the [Suggestions] object. */
    fun build(): Suggestions = Suggestions.create(input, result)
}

/**
 * Helper function to create suggestions using a block.
 */
inline fun suggestions(
    input: String,
    start: Int,
    block: SuggestionsBuilder.() -> Unit
): Suggestions {
    val builder = SuggestionsBuilder(input, start = start)
    builder.block()
    return builder.build()
}
