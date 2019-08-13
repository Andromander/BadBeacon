package com.androsa.badbeacon;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.play.server.SUpdateTileEntityPacket;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.LockableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.LockCode;
import net.minecraft.world.gen.Heightmap;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class BadBeaconTileEntity extends TileEntity implements INamedContainerProvider, ITickableTileEntity {

    public static final Effect[][] EFFECTS_LIST = new Effect[][]{
            {Effects.SLOWNESS, Effects.MINING_FATIGUE},
            {Effects.UNLUCK, Effects.BLINDNESS},
            {Effects.WEAKNESS},
            {Effects.POISON}};
    private static final Set<Effect> VALID_EFFECTS = Arrays.stream(EFFECTS_LIST).flatMap(Arrays::stream).collect(Collectors.toSet());
    private List<BadBeaconTileEntity.BeamSegment> beamSegments = Lists.newArrayList();
    private List<BadBeaconTileEntity.BeamSegment> segmentSize = Lists.newArrayList();
    private int levels = 0;
    private int beamHeight = -1;
    private Effect primaryEffect;
    private Effect secondaryEffect;
    private ITextComponent customName;
    private LockCode lock = LockCode.EMPTY_CODE;
    private final IIntArray intArray = new IIntArray() {
        public int get(int index) {
            switch(index) {
                case 0:
                    return BadBeaconTileEntity.this.levels;
                case 1:
                    return Effect.getId(BadBeaconTileEntity.this.primaryEffect);
                case 2:
                    return Effect.getId(BadBeaconTileEntity.this.secondaryEffect);
                default:
                    return 0;
            }
        }
        public void set(int index, int value) {
            switch(index) {
                case 0:
                    BadBeaconTileEntity.this.levels = value;
                    break;
                case 1:
                    if (!(BadBeaconTileEntity.this.world != null && BadBeaconTileEntity.this.world.isRemote) && !BadBeaconTileEntity.this.beamSegments.isEmpty()) {
                        BadBeaconTileEntity.this.playSound(SoundEvents.BLOCK_BEACON_POWER_SELECT);
                    }

                    BadBeaconTileEntity.this.primaryEffect = BadBeaconTileEntity.isBeaconEffect(value);
                    break;
                case 2:
                    BadBeaconTileEntity.this.secondaryEffect = BadBeaconTileEntity.isBeaconEffect(value);
            }

        }
        public int size() {
            return 3;
        }
    };

    public BadBeaconTileEntity() {
        super(BadBeaconMod.BAD_BEACON_TILEENTITY);
    }

    @Override
    public void tick() {
        int x = this.pos.getX();
        int y = this.pos.getY();
        int z = this.pos.getZ();
        BlockPos blockpos;
        if (this.beamHeight < y) {
            blockpos = this.pos;
            this.segmentSize = Lists.newArrayList();
            this.beamHeight = blockpos.getY() - 1;
        } else {
            blockpos = new BlockPos(x, this.beamHeight + 1, z);
        }

        BadBeaconTileEntity.BeamSegment beaconBeamSegment = this.segmentSize.isEmpty() ? null : this.segmentSize.get(this.segmentSize.size() - 1);
        int l = this.world != null ? this.world.getHeight(Heightmap.Type.WORLD_SURFACE, x, z) : 0;

        for(int i1 = 0; i1 < 10 && blockpos.getY() <= l; ++i1) {
            BlockState blockstate = this.world.getBlockState(blockpos);
            Block block = blockstate.getBlock();
            float[] afloat = blockstate.getBeaconColorMultiplier(this.world, blockpos, getPos());
            if (afloat != null) {
                if (this.segmentSize.size() <= 1) {
                    beaconBeamSegment = new BadBeaconTileEntity.BeamSegment(afloat);
                    this.segmentSize.add(beaconBeamSegment);
                } else if (beaconBeamSegment != null) {
                    if (Arrays.equals(afloat, beaconBeamSegment.colors)) {
                        beaconBeamSegment.incrementHeight();
                    } else {
                        beaconBeamSegment = new BadBeaconTileEntity.BeamSegment(new float[]{(beaconBeamSegment.colors[0] + afloat[0]) / 2.0F, (beaconBeamSegment.colors[1] + afloat[1]) / 2.0F, (beaconBeamSegment.colors[2] + afloat[2]) / 2.0F});
                        this.segmentSize.add(beaconBeamSegment);
                    }
                }
            } else {
                if (beaconBeamSegment == null || blockstate.getOpacity(this.world, blockpos) >= 15 && block != Blocks.BEDROCK) {
                    this.segmentSize.clear();
                    this.beamHeight = l;
                    break;
                }

                beaconBeamSegment.incrementHeight();
            }

            blockpos = blockpos.up();
            ++this.beamHeight;
        }

        int j1 = this.levels;
        if (this.world.getGameTime() % 80L == 0L) {
            if (!this.beamSegments.isEmpty()) {
                this.sendBeam(x, y, z);
            }

            if (this.levels > 0 && !this.beamSegments.isEmpty()) {
                this.addEffectsToEntities();
                this.playSound(SoundEvents.BLOCK_BEACON_AMBIENT);
            }
        }

        if (this.beamHeight >= l) {
            this.beamHeight = -1;
            boolean flag = j1 > 0;
            this.beamSegments = this.segmentSize;
            if (!this.world.isRemote) {
                boolean flag1 = this.levels > 0;
                if (!flag && flag1) {
                    this.playSound(SoundEvents.BLOCK_BEACON_ACTIVATE);
                } else if (flag && !flag1) {
                    this.playSound(SoundEvents.BLOCK_BEACON_DEACTIVATE);
                }
            }
        }
    }

    private void sendBeam(int posX, int posY, int posZ) {
        this.levels = 0;

        for(int i = 1; i <= 4; this.levels = i++) {
            int j = posY - i;
            if (j < 0) {
                break;
            }

            boolean flag = true;

            for(int k = posX - i; k <= posX + i && flag; ++k) {
                for(int l = posZ - i; l <= posZ + i; ++l) {
                    if (!this.world.getBlockState(new BlockPos(k, j, l)).getBlock().isIn(BadBeaconMod.BAD_BEACON_BASE)) {
                        flag = false;
                        break;
                    }
                }
            }

            if (!flag) {
                break;
            }
        }
    }

    @Override
    public void remove() {
        this.playSound(SoundEvents.BLOCK_BEACON_DEACTIVATE);
        super.remove();
    }

    private void addEffectsToEntities() {
        if (!(this.world != null && this.world.isRemote) && this.primaryEffect != null) {
            double d0 = (double)(this.levels * 10 + 10);
            int i = 0;
            if (this.levels >= 4 && this.primaryEffect == this.secondaryEffect) {
                i = 1;
            }

            int j = (9 + this.levels * 2) * 20;
            AxisAlignedBB axisalignedbb = (new AxisAlignedBB(this.pos)).grow(d0).expand(0.0D, (double)this.world.getHeight(), 0.0D);

            if (BadBeaconMod.BadBeaconConfig.affectAllEntities.get()) {
                List<LivingEntity> list = this.world.getEntitiesWithinAABB(LivingEntity.class, axisalignedbb);

                for(LivingEntity entity : list) {
                    entity.addPotionEffect(new EffectInstance(this.primaryEffect, j, i, true, true));
                }

                if (this.levels >= 4 && this.primaryEffect != this.secondaryEffect && this.secondaryEffect != null) {
                    for(LivingEntity entity1 : list) {
                        entity1.addPotionEffect(new EffectInstance(this.secondaryEffect, j, 0, true, true));
                    }
                }
            } else {
                List<PlayerEntity> list = this.world.getEntitiesWithinAABB(PlayerEntity.class, axisalignedbb);

                for(PlayerEntity playerentity : list) {
                    playerentity.addPotionEffect(new EffectInstance(this.primaryEffect, j, i, true, true));
                }

                if (this.levels >= 4 && this.primaryEffect != this.secondaryEffect && this.secondaryEffect != null) {
                    for(PlayerEntity playerentity1 : list) {
                        playerentity1.addPotionEffect(new EffectInstance(this.secondaryEffect, j, 0, true, true));
                    }
                }
            }
        }
    }

    public void playSound(SoundEvent event) {
        if (this.world != null) {
            this.world.playSound(null, this.pos, event, SoundCategory.BLOCKS, 1.0F, 1.0F);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public List<BadBeaconTileEntity.BeamSegment> getBeamSegments() {
        return this.levels == 0 ? ImmutableList.of() : this.beamSegments;
    }

    @Override
    @Nullable
    public SUpdateTileEntityPacket getUpdatePacket() {
        return new SUpdateTileEntityPacket(this.pos, 3, this.getUpdateTag());
    }

    @Override
    public CompoundNBT getUpdateTag() {
        return this.write(new CompoundNBT());
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public double getMaxRenderDistanceSquared() {
        return 65536.0D;
    }

    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        return INFINITE_EXTENT_AABB;
    }

    @Nullable
    private static Effect isBeaconEffect(int value) {
        Effect effect = Effect.get(value);
        return VALID_EFFECTS.contains(effect) ? effect : null;
    }

    @Override
    public void read(CompoundNBT compound) {
        super.read(compound);
        this.primaryEffect = isBeaconEffect(compound.getInt("Primary"));
        this.secondaryEffect = isBeaconEffect(compound.getInt("Secondary"));
        if (compound.contains("CustomName", 8)) {
            this.customName = ITextComponent.Serializer.fromJson(compound.getString("CustomName"));
        }

        this.lock = LockCode.read(compound);
    }

    @Override
    public CompoundNBT write(CompoundNBT compound) {
        super.write(compound);
        compound.putInt("Primary", Effect.getId(this.primaryEffect));
        compound.putInt("Secondary", Effect.getId(this.secondaryEffect));
        compound.putInt("Levels", this.levels);
        if (this.customName != null) {
            compound.putString("CustomName", ITextComponent.Serializer.toJson(this.customName));
        }

        this.lock.write(compound);
        return compound;
    }

    public void setCustomName(@Nullable ITextComponent aname) {
        this.customName = aname;
    }

    @Override
    @Nullable
    public Container createMenu(int id, PlayerInventory playerInv, PlayerEntity player) {
        return LockableTileEntity.canUnlock(player, this.lock, this.getDisplayName()) ? new BadBeaconContainer(id, playerInv, this.intArray, IWorldPosCallable.of(Objects.requireNonNull(this.world), this.getPos())) : null;
    }

    @Override
    public ITextComponent getDisplayName() {
        return this.customName != null ? this.customName : new TranslationTextComponent("badbeacon.container.beacon");
    }

    public static class BeamSegment {
        private final float[] colors;
        private int height;

        public BeamSegment(float[] colorsIn) {
            this.colors = colorsIn;
            this.height = 1;
        }

        protected void incrementHeight() {
            ++this.height;
        }

        /**
         * Returns RGB (0 to 1.0) colors of this beam segment
         */
        @OnlyIn(Dist.CLIENT)
        public float[] getColors() {
            return this.colors;
        }

        @OnlyIn(Dist.CLIENT)
        public int getHeight() {
            return this.height;
        }
    }
}
