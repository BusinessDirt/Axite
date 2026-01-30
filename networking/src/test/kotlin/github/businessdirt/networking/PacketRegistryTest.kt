package github.businessdirt.networking

import github.businessdirt.axite.networking.packet.Packet
import github.businessdirt.axite.networking.packet.PacketRegistry
import io.netty.buffer.ByteBuf
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Packet Registry Tests")
class PacketRegistryTest {

    private lateinit var registry: PacketRegistry

    @BeforeEach
    fun setup() {
        registry = PacketRegistry()
    }

    @Test
    @DisplayName("Should register a packet successfully")
    fun registerPacket() {
        registry.register(TestPacket::class)
        val id = registry.getPacketId(TestPacket::class)
        assertNotNull(id)
        assertEquals(0, id)
    }

    @Test
    @DisplayName("Should return null for unregistered packet")
    fun unregisteredPacket() {
        class UnregisteredPacket : Packet {
            override fun encode(buf: ByteBuf) {}
            override fun decode(buf: ByteBuf) {}
        }
        assertNull(registry.getPacketId(UnregisteredPacket::class))
    }

    @Test
    @DisplayName("Should return correct class for ID")
    fun getClassForId() {
        registry.register(TestPacket::class)
        val clazz = registry.getPacketClass(0)
        assertEquals(TestPacket::class, clazz)
    }

    @Test
    @DisplayName("Should ignore duplicate registration")
    fun ignoreDuplicate() {
        registry.register(TestPacket::class)
        registry.register(TestPacket::class)
        
        // Internal map size isn't exposed, but IDs should remain consistent
        assertEquals(0, registry.getPacketId(TestPacket::class))
        // If it re-registered, it might have bumped ID or thrown error depending on impl, 
        // but current impl just returns if containsKey.
    }

    @Test
    @DisplayName("Should register multiple packets with unique IDs")
    fun multiplePackets() {
        class AnotherPacket : Packet {
            override fun encode(buf: ByteBuf) {}
            override fun decode(buf: ByteBuf) {}
        }

        registry.register(TestPacket::class)
        registry.register(AnotherPacket::class)

        assertEquals(0, registry.getPacketId(TestPacket::class))
        assertEquals(1, registry.getPacketId(AnotherPacket::class))
    }
}
