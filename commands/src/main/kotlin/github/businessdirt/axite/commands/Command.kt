package github.businessdirt.axite.commands

interface Command<S> {
    fun run(command: CommandContext<S>): Int
}