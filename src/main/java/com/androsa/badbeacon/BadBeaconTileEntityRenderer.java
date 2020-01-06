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
    public void func_225616_a_(BadBeaconTileEntity tileEntityIn, float p_225616_2_, MatrixStack p_225616_3_, IRenderTypeBuffer p_225616_4_, int partialTicks, int destroyStage) {
        long i = tileEntityIn.getWorld().getGameTime();
        List<BadBeaconTileEntity.BeamSegment> list = tileEntityIn.getBeamSegments();
        int j = 0;

        for(int k = 0; k < list.size(); ++k) {
            BadBeaconTileEntity.BeamSegment beacontileentity$beamsegment = list.get(k);
            renderSegments(p_225616_3_, p_225616_4_, p_225616_2_, i, j, k == list.size() - 1 ? 1024 : beacontileentity$beamsegment.getHeight(), beacontileentity$beamsegment.getColors());
            j += beacontileentity$beamsegment.getHeight();
        }
    }

    private static void renderSegments(MatrixStack matrix, IRenderTypeBuffer buffer, float p_228841_2_, long p_228841_3_, int p_228841_5_, int p_228841_6_, float[] p_228841_7_) {
        BeaconTileEntityRenderer.func_228842_a_(matrix, buffer, TEXTURE_BEACON_BEAM, p_228841_2_, 1.0F, p_228841_3_, p_228841_5_, p_228841_6_, p_228841_7_, 0.2F, 0.25F);
    }

    @Override
    public boolean isGlobalRenderer(BadBeaconTileEntity te) {
        return true;
    }
}
