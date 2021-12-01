package com.androsa.badbeacon;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class PacketHandler {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel HANDLER = NetworkRegistry.newSimpleChannel(
            new ResourceLocation("badbeacon", "main_channel"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public static void register() {
        HANDLER.messageBuilder(ServerboundBadBeaconPacket.class, 0).encoder(ServerboundBadBeaconPacket::write).decoder(ServerboundBadBeaconPacket::new).consumer(ServerboundBadBeaconPacket.Handler::handle).add();
    }
}
