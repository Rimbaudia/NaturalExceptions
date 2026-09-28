package naturalexceptions.minecraft.gui;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;

import naturalexceptions.minecraft.item.MaterialNamingTarget;

public class ContainerMaterialNaming extends Container {

    private final EntityPlayer player;
    private final ItemStack sampleStack;

    public ContainerMaterialNaming(EntityPlayer player, ItemStack sampleStack) {
        this.player = player;
        this.sampleStack = sampleStack;
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
