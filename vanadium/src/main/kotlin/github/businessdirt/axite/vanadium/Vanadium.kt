package github.businessdirt.axite.vanadium

import github.businessdirt.axite.logging.LoggingConfigurator
import github.businessdirt.axite.logging.PatternBuilder
import github.businessdirt.axite.vanadium.assets.AssetManager
import github.businessdirt.axite.vanadium.assets.loaders.MaterialSerializer
import github.businessdirt.axite.vanadium.assets.loaders.ModelSerializer
import github.businessdirt.axite.vanadium.assets.loaders.ShaderSerializer
import github.businessdirt.axite.vanadium.assets.loaders.TextureSerializer
import github.businessdirt.axite.vanadium.assets.types.Material
import github.businessdirt.axite.vanadium.assets.types.Model
import github.businessdirt.axite.vanadium.assets.types.Shader
import github.businessdirt.axite.vanadium.assets.types.Texture
import github.businessdirt.axite.vanadium.core.events.Event
import github.businessdirt.axite.vanadium.core.events.EventDispatcher
import github.businessdirt.axite.vanadium.core.events.FramebufferResizedEvent
import github.businessdirt.axite.vanadium.core.math.Clock
import github.businessdirt.axite.vanadium.core.profiling.Profiler
import github.businessdirt.axite.vanadium.platform.KeyboardInput
import github.businessdirt.axite.vanadium.platform.MouseInput
import github.businessdirt.axite.vanadium.platform.Window
import github.businessdirt.axite.vanadium.renderer.Renderer
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

    private lateinit var adapter: VanadiumAdapter
    private lateinit var config: VanadiumConfig

    lateinit var window: Window
    lateinit var context: Context
    lateinit var renderer: Renderer
    lateinit var assets: AssetManager

    fun launch(adapterProvider: () -> VanadiumAdapter) {
        config = VanadiumConfig()
        adapter = adapterProvider()

        runBlocking {
            Profiler.profile("Initialization") {
                initCoreSystems()

                window = Window(config).also { it.initialize() }
                context = Context(config)
                context.initialize(window)
                renderer = Renderer(context).also { it.initialize() }
                assets = AssetManager(engineScope).configure {
                    registerLoader<Shader>(ShaderSerializer())
                    registerLoader<Texture>(TextureSerializer())
                    registerLoader<Model>(ModelSerializer())
                    registerLoader<Material>(MaterialSerializer())
                }

                // Initialize Adapter (Suspendable for async asset loading)
                adapter.configure(config)
                Profiler.profile("Adapter Initialization") { adapter.initialize(this) }
            }

            // Start the Engine Loop
            runEngineLoop()
        }
    }

    @OptIn(ExperimentalAtomicApi::class)
    private suspend fun runEngineLoop() {
        isRunning.store(true)

        val clock = Clock(config.updatesPerSecond)

        // Main Loop must remain on the thread that initialized GLFW
        while (isRunning.load() && !window.shouldClose()) {
            clock.tick()
            window.pollEvents()

            // Fixed Timestep Logic Updates & Variable Rate Rendering with interpolation
            while (clock.shouldUpdate()) adapter.update(clock.frameInfo)
            renderer.render(adapter, clock.interpolation)

            MouseInput.endFrame()

            // Yield to allow other coroutines to work if needed
            yield()
        }

        shutdown(adapter)
    }

    fun onEvent(event: Event) {
        if (!::context.isInitialized) return

        KeyboardInput.onEvent(event)
        MouseInput.onEvent(event)

        val dispatcher = EventDispatcher(event)
        dispatcher.dispatch<FramebufferResizedEvent> { context.resize() }

        adapter.onEvent(event)
    }

    private fun initCoreSystems() = Profiler.profile("Core Systems Initialization") {
        if (!glfwInit()) throw IllegalStateException("Unable to initialize GLFW")
        configureLogging()
    }

    private fun shutdown(adapter: VanadiumAdapter) = Profiler.profile("Shutdown") {
        context.device.waitIdle()

        Profiler.profile("Adapter Shutdown") { adapter.shutdown() }
        renderer.shutdown()
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