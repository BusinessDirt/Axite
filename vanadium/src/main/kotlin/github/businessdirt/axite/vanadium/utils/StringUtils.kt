package github.businessdirt.axite.vanadium.utils


fun String.startsWith(vararg prefixes: String): Boolean =
    prefixes.any { this.startsWith(it) }