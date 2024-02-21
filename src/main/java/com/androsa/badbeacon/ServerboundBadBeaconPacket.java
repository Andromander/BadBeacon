package com.androsa.badbeacon;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.PlayPayloadContext;

import java.util.Optional;

public record ServerboundBadBeaconPacket(Optional<MobEffect> primaryEffect, Optional<MobEffect> secondaryEffect) implements CustomPacketPayload {

    public static final ResourceLocation ID = new ResourceLocation(BadBeaconMod.MODID, "update_bad_beacon");

    public ServerboundBadBeaconPacket(FriendlyByteBuf buf) {
        this(buf.readOptional(reader -> reader.readById(BuiltInRegistries.MOB_EFFECT)), buf.readOptional(reader -> reader.readById(BuiltInRegistries.MOB_EFFECT)));
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeOptional(this.primaryEffect, (bb, effect) -> bb.writeId(BuiltInRegistries.MOB_EFFECT, effect));
        buf.writeOptional(this.secondaryEffect, (bb, effect) -> bb.writeId(BuiltInRegistries.MOB_EFFECT, effect));
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    public static void handle(final ServerboundBadBeaconPacket packet, PlayPayloadContext context) {
        context.workHandler().execute(() -> {
            Optional<Player> optional = context.player();
            if (optional.isPresent()) {
                Player player = optional.get();
                if (player.containerMenu instanceof BadBeaconMenu) {
                    if (!player.containerMenu.stillValid(player)) {
                        return;
                    }
                    ((BadBeaconMenu)player.containerMenu).handleSlots(packet.primaryEffect(), packet.secondaryEffect());
                }
            }
        });
    }
}
