package com.androsa.badbeacon;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Effect;
import net.minecraft.util.IIntArray;
import net.minecraft.util.IWorldPosCallable;
import net.minecraft.util.IntArray;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;

public class BadBeaconContainer extends Container {
    private final IInventory tileBeacon = new Inventory(1) {
        public boolean isItemValidForSlot(int index, ItemStack stack) {
            return stack.isBeaconPayment();
        }

        public int getInventoryStackLimit() {
            return 1;
        }
    };
    private final BadBeaconContainer.BeaconSlot beaconSlot;
    private final IWorldPosCallable worldPos;
    private final IIntArray intArray;

    public BadBeaconContainer(int id, IInventory inventory) {
        this(id, inventory, new IntArray(3), IWorldPosCallable.DUMMY);
    }

    public BadBeaconContainer(int id, IInventory inventory, IIntArray array, IWorldPosCallable worldPos) {
        super(BadBeaconMod.BAD_BEACON_CONTAINER.get(), id);
        assertIntArraySize(array, 3);
        this.intArray = array;
        this.worldPos = worldPos;
        this.beaconSlot = new BadBeaconContainer.BeaconSlot(this.tileBeacon, 0, 136, 110);
        this.addSlot(this.beaconSlot);
        this.trackIntArray(array);

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
    public void onContainerClosed(PlayerEntity playerIn) {
        super.onContainerClosed(playerIn);
        if (!playerIn.world.isRemote) {
            ItemStack itemstack = this.beaconSlot.decrStackSize(this.beaconSlot.getSlotStackLimit());
            if (!itemstack.isEmpty()) {
                playerIn.dropItem(itemstack, false);
            }
        }
    }

    @Override
    public boolean canInteractWith(PlayerEntity playerIn) {
        return isWithinUsableDistance(this.worldPos, playerIn, BadBeaconMod.BAD_BEACON.get());
    }

    @Override
    public void updateProgressBar(int id, int data) {
        super.updateProgressBar(id, data);
        this.detectAndSendChanges();
    }

    @Override
    public ItemStack transferStackInSlot(PlayerEntity playerIn, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.inventorySlots.get(index);
        if (slot != null && slot.getHasStack()) {
            ItemStack itemstack1 = slot.getStack();
            itemstack = itemstack1.copy();
            if (index == 0) {
                if (!this.mergeItemStack(itemstack1, 1, 37, true)) {
                    return ItemStack.EMPTY;
                }

                slot.onSlotChange(itemstack1, itemstack);
            } else if (this.mergeItemStack(itemstack1, 0, 1, false)) {
                return ItemStack.EMPTY;
            } else if (index < 28) {
                if (!this.mergeItemStack(itemstack1, 28, 37, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (index < 37) {
                if (!this.mergeItemStack(itemstack1, 1, 28, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.mergeItemStack(itemstack1, 1, 37, false)) {
                return ItemStack.EMPTY;
            }

            if (itemstack1.isEmpty()) {
                slot.putStack(ItemStack.EMPTY);
            } else {
                slot.onSlotChanged();
            }

            if (itemstack1.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(playerIn, itemstack1);
        }
        return itemstack;
    }

    @OnlyIn(Dist.CLIENT)
    public int getLevels() {
        return this.intArray.get(0);
    }

    @Nullable
    @OnlyIn(Dist.CLIENT)
    public Effect getPrimaryEffect() {
        return Effect.get(this.intArray.get(1));
    }

    @Nullable
    @OnlyIn(Dist.CLIENT)
    public Effect getSecondaryEffect() {
        return Effect.get(this.intArray.get(2));
    }

    public void handleSlots(int primary, int secondary) {
        if (this.beaconSlot.getHasStack()) {
            this.intArray.set(1, primary);
            this.intArray.set(2, secondary);
            this.beaconSlot.decrStackSize(1);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public boolean isActive() {
        return !this.tileBeacon.getStackInSlot(0).isEmpty();
    }

    class BeaconSlot extends Slot {
        public BeaconSlot(IInventory inventoryIn, int index, int xIn, int yIn) {
            super(inventoryIn, index, xIn, yIn);
        }

        public boolean isItemValid(ItemStack stack) {
            return stack.getItem().isIn(BadBeaconMod.BAD_BEACON_PAYMENT);
        }

        public int getSlotStackLimit() {
            return 1;
        }
    }
}
