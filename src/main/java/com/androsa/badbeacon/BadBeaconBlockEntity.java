package com.androsa.badbeacon;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ARGB;
import net.minecraft.world.LockCode;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.Nameable;
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
import net.minecraft.world.level.block.entity.BeaconBeamOwner;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class BadBeaconBlockEntity extends BlockEntity implements MenuProvider, Nameable, BeaconBeamOwner {

    public static final List<List<Holder<MobEffect>>> EFFECTS_LIST = List.of(
            List.of(MobEffects.SLOWNESS, MobEffects.MINING_FATIGUE),
            List.of(MobEffects.UNLUCK, MobEffects.BLINDNESS),
            List.of(MobEffects.WEAKNESS),
            List.of(MobEffects.POISON));
    private static final Set<Holder<MobEffect>> VALID_EFFECTS = EFFECTS_LIST.stream().flatMap(Collection::stream).collect(Collectors.toSet());
    private List<Section> beamSegments = Lists.newArrayList();
    private List<Section> checkSegments = Lists.newArrayList();
    private int levels;
    private int lastY = -1;
    private Holder<MobEffect> primaryEffect;
    private Holder<MobEffect> secondaryEffect;
    private Component customName;
    private LockCode lock = LockCode.NO_LOCK;
    private final ContainerData dataAccess = new ContainerData() {
    	@Override
        public int get(int index) {
			return switch (index) {
				case 0 -> BadBeaconBlockEntity.this.levels;
				case 1 -> BadBeaconMenu.encodeEffect(BadBeaconBlockEntity.this.primaryEffect);
				case 2 -> BadBeaconMenu.encodeEffect(BadBeaconBlockEntity.this.secondaryEffect);
				default -> 0;
			};
        }
        @Override
        public void set(int index, int value) {
			switch (index) {
                case 0:
                    BadBeaconBlockEntity.this.levels = value;
                    break;
                case 1:
					if (!BadBeaconBlockEntity.this.level.isClientSide() && !BadBeaconBlockEntity.this.beamSegments.isEmpty()) {
						BadBeaconBlockEntity.playSound(BadBeaconBlockEntity.this.level, BadBeaconBlockEntity.this.worldPosition, SoundEvents.BEACON_POWER_SELECT);
					}
					BadBeaconBlockEntity.this.primaryEffect = BadBeaconBlockEntity.isMobEffect(BadBeaconMenu.decodeEffect(value));
                    break;
                case 2:
                    BadBeaconBlockEntity.this.secondaryEffect = BadBeaconBlockEntity.isMobEffect(BadBeaconMenu.decodeEffect(value));
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

        Section beaconBeamSegment = entity.checkSegments.isEmpty() ? null : entity.checkSegments.get(entity.checkSegments.size() - 1);
        int l = level != null ? level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) : 0;

        for(int i1 = 0; i1 < 10 && blockpos.getY() <= l; ++i1) {
            BlockState blockstate = level.getBlockState(blockpos);
            Integer colmul = blockstate.getBeaconColorMultiplier(level, blockpos, pos);
            if (colmul != null) {
                if (entity.checkSegments.size() <= 1) {
                    beaconBeamSegment = new Section(colmul);
                    entity.checkSegments.add(beaconBeamSegment);
                } else if (beaconBeamSegment != null) {
                    if (colmul == beaconBeamSegment.getColor()) {
                        beaconBeamSegment.increaseHeight();
                    } else {
                        beaconBeamSegment = new Section(ARGB.average(beaconBeamSegment.getColor(), colmul));
                        entity.checkSegments.add(beaconBeamSegment);
                    }
                }
            } else {
                if (beaconBeamSegment == null || blockstate.getLightBlock() >= 15 && !blockstate.is(Blocks.BEDROCK)) {
                    entity.checkSegments.clear();
                    entity.lastY = l;
                    break;
                }

                beaconBeamSegment.increaseHeight();
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
            entity.lastY = level.getMinY() - 1;
            boolean flag = j1 > 0;
            entity.beamSegments = entity.checkSegments;
            if (!level.isClientSide()) {
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
            if (j < level.getMinY()) {
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

    private static void applyEffects(Level level, BlockPos pos, int beaconlevel, Holder<MobEffect> primary, Holder<MobEffect> secondary) {
        if (!level.isClientSide() && primary != null) {
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

    public List<Section> getBeamSections() {
        return this.levels == 0 ? ImmutableList.of() : this.beamSegments;
    }

    @Override
    @Nullable
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        return this.saveCustomOnly(provider);
    }

    @Nullable
    private static Holder<MobEffect> isMobEffect(Holder<MobEffect> value) {
        return VALID_EFFECTS.contains(value) ? value : null;
    }

    @Nullable
    private static Holder<MobEffect> loadEffect(ValueInput tag, String name) {
        return tag.read(name, BuiltInRegistries.MOB_EFFECT.holderByNameCodec()).filter(VALID_EFFECTS::contains).orElse(null);
    }

    private static void saveEffect(ValueOutput tag, String name, Holder<MobEffect> effect) {
        if (effect != null) {
            effect.unwrapKey().ifPresent(key -> tag.putString(name, key.identifier().toString()));
        }
    }

    @Override
    public void loadAdditional(ValueInput compound) {
        super.loadAdditional(compound);
        this.primaryEffect = loadEffect(compound, "Primary");
        this.secondaryEffect = loadEffect(compound, "Secondary");
        this.customName = parseCustomNameSafe(compound, "CustomName");
        this.lock = LockCode.fromTag(compound);
    }

    @Override
    public void saveAdditional(ValueOutput compound) {
        super.saveAdditional(compound);
        saveEffect(compound, "Primary", this.primaryEffect);
        saveEffect(compound, "Secondary", this.secondaryEffect);
        compound.putInt("Levels", this.levels);
        compound.storeNullable("CustomName", ComponentSerialization.CODEC, this.customName);
        this.lock.addToTag(compound);
    }

    public void setCustomName(@Nullable Component aname) {
        this.customName = aname;
    }

    public Component getCustomName() {
        return this.customName;
    }

    @Override
    @Nullable
    public AbstractContainerMenu createMenu(int id, Inventory playerInv, Player player) {
        if (this.lock.canUnlock(player)) {
            return new BadBeaconMenu(id, playerInv, this.dataAccess, ContainerLevelAccess.create(this.level, this.getBlockPos()));
        } else {
            BaseContainerBlockEntity.sendChestLockedNotifications(this.getBlockPos().getCenter(), player, this.getDisplayName());
            return null;
        }
    }

    @Override
    public Component getDisplayName() {
        return this.getName();
    }

    @Override
    public Component getName() {
        return this.customName != null ? this.customName : Component.translatable("badbeacon.container.bad_beacon");
    }
}
