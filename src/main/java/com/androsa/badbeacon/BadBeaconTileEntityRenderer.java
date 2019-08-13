package com.androsa.badbeacon;

import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.renderer.tileentity.BeaconTileEntityRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityRenderer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;
import java.util.Objects;

@OnlyIn(Dist.CLIENT)
public class BadBeaconTileEntityRenderer extends TileEntityRenderer<BadBeaconTileEntity> {
    private static final ResourceLocation TEXTURE_BEACON_BEAM = new ResourceLocation("textures/entity/beacon_beam.png");

    @Override
    public void render(BadBeaconTileEntity tileEntityIn, double x, double y, double z, float partialTicks, int destroyStage) {
        this.renderBeam(x, y, z, (double)partialTicks, tileEntityIn.getBeamSegments(), Objects.requireNonNull(tileEntityIn.getWorld()).getGameTime());
    }

    private void renderBeam(double x, double y, double z, double partialTicks, List<BadBeaconTileEntity.BeamSegment> segments, long time) {
        GlStateManager.alphaFunc(516, 0.1F);
        this.bindTexture(TEXTURE_BEACON_BEAM);
        GlStateManager.disableFog();
        int i = 0;

        for(int j = 0; j < segments.size(); ++j) {
            BadBeaconTileEntity.BeamSegment beacontileentity$beamsegment = segments.get(j);
            renderSegments(x, y, z, partialTicks, time, i, j == segments.size() - 1 ? 1024 : beacontileentity$beamsegment.getHeight(), beacontileentity$beamsegment.getColors());
            i += beacontileentity$beamsegment.getHeight();
        }

        GlStateManager.enableFog();
    }

    private static void renderSegments(double x, double y, double z, double partialTicks, long time, int segment, int height, float[] colours) {
        BeaconTileEntityRenderer.renderBeamSegment(x, y, z, partialTicks, 1.0D, time, segment, height, colours, 0.2D, 0.25D);
    }

    @Override
    public boolean isGlobalRenderer(BadBeaconTileEntity te) {
        return true;
    }
}
