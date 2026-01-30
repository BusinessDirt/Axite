package github.businessdirt.networking.packet

import io.netty.buffer.ByteBuf

/**
 * Represents a network packet.
 *
 * All packets must implement this interface to be transmittable over the network.
 * Packets must provide a no-argument constructor for instantiation during decoding.
 */
interface Packet {
    /**
     * Encodes the packet data into the provided [ByteBuf].
     *
     * @param buf The buffer to write the packet data to.
     */
    fun encode(buf: ByteBuf)

    /**
     * Decodes the packet data from the provided [ByteBuf].
     *
     * @param buf The buffer to read the packet data from.
     */
    fun decode(buf: ByteBuf)
}
