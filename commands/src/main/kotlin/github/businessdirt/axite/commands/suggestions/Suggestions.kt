package github.businessdirt.axite.commands.suggestions

import github.businessdirt.axite.commands.strings.StringRange
import java.util.concurrent.CompletableFuture

data class Suggestions(
    val range: StringRange,
    val list: List<Suggestion>
) {
    val isEmpty: Boolean
        get() = list.isEmpty()

    companion object {
        private val EMPTY = Suggestions(StringRange.at(0), emptyList())

        @JvmStatic
        fun empty(): CompletableFuture<Suggestions> =
            CompletableFuture.completedFuture(EMPTY)

        @JvmStatic
        fun merge(command: String, input: Collection<Suggestions>): Suggestions {
            return when (input.size) {
                0 -> EMPTY
                1 -> input.first()
                else -> {
                    val allSuggestions = input.flatMap { it.list }.toSet()
                    create(command, allSuggestions)
                }
            }
        }

        @JvmStatic
        fun create(command: String, suggestions: Collection<Suggestion>): Suggestions {
            if (suggestions.isEmpty()) return EMPTY

            // Find the total range boundaries across all suggestions
            val start = suggestions.minOf { it.range.start }
            val end = suggestions.maxOf { it.range.end }
            val totalRange = StringRange(start, end)

            // Expand suggestions to the new range, deduplicate via Set, and sort
            val sorted = suggestions
                .map { it.expand(command, totalRange) }
                .distinct()
                .sortedWith { a, b -> a.compareToIgnoreCase(b) }

            return Suggestions(totalRange, sorted)
        }
    }
}