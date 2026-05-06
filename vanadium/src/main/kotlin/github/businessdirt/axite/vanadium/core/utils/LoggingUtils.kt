package github.businessdirt.axite.vanadium.core.utils

import org.apache.logging.log4j.Level
import org.apache.logging.log4j.Logger

/**
 * Prints a collection of strings in a formatted grid grouped by a prefix.
 */
fun Logger.debugGrid(title: String, items: Collection<String>, prefixExtractor: (String) -> String) {
    if (items.isEmpty()) {
        this.debug("{}: None", title)
        return
    }

    // Group items and sort the groups by prefix name
    val grouped = items.groupBy(prefixExtractor).toSortedMap()

    boxedString(BoxCharset.ROUNDED, title) {
        grouped.forEach { (prefix, list) ->
            // Sort items within the group for readability
            val sortedList = list.sortedBy { it.substringAfter("${prefix}_") }
            val chunks = sortedList.chunked(2)

            chunks.forEachIndexed { index, row ->
                // Extract just the descriptive part of the name
                val first = row[0].substringAfter("${prefix}_").padEnd(32)
                val second = row.getOrNull(1)?.substringAfter("${prefix}_") ?: ""

                // Align labels (e.g., "KHR:", "LAYER:")
                val label = (if (index == 0) "$prefix:" else "").padEnd(10)

                appendLine("$label $first $second")
            }
            appendLine()
        }
    }.log(this, Level.DEBUG)
}

fun String.log(logger: Logger, level: Level = Level.DEBUG) =
    this.split("\n").forEach { logger.atLevel(level).log(it) }