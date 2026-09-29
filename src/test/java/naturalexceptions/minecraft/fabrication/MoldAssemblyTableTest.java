package naturalexceptions.minecraft.fabrication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import naturalexceptions.core.fabrication.GeometryId;
import naturalexceptions.minecraft.item.ItemCastingMold;
import naturalexceptions.minecraft.registry.NEItems;

final class MoldAssemblyTableTest {

    @BeforeAll
    static void setup() {
        try {
            net.minecraft.init.Bootstrap.func_151354_b();
        } catch (Throwable ignored) {}
        NEItems.register();
        net.minecraft.tileentity.TileEntity.addMapping(TileEntityMoldAssemblyTable.class, "test_mold_assembly_table");
    }

    @Test
    void blankMoldTransformsIntoSelectedShapeAndConsumesExactlyOne() {
        TileEntityMoldAssemblyTable table = new TileEntityMoldAssemblyTable();
        ItemStack blanks = ItemCastingMold.createStack(null);
        blanks.stackSize = 2;
        table.setInventorySlotContents(0, blanks);
        assertTrue(table.hasValidBlankInput());
        assertTrue(
            table.shape(
                TileEntityMoldAssemblyTable.selectableGeometries()
                    .indexOf(GeometryId.PICK_HEAD)));
        assertEquals(1, table.getStackInSlot(0).stackSize);
        assertEquals(GeometryId.PICK_HEAD, ItemCastingMold.getGeometry(table.getStackInSlot(1)));
    }

    @Test
    void shapingRejectsMissingOrAlreadyShapedInput() {
        TileEntityMoldAssemblyTable table = new TileEntityMoldAssemblyTable();
        assertFalse(table.hasValidBlankInput());
        assertFalse(table.canShape(0));
        assertFalse(table.shape(0));
        table.setInventorySlotContents(0, ItemCastingMold.createStack(GeometryId.INGOT));
        assertFalse(table.hasValidBlankInput());
        assertFalse(table.shape(1));
        assertEquals(GeometryId.INGOT, ItemCastingMold.getGeometry(table.getStackInSlot(0)));
        assertNull(table.getStackInSlot(1));
    }

    @Test
    void everyPartShapeIsSelectableAndPersists() {
        assertEquals(
            9,
            TileEntityMoldAssemblyTable.selectableGeometries()
                .size());
        assertEquals(
            GeometryId.INGOT,
            TileEntityMoldAssemblyTable.selectableGeometries()
                .get(0));
        TileEntityMoldAssemblyTable table = new TileEntityMoldAssemblyTable();
        table.setInventorySlotContents(0, ItemCastingMold.createStack(null));
        assertTrue(
            table.shape(
                TileEntityMoldAssemblyTable.selectableGeometries()
                    .indexOf(GeometryId.BLADE)));
        NBTTagCompound nbt = new NBTTagCompound();
        table.writeToNBT(nbt);
        TileEntityMoldAssemblyTable loaded = new TileEntityMoldAssemblyTable();
        loaded.readFromNBT(nbt);
        assertEquals(GeometryId.BLADE, ItemCastingMold.getGeometry(loaded.getStackInSlot(1)));
    }

    @Test
    void eachGeometryButtonProducesItsOwnCanonicalMoldGeometry() {
        for (int index = 0; index < TileEntityMoldAssemblyTable.selectableGeometries()
            .size(); index++) {
            GeometryId expected = TileEntityMoldAssemblyTable.selectableGeometries()
                .get(index);
            TileEntityMoldAssemblyTable table = new TileEntityMoldAssemblyTable();
            table.setInventorySlotContents(0, ItemCastingMold.createStack(null));

            assertTrue(table.shape(index), "Selection should work for " + expected);
            assertEquals(expected, ItemCastingMold.getGeometry(table.getStackInSlot(1)));
            assertEquals(1, table.getStackInSlot(1).stackSize);
            assertNull(table.getStackInSlot(0));
        }
    }

    @Test
    void visibleShapeOptionsMatchWhatTheTableCanActuallyProduce() {
        TileEntityMoldAssemblyTable table = new TileEntityMoldAssemblyTable();
        ItemStack blanks = ItemCastingMold.createStack(null);
        blanks.stackSize = 2;
        table.setInventorySlotContents(0, blanks);
        assertTrue(table.canShape(0));
        assertTrue(table.shape(0));
        assertTrue(table.canShape(0));
        assertFalse(table.canShape(1));
        assertFalse(table.shape(1));
    }

    @Test
    void shiftClickingAnAlreadyShapedMoldNeverDeletesIt() {
        TileEntityMoldAssemblyTable table = new TileEntityMoldAssemblyTable();
        InventoryPlayer inventory = new InventoryPlayer(null);
        ItemStack shaped = ItemCastingMold.createStack(GeometryId.INGOT);
        shaped.stackSize = 15;
        inventory.mainInventory[9] = shaped;
        ContainerMoldAssemblyTable container = new ContainerMoldAssemblyTable(inventory, table);

        assertNull(container.transferStackInSlot(null, 2));
        assertEquals(15, inventory.mainInventory[9].stackSize);
        assertNull(table.getStackInSlot(0));
    }

    @Test
    void shiftClickingBlankMoldsMovesTheExactStackIntoInput() {
        TileEntityMoldAssemblyTable table = new TileEntityMoldAssemblyTable();
        InventoryPlayer inventory = new InventoryPlayer(null);
        ItemStack blanks = ItemCastingMold.createStack(null);
        blanks.stackSize = 15;
        inventory.mainInventory[9] = blanks;
        ContainerMoldAssemblyTable container = new ContainerMoldAssemblyTable(inventory, table);

        assertEquals(15, container.transferStackInSlot(null, 2).stackSize);
        assertNull(inventory.mainInventory[9]);
        assertEquals(15, table.getStackInSlot(0).stackSize);
        assertTrue(table.canShape(0));
    }

    @Test
    void shapedOutputCanBeShiftClickedBackIntoPlayerInventory() {
        TileEntityMoldAssemblyTable table = new TileEntityMoldAssemblyTable();
        table.setInventorySlotContents(0, ItemCastingMold.createStack(null));
        assertTrue(table.shape(0));
        InventoryPlayer inventory = new InventoryPlayer(null);
        ContainerMoldAssemblyTable container = new ContainerMoldAssemblyTable(inventory, table);

        assertEquals(GeometryId.INGOT, ItemCastingMold.getGeometry(container.transferStackInSlot(null, 1)));
        assertNull(table.getStackInSlot(1));
        assertEquals(GeometryId.INGOT, ItemCastingMold.getGeometry(inventory.mainInventory[8]));
    }
}
