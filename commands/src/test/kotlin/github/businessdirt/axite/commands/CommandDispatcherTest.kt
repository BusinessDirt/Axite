package github.businessdirt.axite.commands

import github.businessdirt.axite.commands.arguments.IntegerArgumentType
import github.businessdirt.axite.commands.builder.LiteralArgumentBuilder.Companion.literal
import github.businessdirt.axite.commands.builder.RequiredArgumentBuilder.Companion.argument
import github.businessdirt.axite.commands.context.CommandContext
import github.businessdirt.axite.commands.exceptions.CommandError
import github.businessdirt.axite.commands.exceptions.CommandSyntaxException
import github.businessdirt.axite.commands.nodes.LiteralCommandNode
import github.businessdirt.axite.commands.strings.StringReader
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class CommandDispatcherTest {
    private lateinit var subject: CommandDispatcher<Any?>
    private val command: Command<Any?> = mock()
    private val source: Any = mock()
    private val consumer: ResultConsumer<Any?> = mock()

    @BeforeEach
    fun setUp() {
        subject = CommandDispatcher()
        whenever(command.run(any())).thenReturn(42)
    }

    private fun inputWithOffset(input: String = "/foo", offset: Int = 1): StringReader {
        val result = StringReader(input)
        result.cursor = offset
        return result
    }

    private fun integer() = IntegerArgumentType()

    @Test
    fun testCreateAndExecuteCommand() {
        subject.register(literal<Any?>("foo").executes(command))

        assertEquals(42, subject.execute("foo", source))
        verify(command).run(any())
    }

    @Test
    fun testCreateAndExecuteOffsetCommand() {
        subject.register(literal<Any?>("foo").executes(command))

        assertEquals(42, subject.execute(inputWithOffset(), source))
        verify(command).run(any())
    }

    @Test
    fun testCreateAndMergeCommands() {
        subject.register(literal<Any?>("base").then(literal<Any?>("foo").executes(command)))
        subject.register(literal<Any?>("base").then(literal<Any?>("bar").executes(command)))

        assertEquals(42, subject.execute("base foo", source))
        assertEquals(42, subject.execute("base bar", source))
        verify(command, times(2)).run(any())
    }

    @Test
    fun testExecuteUnknownCommand() {
        subject.register(literal("bar"))
        subject.register(literal("baz"))

        val ex = assertFailsWith<CommandSyntaxException> {
            subject.execute("foo", source)
        }
        assertEquals(CommandError.UnknownCommand, ex.type)
        assertEquals(0, ex.cursor)
    }

    @Test
    fun testExecuteImpermissibleCommand() {
        subject.register(literal<Any?>("foo").requires { false })

        val ex = assertFailsWith<CommandSyntaxException> {
            subject.execute("foo", source)
        }
        assertEquals(CommandError.UnknownCommand, ex.type)
        assertEquals(0, ex.cursor)
    }

    @Test
    fun testExecuteEmptyCommand() {
        subject.register(literal(""))

        val ex = assertFailsWith<CommandSyntaxException> {
            subject.execute("", source)
        }
        assertEquals(CommandError.UnknownCommand, ex.type)
        assertEquals(0, ex.cursor)
    }

    @Test
    fun testExecuteUnknownSubcommand() {
        subject.register(literal<Any?>("foo").executes(command))

        val ex = assertFailsWith<CommandSyntaxException> {
            subject.execute("foo bar", source)
        }
        assertEquals(CommandError.UnknownArgument, ex.type)
        assertEquals(4, ex.cursor)
    }

    @Test
    fun testExecuteIncorrectLiteral() {
        subject.register(literal<Any?>("foo").executes(command).then(literal("bar")))

        val ex = assertFailsWith<CommandSyntaxException> {
            subject.execute("foo baz", source)
        }
        assertEquals(CommandError.UnknownArgument, ex.type)
        assertEquals(4, ex.cursor)
    }

    @Test
    fun testExecuteAmbiguousIncorrectArgument() {
        subject.register(
            literal<Any?>("foo").executes(command)
                .then(literal("bar"))
                .then(literal("baz"))
        )

        val ex = assertFailsWith<CommandSyntaxException> {
            subject.execute("foo unknown", source)
        }
        assertEquals(CommandError.UnknownArgument, ex.type)
        assertEquals(4, ex.cursor)
    }

    @Test
    fun testExecuteSubcommand() {
        val subCommand: Command<Any?> = mock()
        whenever(subCommand.run(any())).thenReturn(100)

        subject.register(
            literal<Any?>("foo").then(
                literal("a")
            ).then(
                literal<Any?>("=").executes(subCommand)
            ).then(
                literal("c")
            ).executes(command)
        )

        assertEquals(100, subject.execute("foo =", source))
        verify(subCommand).run(any())
    }

    @Test
    fun testParseIncompleteLiteral() {
        subject.register(literal<Any?>("foo").then(literal<Any?>("bar").executes(command)))

        val parse = subject.parse("foo ", source)
        assertEquals(" ", parse.reader.remaining())
        assertEquals(1, parse.context.nodes.size)
    }

    @Test
    fun testParseIncompleteArgument() {
        subject.register(literal<Any?>("foo").then(argument<Any?, Int>("bar", integer()).executes(command)))

        val parse = subject.parse("foo ", source)
        assertEquals(" ", parse.reader.remaining())
        assertEquals(1, parse.context.nodes.size)
    }

    @Test
    fun testExecuteAmbiguousParentSubcommand() {
        val subCommand: Command<Any?> = mock()
        whenever(subCommand.run(any())).thenReturn(100)

        subject.register(
            literal<Any?>("test")
                .then(
                    argument<Any?, Int>("incorrect", integer())
                        .executes(command)
                )
                .then(
                    argument<Any?, Int>("right", integer())
                        .then(
                            argument<Any?, Int>("sub", integer())
                                .executes(subCommand)
                        )
                )
        )

        assertEquals(100, subject.execute("test 1 2", source))
        verify(subCommand).run(any())
        verify(command, never()).run(any())
    }

    @Test
    fun testExecuteAmbiguousParentSubcommandViaRedirect() {
        val subCommand: Command<Any?> = mock()
        whenever(subCommand.run(any())).thenReturn(100)

        val real = subject.register(
            literal<Any?>("test")
                .then(
                    argument<Any?, Int>("incorrect", integer())
                        .executes(command)
                )
                .then(
                    argument<Any?, Int>("right", integer())
                        .then(
                            argument<Any?, Int>("sub", integer())
                                .executes(subCommand)
                        )
                )
        )

        subject.register(literal<Any?>("redirect").redirect(real))

        assertEquals(100, subject.execute("redirect 1 2", source))
        verify(subCommand).run(any())
        verify(command, never()).run(any())
    }

    @Test
    fun testExecuteRedirectedMultipleTimes() {
        val concreteNode = subject.register(literal<Any?>("actual").executes(command))
        val redirectNode = subject.register(literal<Any?>("redirected").redirect(subject.root))

        val input = "redirected redirected actual"

        val parse = subject.parse(input, source)
        assertEquals("redirected", parse.context.range.get(input))
        assertEquals(1, parse.context.nodes.size)
        assertEquals(subject.root, parse.context.rootNode)
        assertEquals(parse.context.range, parse.context.nodes[0].range)
        assertEquals(redirectNode, parse.context.nodes[0].node)

        val child1 = parse.context.child
        assertNotNull(child1)
        assertEquals("redirected", child1.range.get(input))
        assertEquals(1, child1.nodes.size)
        assertEquals(subject.root, child1.rootNode)
        assertEquals(child1.range, child1.nodes[0].range)
        assertEquals(redirectNode, child1.nodes[0].node)

        val child2 = child1.child
        assertNotNull(child2)
        assertEquals("actual", child2.range.get(input))
        assertEquals(1, child2.nodes.size)
        assertEquals(subject.root, child2.rootNode)
        assertEquals(child2.range, child2.nodes[0].range)
        assertEquals(concreteNode, child2.nodes[0].node)

        assertEquals(42, subject.execute(parse))
        verify(command).run(any())
    }

    @Test
    fun testCorrectExecuteContextAfterRedirect() {
        val subject = CommandDispatcher<Int>()

        val root = subject.root
        val add = literal<Int>("add")
        val blank = literal<Int>("blank")
        val addArg = argument<Int, Int>("value", integer())
        val run = literal<Int>("run")

        subject.register(
            add.then(
                addArg.redirect(
                    root
                ) { c -> c.source + c.getArgument("value", Int::class.java) }
            )
        )
        subject.register(blank.redirect(root))
        subject.register(run.executes { c -> c.source })

        assertEquals(0, subject.execute("run", 0))
        assertEquals(1, subject.execute("run", 1))

        assertEquals(1 + 5, subject.execute("add 5 run", 1))
        assertEquals(2 + 5 + 6, subject.execute("add 5 add 6 run", 2))
        assertEquals(1 + 5, subject.execute("add 5 blank run", 1))
        assertEquals(1 + 5, subject.execute("blank add 5 run", 1))
        assertEquals(2 + 5 + 6, subject.execute("add 5 blank add 6 run", 2))
        assertEquals(2 + 5 + 6, subject.execute("add 5 blank blank add 6 run", 2))
    }

    @Test
    fun testSharedRedirectAndExecuteNodes() {
        val subject = CommandDispatcher<Int>()

        val root = subject.root
        val add = literal<Int>("add")
        val addArg = argument<Int, Int>("value", integer())

        subject.register(
            add.then(
                addArg
                    .redirect(
                        root
                    ) { c -> c.source + c.getArgument("value", Int::class.java) }
                    .executes { c -> c.source }
            )
        )

        assertEquals(1, subject.execute("add 5", 1))
        assertEquals(1 + 5, subject.execute("add 5 add 6", 1))
    }

    @Test
    fun testExecuteRedirected() {
        val modifier: RedirectModifier<Any?> = mock()
        val source1 = Any()
        val source2 = Any()

        whenever(modifier.apply(argThat { this.source == source })).thenReturn(listOf(source1, source2))

        val concreteNode = subject.register(literal<Any?>("actual").executes(command))
        val redirectNode = subject.register(literal<Any?>("redirected").fork(subject.root, modifier))

        val input = "redirected actual"
        val parse = subject.parse(input, source)
        assertEquals("redirected", parse.context.range.get(input))
        assertEquals(1, parse.context.nodes.size)
        assertEquals(subject.root, parse.context.rootNode)
        assertEquals(parse.context.range, parse.context.nodes[0].range)
        assertEquals(redirectNode, parse.context.nodes[0].node)
        assertEquals(source, parse.context.source)

        val parent = parse.context.child
        assertNotNull(parent)
        assertEquals("actual", parent.range.get(input))
        assertEquals(1, parent.nodes.size)
        assertEquals(subject.root, parse.context.rootNode)
        assertEquals(parent.range, parent.nodes[0].range)
        assertEquals(concreteNode, parent.nodes[0].node)
        assertEquals(source, parent.source)

        assertEquals(2, subject.execute(parse))
        verify(command).run(argThat { this.source == source1 })
        verify(command).run(argThat { this.source == source2 })
    }

    @Test
    fun testIncompleteRedirectShouldThrow() {
        val foo = subject.register(
            literal<Any?>("foo")
                .then(
                    literal<Any?>("bar")
                        .then(argument<Any?, Int>("value", integer()).executes { context ->
                            context.getArgument("value", Int::class.java)
                        })
                )
                .then(literal<Any?>("awa").executes { 2 })
        )
        subject.register(literal<Any?>("baz").redirect(foo))
        
        val ex = assertFailsWith<CommandSyntaxException> {
            subject.execute("baz bar", source)
        }
        assertEquals(CommandError.UnknownCommand, ex.type)
    }

    @Test
    fun testRedirectModifierEmptyResult() {
        val foo = subject.register(
            literal<Any?>("foo")
                .then(
                    literal<Any?>("bar")
                        .then(argument<Any?, Int>("value", integer()).executes { context ->
                             context.getArgument("value", Int::class.java)
                        })
                )
                .then(literal<Any?>("awa").executes { 2 })
        )
        val emptyModifier = RedirectModifier { _: CommandContext<Any?>? -> mutableListOf() }
        subject.register(literal<Any?>("baz").fork(foo, emptyModifier))
        val result = subject.execute("baz bar 100", source)
        assertEquals(0, result)
    }

    @Test
    fun testExecuteOrphanedSubcommand() {
        subject.register(
            literal<Any?>("foo").then(
                argument("bar", integer())
            ).executes(command)
        )

        val ex = assertFailsWith<CommandSyntaxException> {
            subject.execute("foo 5", source)
        }
        assertEquals(CommandError.UnknownCommand, ex.type)
        assertEquals(5, ex.cursor)
    }

    @Test
    fun testExecute_invalidOther() {
        val wrongCommand: Command<Any?> = mock()
        subject.register(literal<Any?>("w").executes(wrongCommand))
        subject.register(literal<Any?>("world").executes(command))

        assertEquals(42, subject.execute("world", source))
        verify(wrongCommand, never()).run(any())
        verify(command).run(any())
    }

    @Test
    fun parse_noSpaceSeparator() {
        subject.register(literal<Any?>("foo").then(argument<Any?, Int>("bar", integer()).executes(command)))

        val ex = assertFailsWith<CommandSyntaxException> {
            subject.execute("foo$", source)
        }

        assertEquals(CommandError.UnknownCommand, ex.type)
        assertEquals(0, ex.cursor)
    }

    @Test
    fun testExecuteInvalidSubcommand() {
        subject.register(
            literal<Any?>("foo").then(
                argument("bar", integer())
            ).executes(command)
        )

        val ex = assertFailsWith<CommandSyntaxException> { subject.execute("foo bar", source) }
        assertEquals(CommandError.ExpectedType(Int::class), ex.type)
        assertEquals(4, ex.cursor)
    }

    @Test
    fun testGetPath() {
        val bar: LiteralCommandNode<Any?> = literal<Any?>("bar").build()
        subject.register(literal<Any?>("foo").then(bar))

        assertEquals(listOf("foo", "bar"), subject.getPath(bar))
    }

    @Test
    fun testFindNodeExists() {
        val bar: LiteralCommandNode<Any?> = literal<Any?>("bar").build()
        subject.register(literal<Any?>("foo").then(bar))

        assertEquals(bar, subject.findNode(listOf("foo", "bar")))
    }

    @Test
    fun testFindNodeDoesntExist() {
        assertNull(subject.findNode(listOf("foo", "bar")))
    }

    @Test
    fun testResultConsumerInNonErrorRun() {
        subject.setConsumer(consumer)

        subject.register(literal<Any?>("foo").executes(command))
        whenever(command.run(any())).thenReturn(5)

        assertEquals(5, subject.execute("foo", source))
        verify(consumer).onCommandComplete(any(), eq(true), eq(5))
        verifyNoMoreInteractions(consumer)
    }

    @Test
    fun testResultConsumerInForkedNonErrorRun() {
        subject.setConsumer(consumer)

        subject.register(literal<Any?>("foo").executes { c -> c.source as Int })
        val contexts = arrayOf<Any?>(9, 10, 11)

        subject.register(literal<Any?>("repeat").fork(subject.root) { listOf(*contexts) })

        assertEquals(contexts.size, subject.execute("repeat foo", source))
        verify(consumer).onCommandComplete(argThat { this.source == contexts[0] }, eq(true), eq(9))
        verify(consumer).onCommandComplete(argThat { this.source == contexts[1] }, eq(true), eq(10))
        verify(consumer).onCommandComplete(argThat { this.source == contexts[2] }, eq(true), eq(11))
        verifyNoMoreInteractions(consumer)
    }

    @Test
    fun testExceptionInNonForkedCommand() {
        subject.setConsumer(consumer)
        subject.register(literal<Any?>("crash").executes(command))
        val exception = CommandSyntaxException(CommandError.ExpectedType(Boolean::class))
        whenever(command.run(any())).thenThrow(exception)

        val ex = assertFailsWith<CommandSyntaxException> {
            subject.execute("crash", source)
        }
        assertEquals(exception, ex)

        verify(consumer).onCommandComplete(any(), eq(false), eq(0))
        verifyNoMoreInteractions(consumer)
    }

    @Test
    fun testExceptionInNonForkedRedirectedCommand() {
        subject.setConsumer(consumer)
        subject.register(literal<Any?>("crash").executes(command))
        subject.register(literal<Any?>("redirect").redirect(subject.root))

        val exception = CommandSyntaxException(CommandError.ExpectedType(Boolean::class))
        whenever(command.run(any())).thenThrow(exception)

        val ex = assertFailsWith<CommandSyntaxException> {
            subject.execute("redirect crash", source)
        }
        assertEquals(exception, ex)

        verify(consumer).onCommandComplete(any(), eq(false), eq(0))
        verifyNoMoreInteractions(consumer)
    }

    @Test
    fun testExceptionInForkedRedirectedCommand() {
        subject.setConsumer(consumer)
        subject.register(literal<Any?>("crash").executes(command))
        subject.register(literal<Any?>("redirect").fork(subject.root) { o -> mutableSetOf(o) })

        val exception = CommandSyntaxException(CommandError.ExpectedType(Boolean::class))
        whenever(command.run(any())).thenThrow(exception)

        assertEquals(0, subject.execute("redirect crash", source))
        verify(consumer).onCommandComplete(any(), eq(false), eq(0))
        verifyNoMoreInteractions(consumer)
    }

    @Test
    fun testExceptionInNonForkedRedirect() {
        val exception = CommandSyntaxException(CommandError.ExpectedType(Boolean::class))

        subject.setConsumer(consumer)
        subject.register(literal<Any?>("noop").executes(command))
        subject.register(literal<Any?>("redirect").redirect(subject.root) { throw exception })

        whenever(command.run(any())).thenReturn(3)

        val ex = assertFailsWith<CommandSyntaxException> {
            subject.execute("redirect noop", source)
        }
        assertEquals(exception, ex)

        verify(command, never()).run(any())
        verify(consumer).onCommandComplete(any(), eq(false), eq(0))
        verifyNoMoreInteractions(consumer)
    }

    @Test
    fun testExceptionInForkedRedirect() {
        val exception = CommandSyntaxException(CommandError.ExpectedType(Boolean::class))

        subject.setConsumer(consumer)
        subject.register(literal<Any?>("noop").executes(command))
        subject.register(literal<Any?>("redirect").fork(subject.root) { throw exception })

        whenever(command.run(any())).thenReturn(3)

        assertEquals(0, subject.execute("redirect noop", source))

        verify(command, never()).run(any())
        verify(consumer).onCommandComplete(any(), eq(false), eq(0))
        verifyNoMoreInteractions(consumer)
    }

    @Test
    fun testPartialExceptionInForkedRedirect() {
        val exception = CommandSyntaxException(CommandError.ExpectedType(Boolean::class))
        val otherSource = Any()
        val rejectedSource = Any()

        subject.setConsumer(consumer)
        subject.register(literal<Any?>("run").executes(command))
        subject.register(
            literal<Any?>("split").fork(
                subject.root
            ) { listOf(source, rejectedSource, otherSource) }
        )
        subject.register(literal<Any?>("filter").fork(subject.root) { context ->
            val currentSource = context.source
            if (currentSource === rejectedSource) {
                throw exception
            }
            mutableSetOf(currentSource)
        })

        whenever(command.run(any())).thenReturn(3)

        assertEquals(2, subject.execute("split filter run", source))

        verify(command).run(argThat { this.source == source })
        verify(command).run(argThat { this.source == otherSource })
        verifyNoMoreInteractions(command)

        verify(consumer).onCommandComplete(argThat { this.source == rejectedSource }, eq(false), eq(0))
        verify(consumer).onCommandComplete(argThat { this.source == source }, eq(true), eq(3))
        verify(consumer).onCommandComplete(argThat { this.source == otherSource }, eq(true), eq(3))
        verifyNoMoreInteractions(consumer)
    }
}
