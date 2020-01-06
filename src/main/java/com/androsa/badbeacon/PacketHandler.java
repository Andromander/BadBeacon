package com.androsa.badbeacon;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;

public class PacketHandler {
    private static final String PROTOCOL_VERSION = "1";
    private static final SimpleChannel HANDLER = NetworkRegistry.newSimpleChannel(
            new ResourceLocation("badbeacon", "main_channel"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public static void register() {
        HANDLER.registerMessage(0, CUpdateBadBeaconPacket.class, CUpdateBadBeaconPacket::encode, CUpdateBadBeaconPacket::decode, CUpdateBadBeaconPacket.Handler::handle);
    }

    public static <T> void sendToServer(T message) {
        HANDLER.sendToServer(message);
    }
}
