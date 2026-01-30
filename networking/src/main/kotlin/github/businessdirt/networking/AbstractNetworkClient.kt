package github.businessdirt.networking

import github.businessdirt.networking.packet.Packet
import github.businessdirt.networking.packet.PacketRegistry
import io.netty.bootstrap.Bootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.EventLoopGroup
import io.netty.channel.MultiThreadIoEventLoopGroup
import io.netty.channel.nio.NioIoHandler
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import org.slf4j.LoggerFactory

/**
 * Abstract base class for a network client.
 *
 * Handles the connection lifecycle, including connecting to a server,
 * sending packets, and handling the channel pipeline.
 *
 * @param host The server hostname or IP address.
 * @param port The server port.
 * @param packetRegistry The registry containing registered packets.
 */
abstract class AbstractNetworkClient(
    private val host: String,
    private val port: Int,
    private val packetRegistry: PacketRegistry
) {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private var workerGroup: EventLoopGroup? = null
    private var channel: Channel? = null

    /**
     * Configures the [io.netty.bootstrap.Bootstrap] options.
     *
     * @param bootstrap The Netty bootstrap instance.
     */
    protected open fun configureBootstrap(bootstrap: Bootstrap) {
        bootstrap.option(ChannelOption.SO_KEEPALIVE, true)
    }

    /**
     * Initializes the client channel.
     *
     * This method is called after the default pipeline handlers (encoders/decoders) are added.
     * Use this to add custom handlers to the pipeline.
     *
     * @param channel The socket channel.
     */
    protected abstract fun initChannel(channel: SocketChannel)

    /**
     * Connects to the server.
     *
     * Initializes the EventLoopGroup and Bootstrap, and attempts to establish a connection.
     * This method blocks until the connection is established or fails.
     */
    fun connect() {
        workerGroup = MultiThreadIoEventLoopGroup(NioIoHandler.newFactory())

        try {
            val b = Bootstrap()
            b.group(workerGroup)
                .channel(NioSocketChannel::class.java)
                .handler(object : ChannelInitializer<SocketChannel>() {
                    override fun initChannel(ch: SocketChannel) {
                        ChannelConfigurator.configure(ch.pipeline(), packetRegistry)
                        this@AbstractNetworkClient.initChannel(ch)
                    }
                })

            configureBootstrap(b)

            val f: ChannelFuture = b.connect(host, port).sync()
            channel = f.channel()
            logger.info("Connected to {}:{}", host, port)

        } catch (e: Exception) {
            logger.error("Client connection exception", e)
            stop()
        }
    }

    /**
     * Sends a packet to the server.
     *
     * @param packet The packet to send.
     */
    fun sendPacket(packet: Packet) {
        channel?.writeAndFlush(packet)
    }

    /**
     * Stops the client and releases resources.
     *
     * Closes the channel and shuts down the EventLoopGroup.
     */
    fun stop() {
        channel?.close()
        workerGroup?.shutdownGracefully()
        logger.info("Client stopped")
    }

    /**
     * Waits for the client channel to close.
     *
     * This method blocks until the channel is closed.
     */
    fun waitForClose() {
        channel?.closeFuture()?.sync()
    }
}