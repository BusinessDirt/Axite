package github.businessdirt.axite.vanadium

import github.businessdirt.axite.events.EventBus
import github.businessdirt.axite.logging.LoggingConfigurator
import github.businessdirt.axite.logging.PatternBuilder
import github.businessdirt.axite.vanadium.utils.Profiler
import org.lwjgl.glfw.GLFW
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object Vanadium {

    val logger: Logger by lazy { LoggerFactory.getLogger(Vanadium::class.java) }

    val time: Double
        get() = GLFW.glfwGetTime()

    fun launch(gameProvider: () -> VanadiumAdapter) {

        val loggingConfigurationTime: Double = Profiler.profile {
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
        }.first
        logger.info("Logging Configuration took ${loggingConfigurationTime}ms")

        val eventBusInitializationTime: Double = Profiler.profile { EventBus.initialize() }.first
        logger.info("EventBus Initialization took ${eventBusInitializationTime}ms")

        with(gameProvider()) {
            onStart()
            onShutdown()
        }
    }
}