package github.businessdirt.axite.commands.strings

/**
 * Represents a displayable message.
 * Abstracted to allow for different message formats (e.g., plain text, JSON components).
 */
interface Message {
    /** The plain text representation of the message. */
    val text: String
}

/**
 * A simple literal string message.
 *
 * @property text The message text.
 */
data class LiteralMessage(override val text: String) : Message
