package github.businessdirt.axite.commands.exceptions

import kotlin.reflect.KClass

/**
 * Represents a specific type of error encountered during command parsing or execution.
 */
sealed class CommandError {
    abstract val message: String

    // --- Range Errors (Generic for any Number) ---
    /** Error indicating a number was smaller than the minimum allowed. */
    data class TooSmall<T : Number>(val found: T, val min: T) : CommandError() {
        override val message: String
            get() = "${found::class.simpleName} must not be less than $min, found $found"
    }

    /** Error indicating a number was larger than the maximum allowed. */
    data class TooBig<T : Number>(val found: T, val max: T) : CommandError() {
        override val message: String
            get() = "${found::class.simpleName} must not be more than $max, found $found"
    }

    // --- Reader Errors ---
    /** Error indicating a literal was expected but not found. */
    data class InvalidLiteral(val expected: Any) : CommandError() {
        override val message = "Expected literal $expected"
    }

    /** Error indicating a quote was expected to start a string. */
    object ExpectedStartOfQuote : CommandError() {
        override val message = "Expected quote to start a string"
    }

    /** Error indicating a quoted string was not closed. */
    object ExpectedEndOfQuote : CommandError() {
        override val message = "Unclosed quoted string"
    }

    /** Error indicating an invalid escape sequence in a quoted string. */
    data class InvalidEscape(val char: Char) : CommandError() {
        override val message = "Invalid escape sequence '$char' in quoted string"
    }

    /** Error indicating a value was invalid for the expected type. */
    data class InvalidValue(val cls: KClass<*>, val value: Any) : CommandError() {
        override val message = "Invalid ${cls.simpleName} '$value'"
    }

    /** Error indicating a specific type was expected. */
    data class ExpectedType(val cls: KClass<*>) : CommandError() {
        override val message = "Expected ${cls.simpleName}"
    }

    /** Error indicating a specific symbol (char) was expected. */
    data class ExpectedSymbol(val symbol: Char) : CommandError() {
        override val message = "Expected '$symbol'"
    }

    // --- Dispatcher Errors ---
    /** Error indicating the command is unknown. */
    object UnknownCommand : CommandError() {
        override val message = "Unknown command"
    }

    /** Error indicating an argument was incorrect or malformed. */
    object UnknownArgument : CommandError() {
        override val message = "Incorrect argument for command"
    }

    /** Error indicating expected whitespace separator was missing. */
    object ExpectedSeparator : CommandError() {
        override val message = "Expected whitespace to end one argument, but found trailing data"
    }

    /** Generic parse exception. */
    data class ParseException(val reason: String) : CommandError() {
        override val message = "Could not parse command: $reason"
    }
}
