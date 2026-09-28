package naturalexceptions.minecraft.fabrication;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;

import naturalexceptions.core.assembly.AssemblyGraph;
import naturalexceptions.core.fabrication.ComponentInstance;
import naturalexceptions.minecraft.item.ItemGenericAssembly;
import naturalexceptions.minecraft.recipe.RecipeToolAssembly;

/**
 * TileEntity controlling the Assembly Table for assembling physical tools and equipment.
 */
public class TileEntityAssemblyTable extends TileEntity implements IInventory {

    public static final int SLOT_HEAD = 0;
    public static final int SLOT_HANDLE = 1;
    public static final int SLOT_RESULT = 2;

    private final ItemStack[] inventory = new ItemStack[2];

    public TileEntityAssemblyTable() {}

    @Override
    public int getSizeInventory() {
        return 3;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        if (slot == SLOT_HEAD) {
            return inventory[0];
        }
        if (slot == SLOT_HANDLE) {
            return inventory[1];
        }
        if (slot == SLOT_RESULT) {
            return computeResult();
        }
        return null;
    }

    public ItemStack computeResult() {
        if (inventory[0] == null || inventory[1] == null) {
            return null;
        }
        return ToolAssemblyCarrier.assemble(inventory[0], inventory[1]);
    }

    /** A single assembled tool can be laid on an empty table to take it apart. */
    public boolean canAcceptToolForDisassembly(ItemStack tool) {
        return inventory[0] == null && inventory[1] == null
            && tool != null
            && tool.stackSize == 1
            && ToolAssemblyCarrier.isDisassemblable(tool);
    }

    /** Splits the tool into its head and handle, which then sit in the two part slots. */
    public boolean disassembleOntoTable(ItemStack tool) {
        if (!canAcceptToolForDisassembly(tool)) return false;
        ItemStack[] parts = ToolAssemblyCarrier.disassemble(tool);
        if (parts == null) return false;
        inventory[0] = parts[0];
        inventory[1] = parts[1];
        markDirty();
        if (worldObj != null) {
            worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
        }
        return true;
    }

    @Override
    public ItemStack decrStackSize(int slot, int amount) {
        if (slot == SLOT_RESULT) {
            ItemStack result = computeResult();
            if (result != null) {
                decrSlotInternal(0, 1);
                decrSlotInternal(1, 1);
                markDirty();
                if (worldObj != null) {
                    worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
                }
                return result;
            }
            return null;
        }

        if (slot >= 0 && slot < 2) {
            if (inventory[slot] != null) {
                ItemStack itemstack;
                if (inventory[slot].stackSize <= amount) {
                    itemstack = inventory[slot];
                    inventory[slot] = null;
                } else {
                    itemstack = inventory[slot].splitStack(amount);
                    if (inventory[slot].stackSize == 0) {
                        inventory[slot] = null;
                    }
                }
                markDirty();
                if (worldObj != null) {
                    worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
                }
                return itemstack;
            }
        }
        return null;
    }

    private void decrSlotInternal(int slot, int amount) {
        if (inventory[slot] != null) {
            inventory[slot].stackSize -= amount;
            if (inventory[slot].stackSize <= 0) {
                inventory[slot] = null;
            }
        }
    }

    @Override
    public ItemStack getStackInSlotOnClosing(int slot) {
        if (slot >= 0 && slot < 2) {
            ItemStack stack = inventory[slot];
            inventory[slot] = null;
            return stack;
        }
        return null;
    }

    @Override
    public void setInventorySlotContents(int slot, ItemStack stack) {
        if (slot == SLOT_HEAD && stack != null && ToolAssemblyCarrier.isDisassemblable(stack)) {
            disassembleOntoTable(stack);
            return;
        }
        if (slot >= 0 && slot < 2) {
            inventory[slot] = stack;
            if (stack != null && stack.stackSize > getInventoryStackLimit()) {
                stack.stackSize = getInventoryStackLimit();
            }
            markDirty();
            if (worldObj != null) {
                worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
            }
        }
    }

