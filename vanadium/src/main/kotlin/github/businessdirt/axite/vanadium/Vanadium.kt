package github.businessdirt.axite.vanadium

import github.businessdirt.axite.logging.LoggingConfigurator
import github.businessdirt.axite.logging.PatternBuilder
import github.businessdirt.axite.vanadium.core.math.Clock
import github.businessdirt.axite.vanadium.platform.Window
import kotlinx.coroutines.*
import org.lwjgl.glfw.GLFW.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi

object Vanadium {
    private val logger: Logger = LoggerFactory.getLogger(Vanadium::class.java)

    @OptIn(ExperimentalAtomicApi::class)
    private val isRunning = AtomicBoolean(false)

    // Structured Concurrency: Scope for the entire application lifecycle
    private val engineJob = SupervisorJob()
    val engineScope = CoroutineScope(Dispatchers.Default + engineJob)

    private lateinit var window: Window

    fun launch(adapterProvider: () -> VanadiumAdapter) {
        val config = VanadiumConfig()
        val adapter = adapterProvider()

        runBlocking {
            // Initialize System Systems (GLFW, Logging, etc.)
            initCoreSystems(config)

            window = Window(config)
            window.create(adapter)

            // Initialize Adapter (Suspendable for async asset loading)
            adapter.configure(config)
            adapter.initialize(this)

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

        cleanup(adapter)
    }

    private fun initCoreSystems(config: VanadiumConfig) {
        if (!glfwInit()) throw IllegalStateException("Unable to initialize GLFW")
        configureLogging()
        logger.info("Vanadium Infrastructure Initialized: ${config.applicationName}")
    }

    private fun cleanup(adapter: VanadiumAdapter) {
        logger.info("Shutting down Vanadium...")
        adapter.shutdown()
        window.destroy()
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