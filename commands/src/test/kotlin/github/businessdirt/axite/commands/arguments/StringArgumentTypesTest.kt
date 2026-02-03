package github.businessdirt.axite.commands.arguments

import github.businessdirt.axite.commands.strings.StringReader
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@DisplayName("StringArgumentType logic tests")
class StringArgumentTypeTest {

    @Test
    @DisplayName("WordArgumentType should only read unquoted strings")
    fun testParseWord() {
        val reader: StringReader = mock()
        whenever(reader.readUnquotedString()).thenReturn("hello")

        assertEquals("hello", WordArgumentType().parse(reader))
        verify(reader).readUnquotedString()
    }

    @Test
    @DisplayName("StringArgumentType should delegate to readString (handles quotes)")
    fun testParseString() {
        val reader: StringReader = mock()
        whenever(reader.readString()).thenReturn("hello world")

        assertEquals("hello world", StringArgumentType().parse(reader))
        verify(reader).readString()
    }

    @Test
    @DisplayName("GreedyStringArgumentType should consume all remaining input")
    fun testParseGreedyString() {
        val reader = StringReader("Hello world! This is a test.")
        assertEquals("Hello world! This is a test.", GreedyStringArgumentType().parse(reader))
        assertFalse(reader.canRead(), "Reader should be fully exhausted")
    }

    @Test
    @DisplayName("Escape utility: no quotes needed for simple words")
    fun testEscapeIfRequired_notRequired() {
        assertEquals("hello", "hello".escapeIfRequired())
        assertEquals("", "".escapeIfRequired())
    }

    @Test
    @DisplayName("Escape utility: wrap spaces in quotes")
    fun testEscapeIfRequired_multipleWords() {
        assertEquals("\"hello world\"", "hello world".escapeIfRequired())
    }

    @Test
    @DisplayName("Escape utility: handle internal quotes")
    fun testEscapeIfRequired_quote() {
        assertEquals("\"hello \\\"world\\\"!\"", "hello \"world\"!".escapeIfRequired())
    }

    @Test
    @DisplayName("Escape utility: handle backslashes")
    fun testEscapeIfRequired_escapes() {
        assertEquals("\"\\\\\"", "\\".escapeIfRequired())
    }

    @Test
    @DisplayName("toString() representation")
    fun testToString() {
        // Based on our previous data class refactor
        assertTrue(StringArgumentType().toString().contains("StringArgumentType"))
    }
}