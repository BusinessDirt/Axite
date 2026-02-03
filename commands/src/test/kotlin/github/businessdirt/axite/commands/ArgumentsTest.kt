package github.businessdirt.axite.commands

import github.businessdirt.axite.commands.context.CommandContextBuilder
import github.businessdirt.axite.commands.exceptions.CommandSyntaxException
import github.businessdirt.axite.commands.strings.StringReader
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@DisplayName("BooleanArgumentType logic tests")
class BooleanArgumentTypeTest {

    private val type = BooleanArgumentType()
    private val context: CommandContextBuilder<Any> = mock()

    @Test
    @DisplayName("parse() should delegate to StringReader.readBoolean()")
    fun parse() {
        val reader: StringReader = mock()
        whenever(reader.readBoolean()).thenReturn(true)

        val result = type.parse(reader)

        assertEquals(true, result)
        verify(reader).readBoolean()
    }
}

@DisplayName("DoubleArgumentType logic tests")
class DoubleArgumentTypeTest {

    private val type: DoubleArgumentType = DoubleArgumentType(-100.0, 100.0)
    private val context: CommandContextBuilder<Any> = mock()

    @Test
    @DisplayName("parse() should consume a valid double and reach end of input")
    fun parse() {
        val reader = StringReader("15")
        assertEquals(15.0, DoubleArgumentType().parse(reader))
        assertFalse(reader.canRead(), "Reader should be fully consumed")
    }

    @Test
    @DisplayName("parse() should throw exception when value is below minimum")
    fun parse_tooSmall() {
        val reader = StringReader("-5")
        val ex = assertThrows(CommandSyntaxException::class.java) {
            DoubleArgumentType(0.0, 100.0).parse(reader)
        }
        assertEquals(CommandSyntaxException.BUILT_IN_EXCEPTIONS.doubleTooLow(), ex.type)
        assertEquals(0, ex.cursor)
    }

    @Test
    @DisplayName("parse() should throw exception when value is above maximum")
    fun parse_tooBig() {
        val reader = StringReader("5")
        val ex = assertThrows(CommandSyntaxException::class.java) {
            DoubleArgumentType(-100.0, 0.0).parse(reader)
        }
        assertEquals(CommandSyntaxException.BUILT_IN_EXCEPTIONS.doubleTooHigh(), ex.type)
        assertEquals(0, ex.cursor)
    }

    @Test
    @DisplayName("Equality check for data class properties")
    fun testEquals() {
        assertAll(
            { assertEquals(DoubleArgumentType(), DoubleArgumentType()) },
            { assertEquals(DoubleArgumentType(-100.0, 100.0), DoubleArgumentType(-100.0, 100.0)) },
            { assertEquals(DoubleArgumentType(-100.0, 50.0), DoubleArgumentType(-100.0, 50.0)) },
            { assertNotEquals(DoubleArgumentType(-100.0, 100.0), DoubleArgumentType(-50.0, 100.0)) }
        )
    }

    @Test
    @DisplayName("toString() representation")
    fun testToString() {
        assertEquals("DoubleArgumentType(minimum=-1.7976931348623157E308, maximum=1.7976931348623157E308)", DoubleArgumentType().toString())
        assertEquals("DoubleArgumentType(minimum=-100.0, maximum=1.7976931348623157E308)", DoubleArgumentType(minimum = -100.0).toString())
        assertEquals("DoubleArgumentType(minimum=-100.0, maximum=100.0)", DoubleArgumentType(-100.0, 100.0).toString())
    }
}

@ExtendWith(MockitoExtension::class)
@DisplayName("FloatArgumentType logic tests")
class FloatArgumentTypeTest {

    private lateinit var type: FloatArgumentType

    @Mock
    lateinit var context: CommandContextBuilder<Any>

    @BeforeEach
    fun setUp() {
        // Range: -100f to 100f
        type = FloatArgumentType(-100f, 100f)
    }

    @Test
    @DisplayName("parse() should consume a valid float and reach end of input")
    fun parse() {
        val reader = StringReader("15")
        // Note: Kotlin uses 'f' suffix for floats
        assertEquals(15f, FloatArgumentType().parse(reader))
        assertFalse(reader.canRead(), "Reader should be fully consumed")
    }

