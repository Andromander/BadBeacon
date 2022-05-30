package com.androsa.badbeacon;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.LockCode;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class BadBeaconBlockEntity extends BlockEntity implements MenuProvider {

    public static final MobEffect[][] EFFECTS_LIST = new MobEffect[][]{
            {MobEffects.MOVEMENT_SLOWDOWN, MobEffects.DIG_SLOWDOWN},
            {MobEffects.UNLUCK, MobEffects.BLINDNESS},
            {MobEffects.WEAKNESS},
            {MobEffects.POISON}};
    private static final Set<MobEffect> VALID_EFFECTS = Arrays.stream(EFFECTS_LIST).flatMap(Arrays::stream).collect(Collectors.toSet());
    private List<BadBeaconBlockEntity.BeamSegment> beamSegments = Lists.newArrayList();
    private List<BadBeaconBlockEntity.BeamSegment> checkSegments = Lists.newArrayList();
    private int levels;
    private int lastY = -1;
    private MobEffect primaryEffect;
    private MobEffect secondaryEffect;
    private Component customName;
    private LockCode lock = LockCode.NO_LOCK;
    private final ContainerData dataAccess = new ContainerData() {
    	@Override
        public int get(int index) {
			return switch (index) {
				case 0 -> BadBeaconBlockEntity.this.levels;
				case 1 -> MobEffect.getId(BadBeaconBlockEntity.this.primaryEffect);
				case 2 -> MobEffect.getId(BadBeaconBlockEntity.this.secondaryEffect);
				default -> 0;
			};
        }
        @Override
        public void set(int index, int value) {
			switch (index) {
				case 0 -> BadBeaconBlockEntity.this.levels = value;
				case 1 -> {
					if (!BadBeaconBlockEntity.this.level.isClientSide && !BadBeaconBlockEntity.this.beamSegments.isEmpty()) {
						BadBeaconBlockEntity.playSound(BadBeaconBlockEntity.this.level, BadBeaconBlockEntity.this.worldPosition, SoundEvents.BEACON_POWER_SELECT);
					}
					BadBeaconBlockEntity.this.primaryEffect = BadBeaconBlockEntity.isBeaconEffect(value);
				}
				case 2 -> BadBeaconBlockEntity.this.secondaryEffect = BadBeaconBlockEntity.isBeaconEffect(value);
			}

        }
        public int getCount() {
            return 3;
        }
    };

    public BadBeaconBlockEntity(BlockPos pos, BlockState state) {
        super(BadBeaconMod.BAD_BEACON_TILEENTITY.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, BadBeaconBlockEntity entity) {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        BlockPos blockpos;
        if (entity.lastY < y) {
            blockpos = pos;
            entity.checkSegments = Lists.newArrayList();
            entity.lastY = blockpos.getY() - 1;
        } else {
            blockpos = new BlockPos(x, entity.lastY + 1, z);
        }

        BadBeaconBlockEntity.BeamSegment beaconBeamSegment = entity.checkSegments.isEmpty() ? null : entity.checkSegments.get(entity.checkSegments.size() - 1);
        int l = level != null ? level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) : 0;

        for(int i1 = 0; i1 < 10 && blockpos.getY() <= l; ++i1) {
            BlockState blockstate = level.getBlockState(blockpos);
            float[] colmul = blockstate.getBeaconColorMultiplier(level, blockpos, pos);
            if (colmul != null) {
                if (entity.checkSegments.size() <= 1) {
                    beaconBeamSegment = new BadBeaconBlockEntity.BeamSegment(colmul);
                    entity.checkSegments.add(beaconBeamSegment);
                } else if (beaconBeamSegment != null) {
                    if (Arrays.equals(colmul, beaconBeamSegment.colors)) {
                        beaconBeamSegment.incrementHeight();
                    } else {
                        beaconBeamSegment = new BadBeaconBlockEntity.BeamSegment(new float[]{(beaconBeamSegment.colors[0] + colmul[0]) / 2.0F, (beaconBeamSegment.colors[1] + colmul[1]) / 2.0F, (beaconBeamSegment.colors[2] + colmul[2]) / 2.0F});
                        entity.checkSegments.add(beaconBeamSegment);
                    }
                }
            } else {
                if (beaconBeamSegment == null || blockstate.getLightBlock(level, blockpos) >= 15 && !blockstate.is(Blocks.BEDROCK)) {
                    entity.checkSegments.clear();
                    entity.lastY = l;
                    break;
                }

                beaconBeamSegment.incrementHeight();
            }

            blockpos = blockpos.above();
            ++entity.lastY;
        }

        int j1 = entity.levels;
        if (level.getGameTime() % 80L == 0L) {
            if (!entity.beamSegments.isEmpty()) {
                entity.levels = sendBeam(level, x, y, z);
            }

            if (entity.levels > 0 && !entity.beamSegments.isEmpty()) {
                applyEffects(level, pos, entity.levels, entity.primaryEffect, entity.secondaryEffect);
                playSound(level, pos, SoundEvents.BEACON_AMBIENT);
            }
        }

        if (entity.lastY >= l) {
            entity.lastY = level.getMinBuildHeight() - 1;
            boolean flag = j1 > 0;
            entity.beamSegments = entity.checkSegments;
            if (!level.isClientSide) {
                boolean flag1 = entity.levels > 0;
                if (!flag && flag1) {
                    playSound(level, pos, SoundEvents.BEACON_ACTIVATE);
                } else if (flag && !flag1) {
                    playSound(level, pos, SoundEvents.BEACON_DEACTIVATE);
                }
            }
        }
    }

    private static int sendBeam(Level level, int posX, int posY, int posZ) {
        int levels = 0;

        for(int i = 1; i <= 4; levels = i++) {
            int j = posY - i;
            if (j < level.getMinBuildHeight()) {
                break;
            }

            boolean flag = true;

            for(int k = posX - i; k <= posX + i && flag; ++k) {
                for(int l = posZ - i; l <= posZ + i; ++l) {
                    if (!level.getBlockState(new BlockPos(k, j, l)).is(BadBeaconMod.BAD_BEACON_BASE)) {
                        flag = false;
                        break;
                    }
                }
            }

            if (!flag) {
                break;
            }
        }

        return levels;
    }

    @Override
    public void setRemoved() {
        playSound(this.level, this.worldPosition, SoundEvents.BEACON_DEACTIVATE);
        super.setRemoved();
    }

    private static void applyEffects(Level level, BlockPos pos, int beaconlevel, MobEffect primary, MobEffect secondary) {
        if (!level.isClientSide && primary != null) {
            double size = beaconlevel * 10 + 10;
            int mul = 0;
            if (beaconlevel >= 4 && primary == secondary) {
                mul = 1;
            }

            int j = (9 + beaconlevel * 2) * 20;
            AABB area = (new AABB(pos)).inflate(size).expandTowards(0.0D, level.getHeight(), 0.0D);

            if (BadBeaconMod.BadBeaconConfig.affectAllEntities.get()) {
                List<LivingEntity> list = level.getEntitiesOfClass(LivingEntity.class, area);

                for(LivingEntity entity : list) {
                    entity.addEffect(new MobEffectInstance(primary, j, mul, true, true));
                }

                if (beaconlevel >= 4 && primary != secondary && secondary != null) {
                    for(LivingEntity entity1 : list) {
                        entity1.addEffect(new MobEffectInstance(secondary, j, 0, true, true));
                    }
                }
            } else {
                List<Player> list = level.getEntitiesOfClass(Player.class, area);

                for(Player player : list) {
                    player.addEffect(new MobEffectInstance(primary, j, mul, true, true));
                }

                if (beaconlevel >= 4 && primary != secondary && secondary != null) {
                    for(Player player1 : list) {
                        player1.addEffect(new MobEffectInstance(secondary, j, 0, true, true));
                    }
                }
            }
        }
    }

    public static void playSound(Level level, BlockPos pos, SoundEvent event) {
        if (level != null) {
            level.playSound(null, pos, event, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public List<BadBeaconBlockEntity.BeamSegment> getBeamSegments() {
        return this.levels == 0 ? ImmutableList.of() : this.beamSegments;
    }

    @Override
    @Nullable
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag() {
        return this.saveWithoutMetadata();
    }

    @Nullable
    private static MobEffect isBeaconEffect(int value) {
        MobEffect effect = MobEffect.byId(value);
        return VALID_EFFECTS.contains(effect) ? effect : null;
    }

    @Override
    public void load(CompoundTag compound) {
        super.load(compound);
        this.primaryEffect = isBeaconEffect(compound.getInt("Primary"));
        this.secondaryEffect = isBeaconEffect(compound.getInt("Secondary"));
        if (compound.contains("CustomName", 8)) {
            this.customName = Component.Serializer.fromJson(compound.getString("CustomName"));
        }

        this.lock = LockCode.fromTag(compound);
    }

    @Override
    public void saveAdditional(CompoundTag compound) {
        super.saveAdditional(compound);
        compound.putInt("Primary", MobEffect.getId(this.primaryEffect));
        compound.putInt("Secondary", MobEffect.getId(this.secondaryEffect));
        compound.putInt("Levels", this.levels);
        if (this.customName != null) {
            compound.putString("CustomName", Component.Serializer.toJson(this.customName));
        }

        this.lock.addToTag(compound);
    }

    public void setCustomName(@Nullable Component aname) {
        this.customName = aname;
    }

    @Override
    @Nullable
    public AbstractContainerMenu createMenu(int id, Inventory playerInv, Player player) {
        return BaseContainerBlockEntity.canUnlock(player, this.lock, this.getDisplayName()) ? new BadBeaconMenu(id, playerInv, this.dataAccess, ContainerLevelAccess.create(this.level, this.getBlockPos())) : null;
    }

    @Override
    public Component getDisplayName() {
        return this.customName != null ? this.customName : new TranslatableComponent("badbeacon.container.bad_beacon");
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
