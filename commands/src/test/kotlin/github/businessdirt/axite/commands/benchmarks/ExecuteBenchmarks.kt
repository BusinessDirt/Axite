package github.businessdirt.axite.commands.benchmarks

import github.businessdirt.axite.commands.CommandDispatcher
import github.businessdirt.axite.commands.ParseResults
import github.businessdirt.axite.commands.builder.literal
import org.openjdk.jmh.annotations.*
import java.util.concurrent.TimeUnit

// JMH requires the class to be 'open' to generate the proxy subclass
@State(Scope.Benchmark)
class ExecuteBenchmarks {

    private lateinit var dispatcher: CommandDispatcher<Any>
    private lateinit var simple: ParseResults<Any>
    private lateinit var singleRedirect: ParseResults<Any>
    private lateinit var forkedRedirect: ParseResults<Any>

    @Setup
    fun setup() {
        dispatcher = CommandDispatcher()

        // Simple command returning 0
        dispatcher.register(literal("command") { executes { 0 } })

        // Redirect to root
        dispatcher.register(literal("redirect") { redirect(dispatcher.root) })

        // Fork logic using standard Kotlin 'listOf'
        dispatcher.register(literal("fork") {
            fork(dispatcher.root) { listOf(Any(), Any(), Any()) }
        })

        // Pre-parse commands to isolate execution performance
        simple = dispatcher.parse("command", Any())
        singleRedirect = dispatcher.parse("redirect command", Any())
        forkedRedirect = dispatcher.parse("fork command", Any())
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    fun executeSimple() {
        dispatcher.execute(simple)
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    fun executeSingleRedirect() {
        dispatcher.execute(singleRedirect)
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    fun executeForkedRedirect() {
        dispatcher.execute(forkedRedirect)
    }
}