dependencies {
    api("io.netty:netty-all:4.2.9.Final")
}

ksp {
    arg("processor.prefix", "Networking")
    arg("processor.moduleAnnotations", "github.businessdirt.axite.networking.annotations.RegisterPacket")
    arg("processor.RegisterPacket.interface", "github.businessdirt.axite.networking.packet.PacketRegistryProvider")
}
