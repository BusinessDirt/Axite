package github.businessdirt.axite.vanadium

import github.businessdirt.axite.events.EventBus
import github.businessdirt.axite.logging.LoggingConfigurator
import github.businessdirt.axite.logging.PatternBuilder
import github.businessdirt.axite.vanadium.math.Resolution
import github.businessdirt.axite.vanadium.platform.Window
import github.businessdirt.axite.vanadium.utils.profile
import org.lwjgl.glfw.GLFW
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object Vanadium {

    private val logger: Logger by lazy { LoggerFactory.getLogger(Vanadium::class.java) }

    val time: Double
        get() = GLFW.glfwGetTime()

    fun launch(gameProvider: () -> VanadiumAdapter) {

        this::configureLogging.profile(logger)
        EventBus::initialize.profile(logger)

        with(gameProvider()) {
            val config = VanadiumConfig()
            configure(config)
            config.log(logger)

            val window = Window(config)
            ::initialize.profile(logger)

            try {
                with(window) {
                    while (!shouldClose) {
                        pollEvents()
                        update(0.0f)
                    }
                }
            } finally {
                ::shutdown.profile(logger)
                window.shutdown()
            }
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
    var resolution: Resolution = Resolution(1280, 720),
) {
    fun log(logger: Logger) {
        logger.info("")
        logger.info("=== Vanadium Config ===")
        logger.info("Application Name: $applicationName")
        logger.info("Resolution: $resolution")
        logger.info("=======================")
        logger.info("")
    }
}

interface VanadiumAdapter {
    fun configure(config: VanadiumConfig) {}
    fun initialize() {}
    fun update(deltaTime: Float) {}
    fun shutdown() {}
}