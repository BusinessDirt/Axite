package github.businessdirt.axite.vanadium

import github.businessdirt.axite.events.EventBus
import github.businessdirt.axite.logging.LoggingConfigurator
import github.businessdirt.axite.logging.PatternBuilder
import org.lwjgl.glfw.GLFW
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object Vanadium {

    val logger: Logger = LoggerFactory.getLogger(Vanadium::class.java)

    val time: Double
        get() = GLFW.glfwGetTime()

    fun launch(game: VanadiumAdapter) {

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

        EventBus.initialize()

        game.onStart()

        //var running = true
        //while (running) {
        //    game.onUpdate(dt)
        //    game.onRender()
        //}

        game.onShutdown()
    }
}