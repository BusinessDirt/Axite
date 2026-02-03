package github.businessdirt.axite.commands.builder

import github.businessdirt.axite.commands.ArgumentType
import github.businessdirt.axite.commands.Command
import github.businessdirt.axite.commands.IntegerArgumentType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.mockito.Mockito.mock

@DisplayName("RequiredArgumentBuilder logic tests")
class RequiredArgumentBuilderTest {

    val type: ArgumentType<Int> = mock()
    val command: Command<Any> = mock()

    private lateinit var builder: RequiredArgumentBuilder<Any, Int>

    @BeforeEach
    fun setUp() {
        // Using static factory method from the companion object
        builder = RequiredArgumentBuilder.argument("foo", type)
    }

    @Test
    @DisplayName("build() should create a node with the correct name and type")
    fun testBuild() {
        val node = builder.build()

        assertAll("Node properties",
            { assertEquals("foo", node.name) },
            { assertEquals(type, node.type) }
        )
    }

    @Test
    @DisplayName("build() with an executor should preserve the command reference")
    fun testBuildWithExecutor() {
        val node = builder.executes(command).build()

        assertAll("Node properties",
            { assertEquals("foo", node.name) },
            { assertEquals(type, node.type) },
            { assertSame(command, node.command, "Command reference must be identical") }
        )
    }

    @Test
    @DisplayName("build() with children should populate the child node map")
    fun testBuildWithChildren() {
        // We can use concrete types for children here
        builder.then(RequiredArgumentBuilder.argument("bar", IntegerArgumentType()))
        builder.then(RequiredArgumentBuilder.argument("baz", IntegerArgumentType()))

        val node = builder.build()

        assertEquals(2, node.allChildren.size, "Should have exactly 2 child nodes")
    }
}