package github.businessdirt.axite.commands.suggestions

import github.businessdirt.axite.commands.CommandDsl
import github.businessdirt.axite.commands.strings.Message
import github.businessdirt.axite.commands.strings.StringRange
import java.util.*

@CommandDsl
class SuggestionsBuilder(
    val input: String,
    val inputLowerCase: String = input.lowercase(Locale.ROOT),
    val start: Int
) {
    // Internal list to hold the suggestions
    private val result = mutableListOf<Suggestion>()

    val remaining: String = input.substring(start)
    val remainingLowerCase: String = inputLowerCase.substring(start)

    fun suggest(text: String, tooltip: Message? = null) {
        if (text != remaining) {
            result.add(StringSuggestion(StringRange.between(start, input.length), text, tooltip))
        }
    }

    fun suggest(value: Int, tooltip: Message? = null) {
        result.add(IntegerSuggestion(StringRange.between(start, input.length), value, tooltip))
    }

    fun add(other: SuggestionsBuilder) {
        result.addAll(other.result)
    }

    inline fun fork(newStart: Int, block: SuggestionsBuilder.() -> Unit) {
        val subBuilder = SuggestionsBuilder(input, inputLowerCase, newStart)
        subBuilder.block()
        this.add(subBuilder)
    }

    fun build(): Suggestions = Suggestions.create(input, result)
}

inline fun suggestions(
    input: String,
    start: Int,
    block: SuggestionsBuilder.() -> Unit
): Suggestions {
    val builder = SuggestionsBuilder(input, start = start)
    builder.block()
    return builder.build()
}