package github.businessdirt.axite.commands

import github.businessdirt.axite.commands.builder.literal
import github.businessdirt.axite.commands.nodes.CommandNode
import github.businessdirt.axite.commands.strings.StringReader
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.mockito.Mockito.mock
import kotlin.test.Test
import kotlin.test.assertEquals

@DisplayName("Command Dispatcher Usages Tests")
class CommandDispatcherUsagesTest {

    private lateinit var subject: CommandDispatcher<Any>
    private val source: Any = mock()
    private val command: Command<Any> = mock()

    @BeforeEach
    fun setUp() {
        subject = CommandDispatcher()

        subject.register(literal("a") {
            literal("1") {
                literal("i") { executes(command) }
                literal("ii") { executes(command) }
            }
            literal("2") {
                literal("i") { executes(command) }
                literal("ii") { executes(command) }
            }
        })

        subject.register(literal("b") {
            literal("1") { executes(command) }
        })

        subject.register(literal("c") { executes(command) })

        subject.register(literal("d") {
            requires { false }
            executes(command)
        })

        subject.register(literal("e") {
            executes(command)
            literal("1") {
                executes(command)
                literal("i") { executes(command) }
                literal("ii") { executes(command) }
            }
        })

        subject.register(literal("f") {
            literal("1") {
                literal("i") { executes(command) }
                literal("ii") {
                    executes(command)
                    requires { false }
                }
            }
            literal("2") {
                literal("i") {
                    executes(command)
                    requires { false }
                }
                literal("ii") { executes(command) }
            }
        })

        subject.register(literal("g") {
            executes(command)
            literal("1") {
                literal("i") { executes(command) }
            }
        })

        subject.register(literal("h") {
            executes(command)
            literal("1") {
                literal("i") { executes(command) }
            }
            literal("2") {
                literal("i") {
                    literal("ii") { executes(command) }
                }
            }
            literal("3") { executes(command) }
        })

        subject.register(literal("i") {
            executes(command)
            literal("1") { executes(command) }
            literal("2") { executes(command) }
        })

        subject.register(literal("j") { redirect(subject.root) })
        subject.register(literal("k") { redirect(getNode("h")) })
    }

    private fun getNode(command: String): CommandNode<Any> {
        return subject.parse(command, source).context.nodes.last().node
    }

    private fun getNode(reader: StringReader): CommandNode<Any> {
        return subject.parse(reader, source).context.nodes.last().node
    }

    @Nested
    @DisplayName("All Usage (Flat Strings)")
    inner class AllUsage {

        @Test
        @DisplayName("Should return empty array when no commands registered")
        fun testNoCommands() {
            subject = CommandDispatcher()
            val results = subject.getAllUsage(subject.root, source, true)
            assertTrue(results.isEmpty())
        }

        @Test
        @DisplayName("Should generate full usage paths for the root")
        fun testRoot() {
            val results = subject.getAllUsage(subject.root, source, true)
            val expected = arrayOf(
                "a 1 i", "a 1 ii", "a 2 i", "a 2 ii", "b 1", "c",
                "e", "e 1", "e 1 i", "e 1 ii", "f 1 i", "f 2 ii",
                "g", "g 1 i", "h", "h 1 i", "h 2 i ii", "h 3",
                "i", "i 1", "i 2", "j ...", "k -> h"
            )

            assertArrayEquals(expected, results)
        }
    }

    @Nested
    @DisplayName("Smart Usage (Compressed/Visual)")
    inner class SmartUsage {

        @Test
        @DisplayName("Should generate compressed usage maps for root")
        fun testRoot() {
            val results = subject.getSmartUsage(subject.root, source)
            val expected = mapOf(
                getNode("a") to "a (1|2)",
                getNode("b") to "b 1",
                getNode("c") to "c",
                getNode("e") to "e [1]",
                getNode("f") to "f (1|2)",
                getNode("g") to "g [1]",
                getNode("h") to "h [1|2|3]",
                getNode("i") to "i [1|2]",
                getNode("j") to "j ...",
                getNode("k") to "k -> h"
            )
            assertEquals(expected, results)
        }

        @Test
        @DisplayName("Should generate usage for a specific sub-node (h)")
        fun testSpecificNode() {
            val results = subject.getSmartUsage(getNode("h"), source)
            val expected = mapOf(
                getNode("h 1") to "[1] i",
                getNode("h 2") to "[2] i ii",
                getNode("h 3") to "[3]"
            )
            assertEquals(expected, results)
        }

        @Test
        @DisplayName("Should respect StringReader cursor offsets")
        fun testOffset() {
            val reader = StringReader("/|/|/h").apply { cursor = 5 }
            val results = subject.getSmartUsage(getNode(reader), source)

            val expected = mapOf(
                getNode("h 1") to "[1] i",
                getNode("h 2") to "[2] i ii",
                getNode("h 3") to "[3]"
            )
            assertEquals(expected, results)
        }
    }
}