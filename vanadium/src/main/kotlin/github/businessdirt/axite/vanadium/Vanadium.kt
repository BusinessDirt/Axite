package github.businessdirt.axite.vanadium

import github.businessdirt.axite.events.EventBus
import github.businessdirt.axite.logging.LoggingConfigurator
import github.businessdirt.axite.logging.PatternBuilder
import github.businessdirt.axite.vanadium.assets.model.ModelData
import github.businessdirt.axite.vanadium.graph.RenderGraph
import github.businessdirt.axite.vanadium.math.Resolution
import github.businessdirt.axite.vanadium.platform.Window
import github.businessdirt.axite.vanadium.scene.Scene
import github.businessdirt.axite.vanadium.utils.camelToTitleCase
import github.businessdirt.axite.vanadium.utils.log
import github.businessdirt.axite.vanadium.utils.profile
import org.lwjgl.glfw.GLFW
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.lang.reflect.Modifier

object Vanadium {

    private val logger: Logger by lazy { LoggerFactory.getLogger(Vanadium::class.java) }

    val time: Double
        get() = GLFW.glfwGetTime()

    var scene: Scene? = null

    fun launch(gameProvider: () -> VanadiumAdapter) {

        this::configureLogging.profile(logger)
        EventBus::initialize.profile(logger)

        val config = VanadiumConfig()
        with(gameProvider()) { run(config) }
    }

    private fun VanadiumAdapter.run(config: VanadiumConfig) {
        configure(config)
        config.log(logger)

        val timePerUpdate = 1_000_000_000.0 / config.updatesPerSecond
        var previousTime = System.nanoTime()
        var accumulator = 0.0

        Window(config).use { window ->
            val initData: InitData = ::initialize.profile(logger)
            RenderGraph.initialize(window, config, initData)


            while (!window.shouldClose) {
                val currentTime = System.nanoTime()
                val frameTime = currentTime - previousTime
                previousTime = currentTime

                // Add the fraction of an update that this frame time represents
                accumulator += frameTime / timePerUpdate

                window.pollEvents()

                while (accumulator >= 1.0) {
                    val fixedDeltaMillis = (timePerUpdate / 1_000_000).toLong()
                    update(fixedDeltaMillis)

                    accumulator--
                }

                RenderGraph.render()
            }

            ::shutdown.profile(logger)
            RenderGraph.shutdown()
        }
    }

    fun configureLogging() {
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

data class VanadiumConfig(
    var applicationName: String = "Vanadium Application",
    var resolution: Resolution = Resolution(0, 0),
    var updatesPerSecond: Int = 60,
    var validate: Boolean = true,
    var requestedImages: Int = 3,
    var vsync: Boolean = true,
    var recompileShaders: Boolean = true,
) {
    fun log(logger: Logger) = logger.atInfo().log(title = "Vanadium Configuration") {
        val fields = this@VanadiumConfig.javaClass.declaredFields
        for (field in fields) {
            if (Modifier.isStatic(field.modifiers) || field.isSynthetic) continue

            field.isAccessible = true
            val formattedName = field.name.camelToTitleCase()
            val value = field.get(this@VanadiumConfig)

            append(formattedName).append(": ").append(value).appendLine()
        }
    }
}

interface VanadiumAdapter {
    fun configure(config: VanadiumConfig) {}
    fun initialize(): InitData
    fun update(deltaTime: Long) {}
    fun shutdown() {}
}

data class InitData(
    val models: List<ModelData>
)