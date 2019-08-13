package com.androsa.badbeacon;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;

public class PacketHandler {
    private static final String PROTOCOL_VERSION = Integer.toString(1);
    private static final SimpleChannel HANDLER = NetworkRegistry.ChannelBuilder
            .named(new ResourceLocation("badbeacon", "main_channel"))
            .clientAcceptedVersions(PROTOCOL_VERSION::equals)
            .serverAcceptedVersions(PROTOCOL_VERSION::equals)
            .networkProtocolVersion(() -> PROTOCOL_VERSION)
            .simpleChannel();

    public static void register() {
        HANDLER.registerMessage(0, CUpdateBadBeaconPacket.class, CUpdateBadBeaconPacket::encode, CUpdateBadBeaconPacket::decode, CUpdateBadBeaconPacket.Handler::handle);
    }

    public static <T> void sendToServer(T message) {
        HANDLER.sendToServer(message);
    }
}
