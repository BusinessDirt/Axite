package github.businessdirt.axite.commands.suggestions

import github.businessdirt.axite.commands.strings.StringRange
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory
import java.util.stream.Stream
import kotlin.test.Test

@DisplayName("Suggestions Builder Logic")
class SuggestionsBuilderTest {

    inline fun buildTestSuggestions(
        builderAction: SuggestionsBuilder.() -> Unit
    ): Suggestions = buildSuggestions("Hello w", 6) { builderAction() }

    @Test
    @DisplayName("suggest() should append new text to the defined range")
    fun suggest_appends() {
        val result = buildTestSuggestions { suggest("world!") }
        val expectedRange = StringRange.between(6, 7)

        assertEquals(listOf(StringSuggestion(expectedRange, "world!")), result.list, "Should contain the 'world!' suggestion")
        assertEquals(expectedRange, result.range, "Resulting range should match the builder's range")
        assertEquals(false, result.isEmpty, "Result should not be empty")
    }

    @Test
    @DisplayName("suggest() with same-length text should replace the range")
    fun suggest_replaces() {
        val result = buildTestSuggestions { suggest("everybody") }
        val expectedRange = StringRange.between(6, 7)

        assertEquals(listOf(StringSuggestion(expectedRange, "everybody")), result.list, "Should replace with 'everybody'")
    }

    @Test
    @DisplayName("suggest() should do nothing if text matches current input")
    fun suggest_noop() {
        val result = buildTestSuggestions { suggest("w") }
        assertEquals(true, result.isEmpty, "Should be empty because 'w' is already at the current range")
    }

    @Test
    @DisplayName("restart() should create a fresh builder with same configuration")
    fun restart() {
        val builder = SuggestionsBuilder("Hello w", "hello w", 6)
        builder.suggest("won't be included")
        val other = builder.restart()

        assertNotSame(builder, other, "Restarted builder should be a new instance")
        assertEquals(builder.input, other.input, "Input strings should match")
        assertEquals(builder.start, other.start, "Start indices should match")
        assertEquals(builder.remaining, other.remaining, "Remaining strings should match")
        assertEquals(true, other.build().isEmpty, "New builder should have no suggestions")
    }

    @TestFactory
    @DisplayName("Sorting Logic: Alphabetical vs Numerical")
    fun sortingTests(): Stream<DynamicTest> {
        return Stream.of(
            // Alphabetical: "30" comes before "4" because "3" < "4"
            SortingCase(
                "Alphabetical sort (Strings)",
                listOf("2", "4", "6", "8", "30", "32"),
                listOf("2", "30", "32", "4", "6", "8")
            ),
            // Numerical: 4 comes before 30
            SortingCase(
                "Numerical sort (Integers)",
                listOf(2, 4, 6, 8, 30, 32),
                listOf("2", "4", "6", "8", "30", "32")
            ),
            // Mixed: Brigadier sorts numbers then letters, but still largely alphabetical
            SortingCase(
                "Mixed sort (Complex)",
                listOf("11", "22", "33", "a", "b", "c", 2, 4, 6, 8, 30, 32, "3a", "a3"),
                listOf("11", "2", "22", "33", "3a", "4", "6", "8", "30", "32", "a", "a3", "b", "c")
            )
        ).map { tc ->
            dynamicTest(tc.name) {
                val actual = buildTestSuggestions {
                    tc.inputs.forEach { input ->
                        when (input) {
                            is Int -> suggest(input)
                            is String -> suggest(input)
                        }
                    }
                }.list.map { it.text }

                assertEquals(tc.expected, actual, "Sort order mismatch for ${tc.name}")
            }
        }
    }

    private data class SortingCase(val name: String, val inputs: List<Any>, val expected: List<String>)
}