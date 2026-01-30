package github.businessdirt.networking

import github.businessdirt.axite.networking.annotations.RegisterPacket
import github.businessdirt.axite.networking.packet.Packet
import io.netty.buffer.ByteBuf
import io.netty.util.CharsetUtil

@RegisterPacket
class TestPacket : Packet {
    var message: String = ""

    @Suppress("unused")
    constructor()

    constructor(message: String) {
        this.message = message
    }

    override fun encode(buf: ByteBuf) {
        val bytes = message.toByteArray(CharsetUtil.UTF_8)
        buf.writeInt(bytes.size)
        buf.writeBytes(bytes)
    }

    override fun decode(buf: ByteBuf) {
        val length = buf.readInt()
        val bytes = ByteArray(length)
        buf.readBytes(bytes)
        message = String(bytes, CharsetUtil.UTF_8)
    }
}
