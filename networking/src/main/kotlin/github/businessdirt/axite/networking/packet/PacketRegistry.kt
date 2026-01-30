package github.businessdirt.axite.networking.packet

import org.slf4j.LoggerFactory
import kotlin.reflect.KClass

/**
 * Registry for managing network packets.
 *
 * Handles the mapping between packet classes and their unique IDs.
 * Can automatically load packets registered via [github.businessdirt.axite.networking.annotations.RegisterPacket]
 * if the annotation processor is used.
 */
class PacketRegistry {
    private val logger = LoggerFactory.getLogger(PacketRegistry::class.java)
    private val idToPacket = mutableMapOf<Int, KClass<out Packet>>()
    private val packetToId = mutableMapOf<KClass<out Packet>, Int>()

    /**
     * Registers a packet class and assigns it a unique ID.
     *
     * @param packet The packet class to register.
     */
    fun register(packet: KClass<out Packet>) {
        if (packetToId.containsKey(packet)) return
        val id = idToPacket.size
        idToPacket[id] = packet
        packetToId[packet] = id
        logger.debug("Registered packet {} with ID {}", packet.simpleName, id)
    }

    /**
     * Retrieves the unique ID for a registered packet class.
     *
     * @param clazz The packet class.
     * @return The packet ID, or null if not registered.
     */
    fun getPacketId(clazz: KClass<out Packet>): Int? {
        return packetToId[clazz]
    }

    /**
     * Retrieves the packet class for a given ID.
     *
     * @param id The packet ID.
     * @return The packet class, or null if no packet is registered with this ID.
     */
    fun getPacketClass(id: Int): KClass<out Packet>? {
        return idToPacket[id]
    }

    /**
     * Initializes the registry by loading packets discovered by the annotation processor.
     *
     * Looks for the generated `NetworkingRegisterPacketRegistry` class.
     */
    @Suppress("UNCHECKED_CAST")
    fun initialize() {
        try {
            val clazz = Class.forName("github.businessdirt.network.generated.NetworkingRegisterPacketRegistry")
            val modulesField = clazz.getDeclaredField("modules")
            modulesField.isAccessible = true
            val modules = modulesField.get(null) as List<KClass<out Packet>>

            modules.forEach { register(it) }
            logger.info("Initialized PacketRegistry with {} packets", modules.size)
        } catch (e: ClassNotFoundException) {
            logger.warn("NetworkingRegisterPacketRegistry class not found. Make sure KSP has run and packets are annotated.", e)
        } catch (e: Exception) {
            logger.error("Failed to load packet registry", e)
        }
    }
}