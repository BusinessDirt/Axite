package github.businessdirt.axite.commands

import github.businessdirt.axite.commands.strings.StringReader

/**
 * Represents a type of command argument.
 *
 * @param T the type of the argument
 */
interface ArgumentType<T> {

    /**
     * Parses a string into an argument of type `T`.
     *
     * @param reader the string to parse
     * @return the parsed argument
     */
    fun parse(reader: StringReader): T
}

/**
 * Represents a boolean argument type. This argument type will consume a single word and parse it as a boolean.
 */
class BooleanArgumentType : ArgumentType<Boolean> {

    /**
     * Parses a string into a boolean.
     *
     * @param reader the string reader to parse
     * @return the parsed boolean
     */
    override fun parse(reader: StringReader): Boolean = reader.readString().toBoolean()
}

/**
 * Represents an integer argument type. This argument type will consume a single word and parse it as an integer.
 */
class IntegerArgumentType : ArgumentType<Int> {

    /**
     * Parses a string into an integer.
     *
     * @param reader the string reader to parse
     * @return the parsed integer
     * @throws NumberFormatException if the string is not a valid integer
     */
    @Throws(NumberFormatException::class)
    override fun parse(reader: StringReader): Int = reader.readString().toInt()
}

/**
 * Represents a string argument type. This argument type will consume a single word and return it as a string.
 */
class StringArgumentType : ArgumentType<String> {

    /**
     * Parses a string from the reader.
     *
     * @param reader the string reader to parse
     * @return the parsed string
     */
    override fun parse(reader: StringReader): String = reader.readString()
}

/**
 * Represents a greedy string argument type. This argument type will consume the rest of the command string.
 */
class GreedyStringArgumentType : ArgumentType<String> {

    /**
     * Parses the remaining of the reader and returns it as a string.
     *
     * @param reader the string reader to parse
     * @return the parsed string
     */
    override fun parse(reader: StringReader): String = reader.remaining()
}