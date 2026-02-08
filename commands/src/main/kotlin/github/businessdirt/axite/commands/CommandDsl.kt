package github.businessdirt.axite.commands

/**
 * Marker annotation for the Command DSL.
 * This prevents scope leakage when nesting builders (e.g., arguments inside arguments).
 */
@DslMarker
annotation class CommandDsl