package naturalexceptions.minecraft.gear;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** The Gear screen's slots: nineteen gear slots, then the player's inventory and hotbar. */
public final class ContainerGear extends Container {

    public static final int GEAR_SLOTS = GearSlot.COUNT;
    public static final int FIRST_PLAYER_SLOT = GEAR_SLOTS;
    public static final int FIRST_HOTBAR_SLOT = GEAR_SLOTS + 27;
    public static final int SLOT_COUNT = GEAR_SLOTS + 36;

    private final EntityPlayer player;
    private final PlayerGear gear;

    public ContainerGear(EntityPlayer player) {
        this.player = player;
        this.gear = PlayerGear.get(player);
        for (GearSlot slot : GearSlot.values()) {
            int[] at = GearLayout.slot(slot);
            addSlotToContainer(new GearSlotView(gear, slot, at[0], at[1]));
        }
        InventoryPlayer inventory = player.inventory;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlotToContainer(
                    new Slot(inventory, col + row * 9 + 9, 8 + col * 18, GearLayout.PLAYER_INVENTORY_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++)
            addSlotToContainer(new Slot(inventory, col, 8 + col * 18, GearLayout.HOTBAR_Y));
    }

    public PlayerGear gear() {
        return gear;
    }

    @Override
    public boolean canInteractWith(EntityPlayer who) {
        return who == player;
    }

    /** Shift-click: worn gear goes back to the inventory; a wearable goes to its first free matching slot. */
    @Override
    public ItemStack transferStackInSlot(EntityPlayer who, int index) {
        Slot slot = index >= 0 && index < inventorySlots.size() ? (Slot) inventorySlots.get(index) : null;
        if (slot == null || !slot.getHasStack()) return null;
        ItemStack stack = slot.getStack();
        ItemStack original = stack.copy();
        if (index < GEAR_SLOTS) {
            if (!mergeItemStack(stack, FIRST_PLAYER_SLOT, SLOT_COUNT, true)) return null;
        } else {
            int target = freeGearSlotFor(gear, stack);
            if (target >= 0) {
                ItemStack one = stack.splitStack(1);
                ((Slot) inventorySlots.get(target)).putStack(one);
            } else if (index < FIRST_HOTBAR_SLOT) {
                if (!mergeItemStack(stack, FIRST_HOTBAR_SLOT, SLOT_COUNT, false)) return null;
            } else if (!mergeItemStack(stack, FIRST_PLAYER_SLOT, FIRST_HOTBAR_SLOT, false)) {
                return null;
            }
        }
        if (stack.stackSize <= 0) slot.putStack(null);
        else slot.onSlotChanged();
        if (stack.stackSize == original.stackSize) return null;
        slot.onPickupFromSlot(who, stack);
        return original;
    }

    /** Container index of the first empty gear slot that takes {@code stack}, or -1. */
    static int freeGearSlotFor(PlayerGear gear, ItemStack stack) {
        for (GearSlot slot : GearSlot.values()) {
            if (IWearable.fits(slot, stack) && gear.get(slot) == null) return slot.ordinal();
        }
        return -1;
    }

    /** One gear slot: a single item of its own kind, with a grey silhouette while empty. */
    public static final class GearSlotView extends Slot {

        private final GearSlot gearSlot;

        GearSlotView(PlayerGear gear, GearSlot gearSlot, int x, int y) {
            super(gear, gearSlot.ordinal(), x, y);
            this.gearSlot = gearSlot;
        }

        public GearSlot gearSlot() {
            return gearSlot;
        }

        @Override
        public boolean isItemValid(ItemStack stack) {
            return IWearable.fits(gearSlot, stack);
        }

        @Override
        public int getSlotStackLimit() {
            return 1;
        }

        @Override
        @SideOnly(Side.CLIENT)
        public IIcon getBackgroundIconIndex() {
            return naturalexceptions.minecraft.gear.client.GearIcons.slotIcon(gearSlot.kind());
        }
    }
}
