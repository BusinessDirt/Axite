package github.businessdirt.axite.commands.suggestions

import github.businessdirt.axite.commands.strings.StringRange
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory
import java.util.stream.Stream

@DisplayName("Suggestion Transformation Tests")
class SuggestionTest {

    @TestFactory
    @DisplayName("Applying Suggestions to Strings")
    fun applySuggestions(): Stream<DynamicTest> {
        data class ApplyTestCase(
            val name: String,
            val suggestion: Suggestion,
            val input: String,
            val expected: String
        )

        return Stream.of(
            ApplyTestCase("Prepend text", Suggestion(StringRange.at(0), "Note: "), "The end.", "Note: The end."),
            ApplyTestCase("Insert in middle", Suggestion(StringRange.at(6), "very "), "Hello world!", "Hello very world!"),
            ApplyTestCase("Append to end", Suggestion(StringRange.at(5), " suffix"), "Alpha", "Alpha suffix"),
            ApplyTestCase("Replace beginning", Suggestion(StringRange.between(0, 3), "New"), "Old value", "New value"),
            ApplyTestCase("Replace middle word", Suggestion(StringRange.between(4, 8), "best"), "The good parts", "The best parts"),
            ApplyTestCase("Replace end", Suggestion(StringRange.between(10, 16), "fixed"), "Status is broken", "Status is fixed"),
            ApplyTestCase("Overwrite entire string", Suggestion(StringRange.between(0, 4), "Reset"), "Data", "Reset")
        ).map { tc ->
            dynamicTest("Test Case: ${tc.name}") {
                val actual = tc.suggestion.apply(tc.input)
                assertEquals(tc.expected, actual, "Failed applying '${tc.name}' - expected result mismatch")
            }
        }
    }

    @TestFactory
    @DisplayName("Expanding Suggestion Ranges")
    fun expandSuggestions(): Stream<DynamicTest> {
        data class ExpandTestCase(
            val name: String,
            val suggestion: Suggestion,
            val command: String,
            val range: StringRange,
            val expected: Suggestion
        )

        return Stream.of(
            ExpandTestCase("No range change", Suggestion(StringRange.at(1), "bb"), "a", StringRange.at(1), Suggestion(StringRange.at(1), "bb")),
            ExpandTestCase("Grow to the left", Suggestion(StringRange.at(1), "bb"), "a", StringRange.between(0, 1), Suggestion(StringRange.between(0, 1), "abb")),
            ExpandTestCase("Grow to the right", Suggestion(StringRange.at(0), "prefix:"), "item", StringRange.between(0, 4), Suggestion(StringRange.between(0, 4), "prefix:item")),
            ExpandTestCase("Expand both directions", Suggestion(StringRange.at(5), "mid"), "start end", StringRange.between(0, 9), Suggestion(StringRange.between(0, 9), "startmid end")),
            ExpandTestCase("Widening a replacement", Suggestion(StringRange.between(2, 4), "34"), "1256", StringRange.between(0, 4), Suggestion(StringRange.between(0, 4), "1234"))
        ).map { tc ->
            dynamicTest("Test Case: ${tc.name}") {
                val actual = tc.suggestion.expand(tc.command, tc.range)
                assertEquals(tc.expected, actual, "Failed expanding '${tc.name}' - resulting Suggestion range or text is incorrect")
            }
        }
    }
}