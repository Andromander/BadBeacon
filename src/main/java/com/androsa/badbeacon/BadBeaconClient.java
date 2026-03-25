package com.androsa.badbeacon;

import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;

public class BadBeaconClient {

    public static void registerBinds() {
        BlockEntityRenderers.register(BadBeaconMod.BAD_BEACON_TILEENTITY.get(), context -> new BeaconRenderer<>());
    }
}
