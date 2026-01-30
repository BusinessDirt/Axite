package github.businessdirt.networking.pipeline

import github.businessdirt.networking.packet.Packet
import github.businessdirt.networking.packet.PacketRegistry
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToByteEncoder

/**
 * Outbound handler that encodes [Packet] objects into bytes.
 *
 * Looks up the packet ID in [PacketRegistry], writes it as an integer,
 * and then calls [Packet.encode].
 *
 * @param packetRegistry The registry used to resolve packet IDs.
 */
class PacketEncoder(private val packetRegistry: PacketRegistry) : MessageToByteEncoder<Packet>() {
    override fun encode(ctx: ChannelHandlerContext, msg: Packet, out: ByteBuf) {
        val id = packetRegistry.getPacketId(msg::class)
            ?: throw IllegalArgumentException("Packet ${msg::class.simpleName} is not registered!")
        
        out.writeInt(id)
        msg.encode(out)
    }
}