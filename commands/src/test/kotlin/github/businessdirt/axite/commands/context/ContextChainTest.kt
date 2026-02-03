package github.businessdirt.axite.commands.context

import github.businessdirt.axite.commands.Command
import github.businessdirt.axite.commands.CommandDispatcher
import github.businessdirt.axite.commands.ResultConsumer
import github.businessdirt.axite.commands.builder.LiteralArgumentBuilder.Companion.literal
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*

@DisplayName("Context Chain Execution Tests")
class ContextChainTest {

    @Nested
    @DisplayName("Execution Flow")
    inner class ExecutionFlow {

        @Test
        @DisplayName("executeAll() should trigger command and consumer for single command")
        fun testExecuteAllForSingleCommand() {
            val consumer = mock<ResultConsumer<Any>>()
            val command = mock<Command<Any>>()
            whenever(command.run(any())).thenReturn(4)

            val dispatcher = CommandDispatcher<Any>()
            dispatcher.register(literal<Any>("foo").executes(command))

            val result = dispatcher.parse("foo", "compile_source")
            val chain = ContextChain.tryFlatten(result.context.build("foo")).orElseThrow()

            val runtimeSource = "runtime_source"
            assertEquals(4, chain.executeAll(runtimeSource, consumer))

            // Modern JUnit 5 style verification with Mockito-Kotlin
            verify(command).run(argThat { context -> context.source == runtimeSource })
            verify(consumer).onCommandComplete(
                argThat { context -> context.source == runtimeSource },
                eq(true),
                eq(4)
            )
            verifyNoMoreInteractions(consumer)
        }

        @Test
        @DisplayName("executeAll() should respect redirected sources in the chain")
        fun testExecuteAllForRedirectedCommand() {
            val consumer = mock<ResultConsumer<Any>>()
            val command = mock<Command<Any>>()
            whenever(command.run(any())).thenReturn(4)

            val redirectedSource = "redirected_source"
            val dispatcher = CommandDispatcher<Any>()
            dispatcher.register(literal<Any>("foo").executes(command))
            dispatcher.register(literal<Any>("bar").redirect(dispatcher.root) { redirectedSource })

            val result = dispatcher.parse("bar foo", "compile_source")
            val chain = ContextChain.tryFlatten(result.context.build("bar foo")).orElseThrow()

            assertEquals(4, chain.executeAll("runtime_source", consumer))

            // Verify the command used the REDIRECTED source, not the original/runtime source
            verify(command).run(argThat { context -> context.source == redirectedSource })
            verify(consumer).onCommandComplete(argThat { context -> context.source == redirectedSource }, eq(true), eq(4))
        }
    }

    @Nested
    @DisplayName("Stage Transition Logic")
    inner class StageLogic {



        @Test
        @DisplayName("Should correctly identify stages in a multi-redirect command")
        fun testMultiStageExecution() {
            val dispatcher = CommandDispatcher<Any>()
            dispatcher.register(literal<Any>("foo").executes { 1 })
            dispatcher.register(literal<Any>("bar").redirect(dispatcher.root))

            val result = dispatcher.parse("bar bar foo", Any())
            val topContext = result.context.build("bar bar foo")

            val stage0 = ContextChain.tryFlatten(topContext).orElseThrow()

            // assertAll ensures all checks run even if one fails
            assertAll("Chain Stages",
                { assertEquals(ContextChain.Stage.MODIFY, stage0.stage) },
                {
                    val stage1 = stage0.nextStage()
                    assertNotNull(stage1)
                    assertEquals(ContextChain.Stage.MODIFY, stage1?.stage)
                },
                {
                    val stage2 = stage0.nextStage()?.nextStage()
                    assertNotNull(stage2)
                    assertEquals(ContextChain.Stage.EXECUTE, stage2?.stage)
                    assertNull(stage2?.nextStage())
                }
            )
        }

        @Test
        @DisplayName("tryFlatten() should return empty if command is not executable")
        fun testMissingExecute() {
            val dispatcher = CommandDispatcher<Any>()
            dispatcher.register(literal<Any>("foo").executes { 1 })
            dispatcher.register(literal<Any>("bar").redirect(dispatcher.root))

            val result = dispatcher.parse("bar bar", Any())
            val topContext = result.context.build("bar bar")

            val chainOptional = ContextChain.tryFlatten(topContext)
            assertTrue(chainOptional.isEmpty, "Chain should be empty for non-executable parse result")
        }
    }
}