package github.businessdirt.axite.commands.builder

import github.businessdirt.axite.commands.Command
import github.businessdirt.axite.commands.arguments.IntegerArgumentType
import github.businessdirt.axite.commands.builder.argument
import github.businessdirt.axite.commands.nodes.LiteralCommandNode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.mockito.kotlin.mock

@DisplayName("LiteralArgumentBuilder logic tests")
class LiteralArgumentBuilderTest {
    val command: Command<Any> = mock()

    @Test
    @DisplayName("build() should create a node with the correct literal name")
    fun testBuild() {
        val node = literal<Any>("foo")
        assertEquals("foo", node.literal)
    }

    @Test
    @DisplayName("build() with an executor should preserve the command reference")
    fun testBuildWithExecutor() {
        val node = literal("foo") {
            executes(this@LiteralArgumentBuilderTest.command)
        }

        assertAll("Node properties",
            { assertEquals("foo", node.literal) },
            { assertSame(command, node.command, "Command reference must be identical") }
        )
    }

    @Test
    @DisplayName("build() with children should populate the child node map")
    fun testBuildWithChildren() {
        val node = literal<Any>("foo") {
            argument("bar", IntegerArgumentType())
            argument("baz", IntegerArgumentType())
        }

        assertEquals(2, node.allChildren.size, "Should have exactly 2 child nodes")
    }
}