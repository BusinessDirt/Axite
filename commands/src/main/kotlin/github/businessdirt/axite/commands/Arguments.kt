package github.businessdirt.axite.commands

import github.businessdirt.axite.commands.context.CommandContext
import github.businessdirt.axite.commands.exceptions.CommandError
import github.businessdirt.axite.commands.exceptions.CommandSyntaxException
import github.businessdirt.axite.commands.exceptions.error
import github.businessdirt.axite.commands.strings.StringReader
import github.businessdirt.axite.commands.suggestions.Suggestions
import github.businessdirt.axite.commands.suggestions.SuggestionsBuilder
import java.util.concurrent.CompletableFuture

/**
 * The core interface for command arguments.
 */
interface ArgumentType<T> {
    @Throws(CommandSyntaxException::class)
    fun parse(reader: StringReader): T

    /**
     * Optional: Override this if the argument needs the command source to parse
     * (e.g., checking if a player name exists).
     */
    @Throws(CommandSyntaxException::class)
    fun <S> parse(reader: StringReader, source: S): T = parse(reader)

    /**
     * Returns suggestions to the user as they type.
     */
    fun <S> listSuggestions(
        context: CommandContext<S>,
        builder: SuggestionsBuilder
    ): CompletableFuture<Suggestions> = Suggestions.empty()

    /**
     * Used by the dispatcher to test for command ambiguities.
     */
    val examples: Collection<String>
        get() = emptyList()
}

/**
 * Parses "true" or "false".
 */
class BooleanArgumentType : ArgumentType<Boolean> {
    override fun parse(reader: StringReader): Boolean = reader.readBoolean()

    override fun <S> listSuggestions(
        context: CommandContext<S>,
        builder: SuggestionsBuilder
    ): CompletableFuture<Suggestions> {
        if ("true".startsWith(builder.remainingLowerCase)) builder.suggest("true")
        if ("false".startsWith(builder.remainingLowerCase)) builder.suggest("false")
        return builder.buildFuture()
    }

    override val examples: Collection<String> = listOf("true", "false")

    override fun equals(other: Any?): Boolean = other is BooleanArgumentType
    override fun hashCode(): Int = BooleanArgumentType::class.hashCode()
}

/**
 * Parses numbers
 */
abstract class NumberArgumentType<T>(
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
}

/**
 * Parses integers, with optional range bounds.
 */
class IntegerArgumentType(
    minimum: Int = Int.MIN_VALUE,
    maximum: Int = Int.MAX_VALUE
) : NumberArgumentType<Int>(minimum, maximum, StringReader::readInt) {
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
) : NumberArgumentType<Long>(minimum, maximum, StringReader::readLong) {
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
) : NumberArgumentType<Float>(minimum, maximum, StringReader::readFloat) {
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
) : NumberArgumentType<Double>(minimum, maximum, StringReader::readDouble) {
    override val examples: Collection<String> = listOf("0", "1.2", ".5", "-12.34")

    override fun equals(other: Any?): Boolean = other is DoubleArgumentType
    override fun hashCode(): Int = DoubleArgumentType::class.hashCode()
}

/**
 * Parses a single word (no spaces allowed).
 */
class WordArgumentType : ArgumentType<String> {
    override fun parse(reader: StringReader): String = reader.readUnquotedString()
    override val examples: Collection<String> = listOf("word", "foo_bar", "123")

    override fun equals(other: Any?): Boolean = other is WordArgumentType
    override fun hashCode(): Int = WordArgumentType::class.hashCode()
}

/**
 * Parses a string. If it contains spaces, it must be "quoted like this".
 */
class StringArgumentType : ArgumentType<String> {
    override fun parse(reader: StringReader): String = reader.readString()
    override val examples: Collection<String> = listOf("\"quoted string\"", "word", "\"\"")

    override fun equals(other: Any?): Boolean = other is StringArgumentType
    override fun hashCode(): Int = StringArgumentType::class.hashCode()
}

/**
 * Consumes everything from the current cursor until the end of the input.
 */
class GreedyStringArgumentType : ArgumentType<String> {
    override fun parse(reader: StringReader): String {
        val text = reader.remaining()
        reader.cursor = reader.totalLength()
        return text
    }

    override val examples: Collection<String> = listOf("word", "words with spaces", "anything goes")

    override fun equals(other: Any?): Boolean = other is GreedyStringArgumentType
    override fun hashCode(): Int = GreedyStringArgumentType::class.hashCode()
}