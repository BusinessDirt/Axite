package github.businessdirt.axite.logging

import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder
import org.apache.logging.log4j.core.config.builder.api.LayoutComponentBuilder

/**
 * A builder for Log4j2 appenders using a DSL syntax.
 */
@Suppress("unused")
class AppenderBuilder(private val builder: ConfigurationBuilder<*>, private val pattern: PatternBuilder) {
    internal val appenderNames = mutableListOf<String>()

    /**
     * Adds a console appender.
     */
    fun console(
        name: String = "Console",
        target: String = "SYSTEM_OUT",
        follow: Boolean = false,
        colored: Boolean = true,
        pattern: PatternBuilder? = null
    ) {
        val appender = builder.newAppender(name, "Console")
            .addAttribute("target", target)
            .addAttribute("follow", follow)
            .add(createLayout(colored, pattern))
        builder.add(appender)
        appenderNames.add(name)
    }

    /**
     * Adds a file appender.
     */
    fun file(
        name: String,
        fileName: String,
        append: Boolean = true,
        colored: Boolean = false,
        pattern: PatternBuilder? = null
    ) {
        val appender = builder.newAppender(name, "File")
            .addAttribute("fileName", fileName)
            .addAttribute("append", append)
            .add(createLayout(colored, pattern))
        builder.add(appender)
        appenderNames.add(name)
    }

    /**
     * Adds a rolling file appender.
     */
    fun rollingFile(
        name: String,
        fileName: String,
        filePattern: String,
        append: Boolean = true,
        colored: Boolean = false,
        pattern: PatternBuilder? = null,
        block: RollingPolicyBuilder.() -> Unit = { size("10MB") }
    ) {
        val appender = builder.newAppender(name, "RollingFile")
            .addAttribute("fileName", fileName)
            .addAttribute("filePattern", filePattern)
            .addAttribute("append", append)
            .add(createLayout(colored, pattern))

        val policyBuilder = RollingPolicyBuilder(builder)
        policyBuilder.block()
        appender.addComponent(policyBuilder.build())

        builder.add(appender)
        appenderNames.add(name)
    }

    private fun createLayout(colored: Boolean, overridePattern: PatternBuilder? = null): LayoutComponentBuilder {
        val p = overridePattern ?: pattern
        return builder.newLayout("PatternLayout")
            .addAttribute("pattern", if (colored) p.withColors() else p.withoutColors())
    }

    /**
     * A builder for rolling file policies.
     */
    class RollingPolicyBuilder(private val builder: ConfigurationBuilder<*>) {
        private val policies = builder.newComponent("Policies")

        /**
         * Adds a size-based triggering policy.
         */
        fun size(size: String) {
            policies.addComponent(builder.newComponent("SizeBasedTriggeringPolicy").addAttribute("size", size))
        }

        /**
         * Adds a time-based triggering policy.
         */
        fun time(interval: Int = 1, modulate: Boolean = true) {
            policies.addComponent(
                builder.newComponent("TimeBasedTriggeringPolicy")
                    .addAttribute("interval", interval)
                    .addAttribute("modulate", modulate)
            )
        }

        /**
         * Adds a cron-based triggering policy.
         */
        fun cron(schedule: String) {
            policies.addComponent(builder.newComponent("CronTriggeringPolicy").addAttribute("schedule", schedule))
        }

        internal fun build() = policies
    }
}
