package naturalexceptions.minecraft.furniture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class FurnitureTest {

    @BeforeAll
    static void bootstrap() {
        net.minecraft.init.Bootstrap.func_151354_b();
        try {
            net.minecraft.tileentity.TileEntity.addMapping(TileEntityBarrel.class, "naturalexceptions_barrel");
        } catch (IllegalArgumentException alreadyThere) {}
    }

    @Test
    void aBarrelHoldsSixteenStacksOfOneThing() {
        TileEntityBarrel barrel = new TileEntityBarrel();
        assertEquals(64, barrel.insert(new ItemStack(Items.wheat, 64)));
        assertFalse(barrel.accepts(new ItemStack(Items.apple)), "one kind only");
        int total = 64;
        while (total < 16 * 64) total += barrel.insert(new ItemStack(Items.wheat, 64));
        assertEquals(16 * 64, barrel.count());
        assertEquals(0, barrel.insert(new ItemStack(Items.wheat, 64)), "full");
        ItemStack out = barrel.extract(64);
        assertEquals(64, out.stackSize);
        assertEquals(Items.wheat, out.getItem());
        assertEquals(1, barrel.extract(1).stackSize);
        while (barrel.count() > 0) barrel.extract(64);
        assertNull(barrel.kind(), "emptied, it forgets what it held");
        assertTrue(barrel.accepts(new ItemStack(Items.apple)), "and takes anything again");
        // Eggs stack to 16: sixteen stacks of them.
        barrel.insert(new ItemStack(Items.egg, 16));
        assertEquals(16 * 16, barrel.capacity());
    }

    @Test
    void aBarrelHoldsWaterOrItemsNotBoth() {
        TileEntityBarrel barrel = new TileEntityBarrel();
        for (int i = 0; i < TileEntityBarrel.WATER_BUCKETS; i++) assertTrue(barrel.pourWater());
        assertFalse(barrel.pourWater(), "four buckets fill it");
        assertFalse(barrel.accepts(new ItemStack(Items.wheat)), "no items in water");
        assertTrue(barrel.drawBottle());
        assertTrue(barrel.drawWater());
        assertEquals(2 * TileEntityBarrel.BOTTLES_PER_BUCKET + 2, barrel.water());
        NBTTagCompound tag = new NBTTagCompound();
        barrel.writeToNBT(tag);
        TileEntityBarrel loaded = new TileEntityBarrel();
        loaded.readFromNBT(tag);
        assertEquals(barrel.water(), loaded.water(), "water keeps across saving");
    }

    @Test
    void aPantryShelfTakesOnlyFood() {
        TileEntityPantryShelf shelf = new TileEntityPantryShelf();
        assertTrue(shelf.isItemValidForSlot(0, new ItemStack(Items.bread)));
        assertFalse(shelf.isItemValidForSlot(0, new ItemStack(Items.iron_ingot)));
        assertFalse(shelf.addToFreeSlot(new ItemStack(Items.iron_ingot)));
        assertTrue(shelf.addToFreeSlot(new ItemStack(Items.apple, 5)));
        assertEquals(0, shelf.firstFood());
    }

    @Test
    void aCrateKeepsWhatItHoldsInItsItem() {
        TileEntityCrate crate = new TileEntityCrate();
        crate.setInventorySlotContents(3, new ItemStack(Items.diamond, 7));
        crate.setInventorySlotContents(20, new ItemStack(Items.arrow, 64));
        ItemStack packed = new ItemStack(Items.stick); // stands in for the crate item: only the tag matters here
        BlockCrate.write(crate, packed);
        assertNotNull(packed.getTagCompound());
        TileEntityCrate placed = new TileEntityCrate();
        placed.readSlots(ItemBlockCrate.contents(packed));
        assertEquals(7, placed.getStackInSlot(3).stackSize);
        assertEquals(
            Items.arrow,
            placed.getStackInSlot(20)
                .getItem());
        assertNull(placed.getStackInSlot(0));
    }

    @Test
    void aTableHoldsOneOfEachThingInFourPlaces() {
        TileEntityWoodenTable table = new TileEntityWoodenTable();
        for (int i = 0; i < 4; i++) assertTrue(table.addToFreeSlot(new ItemStack(Items.bread)));
        assertFalse(table.addToFreeSlot(new ItemStack(Items.bread)), "four places");
        table.setInventorySlotContents(0, new ItemStack(Items.bread, 10));
        assertEquals(1, table.getStackInSlot(0).stackSize, "one thing per place");
        assertEquals(3, table.lastFilled());
    }
}
