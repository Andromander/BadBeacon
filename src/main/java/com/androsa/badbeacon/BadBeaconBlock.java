package com.androsa.badbeacon;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BeaconBeamBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;

public class BadBeaconBlock extends Block implements EntityBlock, BeaconBeamBlock {

    public BadBeaconBlock() {
        super(Properties.of().mapColor(MapColor.COLOR_BLUE).instrument(NoteBlockInstrument.SNARE).strength(0.3F).lightLevel((val) -> 15).noOcclusion().isRedstoneConductor((state, get, pos) -> false));
    }

    @Override
    public DyeColor getColor() {
        return DyeColor.BLACK;
    }

    @Override
    @Deprecated
    public boolean triggerEvent(BlockState state, Level worldIn, BlockPos pos, int id, int param) {
        super.triggerEvent(state, worldIn, pos, id, param);
        BlockEntity tileentity = worldIn.getBlockEntity(pos);
        return tileentity != null && tileentity.triggerEvent(id, param);
    }

    @Override
    @Nullable
    @Deprecated
    public MenuProvider getMenuProvider(BlockState state, Level worldIn, BlockPos pos) {
        BlockEntity tileentity = worldIn.getBlockEntity(pos);
        return tileentity instanceof MenuProvider ? (MenuProvider)tileentity : null;
    }

    @Override
    @Deprecated
    public InteractionResult use(BlockState state, Level worldIn, BlockPos pos, Player player, InteractionHand handIn, BlockHitResult hit) {
        if (!worldIn.isClientSide) {
            BlockEntity tileentity = worldIn.getBlockEntity(pos);
            if (tileentity instanceof BadBeaconBlockEntity) {
                player.openMenu((BadBeaconBlockEntity) tileentity);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void setPlacedBy(Level worldIn, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        if (stack.hasCustomHoverName()) {
            BlockEntity tileentity = worldIn.getBlockEntity(pos);
            if (tileentity instanceof BadBeaconBlockEntity) {
                ((BadBeaconBlockEntity)tileentity).setCustomName(stack.getDisplayName());
            }
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BadBeaconBlockEntity(pos, state);
    }

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return BadBeaconMod.BAD_BEACON_TILEENTITY.get() == type ? getBlockTicker(BadBeaconBlockEntity::tick) : null;
	}

	private static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> getBlockTicker(BlockEntityTicker<? super E> ticker) {
		return (BlockEntityTicker<A>)ticker;
	}
}
