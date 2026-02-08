package github.businessdirt.axite.commands.suggestions

import github.businessdirt.axite.commands.strings.StringRange

/**
 * A collection of [Suggestion]s for a specific range of input.
 *
 * @property range The range covered by these suggestions.
 * @property list The list of suggestions.
 */
data class Suggestions(
    val range: StringRange,
    val list: List<Suggestion>
) {
    /** Checks if the suggestion list is empty. */
    val isEmpty: Boolean
        get() = list.isEmpty()

    companion object {
        private val EMPTY = Suggestions(StringRange.at(0), emptyList())

        /** Returns an empty suggestions object. */
        @JvmStatic
        fun empty(): Suggestions = EMPTY

        /**
         * Merges multiple suggestion sets into one.
         *
         * @param command The command string context.
         * @param input The list of suggestions to merge.
         * @return A merged [Suggestions] object.
         */
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

        /**
         * Creates a [Suggestions] object from a collection of suggestions.
         * Handles expanding ranges to match the total covered area.
         *
         * @param command The command string.
         * @param suggestions The collection of individual suggestions.
         * @return The created [Suggestions] object.
         */
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
