package github.businessdirt.axite.commands.arguments

import github.businessdirt.axite.commands.exceptions.CommandError
import github.businessdirt.axite.commands.exceptions.error
import github.businessdirt.axite.commands.strings.StringReader

/**
 * Base class for numerical argument types.
 *
 * @param T The number type.
 * @property minimum The minimum allowed value.
 * @property maximum The maximum allowed value.
 * @property read Function to read the number from the [StringReader].
 * @property examples Examples of valid input.
 */
sealed class NumericalArgumentType<T>(
    open val minimum: T,
    open val maximum: T,
    val read: (StringReader) -> T,
    override val examples: Collection<String>
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
}

/**
 * An argument type that parses integers.
 *
 * @property minimum The minimum inclusive value.
 * @property maximum The maximum inclusive value.
 */
data class IntegerArgumentType(
    override val minimum: Int = Int.MIN_VALUE,
    override val maximum: Int = Int.MAX_VALUE
) : NumericalArgumentType<Int>(minimum, maximum, StringReader::readInt, listOf("0", "123", "-123"))

/**
 * An argument type that parses longs.
 *
 * @property minimum The minimum inclusive value.
 * @property maximum The maximum inclusive value.
 */
data class LongArgumentType(
    override val minimum: Long = Long.MIN_VALUE,
    override val maximum: Long = Long.MAX_VALUE
) : NumericalArgumentType<Long>(minimum, maximum, StringReader::readLong, listOf("0", "123", "-123"))

/**
 * An argument type that parses floats.
 *
 * @property minimum The minimum inclusive value.
 * @property maximum The maximum inclusive value.
 */
data class FloatArgumentType(
    override val minimum: Float = -Float.MAX_VALUE,
    override val maximum: Float = Float.MAX_VALUE
) : NumericalArgumentType<Float>(minimum, maximum, StringReader::readFloat, listOf("0", "1.2", ".5", "-12.34"))

/**
 * An argument type that parses doubles.
 *
 * @property minimum The minimum inclusive value.
 * @property maximum The maximum inclusive value.
 */
data class DoubleArgumentType(
    override val minimum: Double = -Double.MAX_VALUE,
    override val maximum: Double = Double.MAX_VALUE
) : NumericalArgumentType<Double>(minimum, maximum, StringReader::readDouble, listOf("0", "1.2", ".5", "-12.34"))
