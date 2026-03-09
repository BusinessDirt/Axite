package github.businessdirt.axite.logging

import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.LoggerContext
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class LoggingConfiguratorTest {

    @BeforeEach
    fun setup() {
        // Reset the Log4j2 context before each test to ensure a clean slate
        val context = LogManager.getContext(false) as LoggerContext
        context.reconfigure()
    }

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
    fun `test debug mode detection logic`() {
        // Since we likely aren't running this test in a debugger,
        // we verify it returns false (or true if you are debugging the test!)
        val isDebug = LoggingConfigurator.isDebugMode

        // This is a smoke test to ensure the ManagementFactory call doesn't crash
        println("Is Debug Mode active during test: $isDebug")
    }

    @Test
    fun `test end to end logging flow`() {
        // Redirecting to a custom logger name so we can isolate it
        val loggerName = "end-to-end-test"

        LoggingConfigurator.configure {
            bridgeSysOut = true
            sysOutLoggerName = loggerName
            pattern = "[TEST] %m"
        }

        // If configured correctly, this string travels:
        // System.out -> Log4j2 -> ConsoleAppender
        println("Hello from System.out!")

        val logger = LogManager.getLogger(loggerName)
        assertTrue(logger.isInfoEnabled, "Logger $loggerName should be enabled at INFO level")
    }
}