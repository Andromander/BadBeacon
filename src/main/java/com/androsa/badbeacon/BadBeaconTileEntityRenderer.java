package com.androsa.badbeacon;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.tileentity.BeaconTileEntityRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;

@OnlyIn(Dist.CLIENT)
public class BadBeaconTileEntityRenderer extends TileEntityRenderer<BadBeaconTileEntity> {
    private static final ResourceLocation TEXTURE_BEACON_BEAM = new ResourceLocation("textures/entity/beacon_beam.png");

    public BadBeaconTileEntityRenderer(TileEntityRendererDispatcher dispatch) {
        super(dispatch);
    }

    @Override
    public void render(BadBeaconTileEntity tileEntityIn, float partialTicks, MatrixStack stack, IRenderTypeBuffer buffer, int light, int destroyStage) {
        long time = tileEntityIn.getWorld().getGameTime();
        List<BadBeaconTileEntity.BeamSegment> segments = tileEntityIn.getBeamSegments();
        int height = 0;

        for(int segment = 0; segment < segments.size(); ++segment) {
            BadBeaconTileEntity.BeamSegment beacontileentity$beamsegment = segments.get(segment);
            renderSegments(stack, buffer, partialTicks, time, height, segment == segments.size() - 1 ? 1024 : beacontileentity$beamsegment.getHeight(), beacontileentity$beamsegment.getColors());
            height += beacontileentity$beamsegment.getHeight();
        }
    }

    private static void renderSegments(MatrixStack matrix, IRenderTypeBuffer buffer, float partialTicks, long time, int height, int segment, float[] colours) {
        BeaconTileEntityRenderer.renderBeamSegment(matrix, buffer, TEXTURE_BEACON_BEAM, partialTicks, 1.0F, time, height, segment, colours, 0.2F, 0.25F);
    }

    @Override
    public boolean isGlobalRenderer(BadBeaconTileEntity te) {
        return true;
    }
}
