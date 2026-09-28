package naturalexceptions.minecraft.recipe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import naturalexceptions.content.BuiltinContent;
import naturalexceptions.content.material.BuiltinMaterials;
import naturalexceptions.core.composition.CanonicalComposition;
import naturalexceptions.core.fabrication.ComponentInstance;
import naturalexceptions.core.fabrication.GeometryId;
import naturalexceptions.core.fabrication.ManufacturingState;
import naturalexceptions.core.id.ResourceId;
import naturalexceptions.core.material.CanonicalMaterialKey;
import naturalexceptions.core.metallurgy.MaterialQuantity;
import naturalexceptions.core.thermal.ActiveThermalState;
import naturalexceptions.minecraft.item.ItemGenericComponent;
import naturalexceptions.minecraft.registry.NEBlocks;
import naturalexceptions.minecraft.registry.NEItems;

/** Every P4/P5 tool the normal loop needs must be reachable from survival materials. */
final class SurvivalEquipmentRecipesTest {

    @BeforeAll
    static void init() {
        BuiltinContent.register();
        NEItems.register();
        NEBlocks.register();
    }

    private static ItemStack cast(GeometryId geometry) {
        CanonicalComposition cu = CanonicalComposition.of(ResourceId.of("naturalexceptions", "copper"));
        return ItemGenericComponent.createStack(
            new ComponentInstance(
                geometry,
                BuiltinMaterials.COPPER.ref(),
                CanonicalMaterialKey.of(cu),
                MaterialQuantity.ofGrams(200.0),
                ManufacturingState.AS_CAST));
    }

    private static InventoryCrafting grid(ItemStack... cells) {
        InventoryCrafting grid = new InventoryCrafting(new Container() {

            @Override
            public boolean canInteractWith(EntityPlayer player) {
                return true;
            }
        }, 3, 3);
        for (int i = 0; i < cells.length; i++) grid.setInventorySlotContents(i, cells[i]);
        return grid;
    }

    private static ItemStack craft(InventoryCrafting grid) {
        for (RecipeCastParts recipe : SurvivalEquipmentRecipes.recipes()) {
            if (recipe.matches(grid, (World) null)) return recipe.getCraftingResult(grid);
        }
        return null;
    }

    private static Item itemOf(ItemStack stack) {
        return stack == null ? null : stack.getItem();
    }

    @Test
    void requiredEquipmentHasSurvivalRoutes() {
        ItemStack stick = new ItemStack(Items.stick);
        ItemStack brick = new ItemStack(NEItems.refractoryBrick);
        ItemStack stone = new ItemStack(Blocks.stone);
        ItemStack brickBlock = new ItemStack(NEBlocks.refractoryBrick);

        ItemStack hammer = craft(grid(cast(GeometryId.INGOT), null, null, stick));
        assertEquals(NEItems.blacksmithHammer, itemOf(hammer));
        assertTrue(hammer.getTagCompound()
            .hasKey(RecipeCastParts.PARTS_TAG), "the hammer records the metal it was made from");

        assertEquals(
            NEItems.blacksmithTongs,
            itemOf(craft(grid(cast(GeometryId.ROD), null, null, null, cast(GeometryId.ROD)))));
        assertEquals(NEItems.foundryLadle, itemOf(craft(grid(brick, null, brick, null, brick, stick))));
        ItemStack plate = cast(GeometryId.PLATE);
        assertEquals(
            Item.getItemFromBlock(NEBlocks.forgingAnvil),
            itemOf(craft(grid(plate, plate.copy(), plate.copy(), stone, stone, stone))));
        assertEquals(
            Item.getItemFromBlock(NEBlocks.smelterTuyere),
            itemOf(craft(grid(brickBlock, cast(GeometryId.ROD), brickBlock))));
    }

    @Test
    void hotOrWoodenPartsAreRefused() {
        ItemStack hot = cast(GeometryId.INGOT);
        ItemGenericComponent.saveThermalState(
            hot,
            ActiveThermalState.atTemperature(900.0, 0.0, 0.2, BuiltinMaterials.COPPER_PROPERTIES));
        assertEquals(null, craft(grid(hot, null, null, new ItemStack(Items.stick))));

        ItemStack woodHandle = ItemGenericComponent.createWoodenHandle();
        assertEquals(null, craft(grid(woodHandle, null, null, null, woodHandle.copy())));
    }

    @Test
    void rawIngotOnAStickIsNotAToolAssembly() {
        RecipeToolAssembly assembly = new RecipeToolAssembly();
        assertFalse(assembly.matches(grid(cast(GeometryId.INGOT), null, null, new ItemStack(Items.stick)), null));
        assertNotNull(RecipeToolAssembly.extractHead(cast(GeometryId.PICK_HEAD)));
    }
}
