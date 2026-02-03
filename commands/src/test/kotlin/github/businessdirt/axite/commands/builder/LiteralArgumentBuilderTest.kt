package github.businessdirt.axite.commands.builder

import github.businessdirt.axite.commands.Command
import github.businessdirt.axite.commands.IntegerArgumentType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.mockito.kotlin.mock

@DisplayName("LiteralArgumentBuilder logic tests")
class LiteralArgumentBuilderTest {
    private lateinit var builder: LiteralArgumentBuilder<Any>

    val command: Command<Any> = mock()

    @BeforeEach
    fun setUp() {
        builder = LiteralArgumentBuilder.literal("foo")
    }

    @Test
    @DisplayName("build() should create a node with the correct literal name")
    fun testBuild() {
        val node = builder.build()
        assertEquals("foo", node.literal)
    }

    @Test
    @DisplayName("build() with an executor should preserve the command reference")
    fun testBuildWithExecutor() {
        val node = builder.executes(command).build()

        assertAll("Node properties",
            { assertEquals("foo", node.literal) },
            { assertSame(command, node.command, "Command reference must be identical") }
        )
    }

    @Test
    @DisplayName("build() with children should populate the child node map")
    fun testBuildWithChildren() {
        // Using our previously defined DSL or static factory helpers
        builder.then(RequiredArgumentBuilder.argument("bar", IntegerArgumentType()))
        builder.then(RequiredArgumentBuilder.argument("baz", IntegerArgumentType()))

        val node = builder.build()

        assertEquals(2, node.allChildren.size, "Should have exactly 2 child nodes")
    }
}