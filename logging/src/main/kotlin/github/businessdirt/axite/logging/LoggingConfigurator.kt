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

    var pattern: PatternBuilder = PatternBuilder.fancy()
    var rootLevel: Level = Level.INFO

    companion object {
        private val logger by lazy { LogManager.getLogger(LoggingConfigurator::class.java) }

        /**
         * Initializes the logging configuration using a lambda block.
         */
        fun configure(block: LoggingConfigurator.() -> Unit) {
            val config = LoggingConfigurator().apply(block)

            val builder = ConfigurationBuilderFactory.newConfigurationBuilder()

            val appenderName = "StdoutAppender"
            val appenderBuilder = builder.newAppender(appenderName, "Console")
                .addAttribute("target", "SYSTEM_OUT")
                .add(builder.newLayout("PatternLayout").addAttribute("pattern", config.pattern.withColors()))

            builder.add(appenderBuilder)

            // Explicitly define loggers for the bridged streams to ensure correct levels
            if (config.bridgeSysOut) {
                builder.add(builder.newLogger(config.sysOutLoggerName, Level.INFO)
                    .add(builder.newAppenderRef(appenderName))
                    .addAttribute("additivity", false))
            }

            if (config.bridgeSysError) {
                builder.add(builder.newLogger(config.sysErrorLoggerName, Level.ERROR)
                    .add(builder.newAppenderRef(appenderName))
                    .addAttribute("additivity", false))
            }

            builder.add(builder.newRootLogger(config.rootLevel).add(builder.newAppenderRef(appenderName)))

            config.applySystemProperties()
            config.applyStreams()

            isDebugMode.let { Configurator.setRootLevel(Level.DEBUG) }
            Configurator.reconfigure(builder.build())
            isDebugMode.let { logger.info("Debug mode detected: Root level set to DEBUG.") }
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