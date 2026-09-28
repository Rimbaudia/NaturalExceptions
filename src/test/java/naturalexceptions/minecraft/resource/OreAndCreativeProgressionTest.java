package naturalexceptions.minecraft.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import naturalexceptions.content.BuiltinContent;
import naturalexceptions.minecraft.item.ItemGenericAssembly;
import naturalexceptions.minecraft.item.ItemGenericComponent;
import naturalexceptions.minecraft.metallurgy.FeedstockItemRegistry;
import naturalexceptions.minecraft.registry.NEBlocks;
import naturalexceptions.minecraft.registry.NECreativeTab;
import naturalexceptions.minecraft.registry.NEItems;

final class OreAndCreativeProgressionTest {

    @BeforeAll
    static void init() {
        net.minecraft.init.Bootstrap.func_151354_b();
        BuiltinContent.register();
        NEItems.register();
        NEBlocks.register();
    }

    @Test
    void copperOreDropsItselfEvenWithFortuneAndRemainsSmeltable() {
        for (int fortune : new int[] { 0, 1, 3, 10 }) {
            assertSame(
                Item.getItemFromBlock(NEBlocks.copperOre),
                NEBlocks.copperOre.getItemDropped(0, new Random(1), fortune));
            assertEquals(1, NEBlocks.copperOre.quantityDroppedWithBonus(fortune, new Random(1)));
        }
        assertTrue(FeedstockItemRegistry.resolve(new ItemStack(NEBlocks.copperOre)) != null);
    }

    @Test
    void creativeComponentsAndToolsAreUnboundGeometryTemplates() {
        List<ItemStack> components = new ArrayList<>();
        NEItems.genericComponent.getSubItems(NEItems.genericComponent, NECreativeTab.INSTANCE, components);
        assertEquals(10, components.size());
        for (ItemStack stack : components) {
            assertNull(ItemGenericComponent.getComponent(stack));
            assertTrue(
                stack.getDisplayName()
                    .startsWith("Blank "),
                stack.getDisplayName());
        }
        List<ItemStack> tools = new ArrayList<>();
        NEItems.genericAssembly.getSubItems(NEItems.genericAssembly, NECreativeTab.INSTANCE, tools);
        assertEquals(5, tools.size());
        for (ItemStack stack : tools) {
            assertNull(ItemGenericAssembly.getAssembly(stack));
            assertTrue(
                stack.getDisplayName()
                    .startsWith("Blank "),
                stack.getDisplayName());
            assertFalse(
                stack.getItem()
                    .canHarvestBlock(NEBlocks.copperOre, stack));
        }
    }
}
