package naturalexceptions.minecraft.gear;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraftforge.common.IExtendedEntityProperties;

/**
 * What a player is wearing: one stack per {@link GearSlot}, saved with the player. Each slot holds a single item of
 * its own kind. Changes to the Face slot are reported so other players can see the mask.
 */
public final class PlayerGear implements IExtendedEntityProperties, IInventory {

    public static final String KEY = "naturalexceptions.gear.v1";
    private static final String TAG_ITEMS = "Items";
    private static final String TAG_SLOT = "Slot";

    private final ItemStack[] stacks = new ItemStack[GearSlot.COUNT];
    private EntityPlayer owner;
    /** Set when the Face slot changed and trackers have not been told yet. */
    private boolean appearanceDirty = true;
    /** Set when anything changed and the owner's client has not been told yet (it needs its gear for movement). */
    private boolean ownerDirty = true;
    private long wornTreasuresMask = -1L;
    private final byte[] wornTreasureCounts = new byte[32];

    public static PlayerGear get(EntityPlayer player) {
        if (player == null) return null;
        IExtendedEntityProperties current = player.getExtendedProperties(KEY);
        if (current instanceof PlayerGear) return (PlayerGear) current;
        PlayerGear created = new PlayerGear();
        created.owner = player;
        player.registerExtendedProperties(KEY, created);
        return created;
    }

    public ItemStack get(GearSlot slot) {
        return stacks[slot.ordinal()];
    }

    private long wornTreasuresMask() {
        if (wornTreasuresMask == -1L) {
            long mask = 0L;
            java.util.Arrays.fill(wornTreasureCounts, (byte) 0);
            for (int i = 0; i < stacks.length; i++) {
                ItemStack stack = stacks[i];
                if (stack != null && stack.getItem() instanceof naturalexceptions.minecraft.item.ItemTreasureGear) {
                    Treasure treasure = ((naturalexceptions.minecraft.item.ItemTreasureGear) stack.getItem())
                        .treasure();
                    int ord = treasure.ordinal();
                    mask |= (1L << ord);
                    wornTreasureCounts[ord]++;
                }
            }
            wornTreasuresMask = mask;
        }
        return wornTreasuresMask;
    }

    public boolean isWearing(Treasure treasure) {
        return treasure != null && (wornTreasuresMask() & (1L << treasure.ordinal())) != 0L;
    }

    public int wornCount(Treasure treasure) {
        if (treasure == null || (wornTreasuresMask() & (1L << treasure.ordinal())) == 0L) return 0;
        return wornTreasureCounts[treasure.ordinal()];
    }

    /** The first worn stack of a kind, or null. */
    public ItemStack worn(GearKind kind) {
        for (GearSlot slot : GearSlot.VALUES) {
            if (slot.kind() == kind && stacks[slot.ordinal()] != null) return stacks[slot.ordinal()];
        }
        return null;
    }

    /** Puts a single item of {@code stack} into the first free slot of its kind. Returns true if worn. */
    public boolean wear(ItemStack stack) {
        GearKind kind = IWearable.kindOf(stack);
        if (kind == null) return false;
        for (GearSlot slot : GearSlot.VALUES) {
            if (slot.kind() == kind && stacks[slot.ordinal()] == null) {
                ItemStack one = stack.copy();
                one.stackSize = 1;
                setInventorySlotContents(slot.ordinal(), one);
                return true;
            }
        }
        return false;
    }

    /** Empties every slot and returns what was worn (for dropping on death). */
    public java.util.List<ItemStack> takeAll() {
        java.util.List<ItemStack> result = new java.util.ArrayList<>();
        for (int i = 0; i < stacks.length; i++) {
            if (stacks[i] != null) result.add(stacks[i]);
            stacks[i] = null;
        }
        wornTreasuresMask = -1L;
        appearanceDirty = true;
        ownerDirty = true;
        return result;
    }

    public boolean consumeOwnerDirty() {
        boolean dirty = ownerDirty;
        ownerDirty = false;
        return dirty;
    }

    /** Client side: replace what this player wears with what the server says they wear. */
    public void applySync(ItemStack[] synced) {
        for (int i = 0; i < stacks.length; i++) stacks[i] = synced != null && i < synced.length ? synced[i] : null;
        wornTreasuresMask = -1L;
    }

