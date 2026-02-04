package github.businessdirt.axite.commands.builder

import github.businessdirt.axite.commands.Command
import github.businessdirt.axite.commands.arguments.ArgumentType
import github.businessdirt.axite.commands.arguments.IntegerArgumentType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.mockito.Mockito.mock

@DisplayName("RequiredArgumentBuilder logic tests")
class RequiredArgumentBuilderTest {

    val type: ArgumentType<Int> = mock()
    val command: Command<Any> = mock()

    @Test
    @DisplayName("build() should create a node with the correct name and type")
    fun testBuild() {
        val node = argument<Any, Int>("foo", type)

        assertAll("Node properties",
            { assertEquals("foo", node.name) },
            { assertEquals(type, node.type) }
        )
    }

    @Test
    @DisplayName("build() with an executor should preserve the command reference")
    fun testBuildWithExecutor() {
        val node = argument("foo", type) {
            executes(command)
        }

        assertAll("Node properties",
            { assertEquals("foo", node.name) },
            { assertEquals(type, node.type) },
            { assertSame(command, node.command, "Command reference must be identical") }
        )
    }

    @Test
    @DisplayName("build() with children should populate the child node map")
    fun testBuildWithChildren() {
        val node = argument<Any, Int>("foo", type) {
            argument("bar", IntegerArgumentType())
            argument("baz", IntegerArgumentType())
        }

        assertEquals(2, node.allChildren.size, "Should have exactly 2 child nodes")
    }
}