package com.androsa.badbeacon;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Optional;

public record ServerboundBadBeaconPacket(Optional<Holder<MobEffect>> primaryEffect, Optional<Holder<MobEffect>> secondaryEffect) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ServerboundBadBeaconPacket> ID = new Type<>(ResourceLocation.fromNamespaceAndPath(BadBeaconMod.MODID, "update_bad_beacon"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundBadBeaconPacket> CODEC = StreamCodec.composite(
            MobEffect.STREAM_CODEC.apply(ByteBufCodecs::optional),
            ServerboundBadBeaconPacket::primaryEffect,
            MobEffect.STREAM_CODEC.apply(ByteBufCodecs::optional),
            ServerboundBadBeaconPacket::secondaryEffect,
            ServerboundBadBeaconPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    public static void handle(final ServerboundBadBeaconPacket packet, IPayloadContext context) {
        AbstractContainerMenu menu = context.player().containerMenu;
        if (menu instanceof BadBeaconMenu beacon) {
            if (!menu.stillValid(context.player())) {
                return;
            }
            beacon.handleSlots(packet.primaryEffect(), packet.secondaryEffect());
        }
    }
}
