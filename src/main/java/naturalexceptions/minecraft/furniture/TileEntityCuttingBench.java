package naturalexceptions.minecraft.furniture;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntityFurnace;

/**
 * A working bench's slots: what is waiting to be worked (slot 0), and for a bench that needs a fire, its fuel (slot
 * 1). Which bench it is comes from its block.
 */
public final class TileEntityCuttingBench extends TileEntityStorage {

    public static final int INPUT = 0, FUEL = 1;
    /** Fuel burn time (in furnace ticks) that one piece takes: a coal fires eight. */
    public static final int HEAT_PER_PIECE = 200;

    /** Pieces the fire has left in it before it needs feeding. */
    private int heat;

    /** The kind it was made as, for when it stands in no world. */
    private final CuttingRecipes.Kind made;

    public TileEntityCuttingBench() {
        this(CuttingRecipes.Kind.MASON);
    }

    TileEntityCuttingBench(CuttingRecipes.Kind kind) {
        super(2, "container.naturalexceptions.cutting");
        this.made = kind;
    }

    public CuttingRecipes.Kind kind() {
        return worldObj != null && worldObj.getBlock(xCoord, yCoord, zCoord) instanceof BlockCuttingBench
            ? ((BlockCuttingBench) worldObj.getBlock(xCoord, yCoord, zCoord)).kind
            : made;
    }

    @Override
    public String getInventoryName() {
        return BlockCuttingBench.NAMES[kind().ordinal()];
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == FUEL) return kind().fired() && TileEntityFurnace.getItemBurnTime(stack) > 0;
        return CuttingRecipes.accepts(kind(), stack);
    }

    /** Whether this bench can work a piece now: always, or with a fire lit or fuel to light one. */
    public boolean hot() {
        if (!kind().fired()) return true;
        return heat > 0 || TileEntityFurnace.getItemBurnTime(getStackInSlot(FUEL)) >= HEAT_PER_PIECE;
    }

    /** Uses the fire for one piece, feeding it from the fuel slot if it has gone out. */
    void burn() {
        if (!kind().fired()) return;
        if (heat <= 0) {
            ItemStack fuel = getStackInSlot(FUEL);
            int time = TileEntityFurnace.getItemBurnTime(fuel);
            if (time < HEAT_PER_PIECE) return;
            heat = time / HEAT_PER_PIECE;
            ItemStack left = fuel.getItem()
                .getContainerItem(fuel);
            decrStackSize(FUEL, 1);
            if (left != null && getStackInSlot(FUEL) == null) setInventorySlotContents(FUEL, left);
        }
        heat--;
        markDirty();
    }

    public int heat() {
        return heat;
    }

    /** The client's copy of the fire, as the screen is told it. */
    void setHeat(int heat) {
        this.heat = heat;
    }

    /** Keeps the bench's look in step with its input slot: the piece is on it only while there is one. */
    @Override
    public void markDirty() {
        super.markDirty();
        if (worldObj == null || worldObj.isRemote) return;
        int meta = worldObj.getBlockMetadata(xCoord, yCoord, zCoord);
        int want = getStackInSlot(INPUT) != null ? meta | BlockCuttingBench.LOADED : meta & ~BlockCuttingBench.LOADED;
        if (want != meta) worldObj.setBlockMetadataWithNotify(xCoord, yCoord, zCoord, want, 2);
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setInteger("Heat", heat);
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        heat = tag.getInteger("Heat");
    }
}
