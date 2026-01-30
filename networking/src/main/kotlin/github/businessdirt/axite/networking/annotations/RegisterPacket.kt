package github.businessdirt.axite.networking.annotations

/**
 * Annotation to register a packet automatically.
 *
 * Classes annotated with this will be picked up by the annotation processor
 * and registered in the [github.businessdirt.axite.networking.packet.PacketRegistry].
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class RegisterPacket()
