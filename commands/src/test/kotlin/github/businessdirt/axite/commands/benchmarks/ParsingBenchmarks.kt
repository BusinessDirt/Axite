package github.businessdirt.axite.commands.benchmarks

import github.businessdirt.axite.commands.CommandDispatcher
import github.businessdirt.axite.commands.builder.literal
import org.openjdk.jmh.annotations.*
import java.util.concurrent.TimeUnit

@State(Scope.Benchmark)
class ParsingBenchmarks {

    private lateinit var subject: CommandDispatcher<Any>

    @Setup
    fun setup() {
        subject = CommandDispatcher()

        // Command "a"
        subject.register(literal("a") {
            literal("1") {
                literal("i") { executes { 0 } }
                literal("ii") { executes { 0 } }
            }
            literal("2") {
                literal("i") { executes { 0 } }
                literal("ii") { executes { 0 } }
            }
        })

        // Command "b"
        subject.register(literal("b") {
            literal("1") { executes { 0 } }
        })

        // Command "c"
        subject.register(literal("c") { executes { 0 } })

        // Command "d" (Permission test)
        subject.register(literal("d") {
            requires { false }
            executes { 0 }
        })

        // Command "e"
        subject.register(literal("e") {
            executes { 0 }
            literal("1") {
                executes { 0 }
                literal("i") { executes { 0 } }
                literal("ii") { executes { 0 } }
            }
        })

        // Command "f"
        subject.register(literal("f") {
            literal("1") {
                literal("i") { executes { 0 } }
                literal("ii") {
                    executes { 0 }
                    requires { false }
                }
            }
            literal("1") {
                literal("ii") { executes { 0 } }
                literal("i") {
                    executes { 0 }
                    requires { false }
                }
            }
        })

        // Command "g"
        subject.register(literal("g") {
            executes { 0 }
            literal("1") {
                literal("i") { executes { 0 } }
            }
        })

        // Command "h"
        val h = subject.register(literal("h") {
            executes { 0 }
            literal("1") {
                literal("i") { executes { 0 } }
            }
            literal("2") {
                literal("i") { executes { 0 } }
                literal("ii") { executes { 0 } }
            }
            literal("3") { executes { 0 } }
        })

        // Command "i"
        subject.register(literal("i") {
            executes { 0 }
            literal("1") { executes { 0 } }
            literal("2") { executes { 0 } }
        })

        // Command "j" (Redirect Loop)
        subject.register(literal("j") { redirect(subject.root) })

        // Command "k" (Redirect to H)
        subject.register(literal("k") { redirect(h) })
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    fun parseA1i() {
        subject.parse("a 1 i", Any())
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    fun parseC() {
        subject.parse("c", Any())
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    fun parseK1i() {
        subject.parse("k 1 i", Any())
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    fun parseEmptyC() {
        subject.parse("c", Any())
    }
}