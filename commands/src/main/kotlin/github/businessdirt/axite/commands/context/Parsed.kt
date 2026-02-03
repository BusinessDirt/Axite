package github.businessdirt.axite.commands.context

import github.businessdirt.axite.commands.strings.StringRange

data class ParsedArgument<S, T>(
    val range: StringRange,
    val result: T
) {
    // Secondary constructor to maintain your existing API (start, end)
    constructor(start: Int, end: Int, result: T) : this(StringRange.between(start, end), result)
}