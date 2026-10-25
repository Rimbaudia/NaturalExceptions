package naturalexceptions.minecraft.power;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTTagCompound;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import naturalexceptions.content.BuiltinContent;
import naturalexceptions.core.power.Cable;
import naturalexceptions.core.power.Grid;
import naturalexceptions.minecraft.registry.NEBlocks;
import naturalexceptions.minecraft.registry.NEItems;

/**
 * High tension: Storm Cells racked in series reach some 130 V, in parallel they charge from 24 V; the heater wants
 * the high tension; a line broken under it arcs, bare copper at it bites, and a fuse spares the rest.
 */
final class HighTensionTest {

    @BeforeAll
    static void init() {
        BuiltinContent.register();
        NEItems.register();
        NEBlocks.register();
    }

    private static ItemStack cell(double charge) {
        ItemStack stack = new ItemStack(NEBlocks.stormCell);
        stack.setTagCompound(new NBTTagCompound());
        stack.getTagCompound()
            .setDouble("Charge", charge);
        return stack;
    }

    private static TileBatteryRack fullRack(boolean series) {
        TileBatteryRack rack = new TileBatteryRack();
        for (int i = 0; i < TileBatteryRack.CELLS; i++) assertTrue(rack.rack(cell(TileStormCell.CAPACITY)));
        assertTrue(!rack.rack(cell(0.0)), "five is all a rack holds");
        if (series != rack.series()) flip(rack);
        return rack;
    }

    /** The lever, without a world to arc in. */
    private static void flip(TileBatteryRack rack) {
        NBTTagCompound tag = new NBTTagCompound();
        rack.writeToNBT(tag);
        tag.setBoolean("Series", !rack.series());
        rack.readFromNBT(tag);
    }

    /** Power into {@code heaters} Electric Heaters in parallel on {@code rack}, each. */
    private static double heaterWatts(TileBatteryRack rack, int heaters) {
        Grid grid = new Grid(1);
        rack.stamp(grid, 0);
        int[] loads = new int[heaters];
        for (int i = 0; i < heaters; i++) loads[i] = grid.load(0, TileElectricHeater.OHMS);
        grid.solve();
        return grid.power(loads[0]);
    }

    @Test
    void inSeriesTheCellsAddUpToHighTension() {
        TileBatteryRack rack = fullRack(true);
        assertEquals(TileStormCell.FULL_VOLTS * 5, rack.emf(), 1e-6);
        assertTrue(rack.emf() >= 120.0, "a full rack in series is high tension: " + rack.emf());
        assertTrue(fullRack(false).emf() < 30.0, "in parallel, a cell's voltage");
    }

    @Test
    void inParallelA24VoltWindmillChargesIt() {
        TileBatteryRack rack = new TileBatteryRack();
        rack.rack(cell(0.0));
        rack.rack(cell(0.0));
        assertTrue(!rack.series());
        Grid grid = new Grid(1);
        grid.source(0, TileWindmill.VOLTS, 0.3);
        rack.stamp(grid, 0);
        grid.solve();
        rack.powered(grid, 0, 60.0);
        assertTrue(rack.fraction() > 0.0, "the windmill charged the racked cells");
    }

    @Test
    void aCellTakenOffCarriesItsShare() {
        TileBatteryRack rack = new TileBatteryRack();
        rack.rack(cell(TileStormCell.CAPACITY));
        rack.rack(cell(0.0));
        ItemStack off = rack.unrack();
        assertEquals(
            TileStormCell.CAPACITY / 2,
            off.getTagCompound()
                .getDouble("Charge"),
            1.0,
            "the rack keeps its cells evenly charged");
        assertEquals(1, rack.cells());
        assertEquals(TileStormCell.CAPACITY / 2, rack.charge(), 1.0);
    }

    @Test
    void oneHeaterMeltsCopperTwoFromAFullRackMeltIron() {
        // What the smelter loses at copper's and at iron's melting points (VesselState's radiation and air).
        double copperWatts = 3400.0, ironWatts = 10200.0;
        assertTrue(heaterWatts(fullRack(true), 1) > copperWatts, "one heater on a rack is past copper");
        assertTrue(2 * heaterWatts(fullRack(true), 2) > ironWatts, "two, on a full rack, are past iron");
        TileBatteryRack half = fullRack(true);
        half.addCharge(-half.charge() / 2);
        assertTrue(2 * heaterWatts(half, 2) < ironWatts, "a half-flat rack is not enough for iron");
        assertTrue(heaterWatts(fullRack(false), 1) < 400.0, "on 24 V a heater only warms");
    }

    @Test
    void highTensionWantsHeavyInsulatedCable() {
        double amps = TileElectricHeater.RATED_WATTS / TileElectricHeater.RATED_VOLTS;
        assertTrue(amps > Cable.STANDARD_INSULATED.ratedAmps(), "standard insulated chars under a heater");
        assertTrue(amps < Cable.HEAVY_INSULATED.ratedAmps(), "heavy insulated carries one");
    }

    @Test
    void bareCopperBitesAtHighTensionOnly() {
        assertEquals(0.0F, BlockCable.shockDamage(24.0), 1e-6);
        assertEquals(0.0F, BlockCable.shockDamage(26.0), 1e-6);
        assertTrue(BlockCable.shockDamage(130.0) >= 6.0F, "130 V bites hard");
        assertTrue(BlockCable.shockDamage(1.0e6) <= 8.0F, "but never one-shots");
    }

    @Test
    void aLineBrokenUnderLoadArcsOnlyAtHighTension() {
        assertTrue(!Arc.strikes(24.0, 200.0), "24 V only sparks");
        assertTrue(!Arc.strikes(130.0, 0.5), "an idle line does not arc");
        assertTrue(Arc.strikes(130.0, 50.0), "a loaded high-tension line does");
        assertTrue(Arc.damage(130.0, 100.0) <= Arc.MAX_DAMAGE);
    }

    @Test
    void fusesAreRatedByTheirLinkAndCanBeMade() {
        for (int i = 1; i < BlockFuse.RATED_AMPS.length; i++)
            assertTrue(BlockFuse.RATED_AMPS[i] > BlockFuse.RATED_AMPS[i - 1]);
        assertTrue(
            BlockFuse.RATED_AMPS[0] < Cable.LIGHT_INSULATED.ratedAmps(),
            "the lightest fuse spares light insulated wire");
        assertTrue(!BlockFuse.blown(0) && BlockFuse.blown(BlockFuse.BLOWN));
        Item fuse = Item.getItemFromBlock(NEBlocks.fuse), rack = Item.getItemFromBlock(NEBlocks.batteryRack);
        boolean[] made = new boolean[3];
        boolean rackMade = false;
        for (Object entry : CraftingManager.getInstance()
            .getRecipeList()) {
            ItemStack out = ((IRecipe) entry).getRecipeOutput();
            if (out == null) continue;
            if (out.getItem() == fuse) made[out.getItemDamage() & 3] = true;
            if (out.getItem() == rack) rackMade = true;
        }
        assertTrue(made[0] && made[1] && made[2], "every rating can be made");
        assertTrue(rackMade, "and the rack");
    }
}