    public ItemStack[] snapshot() {
        ItemStack[] copy = new ItemStack[stacks.length];
        for (int i = 0; i < stacks.length; i++) copy[i] = stacks[i] == null ? null : stacks[i].copy();
        return copy;
    }

    public boolean consumeAppearanceDirty() {
        boolean dirty = appearanceDirty;
        appearanceDirty = false;
        return dirty;
    }

    public void copyFrom(PlayerGear source) {
        NBTTagCompound copy = new NBTTagCompound();
        source.saveNBTData(copy);
        loadNBTData(copy);
    }

    // --- persistence -----------------------------------------------------------------------------------------

    @Override
    public void saveNBTData(NBTTagCompound compound) {
        NBTTagList list = new NBTTagList();
        for (int i = 0; i < stacks.length; i++) {
            if (stacks[i] == null) continue;
            NBTTagCompound entry = new NBTTagCompound();
            entry.setString(TAG_SLOT, GearSlot.VALUES[i].name());
            stacks[i].writeToNBT(entry);
            list.appendTag(entry);
        }
        NBTTagCompound gear = new NBTTagCompound();
        gear.setTag(TAG_ITEMS, list);
        compound.setTag(KEY, gear);
    }

    @Override
    public void loadNBTData(NBTTagCompound compound) {
        for (int i = 0; i < stacks.length; i++) stacks[i] = null;
        wornTreasuresMask = -1L;
        if (compound == null || !compound.hasKey(KEY, 10)) return;
        NBTTagList list = compound.getCompoundTag(KEY)
            .getTagList(TAG_ITEMS, 10);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound entry = list.getCompoundTagAt(i);
            GearSlot slot = GearSlot.byName(entry.getString(TAG_SLOT));
            if (slot == null) continue;
            stacks[slot.ordinal()] = ItemStack.loadItemStackFromNBT(entry);
        }
        appearanceDirty = true;
        ownerDirty = true;
    }

    @Override
    public void init(Entity entity, World world) {
        if (entity instanceof EntityPlayer) owner = (EntityPlayer) entity;
    }

    // --- IInventory ------------------------------------------------------------------------------------------

    @Override
    public int getSizeInventory() {
        return stacks.length;
    }

    @Override
    public ItemStack getStackInSlot(int index) {
        return index >= 0 && index < stacks.length ? stacks[index] : null;
    }

    @Override
    public ItemStack decrStackSize(int index, int count) {
        ItemStack stack = getStackInSlot(index);
        if (stack == null || count <= 0) return null;
        ItemStack taken;
        if (stack.stackSize <= count) {
            taken = stack;
            stacks[index] = null;
        } else {
            taken = stack.splitStack(count);
        }
        markDirty(index);
        return taken;
    }

    @Override
    public ItemStack getStackInSlotOnClosing(int index) {
        wornTreasuresMask = -1L;
        return null;
    }

    @Override
    public void setInventorySlotContents(int index, ItemStack stack) {
        if (index < 0 || index >= stacks.length) return;
        if (stack != null && stack.stackSize > getInventoryStackLimit()) stack.stackSize = getInventoryStackLimit();
        stacks[index] = stack;
        markDirty(index);
    }

    private void markDirty(int index) {
        wornTreasuresMask = -1L;
        ownerDirty = true;
        if (GearSlot.byIndex(index) == GearSlot.FACE || GearSlot.byIndex(index) == GearSlot.BACK)
            appearanceDirty = true;
    }

    @Override
    public String getInventoryName() {
        return "container.naturalexceptions.gear";
    }

    @Override
    public boolean hasCustomInventoryName() {
        return false;
    }

    @Override
    public int getInventoryStackLimit() {
        return 1;
    }

    @Override
    public void markDirty() {
        wornTreasuresMask = -1L;
    }

    @Override
    public boolean isUseableByPlayer(EntityPlayer player) {
        return player != null && player == owner;
    }

    @Override
    public void openInventory() {}

    @Override
    public void closeInventory() {}

    @Override
    public boolean isItemValidForSlot(int index, ItemStack stack) {
        return IWearable.fits(GearSlot.byIndex(index), stack);
    }
}
