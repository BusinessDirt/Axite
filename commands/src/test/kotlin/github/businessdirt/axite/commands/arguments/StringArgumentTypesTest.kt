package github.businessdirt.axite.commands.arguments

import github.businessdirt.axite.commands.strings.StringReader
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("String Argument Type Tests")
sealed class StringArgumentTypeTest(
    protected val type: ArgumentType<String>
) {

    @Test
    fun testEquals() {
        assertEquals(type, type)
        assertNotEquals(type, object : ArgumentType<String> {
            override fun parse(reader: StringReader) = ""
            override val examples = listOf<String>()
        })
    }

    @Test
    fun testToString() {
        val name = type::class.simpleName ?: "StringArgumentType"
        assertTrue(type.toString().contains(name))
    }

    class WordTest : StringArgumentTypeTest(StringArgumentType.Word) {
        @Test
        @DisplayName("parse() should consume a single word")
        fun testParse() {
            val reader = StringReader("hello world")
            assertEquals("hello", type.parse(reader))
            assertEquals(5, reader.cursor) // Stopped at the space
        }
    }

    class QuotableTest : StringArgumentTypeTest(StringArgumentType.Quotable) {
        @Test
        @DisplayName("parse() should consume a quoted string")
        fun testParse() {
            val reader = StringReader("\"quoted string\" remaining")
            assertEquals("quoted string", type.parse(reader))
            assertEquals(15, reader.cursor)
        }
    }

    class GreedyTest : StringArgumentTypeTest(StringArgumentType.Greedy) {
        @Test
        @DisplayName("parse() should consume the entire remaining input")
        fun testParse() {
            val input = "Hello world! This is a test."
            val reader = StringReader(input)
            assertEquals(input, type.parse(reader))
            assertFalse(reader.canRead(), "Reader should be fully exhausted")
        }
    }
}

@DisplayName("String Utility Tests")
class StringEscapeTest {

    @Test
    @DisplayName("Should not escape simple words")
    fun testNotRequired() {
        assertEquals("hello", "hello".escapeIfRequired())
        assertEquals("", "".escapeIfRequired())
    }

    @Test
    @DisplayName("Should wrap spaces in quotes")
    fun testMultipleWords() {
        assertEquals("\"hello world\"", "hello world".escapeIfRequired())
    }

    @Test
    @DisplayName("Should handle internal quotes and backslashes")
    fun testComplexEscaping() {
        assertAll(
            { assertEquals("\"hello \\\"world\\\"!\"", "hello \"world\"!".escapeIfRequired()) },
            { assertEquals("\"\\\\\"", "\\".escapeIfRequired()) }
        )
    }
}