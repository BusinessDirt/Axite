package github.businessdirt.axite.logging

import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.config.Configurator
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory
import org.apache.logging.log4j.io.IoBuilder
import java.lang.management.ManagementFactory

/**
 * A DSL-based utility to configure Log4j2 bridges and dynamic log levels.
 */
class LoggingConfigurator private constructor() {

    /**
     * If true, redirects Java Util Logging (JUL) to Log4j2.
     * Note: This must be set before any JUL Loggers are initialized.
     */
    var bridgeJUL: Boolean = false

    /** If true, redirects all [System.out] output to a Log4j2 logger at INFO level. */
    var bridgeSysOut: Boolean = false

    /** If true, redirects all [System.err] output to a Log4j2 logger. */
    var bridgeSysError: Boolean = false

    /** The logger name used for System.out redirection. */
    var sysOutLoggerName: String = "sys::out"

    /** The logger name used for System.err redirection. */
    var sysErrorLoggerName: String = "sys::err"

    var pattern = "[%d{HH:mm:ss}] [%t/%level] (%c{1}%notEmpty{/%style{%marker}{bold,magenta}}): %highlight{%msg}{FATAL=red bright, ERROR=red, WARN=yellow, INFO=green, DEBUG=blue, TRACE=white}%n"
    var rootLevel: Level = Level.INFO

    companion object {
        private val logger by lazy { LogManager.getLogger(LoggingConfigurator::class.java) }

        /**
         * Initializes the logging configuration using a lambda block.
         */
        fun configure(block: LoggingConfigurator.() -> Unit) {
            val config = LoggingConfigurator().apply(block)

            applyCodeConfiguration(config)

            config.applySystemProperties()
            config.applyStreams()

            configureLogLevel()
        }

        /**
         * Programmatically defines a Console Appender.
         * This mirrors what you would usually do in log4j2.xml.
         */
        private fun applyCodeConfiguration(config: LoggingConfigurator) {
            val builder = ConfigurationBuilderFactory.newConfigurationBuilder()

            // Define a Console Appender
            val appenderBuilder = builder.newAppender("Stdout", "Console")
                .addAttribute("target", "SYSTEM_OUT")
                .add(builder.newLayout("PatternLayout")
                    .addAttribute("pattern", config.pattern))

            builder.add(appenderBuilder)

            // Setup Root Logger
            builder.add(builder.newRootLogger(config.rootLevel)
                .add(builder.newAppenderRef("Stdout")))

            // Initialize/Override with this configuration
            Configurator.reconfigure(builder.build())
        }

        private fun LoggingConfigurator.applySystemProperties() {
            if (bridgeJUL) {
                System.setProperty("java.util.logging.manager", "org.apache.logging.log4j.jul.LogManager")
            }
        }

        private fun LoggingConfigurator.applyStreams() {
            if (bridgeSysOut) {
                System.setOut(
                    IoBuilder.forLogger(LogManager.getLogger(sysOutLoggerName))
                        .setLevel(Level.INFO)
                        .buildPrintStream()
                )
            }

            if (bridgeSysError) {
                System.setErr(
                    IoBuilder.forLogger(LogManager.getLogger(sysErrorLoggerName))
                        .setLevel(Level.ERROR)
                        .buildPrintStream()
                )
            }
        }

        /**
         * Detects if a debugger is attached via JDWP and elevates the console log level to DEBUG.
         */
        fun configureLogLevel() = isDebugMode.let {
            System.setProperty("consoleLevel", "DEBUG")
            Configurator.reconfigure()
            logger.info("Debug mode detected via JDWP: Log level set to DEBUG.")
        }

        /**
         * Returns true if the JVM was started with JDWP (Java Debug Wire Protocol) arguments.
         */
        val isDebugMode: Boolean
            get() = try {
                ManagementFactory.getRuntimeMXBean().inputArguments.any {
                    it.contains("-agentlib:jdwp") || it.contains("-Xrunjdwp")
                }
            } catch (_: Exception) {
                false
            }
    }
}