    @Override
    public String getInventoryName() {
        return "container.assemblyTable";
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
    public boolean isUseableByPlayer(EntityPlayer player) {
        if (worldObj == null) {
            return true;
        }
        return worldObj.getTileEntity(xCoord, yCoord, zCoord) == this
            && player.getDistanceSq(xCoord + 0.5D, yCoord + 0.5D, zCoord + 0.5D) <= 64.0D;
    }

    @Override
    public void openInventory() {}

    @Override
    public void closeInventory() {}

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == SLOT_HEAD) {
            return RecipeToolAssembly.extractHead(stack) != null || canAcceptToolForDisassembly(stack);
        }
        if (slot == SLOT_HANDLE) {
            return RecipeToolAssembly.extractHandle(stack) != null;
        }
        return false;
    }

    public boolean placeComponentInWorld(EntityPlayer player, ItemStack held) {
        if (held == null) {
            return false;
        }
        if (canAcceptToolForDisassembly(held)) {
            String name = held.getDisplayName();
            disassembleOntoTable(held);
            player.setCurrentItemOrArmor(0, null);
            player.inventoryContainer.detectAndSendChanges();
            if (worldObj != null) {
                worldObj.playSoundEffect(xCoord + 0.5D, yCoord + 0.5D, zCoord + 0.5D, "random.anvil_use", 0.4F, 0.8F);
            }
            player.addChatMessage(
                new ChatComponentText(
                    "\u00a7eTook apart \u00a7f" + name + "\u00a7e. Its head and handle are on the table."));
            return true;
        }
        if (inventory[0] == null && RecipeToolAssembly.extractHead(held) != null) {
            inventory[0] = held.splitStack(1);
            if (held.stackSize <= 0) {
                player.setCurrentItemOrArmor(0, null);
            }
            player.inventoryContainer.detectAndSendChanges();
            markDirty();
            if (worldObj != null) {
                worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
                worldObj.playSoundEffect(xCoord + 0.5D, yCoord + 0.5D, zCoord + 0.5D, "step.wood", 1.0F, 1.0F);
            }
            player.addChatMessage(
                new ChatComponentText(
                    "\u00a7aPlaced \u00a7f" + inventory[0].getDisplayName() + " \u00a7aon Assembly Table."));
            return true;
        }
        if (inventory[1] == null && RecipeToolAssembly.extractHandle(held) != null) {
            inventory[1] = held.splitStack(1);
            if (held.stackSize <= 0) {
                player.setCurrentItemOrArmor(0, null);
            }
            player.inventoryContainer.detectAndSendChanges();
            markDirty();
            if (worldObj != null) {
                worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
                worldObj.playSoundEffect(xCoord + 0.5D, yCoord + 0.5D, zCoord + 0.5D, "step.wood", 1.0F, 1.0F);
            }
            player.addChatMessage(
                new ChatComponentText(
                    "\u00a7aPlaced \u00a7f" + inventory[1].getDisplayName() + " \u00a7aon Assembly Table."));
            return true;
        }
        return false;
    }

    public boolean assembleInWorld(EntityPlayer player) {
        ItemStack assembled = decrStackSize(SLOT_RESULT, 1);
        if (assembled != null) {
            if (!player.inventory.addItemStackToInventory(assembled)) {
                player.dropPlayerItemWithRandomChoice(assembled, false);
            }
            player.inventoryContainer.detectAndSendChanges();
            if (worldObj != null) {
                worldObj.playSoundEffect(xCoord + 0.5D, yCoord + 0.5D, zCoord + 0.5D, "random.anvil_use", 0.6F, 1.2F);
            }
            player.addChatMessage(
                new ChatComponentText("\u00a7aAssembled \u00a7f" + assembled.getDisplayName() + " \u00a7a!"));
            return true;
        }
        return false;
    }

    public boolean retrieveInWorld(EntityPlayer player) {
        for (int i = 1; i >= 0; i--) {
            if (inventory[i] != null) {
                ItemStack ret = inventory[i];
                inventory[i] = null;
                if (!player.inventory.addItemStackToInventory(ret)) {
                    player.dropPlayerItemWithRandomChoice(ret, false);
                }
                player.inventoryContainer.detectAndSendChanges();
                markDirty();
                if (worldObj != null) {
                    worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
                    worldObj.playSoundEffect(xCoord + 0.5D, yCoord + 0.5D, zCoord + 0.5D, "random.pop", 0.8F, 1.0F);
                }
                player.addChatMessage(
                    new ChatComponentText(
                        "\u00a7eRetrieved \u00a7f" + ret.getDisplayName() + " \u00a7efrom Assembly Table."));
                return true;
            }
        }
        return false;
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        NBTTagList list = new NBTTagList();
        for (int i = 0; i < inventory.length; i++) {
            if (inventory[i] != null) {
                NBTTagCompound itemTag = new NBTTagCompound();
                itemTag.setByte("Slot", (byte) i);
                inventory[i].writeToNBT(itemTag);
                list.appendTag(itemTag);
            }
        }
        nbt.setTag("Items", list);
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        NBTTagList list = nbt.getTagList("Items", 10);
        inventory[0] = null;
        inventory[1] = null;
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound itemTag = list.getCompoundTagAt(i);
            int slot = itemTag.getByte("Slot") & 255;
            if (slot >= 0 && slot < inventory.length) {
                inventory[slot] = ItemStack.loadItemStackFromNBT(itemTag);
            }
        }
    }

    @Override
    public Packet getDescriptionPacket() {
        NBTTagCompound nbt = new NBTTagCompound();
        writeToNBT(nbt);
        return new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, 1, nbt);
    }

    @Override
    public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity pkt) {
        readFromNBT(pkt.func_148857_g());
    }
}
