package github.businessdirt.axite.commands.exceptions

import kotlin.reflect.KClass

sealed class CommandError {
    abstract val message: String

    // --- Range Errors (Generic for any Number) ---
    data class TooSmall<T : Number>(val found: T, val min: T) : CommandError() {
        override val message: String
            get() = "${found::class.simpleName} must not be less than $min, found $found"
    }

    data class TooBig<T : Number>(val found: T, val max: T) : CommandError() {
        override val message: String
            get() = "${found::class.simpleName} must not be more than $max, found $found"
    }

    // --- Reader Errors ---
    data class InvalidLiteral(val expected: Any) : CommandError() {
        override val message = "Expected literal $expected"
    }

    object ExpectedStartOfQuote : CommandError() {
        override val message = "Expected quote to start a string"
    }

    object ExpectedEndOfQuote : CommandError() {
        override val message = "Unclosed quoted string"
    }

    data class InvalidEscape(val char: Char) : CommandError() {
        override val message = "Invalid escape sequence '$char' in quoted string"
    }

    data class InvalidValue(val cls: KClass<*>, val value: Any) : CommandError() {
        override val message = "Invalid ${cls.simpleName} '$value'"
    }

    data class ExpectedType(val cls: KClass<*>) : CommandError() {
        override val message = "Expected ${cls.simpleName}"
    }

    data class ExpectedSymbol(val symbol: Char) : CommandError() {
        override val message = "Expected '$symbol'"
    }

    // --- Dispatcher Errors ---
    object UnknownCommand : CommandError() {
        override val message = "Unknown command"
    }

    object UnknownArgument : CommandError() {
        override val message = "Incorrect argument for command"
    }

    object ExpectedSeparator : CommandError() {
        override val message = "Expected whitespace to end one argument, but found trailing data"
    }

    data class ParseException(val reason: String) : CommandError() {
        override val message = "Could not parse command: $reason"
    }
}