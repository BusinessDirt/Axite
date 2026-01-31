package github.businessdirt.axite.networking.packet

import kotlin.reflect.KClass

interface PacketRegistryProvider {
    val modules: List<KClass<*>>
}