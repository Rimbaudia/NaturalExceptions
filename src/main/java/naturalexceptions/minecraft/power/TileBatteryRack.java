package naturalexceptions.minecraft.power;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import naturalexceptions.minecraft.registry.NEBlocks;

/**
 * A Battery Rack: up to {@link #CELLS} Storm Cells on an iron frame, and a big two-way lever, the way the old battery
 * rooms were worked. In PARALLEL the cells stand side by side: a Storm Cell's voltage, very stiff, and a 24 V windmill
 * or dynamo charges them all. In SERIES they are stacked end to end: their voltages add, up to some 130 V, for the
 * high-tension machines. The rack keeps its cells evenly charged; a cell taken out carries its share.
 */
public final class TileBatteryRack extends TileBattery {

    public static final int CELLS = 5;
    /** Throwing the lever while it carries more than this draws an arc. */
    public static final double LEVER_ARC_AMPS = Arc.AMPS;

    private int cells;
    private boolean series;

    public int cells() {
        return cells;
    }

    public boolean series() {
        return series;
    }

    /** How the cells are joined: their voltages add in series, their currents in parallel. */
    private int stacked() {
        return series ? cells : 1;
    }

    @Override
    protected double capacity() {
        return cells * TileStormCell.CAPACITY;
    }

    @Override
    protected double emptyVolts() {
        return TileStormCell.EMPTY_VOLTS * stacked();
    }

    @Override
    protected double fullVolts() {
        return TileStormCell.FULL_VOLTS * stacked();
    }

    @Override
    protected double internalOhms() {
        return series ? TileStormCell.INTERNAL_OHMS * cells : TileStormCell.INTERNAL_OHMS / Math.max(1, cells);
    }

    @Override
    protected boolean present() {
        return cells > 0;
    }

    /** Racks a Storm Cell (from a player's hand), with what charge it holds. Whether it took it. */
    public boolean rack(ItemStack stack) {
        if (cells >= CELLS || stack == null || stack.getItem() != Item.getItemFromBlock(NEBlocks.stormCell))
            return false;
        cells++;
        addCharge(
            stack.hasTagCompound() ? stack.getTagCompound()
                .getDouble("Charge") : 0.0);
        changedCells();
        return true;
    }

    /** Takes a cell off the rack, with its share of the charge (null if there is none). */
    public ItemStack unrack() {
        if (cells <= 0 || NEBlocks.stormCell == null) return null;
        double share = charge / cells;
        charge -= share;
        cells--;
        if (cells == 0) charge = 0.0;
        ItemStack out = new ItemStack(NEBlocks.stormCell);
        if (share > 0.0) {
            out.setTagCompound(new NBTTagCompound());
            out.getTagCompound()
                .setDouble("Charge", share);
        }
        markDirty();
        changedCells();
        return out;
    }

    /** Throws the lever. Under load, the current arcs across it as it goes, and burns whoever threw it. */
    public void throwLever(EntityPlayer player) {
        Arc.at(worldObj, xCoord, yCoord, zCoord, volts, amps, player);
        series = !series;
        worldObj.playSoundEffect(xCoord + 0.5, yCoord + 0.8, zCoord + 0.5, "random.click", 0.9F, 0.5F);
        markDirty();
        changedCells();
    }

    private void changedCells() {
        if (worldObj == null) return;
        PowerNetworks.dirty(worldObj, xCoord, yCoord, zCoord); // solved again at once, not at the idle pace
        int meta = BlockPowerMachine.BatteryRack.meta(cells, series);
        if (worldObj.getBlockMetadata(xCoord, yCoord, zCoord) != meta)
            worldObj.setBlockMetadataWithNotify(xCoord, yCoord, zCoord, meta, 3);
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setInteger("Cells", cells);
        tag.setBoolean("Series", series);
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        // Cells first: the charge read after is held to what they can store.
        cells = Math.max(0, Math.min(CELLS, tag.getInteger("Cells")));
        series = tag.getBoolean("Series");
        super.readFromNBT(tag);
    }

    @Override
    public String readout() {
        if (cells == 0) return "Battery Rack: empty (rack Storm Cells in it)";
        return String.format(
            "Battery Rack: %d cell%s in %s, %.0f%% charged (%.0f kJ), %.1f V, %+.1f A",
            cells,
            cells == 1 ? "" : "s",
            series ? "SERIES" : "PARALLEL",
            fraction() * 100.0,
            charge / 1000.0,
            volts,
            amps);
    }
}
