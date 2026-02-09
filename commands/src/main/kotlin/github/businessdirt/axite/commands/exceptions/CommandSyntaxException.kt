package github.businessdirt.axite.commands.exceptions

import github.businessdirt.axite.commands.strings.ImmutableStringReader

/**
 * Exception thrown when a syntax error occurs during command parsing.
 *
 * @property type The type of error.
 * @property input The input string where the error occurred (optional).
 * @property cursor The position in the input string where the error occurred (optional).
 */
class CommandSyntaxException(
    val type: CommandError,
    val input: String? = null,
    val cursor: Int = -1,
) : RuntimeException(type.message)

/** Create an exception at the current cursor */
fun ImmutableStringReader.error(error: CommandError, at: Int = this.cursor): CommandSyntaxException =
    CommandSyntaxException(error, this.string, at)

/** Check a condition and throw if it fails */
@Throws(CommandSyntaxException::class)
fun ImmutableStringReader.expect(
    error: CommandError,
    at: Int = this.cursor,
    conditionBlock: ImmutableStringReader.() -> Boolean
) {
    if (!conditionBlock(this)) throw this.error(error, at)
}
