package github.businessdirt.axite.commands.context

import github.businessdirt.axite.commands.strings.StringReader
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.assertAll
import kotlin.test.Test

@DisplayName("Parsed Argument Tests")
class ParsedArgumentTest {
    @Test
    @DisplayName("Equality and HashCode should be based on range and result")
    fun testEquals() {
        val arg1 = ParsedArgument<Any, String>(0, 3, "bar")
        val arg2 = ParsedArgument<Any, String>(0, 3, "bar")
        val differentRange = ParsedArgument<Any, String>(3, 6, "bar")
        val differentResult = ParsedArgument<Any, String>(0, 3, "baz")

        assertAll(
            // Check equality
            { assertEquals(arg1, arg2, "Arguments with same properties should be equal") },
            { assertEquals(arg1.hashCode(), arg2.hashCode(), "HashCodes should match") },

            // Check inequality
            { assertNotEquals(arg1, differentRange, "Different range should not be equal") },
            { assertNotEquals(arg1, differentResult, "Different result should not be equal") }
        )
    }

    @Test
    @DisplayName("getRange() should correctly extract substring from a StringReader")
    fun getRaw() {
        val reader = StringReader("0123456789")

        // ParsedArgument range is [start, end)
        val argument = ParsedArgument<Any, String>(2, 5, "some-result")

        val extracted = argument.range.get(reader)

        assertEquals("234", extracted, "The range [2, 5) should extract characters at indices 2, 3, and 4")
    }
}