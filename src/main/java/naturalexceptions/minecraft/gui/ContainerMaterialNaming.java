package naturalexceptions.minecraft.gui;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import naturalexceptions.minecraft.item.MaterialNamingTarget;

public class ContainerMaterialNaming extends Container {

    private final EntityPlayer player;
    private final ItemStack sampleStack;

    public ContainerMaterialNaming(EntityPlayer player, ItemStack sampleStack) {
        this.player = player;
        this.sampleStack = sampleStack;
        // Right-click synchronization looks up the held slot in the newly opened container.
        // Keep all player slots synchronized, but outside this text-only dialog.
        for (int index = 0; index < 36; index++) {
            addSlotToContainer(new Slot(player.inventory, index, -10000, -10000) {

                @Override
                public boolean isItemValid(ItemStack stack) {
                    return false;
                }

                @Override
                public boolean canTakeStack(EntityPlayer player) {
                    return false;
                }
            });
        }
    }

    @Override
    public ItemStack slotClick(int slotId, int clickedButton, int mode, EntityPlayer player) {
        return null;
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        return null;
    }

    public ItemStack getSampleStack() {
        return sampleStack;
    }

    public EntityPlayer getPlayer() {
        return player;
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        if (player == null || !player.isEntityAlive() || player != this.player) {
            return false;
        }
        ItemStack held = player.getHeldItem();
        if (held == null) {
            return false;
        }
        if (held == sampleStack) {
            return true;
        }
        return MaterialNamingTarget.canOpenNaming(held);
    }
}
