package github.businessdirt.axite.commands.strings

interface Message { val text: String }

data class LiteralMessage(override val text: String) : Message