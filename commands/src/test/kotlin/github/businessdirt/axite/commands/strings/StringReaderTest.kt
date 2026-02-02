package github.businessdirt.axite.commands.strings

import github.businessdirt.axite.commands.exceptions.CommandError
import github.businessdirt.axite.commands.exceptions.CommandSyntaxException
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.assertThrows
import kotlin.reflect.KClass
import kotlin.test.Test

@DisplayName("String Reader Tests")
class StringReaderTest {

    @Test
    @DisplayName("Should return true if there is text remaining to read")
    fun canRead() {
        val reader = StringReader("abc")
        assertTrue { reader.canRead() }
        reader.skip() // 'a'
        assertTrue { reader.canRead() }
        reader.skip() // 'b'
        assertTrue { reader.canRead() }
        reader.skip() // 'c'
        assertFalse { reader.canRead() }
    }

    @TestFactory
    @DisplayName("Should return the correct remaining length")
    fun remainingLength() = listOf(
        0 to 3,
        1 to 2,
        2 to 1,
        3 to 0
    ).map { (cursor, expected) ->
        dynamicTest("cursor: $cursor, expected: $expected") {
            val reader = StringReader("abc")
            reader.cursor = cursor
            assertEquals(expected, reader.remainingLength())
        }
    }

    @TestFactory
    @DisplayName("Should return true if there are at least 'length' characters remaining")
    fun `canRead with length`() = listOf(
        1 to true,
        2 to true,
        3 to true,
        4 to false,
        5 to false
    ).map { (length, expected) ->
        dynamicTest("length: $length, expected: $expected") {
            val reader = StringReader("abc")
            assertEquals(expected, reader.canRead(length))
        }
    }

    @TestFactory
    @DisplayName("Should peek the next character without advancing the cursor")
    fun peek() = listOf(
        0 to 'a',
        2 to 'c'
    ).map { (cursor, expected) ->
        dynamicTest("cursor: $cursor, expected: $expected") {
            val reader = StringReader("abc")
            reader.cursor = cursor
            assertEquals(expected, reader.peek())
            assertEquals(cursor, reader.cursor)
        }
    }

    @TestFactory
    @DisplayName("Should peek the character at the specified offset without advancing the cursor")
    fun `peek with length`() = listOf(
        Triple(0, 0, 'a'),
        Triple(0, 2, 'c'),
        Triple(1, 1, 'c')
    ).map { (cursor, offset, expected) ->
        dynamicTest("cursor: $cursor, offset: $offset, expected: $expected") {
            val reader = StringReader("abc")
            reader.cursor = cursor
            assertEquals(expected, reader.peek(offset))
            assertEquals(cursor, reader.cursor)
        }
    }

    @Test
    @DisplayName("Should read the next character and advance the cursor")
    fun read() {
        val reader = StringReader("abc")
        assertEquals('a', reader.read())
        assertEquals('b', reader.read())
        assertEquals('c', reader.read())
        assertEquals(3, reader.cursor)
    }

    @Test
    @DisplayName("Should skip the next character")
    fun skip() {
        val reader = StringReader("abc")
        reader.skip()
        assertEquals(1, reader.cursor)
    }

    @TestFactory
    @DisplayName("Should return the remaining string from the current cursor")
    fun remaining() = listOf(
        0 to "Hello!",
        3 to "lo!",
        6 to ""
    ).map { (cursor, expected) ->
        dynamicTest("cursor: $cursor, expected: $expected") {
            val reader = StringReader("Hello!")
            reader.cursor = cursor
            assertEquals(expected, reader.remaining())
        }
    }

    @TestFactory
    @DisplayName("Should return the consumed string up to the current cursor")
    fun consumed() = listOf(
        0 to "",
        3 to "Hel",
        6 to "Hello!"
    ).map { (cursor, expected) ->
        dynamicTest("cursor: $cursor, expected: $expected") {
            val reader = StringReader("Hello!")
            reader.cursor = cursor
            assertEquals(expected, reader.consumed())
        }
    }

