package naturalexceptions.minecraft.fabrication;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;

import naturalexceptions.core.fabrication.GeometryId;
import naturalexceptions.minecraft.item.ItemCastingMold;

/** Persistent two-slot mold shaping station. */
public final class TileEntityMoldAssemblyTable extends TileEntity implements IInventory {

    private final ItemStack[] slots = new ItemStack[2];
    private static final List<GeometryId> SELECTABLE = ItemCastingMold.shapes();

    public static List<GeometryId> selectableGeometries() {
        return SELECTABLE;
    }

    public boolean hasValidBlankInput() {
        ItemStack input = slots[0];
        return input != null && input.getItem() == naturalexceptions.minecraft.registry.NEItems.castingMold
            && ItemCastingMold.getGeometry(input) == null
            && !ItemCastingMold.hasMetal(input);
    }

    public boolean canShape(int index) {
        if (!hasValidBlankInput() || index < 0 || index >= SELECTABLE.size()) return false;
        ItemStack result = ItemCastingMold.createStack(SELECTABLE.get(index));
        ItemStack output = slots[1];
        return output == null
            || output.getItem() == result.getItem() && output.getItemDamage() == result.getItemDamage()
                && ItemStack.areItemStackTagsEqual(output, result)
                && output.stackSize < output.getMaxStackSize();
    }

    /** Server-only transformation. One blank is consumed for exactly one shaped output. */
    public boolean shape(int index) {
        if (worldObj != null && worldObj.isRemote || !canShape(index)) return false;
        ItemStack input = slots[0];
        ItemStack output = slots[1];
        ItemStack result = ItemCastingMold.createStack(SELECTABLE.get(index));
        input.stackSize--;
        if (input.stackSize <= 0) slots[0] = null;
        if (output == null) slots[1] = result;
        else output.stackSize++;
        markDirty();
        if (worldObj != null) worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
        return true;
    }

    @Override
    public int getSizeInventory() {
        return slots.length;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return slot >= 0 && slot < slots.length ? slots[slot] : null;
    }

    @Override
    public ItemStack decrStackSize(int slot, int amount) {
        if (slot < 0 || slot >= slots.length || slots[slot] == null) return null;
        ItemStack stack = slots[slot];
        if (stack.stackSize <= amount) {
            slots[slot] = null;
            return stack;
        }
        ItemStack split = stack.splitStack(amount);
        if (stack.stackSize == 0) slots[slot] = null;
        return split;
    }

    @Override
    public ItemStack getStackInSlotOnClosing(int slot) {
        return decrStackSize(slot, Integer.MAX_VALUE);
    }

    @Override
    public void setInventorySlotContents(int slot, ItemStack stack) {
        if (slot < 0 || slot >= slots.length) return;
        slots[slot] = stack;
        if (stack != null && stack.stackSize > getInventoryStackLimit()) stack.stackSize = getInventoryStackLimit();
        markDirty();
    }

    @Override
    public String getInventoryName() {
        return "container.naturalexceptions.moldAssemblyTable";
    }

    @Override
    public boolean hasCustomInventoryName() {
        return false;
    }

    @Override
    public int getInventoryStackLimit() {
        return 64;
    }

    @Override
    public void markDirty() {
        super.markDirty();
    }

    @Override
    public boolean isUseableByPlayer(EntityPlayer player) {
        return worldObj != null && worldObj.getTileEntity(xCoord, yCoord, zCoord) == this
            && player.getDistanceSq(xCoord + 0.5, yCoord + 0.5, zCoord + 0.5) <= 64.0;
    }

    @Override
    public void openInventory() {}

    @Override
    public void closeInventory() {}

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return slot == 0 && stack != null
            && stack.getItem() == naturalexceptions.minecraft.registry.NEItems.castingMold
            && ItemCastingMold.getGeometry(stack) == null
            && !ItemCastingMold.hasMetal(stack);
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        NBTTagList list = tag.getTagList("Items", 10);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound item = list.getCompoundTagAt(i);
            int slot = item.getByte("Slot") & 255;
            if (slot < slots.length) slots[slot] = ItemStack.loadItemStackFromNBT(item);
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        NBTTagList list = new NBTTagList();
        for (int i = 0; i < slots.length; i++) if (slots[i] != null) {
            NBTTagCompound item = new NBTTagCompound();
            item.setByte("Slot", (byte) i);
            slots[i].writeToNBT(item);
            list.appendTag(item);
        }
        tag.setTag("Items", list);
    }
}
