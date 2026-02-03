package github.businessdirt.axite.commands.suggestions

import github.businessdirt.axite.commands.strings.Message
import github.businessdirt.axite.commands.strings.StringRange
import java.util.*
import java.util.concurrent.CompletableFuture

class SuggestionsBuilder(
    val input: String,
    val inputLowerCase: String = input.lowercase(Locale.ROOT),
    val start: Int
) {
    private val result = mutableListOf<Suggestion>()

    val remaining: String = input.substring(start)
    val remainingLowerCase: String = inputLowerCase.substring(start)

    /**
     * Core suggestion logic with optional tooltip.
     * Supports both String and Int (via IntegerSuggestion).
     */
    fun suggest(text: String, tooltip: Message? = null): SuggestionsBuilder {
        if (text != remaining) {
            result.add(StringSuggestion(StringRange.between(start, input.length), text, tooltip))
        }
        return this
    }

    fun suggest(value: Int, tooltip: Message? = null): SuggestionsBuilder {
        result.add(IntegerSuggestion(StringRange.between(start, input.length), value, tooltip))
        return this
    }

    /**
     * DSL Operator: Allows adding another builder using the `+` sign
     */
    operator fun plusAssign(other: SuggestionsBuilder) {
        result.addAll(other.result)
    }

    fun add(other: SuggestionsBuilder): SuggestionsBuilder = apply {
        result.addAll(other.result)
    }

    fun build(): Suggestions = Suggestions.create(input, result)

    fun buildFuture(): CompletableFuture<Suggestions> =
        CompletableFuture.completedFuture(build())

    fun createOffset(start: Int): SuggestionsBuilder =
        SuggestionsBuilder(input, inputLowerCase, start)

    fun restart(): SuggestionsBuilder = createOffset(start)
}

/**
 * DSL entry point for building suggestions.
 * Usage:
 * val suggestions = buildSuggestions(input, start) {
 * suggest("foo")
 * suggest(42, LiteralMessage("The answer"))
 * }
 */
inline fun buildSuggestions(
    input: String,
    start: Int,
    builderAction: SuggestionsBuilder.() -> Unit
): Suggestions =
    SuggestionsBuilder(input, input.lowercase(Locale.ROOT), start)
        .apply(builderAction)
        .build()