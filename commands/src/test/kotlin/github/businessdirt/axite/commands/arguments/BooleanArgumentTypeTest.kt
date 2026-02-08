package github.businessdirt.axite.commands.arguments

import github.businessdirt.axite.commands.context.CommandContext
import github.businessdirt.axite.commands.exceptions.CommandSyntaxException
import github.businessdirt.axite.commands.strings.StringReader
import github.businessdirt.axite.commands.suggestions.SuggestionsBuilder
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.mockito.kotlin.mock
import java.util.stream.Stream

@DisplayName("BooleanArgumentType logic tests")
class BooleanArgumentTypeTest {

    @TestFactory
    @DisplayName("parse() should consume valid booleans")
    fun testParse(): Stream<DynamicTest> {
        data class TestCase(val input: String, val assertion: (Boolean) -> Unit)
        return Stream.of(
            TestCase("true", ::assertTrue),
            TestCase("false", ::assertFalse)
        ).map { tc ->
            dynamicTest("parsing ${tc.input} should return ${tc.input}") {
                val reader = StringReader(tc.input)
                tc.assertion.invoke(BooleanArgumentType.parse(reader))
                assertFalse(reader.canRead())
            }
        }
    }

    @Test
    @DisplayName("parse() should throw exception on invalid input")
    fun testParse_invalid() {
        val reader = StringReader("notABool")
        assertThrows(CommandSyntaxException::class.java) { BooleanArgumentType.parse(reader) }
    }

    @Test
    @DisplayName("listSuggestions() should suggest both options on empty input")
    fun testSuggestions_empty() = runTest {
        val builder = SuggestionsBuilder("executable ", "executable ", 11)
        val suggestions = BooleanArgumentType.listSuggestions(mock<CommandContext<Any>>(), builder).list.map { it.text }

        assertTrue(suggestions.containsAll(listOf("true", "false")))
    }

    @Test
    @DisplayName("listSuggestions() should filter based on remaining input")
    fun testSuggestions_filtered() = runTest {
        val builder = SuggestionsBuilder("executable t", "executable t", 11)

        val suggestions = BooleanArgumentType.listSuggestions(mock<CommandContext<Any>>(), builder).list.map { it.text }

        assertTrue(suggestions.contains("true"))
        assertFalse(suggestions.contains("false"))
    }

    @Test
    @DisplayName("Equality check")
    fun testEquals() {
        assertEquals(BooleanArgumentType, BooleanArgumentType)
    }

    @Test
    @DisplayName("Hash Code check")
    fun testHashCode() {
        assertEquals(BooleanArgumentType.hashCode(), BooleanArgumentType.hashCode())
    }
}