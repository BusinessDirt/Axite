package github.businessdirt.axite.commands.arguments

import github.businessdirt.axite.commands.exceptions.CommandError
import github.businessdirt.axite.commands.exceptions.error
import github.businessdirt.axite.commands.strings.StringReader


/**
 * Parses numbers
 */
abstract class NumericalArgumentType<T>(
    val minimum: T,
    val maximum: T,
    val read: (StringReader) -> T
) : ArgumentType<T> where T : Number, T : Comparable<T> {

    override fun parse(reader: StringReader): T {
        val start = reader.cursor
        val result: T = read.invoke(reader)
        if (result < minimum) {
            reader.cursor = start
            throw reader.error(CommandError.TooSmall(result, minimum))
        }
        if (result > maximum) {
            reader.cursor = start
            throw reader.error(CommandError.TooBig(result, maximum))
        }
        return result
    }

    override fun toString(): String = "${this::class.simpleName}(minimum=$minimum, maximum=$maximum)"
}

/**
 * Parses integers, with optional range bounds.
 */
class IntegerArgumentType(
    minimum: Int = Int.MIN_VALUE,
    maximum: Int = Int.MAX_VALUE
) : NumericalArgumentType<Int>(minimum, maximum, StringReader::readInt) {
    override val examples: Collection<String> = listOf("0", "123", "-123")

    override fun equals(other: Any?): Boolean = other is IntegerArgumentType
    override fun hashCode(): Int = IntegerArgumentType::class.hashCode()
}

/**
 * Parses longs, with optional range bounds.
 */
class LongArgumentType(
    minimum: Long = Long.MIN_VALUE,
    maximum: Long = Long.MAX_VALUE
) : NumericalArgumentType<Long>(minimum, maximum, StringReader::readLong) {
    override val examples: Collection<String> = listOf("0", "123", "-123")

    override fun equals(other: Any?): Boolean = other is LongArgumentType
    override fun hashCode(): Int = LongArgumentType::class.hashCode()
}

/**
 * Parses floats, with optional range bounds.
 */
class FloatArgumentType(
    minimum: Float = -Float.MAX_VALUE,
    maximum: Float = Float.MAX_VALUE
) : NumericalArgumentType<Float>(minimum, maximum, StringReader::readFloat) {
    override val examples: Collection<String> = listOf("0", "1.2", ".5", "-12.34")

    override fun equals(other: Any?): Boolean = other is FloatArgumentType
    override fun hashCode(): Int = FloatArgumentType::class.hashCode()
}

/**
 * Parses doubles, with optional range bounds.
 */
class DoubleArgumentType(
    minimum: Double = -Double.MAX_VALUE,
    maximum: Double = Double.MAX_VALUE
) : NumericalArgumentType<Double>(minimum, maximum, StringReader::readDouble) {
    override val examples: Collection<String> = listOf("0", "1.2", ".5", "-12.34")

    override fun equals(other: Any?): Boolean = other is DoubleArgumentType
    override fun hashCode(): Int = DoubleArgumentType::class.hashCode()
}