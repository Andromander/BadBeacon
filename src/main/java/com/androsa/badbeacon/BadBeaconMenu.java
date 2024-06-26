package com.androsa.badbeacon;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.Optional;

public class BadBeaconMenu extends AbstractContainerMenu {
    private final Container tileBeacon = new SimpleContainer(1) {
        public boolean canPlaceItem(int index, ItemStack stack) {
            return stack.is(BadBeaconMod.BAD_BEACON_PAYMENT);
        }

        public int getMaxStackSize() {
            return 1;
        }
    };
    private final BadBeaconMenu.BeaconSlot beaconSlot;
    private final ContainerLevelAccess access;
    private final ContainerData data;

    public BadBeaconMenu(int id, Container inventory) {
        this(id, inventory, new SimpleContainerData(3), ContainerLevelAccess.NULL);
    }

    public BadBeaconMenu(int id, Container inventory, ContainerData array, ContainerLevelAccess access) {
        super(BadBeaconMod.BAD_BEACON_CONTAINER.get(), id);
        checkContainerDataCount(array, 3);
        this.data = array;
        this.access = access;
        this.beaconSlot = new BeaconSlot(this.tileBeacon, 0, 136, 110);
        this.addSlot(this.beaconSlot);
        this.addDataSlots(array);

        for(int k = 0; k < 3; ++k) {
            for(int l = 0; l < 9; ++l) {
                this.addSlot(new Slot(inventory, l + k * 9 + 9, 36 + l * 18, 137 + k * 18));
            }
        }

        for(int i1 = 0; i1 < 9; ++i1) {
            this.addSlot(new Slot(inventory, i1, 36 + i1 * 18, 195));
        }
    }

    @Override
    public void removed(Player playerIn) {
        super.removed(playerIn);
        if (!playerIn.level().isClientSide) {
            ItemStack itemstack = this.beaconSlot.remove(this.beaconSlot.getSlotStackLimit());
            if (!itemstack.isEmpty()) {
                playerIn.drop(itemstack, false);
            }
        }
    }

    @Override
    public boolean stillValid(Player playerIn) {
        return stillValid(this.access, playerIn, BadBeaconMod.BAD_BEACON.get());
    }

    @Override
    public void setData(int id, int data) {
        super.setData(id, data);
        this.broadcastChanges();
    }

    @Override
    public ItemStack quickMoveStack(Player playerIn, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();
            if (index == 0) {
                if (!this.moveItemStackTo(itemstack1, 1, 37, true)) {
                    return ItemStack.EMPTY;
                }

                slot.onQuickCraft(itemstack1, itemstack);
            } else if (this.moveItemStackTo(itemstack1, 0, 1, false)) {
                return ItemStack.EMPTY;
            } else if (index < 28) {
                if (!this.moveItemStackTo(itemstack1, 28, 37, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (index < 37) {
                if (!this.moveItemStackTo(itemstack1, 1, 28, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(itemstack1, 1, 37, false)) {
                return ItemStack.EMPTY;
            }

            if (itemstack1.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (itemstack1.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(playerIn, itemstack1);
        }
        return itemstack;
    }

    public int getLevels() {
        return this.data.get(0);
    }

    public static int encodeEffect(Holder<MobEffect> effect) {
        return effect == null ? 0 : BuiltInRegistries.MOB_EFFECT.asHolderIdMap().getId(effect) + 1;
    }

    public static Holder<MobEffect> decodeEffect(int id) {
        return id == 0 ? null : BuiltInRegistries.MOB_EFFECT.asHolderIdMap().byId(id - 1);
    }

    @Nullable
    public Holder<MobEffect> getPrimaryEffect() {
        return decodeEffect(this.data.get(1));
    }

    @Nullable
    public Holder<MobEffect> getSecondaryEffect() {
        return decodeEffect(this.data.get(2));
    }

    public void handleSlots(Optional<Holder<MobEffect>> primary, Optional<Holder<MobEffect>> secondary) {
        if (this.beaconSlot.hasItem()) {
            this.data.set(1, encodeEffect(primary.orElse(null)));
            this.data.set(2, encodeEffect(secondary.orElse(null)));
            this.beaconSlot.remove(1);
        }
    }

    public boolean isActive() {
        return !this.tileBeacon.getItem(0).isEmpty();
    }

    static class BeaconSlot extends Slot {
        public BeaconSlot(Container inventoryIn, int index, int xIn, int yIn) {
            super(inventoryIn, index, xIn, yIn);
        }

        public boolean mayPlace(ItemStack stack) {
            return stack.is(BadBeaconMod.BAD_BEACON_PAYMENT);
        }

        public int getSlotStackLimit() {
            return 1;
        }
    }
}
