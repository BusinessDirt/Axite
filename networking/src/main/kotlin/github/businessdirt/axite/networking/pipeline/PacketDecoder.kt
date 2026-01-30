package github.businessdirt.axite.networking.pipeline

import github.businessdirt.axite.networking.packet.PacketRegistry
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.ByteToMessageDecoder

/**
 * Inbound handler that decodes incoming bytes into [github.businessdirt.axite.networking.packet.Packet] objects.
 *
 * Reads an integer packet ID, looks up the corresponding class in [PacketRegistry],
 * instantiates it, and calls [github.businessdirt.axite.networking.packet.Packet.decode].
 *
 * @param packetRegistry The registry used to resolve packet IDs.
 */
class PacketDecoder(private val packetRegistry: PacketRegistry) : ByteToMessageDecoder() {
    override fun decode(ctx: ChannelHandlerContext, `in`: ByteBuf, out: MutableList<Any>) {
        if (`in`.readableBytes() < 4) return

        `in`.markReaderIndex()
        val id = `in`.readInt()

        val packetClass = packetRegistry.getPacketClass(id)
        if (packetClass == null) {
            `in`.resetReaderIndex()
            throw IllegalArgumentException("Unknown packet ID: $id")
        }

        try {
            val packet = packetClass.java.getDeclaredConstructor().newInstance()
            packet.decode(`in`)
            out.add(packet)
        } catch (e: Exception) {
            `in`.resetReaderIndex()
            throw e
        }
    }
}