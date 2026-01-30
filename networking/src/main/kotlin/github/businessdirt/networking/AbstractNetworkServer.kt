package github.businessdirt.networking

import github.businessdirt.networking.packet.Packet
import github.businessdirt.networking.packet.PacketRegistry
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.EventLoopGroup
import io.netty.channel.MultiThreadIoEventLoopGroup
import io.netty.channel.group.DefaultChannelGroup
import io.netty.channel.nio.NioIoHandler
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.util.concurrent.GlobalEventExecutor
import org.slf4j.LoggerFactory

/**
 * Abstract base class for a network server.
 *
 * Handles binding to a port, accepting client connections,
 * and managing the lifecycle of the server.
 *
 * @param port The port to listen on.
 * @param packetRegistry The registry containing registered packets.
 */
abstract class AbstractNetworkServer(
    private val port: Int,
    private val packetRegistry: PacketRegistry
) {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val connectedClients = DefaultChannelGroup(GlobalEventExecutor.INSTANCE)
    private var bossGroup: EventLoopGroup? = null
    private var workerGroup: EventLoopGroup? = null
    private var channelFuture: ChannelFuture? = null

    /**
     * Configures the [io.netty.bootstrap.ServerBootstrap] options.
     *
     * @param bootstrap The Netty server bootstrap instance.
     */
    protected open fun configureBootstrap(bootstrap: ServerBootstrap) {
        bootstrap.option(ChannelOption.SO_BACKLOG, 128)
            .childOption(ChannelOption.SO_KEEPALIVE, true)
    }

    /**
     * Initializes the child channel (connected client).
     *
     * This method is called after the default pipeline handlers (encoders/decoders) are added.
     * Use this to add custom handlers to the client pipeline.
     *
     * @param channel The socket channel of the connected client.
     */
    protected abstract fun initChannel(channel: SocketChannel)

    /**
     * Starts the server.
     *
     * Initializes the Boss and Worker EventLoopGroups, binds to the port,
     * and listens for incoming connections.
     * This method blocks until the server socket is closed.
     */
    fun start() {
        bossGroup = MultiThreadIoEventLoopGroup(NioIoHandler.newFactory())
        workerGroup = MultiThreadIoEventLoopGroup(NioIoHandler.newFactory())

        try {
            val b = ServerBootstrap()
            b.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel::class.java)
                .childHandler(object : ChannelInitializer<SocketChannel>() {
                    override fun initChannel(ch: SocketChannel) {
                        connectedClients.add(ch)
                        ChannelConfigurator.configure(ch.pipeline(), packetRegistry)
                        this@AbstractNetworkServer.initChannel(ch)
                    }
                })

            configureBootstrap(b)

            channelFuture = b.bind(port).sync()
            logger.info("Server started on port {}", port)
            channelFuture?.channel()?.closeFuture()?.sync()
        } catch (e: Exception) {
            logger.error("Server exception", e)
        } finally {
            stop()
        }
    }

    /**
     * Broadcasts a packet to all connected clients.
     *
     * @param packet The packet to send.
     */
    fun broadcast(packet: Packet) {
        connectedClients.writeAndFlush(packet)
    }

    /**
     * Retrieves the remote addresses of all connected clients.
     *
     * @return A list of client addresses as strings.
     */
    fun getConnectedClients(): List<String> {
        return connectedClients.map { it.remoteAddress().toString() }
    }

    /**
     * Logs the list of connected clients to the info log.
     */
    fun logConnectedClients() {
        val clients = getConnectedClients()
        logger.info("Connected Clients ({}):", clients.size)
        clients.forEach { logger.info(" - {}", it) }
    }

    /**
     * Stops the server and releases resources.
     *
     * Closes all client connections and shuts down the EventLoopGroups.
     */
    fun stop() {
        connectedClients.close().awaitUninterruptibly()
        workerGroup?.shutdownGracefully()
        bossGroup?.shutdownGracefully()
        logger.info("Server stopped")
    }
}