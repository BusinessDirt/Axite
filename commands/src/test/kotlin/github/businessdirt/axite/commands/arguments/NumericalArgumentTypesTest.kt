package github.businessdirt.axite.commands.arguments

import github.businessdirt.axite.commands.exceptions.CommandError
import github.businessdirt.axite.commands.exceptions.CommandSyntaxException
import github.businessdirt.axite.commands.strings.StringReader
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
sealed class NumericalArgumentTypeTest<T>(
    protected val minValue: T,
    protected val maxValue: T,
) where T : Number, T : Comparable<T> {

    protected val type: NumericalArgumentType<T> = createType(minValue, maxValue)

    // Helper to create a new instance of the type for equality tests
    abstract fun createType(min: T, max: T): NumericalArgumentType<T>
    abstract fun createExpected(x: Int): T

    private fun createExpectedType(min: Int, max: Int): NumericalArgumentType<T> =
        createType(createExpected(min), createExpected(max))

    @Test
    @DisplayName("parse() should consume a valid number and reach end of input")
    fun parse() {
        val reader = StringReader("15")
        assertEquals(createExpected(15), type.parse(reader))
        assertFalse(reader.canRead(), "Reader should be fully consumed")
    }

    @Test
    @DisplayName("parse() should throw exception when value is below minimum")
    fun parse_tooSmall() {
        val reader = StringReader("-105")
        val ex = assertThrows(CommandSyntaxException::class.java) { type.parse(reader) }
        assertEquals(CommandError.TooSmall(createExpected(-105), createExpected(-100)), ex.type)
        assertEquals(0, ex.cursor)
    }

    @Test
    @DisplayName("parse() should throw exception when value is above maximum")
    fun parse_tooBig() {
        val reader = StringReader("105")
        val ex = assertThrows(CommandSyntaxException::class.java) { type.parse(reader) }

        assertEquals(CommandError.TooBig(createExpected(105), createExpected(100)), ex.type)
        assertEquals(0, ex.cursor)
    }

    @Test
    @DisplayName("Equality check for data class properties")
    fun testEquals() {
        assertAll(
            { assertEquals(createExpectedType(-100, 100), createExpectedType(-100, 100)) },
            { assertEquals(createExpectedType(-100, 50), createExpectedType(-100, 50)) },
            { assertNotEquals(createExpectedType(-100, 100), createExpectedType(-50, 100)) }
        )
    }

    @Test
    fun testToString() {
        val typeString = type.toString()
        assertTrue(typeString.contains("minimum=$minValue"))
        assertTrue(typeString.contains("maximum=$maxValue"))
    }
}

@DisplayName("Integer Argument Type Parsing Test")
class IntegerArgumentTypeTest : NumericalArgumentTypeTest<Int>(-100, 100) {
    override fun createType(min: Int, max: Int) = IntegerArgumentType(min, max)
    override fun createExpected(x: Int): Int = x
}

@DisplayName("Long Argument Type Parsing Test")
class LongArgumentTypeTest : NumericalArgumentTypeTest<Long>(-100L, 100L) {
    override fun createType(min: Long, max: Long) = LongArgumentType(min, max)
    override fun createExpected(x: Int): Long = x.toLong()
}

@DisplayName("Float Argument Type Parsing Test")
class FloatArgumentTypeTest : NumericalArgumentTypeTest<Float>(-100.0f, 100.0f) {
    override fun createType(min: Float, max: Float) = FloatArgumentType(min, max)
    override fun createExpected(x: Int): Float = x.toFloat()
}

@DisplayName("Double Argument Type Parsing Test")
class DoubleArgumentTypeTest : NumericalArgumentTypeTest<Double>(-100.0, 100.0) {
    override fun createType(min: Double, max: Double) = DoubleArgumentType(min, max)
    override fun createExpected(x: Int): Double = x.toDouble()
}