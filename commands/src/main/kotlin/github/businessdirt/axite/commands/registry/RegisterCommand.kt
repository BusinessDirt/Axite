package github.businessdirt.axite.commands.registry

/**
 * Annotation to mark a class as a command that should be registered.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class RegisterCommand