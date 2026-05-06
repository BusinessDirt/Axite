package github.businessdirt.axite.vanadium

import github.businessdirt.axite.logging.LoggingConfigurator
import github.businessdirt.axite.logging.PatternBuilder
import github.businessdirt.axite.vanadium.core.math.Clock
import github.businessdirt.axite.vanadium.core.profiling.Profiler
import github.businessdirt.axite.vanadium.platform.Window
import github.businessdirt.axite.vanadium.vulkan.Context
import kotlinx.coroutines.*
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.lwjgl.glfw.GLFW.glfwInit
import org.lwjgl.glfw.GLFW.glfwTerminate
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi

object Vanadium {
    private val logger: Logger = LogManager.getLogger(Vanadium::class.java)

    @OptIn(ExperimentalAtomicApi::class)
    private val isRunning = AtomicBoolean(false)

    // Structured Concurrency: Scope for the entire application lifecycle
    private val engineJob = SupervisorJob()
    val engineScope = CoroutineScope(Dispatchers.Default + engineJob)

    lateinit var window: Window
    lateinit var context: Context

    fun launch(adapterProvider: () -> VanadiumAdapter) {
        val config = VanadiumConfig()
        val adapter = adapterProvider()

        runBlocking {
            Profiler.profile("Initialization") {
                // Initialize System Systems (GLFW, Logging, etc.)
                initCoreSystems(config)

                window = Window(config).also { it.initialize(adapter) }
                context = Context(config).also { it.initialize(window) }

                // Initialize Adapter (Suspendable for async asset loading)
                adapter.configure(config)
                Profiler.profile("Adapter Initialization") { adapter.initialize(this) }
            }

            // Start the Engine Loop
            runEngineLoop(adapter, config)
        }
    }

    @OptIn(ExperimentalAtomicApi::class)
    private suspend fun runEngineLoop(adapter: VanadiumAdapter, config: VanadiumConfig) {
        isRunning.store(true)

        val clock = Clock(config.updatesPerSecond)

        // Main Loop must remain on the thread that initialized GLFW
        while (isRunning.load() && !window.shouldClose()) {
            clock.tick()
            window.pollEvents()

            // Fixed Timestep Logic Updates & Variable Rate Rendering with interpolation
            while (clock.shouldUpdate()) adapter.update(clock.frameInfo)
            adapter.render(clock.interpolation)

            // Yield to allow other coroutines to work if needed
            yield()
        }

        shutdown(adapter)
    }

    private fun initCoreSystems(config: VanadiumConfig) = Profiler.profile("Core Systems Initialization") {
        if (!glfwInit()) throw IllegalStateException("Unable to initialize GLFW")
        configureLogging()
    }

    private fun shutdown(adapter: VanadiumAdapter) = Profiler.profile("Shutdown") {
        Profiler.profile("Adapter Shutdown") { adapter.shutdown() }
        context.shutdown()
        window.shutdown()

        engineJob.cancel()
        glfwTerminate()
    }

    private fun configureLogging() {
        LoggingConfigurator.configure {
            bridgeSysOut = true
            bridgeSysError = true
            bridgeJUL = true

            pattern = PatternBuilder.fancy()

            appenders {
                console("Console")
                rollingFile("Rolling", "logs/latest.log", "logs/rolling-%d{MM-dd-yy}.log.gz") {
                    size("5MB")
                    time(1)
                }
            }
        }
    }
}