    @TestFactory
    @DisplayName("Should skip whitespace characters")
    fun skipWhitespace() = listOf(
        "Hello!" to 0,
        " \t \t\nHello!" to 5,
        "" to 0
    ).map { (input, expected) ->
        dynamicTest("input: \"$input\", expected: $expected") {
            val reader = StringReader(input)
            reader.skipWhitespace()
            assertEquals(expected, reader.cursor)
        }
    }

    @TestFactory
    @DisplayName("Should read an unquoted string")
    fun readUnquotedString() = listOf(
        Triple("hello world", "hello", " world"),
        Triple("", "", ""),
        Triple(" hello world", "", " hello world")
    ).map { (input, expected, remaining) ->
        dynamicTest("input: \"$input\", expected: \"$expected\"") {
            val reader = StringReader(input)
            assertEquals(expected, reader.readUnquotedString())
            assertEquals(expected, reader.consumed())
            assertEquals(remaining, reader.remaining())
        }
    }

    @TestFactory
    @DisplayName("Should read a quoted string")
    fun readQuotedString() = listOf(
        ReadStringCase("\"hello world\"", "hello world", "\"hello world\"", ""),
        ReadStringCase("'hello world'", "hello world", "'hello world'", ""),
        ReadStringCase("'hello \"world\"'", "hello \"world\"", "'hello \"world\"'", ""),
        ReadStringCase("\"hello 'world'\"", "hello 'world'", "\"hello 'world'\"", ""),
        ReadStringCase("", "", "", ""),
        ReadStringCase("\"\"", "", "\"\"", ""),
        ReadStringCase("\"\" hello world", "", "\"\"", " hello world"),
        ReadStringCase("\"hello \\\"world\\\"\"", "hello \"world\"", "\"hello \\\"world\\\"\"", ""),
        ReadStringCase("\"\\\\o/\"", "\\o/", "\"\\\\o/\"", ""),
        ReadStringCase("\"hello world\" foo bar", "hello world", "\"hello world\"", " foo bar"),
        ReadStringCase("\"hello world\"foo bar", "hello world", "\"hello world\"", "foo bar")
    ).map { (input, expected, consumed, remaining) ->
        dynamicTest("input: \"$input\", expected: \"$expected\"") {
            val reader = StringReader(input)
            assertEquals(expected, reader.readQuotedString())
            assertEquals(consumed, reader.consumed())
            assertEquals(remaining, reader.remaining())
        }
    }

    @TestFactory
    @DisplayName("Should throw exceptions for invalid quoted strings")
    fun `readQuotedString exceptions`() = listOf(
        Triple("hello world\"", CommandError.ExpectedStartOfQuote, 0),
        Triple("\"hello world", CommandError.ExpectedEndOfQuote, 12),
        Triple("\"hello\\\nworld\"", CommandError.InvalidEscape('\n'), 7),
        Triple("'hello\\\"'world", CommandError.InvalidEscape('\"'), 7)
    ).map { (input, error, cursor) ->
        dynamicTest("input: \"$input\", error: $error") {
            val reader = StringReader(input)
            val ex = assertThrows<CommandSyntaxException> { reader.readQuotedString() }
            assertEquals(error, ex.type)
            assertEquals(cursor, ex.cursor)
        }
    }

    @TestFactory
    @DisplayName("Should read a string (quoted or unquoted)")
    fun readString() = listOf(
        ReadStringCase("hello world", "hello", "hello", " world"),
        ReadStringCase("'hello world'", "hello world", "'hello world'", ""),
        ReadStringCase("\"hello world\"", "hello world", "\"hello world\"", "")
    ).map { (input, expected, consumed, remaining) ->
        dynamicTest("input: \"$input\", expected: \"$expected\"") {
            val reader = StringReader(input)
            assertEquals(expected, reader.readString())
            assertEquals(consumed, reader.consumed())
            assertEquals(remaining, reader.remaining())
        }
    }