    @Test
    @DisplayName("parse() should throw exception when value is below minimum")
    fun parse_tooSmall() {
        val reader = StringReader("-5")
        val ex = assertThrows(CommandSyntaxException::class.java) {
            FloatArgumentType(0f, 100f).parse(reader)
        }
        assertEquals(CommandSyntaxException.BUILT_IN_EXCEPTIONS.floatTooLow(), ex.type)
        assertEquals(0, ex.cursor)
    }

    @Test
    @DisplayName("parse() should throw exception when value is above maximum")
    fun parse_tooBig() {
        val reader = StringReader("5")
        val ex = assertThrows(CommandSyntaxException::class.java) {
            FloatArgumentType(-100f, 0f).parse(reader)
        }
        assertEquals(CommandSyntaxException.BUILT_IN_EXCEPTIONS.floatTooHigh(), ex.type)
        assertEquals(0, ex.cursor)
    }

    @Test
    @DisplayName("Equality check for data class properties")
    fun testEquals() {
        assertAll(
            { assertEquals(FloatArgumentType(), FloatArgumentType()) },
            { assertEquals(FloatArgumentType(-100f, 100f), FloatArgumentType(-100f, 100f)) },
            { assertEquals(FloatArgumentType(-100f, 50f), FloatArgumentType(-100f, 50f)) },
            { assertNotEquals(FloatArgumentType(-100f, 100f), FloatArgumentType(-50f, 100f)) }
        )
    }

    @Test
    @DisplayName("toString() representation")
    fun testToString() {
        // Data class default toString includes field names
        assertEquals("FloatArgumentType(minimum=-3.4028235E38, maximum=3.4028235E38)", FloatArgumentType().toString())
        assertEquals("FloatArgumentType(minimum=-100.0, maximum=3.4028235E38)", FloatArgumentType(minimum = -100f).toString())
        assertEquals("FloatArgumentType(minimum=-100.0, maximum=100.0)", FloatArgumentType(-100f, 100f).toString())
    }
}

@ExtendWith(MockitoExtension::class)
@DisplayName("IntegerArgumentType logic tests")
class IntegerArgumentTypeTest {

    private lateinit var type: IntegerArgumentType

    @Mock
    lateinit var context: CommandContextBuilder<Any>

    @BeforeEach
    fun setUp() {
        // Range: -100 to 100
        type = IntegerArgumentType(-100, 100)
    }

    @Test
    @DisplayName("parse() should consume a valid integer and reach end of input")
    fun parse() {
        val reader = StringReader("15")
        assertEquals(15, IntegerArgumentType().parse(reader))
        assertFalse(reader.canRead(), "Reader should be fully consumed")
    }

    @Test
    @DisplayName("parse() should throw exception when value is below minimum")
    fun parse_tooSmall() {
        val reader = StringReader("-5")
        val ex = assertThrows(CommandSyntaxException::class.java) {
            IntegerArgumentType(0, 100).parse(reader)
        }
        assertEquals(CommandSyntaxException.BUILT_IN_EXCEPTIONS.integerTooLow(), ex.type)
        assertEquals(0, ex.cursor)
    }

    @Test
    @DisplayName("parse() should throw exception when value is above maximum")
    fun parse_tooBig() {
        val reader = StringReader("5")
        val ex = assertThrows(CommandSyntaxException::class.java) {
            IntegerArgumentType(-100, 0).parse(reader)
        }
        assertEquals(CommandSyntaxException.BUILT_IN_EXCEPTIONS.integerTooHigh(), ex.type)
        assertEquals(0, ex.cursor)
    }

    @Test
    @DisplayName("Equality check for data class properties")
    fun testEquals() {
        assertAll(
            { assertEquals(IntegerArgumentType(), IntegerArgumentType()) },
            { assertEquals(IntegerArgumentType(-100, 100), IntegerArgumentType(-100, 100)) },
            { assertEquals(IntegerArgumentType(-100, 50), IntegerArgumentType(-100, 50)) },
            { assertNotEquals(IntegerArgumentType(-100, 100), IntegerArgumentType(-50, 100)) }
        )
    }

