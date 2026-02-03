package github.businessdirt.axite.commands.suggestions

import github.businessdirt.axite.commands.strings.StringRange
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory
import java.util.stream.Stream
import kotlin.test.Test

@DisplayName("Suggestions Merging Logic")
class SuggestionsTest {

    @Test
    @DisplayName("Merging an empty list of suggestions results in an empty Suggestions object")
    fun merge_empty() {
        val merged = Suggestions.merge("foo b", emptyList())
        assertTrue(merged.isEmpty, "Merged suggestions should be empty when input is empty")
    }

    @Test
    @DisplayName("Merging a single Suggestions object returns an equivalent object")
    fun merge_single() {
        val range = StringRange.at(5)
        val suggestions = Suggestions(range, listOf(Suggestion(range, "ar")))
        val merged = Suggestions.merge("foo b", listOf(suggestions))

        assertEquals(suggestions, merged, "Merging a single source should return it unchanged")
    }

    @TestFactory
    @DisplayName("Complex Merging and Normalization")
    fun merge_complex(): Stream<DynamicTest> {
        return Stream.of(
            Triple(
                "Multiple sources with range expansion and sorting",
                // Command: "foo b"
                listOf(
                    Suggestions(
                        StringRange.at(5), // Position after the 'b'
                        listOf(
                            Suggestion(StringRange.at(5), "ar"), // Result: "bar"
                            Suggestion(StringRange.at(5), "az"), // Result: "baz"
                            Suggestion(StringRange.at(5), "Az")  // Result: "bAz"
                        )
                    ),
                    Suggestions(
                        StringRange.between(4, 5), // Position covering 'b'
                        listOf(
                            Suggestion(StringRange.between(4, 5), "foo"),
                            Suggestion(StringRange.between(4, 5), "qux"),
                            Suggestion(StringRange.between(4, 5), "apple"),
                            Suggestion(StringRange.between(4, 5), "Bar")
                        )
                    )
                ),
                listOf(
                    // Expectations are normalized to the widest range (4, 5)
                    // and sorted alphabetically (case-insensitive)
                    Suggestion(StringRange.between(4, 5), "apple"),
                    Suggestion(StringRange.between(4, 5), "bar"), // "ar" expanded to "bar"
                    Suggestion(StringRange.between(4, 5), "Bar"),
                    Suggestion(StringRange.between(4, 5), "baz"), // "az" expanded to "baz"
                    Suggestion(StringRange.between(4, 5), "bAz"), // "Az" expanded to "bAz"
                    Suggestion(StringRange.between(4, 5), "foo"),
                    Suggestion(StringRange.between(4, 5), "qux")
                )
            )
        ).map { (name, input, expectedList) ->
            dynamicTest(name) {
                val command = "foo b"
                val merged = Suggestions.merge(command, input)

                assertEquals(expectedList, merged.list) {
                    "Merged list mismatch for '$name'. Check normalization and sorting order."
                }
            }
        }
    }
}