    private data class ReadStringCase(
        val input: String,
        val expected: String,
        val consumed: String,
        val remaining: String
    )

    @TestFactory
    @DisplayName("Should parse different number types correctly")
    fun generateNumberTests() = listOf(
        NumberTestCase(Int::class, "123", "-123", "12.3", 123, -123),
        NumberTestCase(Long::class, "123", "-123", "12.3", 123L, -123L),
        NumberTestCase(Float::class, "12.34", "-12.34", "12.34.56", 12.34f, -12.34f),
        NumberTestCase(Double::class, "12.34", "-12.34", "12.34.56", 12.34, -12.34)
    ).flatMap { config ->
        val typeName = config.type.simpleName

        listOf(
            dynamicTest("$typeName: read valid") {
                val reader = StringReader(config.validInput)
                assertEquals(config.expectedValid, reader.readType(config.type))
                assertEquals(config.validInput, reader.consumed())
            },
            dynamicTest("$typeName: read negative") {
                val reader = StringReader(config.negativeInput)
                assertEquals(config.expectedNegative, reader.readType(config.type))
                assertEquals(config.negativeInput, reader.consumed())
            },
            dynamicTest("$typeName: invalid format resets cursor") {
                val reader = StringReader(config.invalidInput)
                val ex = assertThrows<CommandSyntaxException> { reader.readType(config.type) }
                assertEquals(CommandError.InvalidValue(config.type, config.invalidInput), ex.type)
                assertEquals(0, ex.cursor)
            },
            dynamicTest("$typeName: empty input throws ExpectedType") {
                val reader = StringReader("")
                val ex = assertThrows<CommandSyntaxException> { reader.readType(config.type) }
                assertEquals(CommandError.ExpectedType(config.type), ex.type)
            },
            dynamicTest("$typeName: read with remaining") {
                val reader = StringReader("${config.validInput} foo")
                assertEquals(config.expectedValid, reader.readType(config.type))
                assertEquals(" foo", reader.remaining())
            }
        )
    }

    private fun StringReader.readType(clazz: KClass<out Number>): Number = when (clazz) {
        Int::class -> readInt()
        Long::class -> readLong()
        Float::class -> readFloat()
        Double::class -> readDouble()
        else -> throw IllegalArgumentException()
    }

    data class NumberTestCase(
        val type: KClass<out Number>,
        val validInput: String,
        val negativeInput: String,
        val invalidInput: String,
        val expectedValid: Number,
        val expectedNegative: Number
    )

    @TestFactory
    @DisplayName("Should expect and consume a specific character")
    fun expect() = listOf(
        Triple("abc", 'a', true),
        Triple("bcd", 'a', false),
        Triple("", 'a', false)
    ).map { (input, char, success) ->
        dynamicTest("input: \"$input\", char: '$char', success: $success") {
            val reader = StringReader(input)
            if (success) {
                reader.expect(char)
                assertEquals(1, reader.cursor)
            } else {
                val ex = assertThrows<CommandSyntaxException> { reader.expect(char) }
                assertEquals(CommandError.ExpectedSymbol(char), ex.type)
                assertEquals(0, ex.cursor)
            }
        }
    }

    @TestFactory
    @DisplayName("Should read a boolean value")
    fun readBoolean() = listOf(
        Triple("true", true, null),
        Triple("tuesday", false, CommandError.InvalidValue(Boolean::class, "tuesday")),
        Triple("", false, CommandError.ExpectedType(Boolean::class))
    ).map { (input, expected, error) ->
        dynamicTest("input: \"$input\", expected: $expected") {
            val reader = StringReader(input)
            if (error == null) {
                assertEquals(expected, reader.readBoolean())
                assertEquals(input, reader.consumed())
            } else {
                val ex = assertThrows<CommandSyntaxException> { reader.readBoolean() }
                assertEquals(error, ex.type)
                assertEquals(0, ex.cursor)
            }
        }
    }
}