    @Test
    @DisplayName("toString() representation")
    fun testToString() {
        assertEquals("IntegerArgumentType(minimum=-2147483648, maximum=2147483647)", IntegerArgumentType().toString())
        assertEquals("IntegerArgumentType(minimum=-100, maximum=2147483647)", IntegerArgumentType(minimum = -100).toString())
        assertEquals("IntegerArgumentType(minimum=-100, maximum=100)", IntegerArgumentType(-100, 100).toString())
    }
}

@ExtendWith(MockitoExtension::class)
@DisplayName("LongArgumentType logic tests")
class LongArgumentTypeTest {

    private lateinit var type: LongArgumentType

    @Mock
    lateinit var context: CommandContextBuilder<Any>

    @BeforeEach
    fun setUp() {
        // Range: -100 to 1 trillion
        type = LongArgumentType(-100L, 1_000_000_000_000L)
    }

    @Test
    @DisplayName("parse() should consume a valid long and reach end of input")
    fun parse() {
        val reader = StringReader("15")
        assertEquals(15L, LongArgumentType().parse(reader))
        assertFalse(reader.canRead(), "Reader should be fully consumed")
    }

    @Test
    @DisplayName("parse() should throw exception when value is below minimum")
    fun parse_tooSmall() {
        val reader = StringReader("-5")
        val ex = assertThrows(CommandSyntaxException::class.java) {
            LongArgumentType(0L, 100L).parse(reader)
        }
        assertEquals(CommandSyntaxException.BUILT_IN_EXCEPTIONS.longTooLow(), ex.type)
        assertEquals(0, ex.cursor)
    }

    @Test
    @DisplayName("parse() should throw exception when value is above maximum")
    fun parse_tooBig() {
        val reader = StringReader("5")
        val ex = assertThrows(CommandSyntaxException::class.java) {
            LongArgumentType(-100L, 0L).parse(reader)
        }
        assertEquals(CommandSyntaxException.BUILT_IN_EXCEPTIONS.longTooHigh(), ex.type)
        assertEquals(0, ex.cursor)
    }

    @Test
    @DisplayName("Equality check for data class properties")
    fun testEquals() {
        assertAll(
            { assertEquals(LongArgumentType(), LongArgumentType()) },
            { assertEquals(LongArgumentType(-100L, 100L), LongArgumentType(-100L, 100L)) },
            { assertEquals(LongArgumentType(-100L, 50L), LongArgumentType(-100L, 50L)) },
            { assertNotEquals(LongArgumentType(-100L, 100L), LongArgumentType(-50L, 100L)) }
        )
    }

    @Test
    @DisplayName("toString() representation")
    fun testToString() {
        assertEquals("LongArgumentType(minimum=-9223372036854775808, maximum=9223372036854775807)", LongArgumentType().toString())
        assertEquals("LongArgumentType(minimum=-100, maximum=9223372036854775807)", LongArgumentType(minimum = -100L).toString())
        assertEquals("LongArgumentType(minimum=-100, maximum=100)", LongArgumentType(-100L, 100L).toString())
    }
}

@ExtendWith(MockitoExtension::class)
@DisplayName("StringArgumentType logic tests")
class StringArgumentTypeTest {

    @Mock
    lateinit var context: CommandContextBuilder<Any>

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
        assertEquals("hello", StringArgumentType.escapeIfRequired("hello"))
        assertEquals("", StringArgumentType.escapeIfRequired(""))
    }

    @Test
    @DisplayName("Escape utility: wrap spaces in quotes")
    fun testEscapeIfRequired_multipleWords() {
        assertEquals("\"hello world\"", StringArgumentType.escapeIfRequired("hello world"))
    }

    @Test
    @DisplayName("Escape utility: handle internal quotes")
    fun testEscapeIfRequired_quote() {
        assertEquals("\"hello \\\"world\\\"!\"", StringArgumentType.escapeIfRequired("hello \"world\"!"))
    }

    @Test
    @DisplayName("Escape utility: handle backslashes")
    fun testEscapeIfRequired_escapes() {
        assertEquals("\"\\\\\"", StringArgumentType.escapeIfRequired("\\"))
    }

    @Test
    @DisplayName("toString() representation")
    fun testToString() {
        // Based on our previous data class refactor
        assertTrue(StringArgumentType().toString().contains("StringArgumentType"))
    }
}