package naturalexceptions.minecraft.fabrication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import naturalexceptions.content.material.BuiltinMaterials;
import naturalexceptions.core.assembly.AssemblyGraph;
import naturalexceptions.core.assembly.DerivedCapability;
import naturalexceptions.core.fabrication.ComponentGeometry;
import naturalexceptions.core.fabrication.ComponentInstance;
import naturalexceptions.core.fabrication.GeometryId;
import naturalexceptions.core.fabrication.ManufacturingState;
import naturalexceptions.core.material.CanonicalMaterialKey;
import naturalexceptions.core.material.MaterialDefinition;
import naturalexceptions.minecraft.item.ItemGenericAssembly;
import naturalexceptions.minecraft.item.ItemGenericComponent;
import naturalexceptions.minecraft.item.ToolEdge;
import naturalexceptions.minecraft.recipe.RecipeToolEdge;
import naturalexceptions.minecraft.registry.NEItems;

/** Tools mine by what their heads are made of; a steel one takes an edge, which makes it faster. */
final class ToolTierTest {

    @BeforeAll
    static void setUp() {
        NEItems.ensureBootstrapped();
    }

    private static ItemStack pick(MaterialDefinition material) {
        ComponentInstance head = new ComponentInstance(
            GeometryId.PICK_HEAD,
            material.ref(),
            CanonicalMaterialKey.of(material.standardComposition()),
            ComponentGeometry.pickHead()
                .requiredMass(material.properties()),
            ManufacturingState.FINISHED);
        ComponentInstance handle = ItemGenericComponent.getComponent(ItemGenericComponent.createWoodenHandle());
        return ItemGenericAssembly.createStack(AssemblyGraph.ofTool(head, handle));
    }

    private static int level(MaterialDefinition material) {
        return NEItems.genericAssembly.getHarvestLevel(pick(material), "pickaxe");
    }

    private static InventoryCrafting grid(ItemStack... stacks) {
        InventoryCrafting inv = new InventoryCrafting(new Container() {

            @Override
            public boolean canInteractWith(net.minecraft.entity.player.EntityPlayer player) {
                return true;
            }
        }, 3, 3);
        for (int i = 0; i < stacks.length; i++) inv.setInventorySlotContents(i, stacks[i]);
        return inv;
    }

    @Test
    void eachMetalMinesAtItsOwnTier() {
        assertEquals(1, level(BuiltinMaterials.COPPER), "copper mines like stone");
        assertEquals(2, level(BuiltinMaterials.IRON), "iron mines like iron");
        assertEquals(2, level(BuiltinMaterials.BRONZE), "and bronze");
        assertEquals(3, level(BuiltinMaterials.STEEL), "steel mines like diamond");
        assertEquals(0, level(BuiltinMaterials.GOLD), "soft gold, like wood");
        assertEquals(0, DerivedCapability.harvestLevel(BuiltinMaterials.TIN_PROPERTIES.hardness()));
    }

    @Test
    void onlyASteelHeadTakesAnEdge() {
        RecipeToolEdge recipe = new RecipeToolEdge();
        ItemStack shard = new ItemStack(NEItems.lumeniteShard);
        assertNull(
            recipe.getCraftingResult(grid(pick(BuiltinMaterials.IRON), shard, shard.copy(), shard.copy())),
            "iron is too soft to hold an edge");
        ItemStack edged = recipe
            .getCraftingResult(grid(pick(BuiltinMaterials.STEEL), shard, shard.copy(), shard.copy()));
        assertNotNull(edged, "steel takes a Lumenite edge");
        assertEquals(ToolEdge.LUMENITE, ToolEdge.of(edged));
        assertNull(recipe.getCraftingResult(grid(edged, shard, shard.copy(), shard.copy())), "and only one");
        assertNull(
            recipe.getCraftingResult(grid(pick(BuiltinMaterials.STEEL), shard, shard.copy())),
            "it takes three shards");
        assertTrue(
            NEItems.genericAssembly.getDigSpeed(edged, net.minecraft.init.Blocks.stone, 0) > NEItems.genericAssembly
                .getDigSpeed(pick(BuiltinMaterials.STEEL), net.minecraft.init.Blocks.stone, 0),
            "an edge cuts faster");
        assertTrue(
            NEItems.genericAssembly.getItemStackDisplayName(edged)
                .startsWith("Lumenite-edged"));
    }
}
