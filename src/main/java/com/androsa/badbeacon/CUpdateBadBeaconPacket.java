package com.androsa.badbeacon;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public class CUpdateBadBeaconPacket {
    private int primaryEffect;
    private int secondaryEffect;

    public CUpdateBadBeaconPacket(int primaryEffectIn, int secondaryEffectIn) {
        this.primaryEffect = primaryEffectIn;
        this.secondaryEffect = secondaryEffectIn;
    }

    public static void encode(CUpdateBadBeaconPacket message, PacketBuffer buf) {
        buf.writeVarInt(message.primaryEffect);
        buf.writeVarInt(message.secondaryEffect);
    }

    public static CUpdateBadBeaconPacket decode(PacketBuffer buf) {
        return new CUpdateBadBeaconPacket(buf.readVarInt(), buf.readVarInt());
    }

    public int getPrimaryEffect() {
        return this.primaryEffect;
    }

    public int getSecondaryEffect() {
        return this.secondaryEffect;
    }

    public static class Handler {

        public static void handle(final CUpdateBadBeaconPacket packet, final Supplier<NetworkEvent.Context> context) {
            context.get().enqueueWork(() -> {
                PlayerEntity player = context.get().getSender();
                if (player != null) {
                    if (player.openContainer instanceof BadBeaconContainer) {
                        ((BadBeaconContainer)player.openContainer).handleSlots(packet.getPrimaryEffect(), packet.getSecondaryEffect());
                    }
                }

            });
            context.get().setPacketHandled(true);
        }
    }
}
