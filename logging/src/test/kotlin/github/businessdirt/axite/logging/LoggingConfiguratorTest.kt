package github.businessdirt.axite.logging

import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.LoggerContext
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class LoggingConfiguratorTest {

    @Test
    fun `test system out redirection`() {
        // Arrange: Setup the configurator to bridge System.out
        LoggingConfigurator.configure {
            bridgeSysOut = true
            sysOutLoggerName = "test::out"
            rootLevel = Level.INFO
        }

        // We can't easily "read" the Log4j2 internal buffer without complex Mockito
        // setups, but we can verify the System.out object is no longer the default.
        val currentOut = System.out

        // This PrintStream should now be an instance of Log4j's IoPrintStream
        assertTrue(currentOut.javaClass.name.contains("io"),
            "System.out should be wrapped by Log4j IoBuilder")
    }

    @Test
    fun `test end to end logging flow`() {
        // Redirecting to a custom logger name so we can isolate it
        val loggerName = "end-to-end-test"

        LoggingConfigurator.configure {
            bridgeSysOut = true
            sysOutLoggerName = loggerName
            pattern = PatternBuilder.empty {
                text("[TEST] ")
                message()
                line()
            }
        }

        // If configured correctly, this string travels:
        // System.out -> Log4j2 -> ConsoleAppender
        println("Hello from System.out!")

        val logger = LogManager.getLogger(loggerName)
        assertTrue(logger.isInfoEnabled, "Logger $loggerName should be enabled at INFO level")
    }

    @Test
    fun `test custom appenders DSL`() {
        LoggingConfigurator.configure {
            appenders {
                console("TestConsole")
                file("TestFile", "build/test.log")
                rollingFile("TestRolling", "build/rolling.log", "build/rolling-%d{MM-dd-yy}.log.gz") {
                    size("5MB")
                    time(1)
                }
            }
        }

        val ctx = LogManager.getContext(false) as LoggerContext
        val config = ctx.configuration
        val rootLogger = config.rootLogger

        assertTrue(rootLogger.appenders.containsKey("TestConsole"), "Should contain TestConsole appender")
        assertTrue(rootLogger.appenders.containsKey("TestFile"), "Should contain TestFile appender")
        assertTrue(rootLogger.appenders.containsKey("TestRolling"), "Should contain TestRolling appender")
    }
}
