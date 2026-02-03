package github.businessdirt.axite.commands.exceptions

import github.businessdirt.axite.commands.strings.ImmutableStringReader

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
fun ImmutableStringReader.expect(error: CommandError, at: Int = this.cursor, conditionBlock: () -> Boolean) {
    if (!conditionBlock()) throw this.error(error, at)
}

/**
 * Executes [block]. If any exception occurs, it throws a CommandException
 * using the provided [error].
 */
@Throws(CommandSyntaxException::class)
inline fun <T> ImmutableStringReader.tryOrError(error: CommandError, at: Int = this.cursor, block: () -> T): T {
    return try {
        block()
    } catch (_: Exception) {
        throw CommandSyntaxException(error, this.string, at)
    }
}