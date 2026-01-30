package github.businessdirt.networking

import github.businessdirt.networking.packet.Packet
import github.businessdirt.networking.packet.PacketRegistry
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.channel.socket.SocketChannel
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@DisplayName("Server Integration Tests")
class ServerIntegrationTest {

    private val port = 50000 + (Math.random() * 1000).toInt()
    private lateinit var registry: PacketRegistry
    private var server: TestServer? = null
    private val clients = mutableListOf<TestClient>()

    @BeforeEach
    fun setup() {
        registry = PacketRegistry()
        registry.register(TestPacket::class)
        server = TestServer(port, registry)
        server?.startAsync()
        // Give server time to bind
        Thread.sleep(200)
    }

    @AfterEach
    fun tearDown() {
        clients.forEach { it.stop() }
        clients.clear()
        server?.stop()
        server = null
    }

    @Test
    @DisplayName("Client should connect and exchange packets")
    fun clientConnection() {
        val received = CountDownLatch(1)
        val serverReceived = CountDownLatch(1)
        
        server?.onPacket = { _ -> serverReceived.countDown() }

        val client = createClient()
        client.connect()
        client.onPacket = { received.countDown() }

        // Client -> Server
        client.sendPacket(TestPacket("Hello Server"))
        assertTrue(serverReceived.await(2, TimeUnit.SECONDS), "Server did not receive packet")

        // Server -> Client
        server?.broadcast(TestPacket("Hello Client"))
        assertTrue(received.await(2, TimeUnit.SECONDS), "Client did not receive packet")
        
        assertEquals(1, server?.getConnectedClients()?.size)
    }

    @Test
    @DisplayName("Server should track multiple clients")
    fun multipleClients() {
        val clientCount = 3
        
        // We can't easily hook into "onConnect" without modifying AbstractNetworkServer logic heavily,
        // so we'll just poll or wait.
        
        repeat(clientCount) {
            val c = createClient()
            c.connect()
        }
        
        // Wait for connections to establish
        Thread.sleep(500)
        
        assertEquals(clientCount, server?.getConnectedClients()?.size)
        
        // Broadcast
        val receivedCount = AtomicInteger(0)
        val broadcastLatch = CountDownLatch(clientCount)
        
        clients.forEach { 
            it.onPacket = { 
                receivedCount.incrementAndGet()
                broadcastLatch.countDown()
            }
        }
        
        server?.broadcast(TestPacket("Broadcast"))
        
        assertTrue(broadcastLatch.await(2, TimeUnit.SECONDS), "Not all clients received broadcast")
        assertEquals(clientCount, receivedCount.get())
    }

    @Test
    @DisplayName("Server should remove disconnected clients")
    fun clientDisconnect() {
        val client = createClient()
        client.connect()
        Thread.sleep(200)
        assertEquals(1, server?.getConnectedClients()?.size)
        
        client.stop()
        Thread.sleep(200) // Wait for Netty to process disconnect
        
        assertEquals(0, server?.getConnectedClients()?.size)
    }

    // Helper Classes

    private fun createClient(): TestClient {
        val client = TestClient("localhost", port, registry)
        clients.add(client)
        return client
    }

    class TestServer(port: Int, registry: PacketRegistry) : AbstractNetworkServer(port, registry) {
        var onPacket: ((Packet) -> Unit)? = null
        
        fun startAsync() {
            Thread { start() }.start()
        }

        override fun initChannel(channel: SocketChannel) {
            channel.pipeline().addLast(object : SimpleChannelInboundHandler<Packet>() {
                override fun channelRead0(ctx: ChannelHandlerContext, msg: Packet) {
                    onPacket?.invoke(msg)
                }
            })
        }
    }

    class TestClient(host: String, port: Int, registry: PacketRegistry) : AbstractNetworkClient(host, port, registry) {
        var onPacket: ((Packet) -> Unit)? = null

        override fun initChannel(channel: SocketChannel) {
            channel.pipeline().addLast(object : SimpleChannelInboundHandler<Packet>() {
                override fun channelRead0(ctx: ChannelHandlerContext, msg: Packet) {
                    onPacket?.invoke(msg)
                }
            })
        }
    }
}
