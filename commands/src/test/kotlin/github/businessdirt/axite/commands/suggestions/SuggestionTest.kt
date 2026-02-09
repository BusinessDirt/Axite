package github.businessdirt.axite.commands.suggestions

import github.businessdirt.axite.commands.strings.StringRange
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory
import java.util.stream.Stream

@DisplayName("Suggestion Transformation Tests")
sealed class SuggestionTest<T : Suggestion> {

    abstract val applyCases: List<ApplyCase<T>>
    abstract val expandCases: List<ExpandCase<T>>

    data class ApplyCase<T>(val name: String, val suggestion: T, val input: String, val expected: String)
    data class ExpandCase<T>(val name: String, val suggestion: T, val command: String, val range: StringRange, val expected: Suggestion)

    @TestFactory
    @DisplayName("Applying Suggestions")
    fun applySuggestions(): Stream<DynamicTest> = applyCases.stream().map { tc ->
        dynamicTest("Apply: ${tc.name}") {
            assertEquals(tc.expected, tc.suggestion.apply(tc.input))
        }
    }

    @TestFactory
    @DisplayName("Expanding Suggestions")
    fun expandSuggestions(): Stream<DynamicTest> = expandCases.stream().map { tc ->
        dynamicTest("Expand: ${tc.name}") {
            assertEquals(tc.expected, tc.suggestion.expand(tc.command, tc.range))
        }
    }
}

@DisplayName("String Suggestion Transformation Tests")
class StringSuggestionTest : SuggestionTest<StringSuggestion>() {

    override val applyCases: List<ApplyCase<StringSuggestion>> = listOf(
        ApplyCase("Prepend text", StringSuggestion(StringRange.at(0), "Note: "), "The end.", "Note: The end."),
        ApplyCase("Insert in middle", StringSuggestion(StringRange.at(6), "very "), "Hello world!", "Hello very world!"),
        ApplyCase("Append to end", StringSuggestion(StringRange.at(5), " suffix"), "Alpha", "Alpha suffix"),
        ApplyCase("Replace beginning", StringSuggestion(StringRange.between(0, 3), "New"), "Old value", "New value"),
        ApplyCase("Replace middle word", StringSuggestion(StringRange.between(4, 8), "best"), "The good parts", "The best parts"),
        ApplyCase("Replace end", StringSuggestion(StringRange.between(10, 16), "fixed"), "Status is broken", "Status is fixed"),
        ApplyCase("Overwrite entire string", StringSuggestion(StringRange.between(0, 4), "Reset"), "Data", "Reset")
    )

    override val expandCases: List<ExpandCase<StringSuggestion>> = listOf(
        ExpandCase("No range change", StringSuggestion(StringRange.at(1), "bb"), "a", StringRange.at(1), StringSuggestion(StringRange.at(1), "bb")),
        ExpandCase("Grow to the left", StringSuggestion(StringRange.at(1), "bb"), "a", StringRange.between(0, 1), StringSuggestion(StringRange.between(0, 1), "abb")),
        ExpandCase("Grow to the right", StringSuggestion(StringRange.at(0), "prefix:"), "item", StringRange.between(0, 4), StringSuggestion(StringRange.between(0, 4), "prefix:item")),
        ExpandCase("Expand both directions", StringSuggestion(StringRange.at(5), "mid"), "start end", StringRange.between(0, 9), StringSuggestion(StringRange.between(0, 9), "startmid end")),
        ExpandCase("Widening a replacement", StringSuggestion(StringRange.between(2, 4), "34"), "1256", StringRange.between(0, 4), StringSuggestion(StringRange.between(0, 4), "1234"))
    )
}

@DisplayName("Integer Suggestion Transformation Tests")
class IntegerSuggestionTest : SuggestionTest<IntegerSuggestion>() {

    override val applyCases: List<ApplyCase<IntegerSuggestion>> = listOf(
        ApplyCase("Insert integer in middle", IntegerSuggestion(StringRange.at(7), 42), "Level: ", "Level: 42"),
        ApplyCase("Replace string with integer", IntegerSuggestion(StringRange.between(0, 4), 100), "Zero percent", "100 percent"),
        ApplyCase("Replace integer with larger integer", IntegerSuggestion(StringRange.between(0, 1), 1000), "0", "1000"),
        ApplyCase("Append integer to end", IntegerSuggestion(StringRange.at(5), 7), "Error", "Error7")
    )

    override val expandCases: List<ExpandCase<IntegerSuggestion>> = listOf(
        ExpandCase("No range change (Int)", IntegerSuggestion(StringRange.at(1), 5), "0", StringRange.at(1), IntegerSuggestion(StringRange.at(1), 5)),
        ExpandCase("Grow to the left (Int becomes String)", IntegerSuggestion(StringRange.at(1), 2), "1", StringRange.between(0, 1), StringSuggestion(StringRange.between(0, 1), "12")),
        ExpandCase("Grow to the right (Int becomes String)", IntegerSuggestion(StringRange.at(0), 10), " units", StringRange.between(0, 6), StringSuggestion(StringRange.between(0, 6), "10 units"))
    )
}