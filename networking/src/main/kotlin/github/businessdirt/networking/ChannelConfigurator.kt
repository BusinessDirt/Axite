package github.businessdirt.networking

import github.businessdirt.networking.packet.PacketRegistry
import github.businessdirt.networking.pipeline.PacketDecoder
import github.businessdirt.networking.pipeline.PacketEncoder
import io.netty.channel.ChannelPipeline
import io.netty.handler.codec.LengthFieldBasedFrameDecoder
import io.netty.handler.codec.LengthFieldPrepender

object ChannelConfigurator {

    // Max length: 1MB, Length offset: 0, Length field size: 4, Adjustment: 0, Strip header: 4
    private const val MAX_FRAME_LENGTH = 1024 * 1024
    private const val LENGTH_FIELD_OFFSET = 0
    private const val LENGTH_FIELD_LENGTH = 4
    private const val LENGTH_ADJUSTMENT = 0
    private const val INITIAL_BYTES_TO_STRIP = 4

    /**
     * Configures a provided [ChannelPipeline] with a [LengthFieldBasedFrameDecoder] and a [PacketDecoder]
     * that is configured using the provided [PacketRegistry]
     *
     * @param pipeline the [ChannelPipeline] to configure
     * @param packetRegistry the [PacketRegistry] to use as the PacketDecoder
     */
    fun configure(pipeline: ChannelPipeline, packetRegistry: PacketRegistry) {
        // Inbound: Split stream into frames based on length field
        pipeline.addLast("frameDecoder", LengthFieldBasedFrameDecoder(
            MAX_FRAME_LENGTH, LENGTH_FIELD_OFFSET, LENGTH_FIELD_LENGTH,
            LENGTH_ADJUSTMENT, INITIAL_BYTES_TO_STRIP))
        pipeline.addLast("decoder", PacketDecoder(packetRegistry))

        // Outbound: Prepend length field
        pipeline.addLast("frameEncoder", LengthFieldPrepender(LENGTH_FIELD_LENGTH))
        pipeline.addLast("encoder", PacketEncoder(packetRegistry))
    }
}