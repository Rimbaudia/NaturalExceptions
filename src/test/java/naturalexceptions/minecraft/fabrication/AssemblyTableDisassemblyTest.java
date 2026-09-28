package naturalexceptions.minecraft.fabrication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.item.ItemStack;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import naturalexceptions.content.BuiltinContent;
import naturalexceptions.content.material.BuiltinMaterials;
import naturalexceptions.core.assembly.AssemblyGraph;
import naturalexceptions.core.composition.CanonicalComposition;
import naturalexceptions.core.fabrication.ComponentInstance;
import naturalexceptions.core.fabrication.GeometryId;
import naturalexceptions.core.fabrication.ManufacturingState;
import naturalexceptions.core.id.ResourceId;
import naturalexceptions.core.material.CanonicalMaterialKey;
import naturalexceptions.core.metallurgy.MaterialQuantity;
import naturalexceptions.minecraft.item.ItemGenericAssembly;
import naturalexceptions.minecraft.item.ItemGenericComponent;
import naturalexceptions.minecraft.registry.NEBlocks;
import naturalexceptions.minecraft.registry.NEItems;

/** assembled tool -> Assembly Table -> the original components, with nothing duplicated or lost. */
final class AssemblyTableDisassemblyTest {

    @BeforeAll
    static void init() {
        BuiltinContent.register();
        NEItems.register();
        NEBlocks.register();
    }

    private static ItemStack copperPickHead() {
        CanonicalComposition cu = CanonicalComposition.of(ResourceId.of("naturalexceptions", "copper"));
        ItemStack head = ItemGenericComponent.createStack(
            new ComponentInstance(
                GeometryId.PICK_HEAD,
                BuiltinMaterials.COPPER.ref(),
                CanonicalMaterialKey.of(cu),
                MaterialQuantity.ofGrams(240.0),
                ManufacturingState.FINISHED));
        head.getTagCompound()
            .setString("NEProvenance", "cast:smelter@12,64,-4");
        return head;
    }

    @Test
    void toolReturnsToItsOriginalWornComponents() {
        TileEntityAssemblyTable table = new TileEntityAssemblyTable();
        ItemStack head = copperPickHead();
        ComponentInstance originalHead = ItemGenericComponent.getComponent(head);
        ItemStack handle = ItemGenericComponent.createWoodenHandle();
        ComponentInstance originalHandle = ItemGenericComponent.getComponent(handle);
        table.setInventorySlotContents(TileEntityAssemblyTable.SLOT_HEAD, head);
        table.setInventorySlotContents(TileEntityAssemblyTable.SLOT_HANDLE, handle);

        ItemStack tool = table.decrStackSize(TileEntityAssemblyTable.SLOT_RESULT, 1);
        assertNotNull(tool);
        assertNull(table.getStackInSlot(TileEntityAssemblyTable.SLOT_HEAD));
        assertNull(table.getStackInSlot(TileEntityAssemblyTable.SLOT_HANDLE));

        // Use wears the head.
        AssemblyGraph worn = ItemGenericAssembly.getAssembly(tool);
        worn.head()
            .damage()
            .addWear(0.25);
        ItemGenericAssembly.saveAssembly(tool, worn);

        // Laying the tool on the empty table takes it apart.
        table.setInventorySlotContents(TileEntityAssemblyTable.SLOT_HEAD, tool);
        ItemStack backHead = table.getStackInSlot(TileEntityAssemblyTable.SLOT_HEAD);
        ItemStack backHandle = table.getStackInSlot(TileEntityAssemblyTable.SLOT_HANDLE);
        assertNotNull(backHead);
        assertNotNull(backHandle);

        ComponentInstance h = ItemGenericComponent.getComponent(backHead);
        ComponentInstance g = ItemGenericComponent.getComponent(backHandle);
        assertEquals(originalHead.instanceId(), h.instanceId(), "identity preserved");
        assertEquals(originalHead.materialRef(), h.materialRef());
        assertEquals(originalHead.mass(), h.mass(), "no mass lost");
        assertEquals(originalHead.manufacturingState(), h.manufacturingState());
        assertEquals(0.25, h.damage()
            .wear(), 1.0e-9, "damage from use travels with the part");
        assertEquals(originalHandle.instanceId(), g.instanceId());
        assertEquals(originalHandle.mass(), g.mass());
        assertEquals(
            "cast:smelter@12,64,-4",
            backHead.getTagCompound()
                .getString("NEProvenance"),
            "part history preserved");

        // Reassembly is available again; the tool was consumed (no duplication).
        assertNotNull(table.computeResult());
    }

    @Test
    void tableRefusesToolWhenPartsAreAlreadyPresent() {
        TileEntityAssemblyTable table = new TileEntityAssemblyTable();
        ItemStack tool = ToolAssemblyCarrier.assemble(copperPickHead(), ItemGenericComponent.createWoodenHandle());
        assertNotNull(tool);
        table.setInventorySlotContents(TileEntityAssemblyTable.SLOT_HANDLE, ItemGenericComponent.createWoodenHandle());
        assertFalse(table.canAcceptToolForDisassembly(tool));
        assertFalse(table.isItemValidForSlot(TileEntityAssemblyTable.SLOT_HEAD, tool));
    }

    @Test
    void assembledToolNoLongerDropsOnlyItsHandleAsAContainerItem() {
        ItemStack tool = ToolAssemblyCarrier.assemble(copperPickHead(), ItemGenericComponent.createWoodenHandle());
        assertFalse(tool.getItem()
            .hasContainerItem(tool));
        assertTrue(ToolAssemblyCarrier.isDisassemblable(tool));
    }
}
