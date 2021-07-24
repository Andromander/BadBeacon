package com.androsa.badbeacon;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fmllegacy.network.NetworkEvent;

import java.util.function.Supplier;

public class ServerboundBadBeaconPacket {
    private final int primaryEffect;
    private final int secondaryEffect;

    public ServerboundBadBeaconPacket(int primaryEffectIn, int secondaryEffectIn) {
        this.primaryEffect = primaryEffectIn;
        this.secondaryEffect = secondaryEffectIn;
    }

    public ServerboundBadBeaconPacket(FriendlyByteBuf buf) {
        this.primaryEffect = buf.readVarInt();
        this.secondaryEffect = buf.readVarInt();
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(this.primaryEffect);
        buf.writeVarInt(this.secondaryEffect);
    }

	public int getPrimaryEffect() {
        return this.primaryEffect;
    }

    public int getSecondaryEffect() {
        return this.secondaryEffect;
    }

    public static class Handler {

        public static void handle(final ServerboundBadBeaconPacket packet, final Supplier<NetworkEvent.Context> context) {
            context.get().enqueueWork(() -> {
                Player player = context.get().getSender();
                if (player != null) {
                    if (player.containerMenu instanceof BadBeaconMenu) {
                        ((BadBeaconMenu)player.containerMenu).handleSlots(packet.getPrimaryEffect(), packet.getSecondaryEffect());
                    }
                }

            });
            context.get().setPacketHandled(true);
        }
    }
}
