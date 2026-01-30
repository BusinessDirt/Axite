package github.businessdirt.networking.pipeline

import github.businessdirt.networking.TestPacket
import github.businessdirt.networking.packet.PacketRegistry
import io.netty.channel.embedded.EmbeddedChannel
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import java.util.stream.Stream

@DisplayName("Packet Codec Tests")
class PacketCodecTest {

    private lateinit var registry: PacketRegistry

    @BeforeEach
    fun setup() {
        registry = PacketRegistry()
        registry.register(TestPacket::class)
    }

    @TestFactory
    @DisplayName("Should encode and decode packets correctly")
    fun testPacketEncodingDecoding(): Stream<DynamicTest> {
        val testCases = listOf(
            "Hello World",
            "",
            "A",
            "Multi\nLine\nString",
            "Special Chars: !@#$%^&*()_+",
            "Unicode: 🚀あ",
            (1..1000).joinToString("") { "a" } // Long string
        )

        return testCases.stream().map {
            DynamicTest.dynamicTest("Test case: '$it'") {
                val channel = EmbeddedChannel(
                    PacketEncoder(registry),
                    PacketDecoder(registry)
                )

                val inputPacket = TestPacket(it)
                
                // Write outbound (encode)
                assertTrue(channel.writeOutbound(inputPacket))
                val encoded = channel.readOutbound<io.netty.buffer.ByteBuf>()
                assertNotNull(encoded)

                // Write inbound (decode) - simulating receiving the bytes
                // We need to write the bytes back to the inbound pipeline
                assertTrue(channel.writeInbound(encoded))
                
                val outputPacket = channel.readInbound<TestPacket>()
                assertNotNull(outputPacket)
                assertEquals(it, outputPacket.message)
                
                channel.finish()
            }
        }
    }
}
