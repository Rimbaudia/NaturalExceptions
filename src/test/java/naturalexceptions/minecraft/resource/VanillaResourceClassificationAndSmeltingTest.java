package naturalexceptions.minecraft.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import naturalexceptions.content.BuiltinContent;
import naturalexceptions.core.material.ResourceMaterialClass;
import naturalexceptions.core.material.VanillaResourceClassification;
import naturalexceptions.minecraft.metallurgy.TileEntityStarterSmelter;
import naturalexceptions.minecraft.registry.NEBlocks;
import naturalexceptions.minecraft.registry.NEItems;

public final class VanillaResourceClassificationAndSmeltingTest {

    @BeforeAll
    static void init() {
        try {
            net.minecraft.init.Bootstrap.func_151354_b();
        } catch (Throwable ignored) {}
        BuiltinContent.register();
        NEItems.register();
        NEBlocks.register();
    }

    @Test
    void metallicResourcesAreMeltableAndClassified() {
        ItemStack ironIngot = new ItemStack(Items.iron_ingot);
        ItemStack ironOre = new ItemStack(Blocks.iron_ore);
        ItemStack ironBlock = new ItemStack(Blocks.iron_block);

        ItemStack goldIngot = new ItemStack(Items.gold_ingot);
        ItemStack goldOre = new ItemStack(Blocks.gold_ore);
        ItemStack goldBlock = new ItemStack(Blocks.gold_block);

        ItemStack copperIngot = new ItemStack(NEItems.copperIngot);
        ItemStack copperOre = new ItemStack(NEBlocks.copperOre);
        ItemStack copperBlock = new ItemStack(NEBlocks.copperBlock);

        for (ItemStack stack : new ItemStack[] { ironIngot, ironOre, ironBlock, goldIngot, goldOre, goldBlock,
            copperIngot, copperOre, copperBlock }) {
            if (stack.getItem() == null) continue;
            String name = stack.getItem()
                .getUnlocalizedName();
            assertEquals(
                ResourceMaterialClass.METALLIC,
                VanillaResourceClassification.getClassification(stack),
                "Must classify as METALLIC: " + name);
            assertTrue(VanillaResourceClassification.isMeltable(stack), "Must be meltable: " + name);
            assertNull(
                VanillaResourceClassification.getRejectionReason(stack),
                "Rejection reason must be null for meltable: " + name);
            assertNotNull(
                VanillaResourceClassification.getMaterialRef(stack),
                "MaterialRef must resolve for metallic resource: " + name);
        }
    }

    @Test
    void crystallineAndMineralResourcesAreNotMeltableAndProvideInformativeReasons() {
        ItemStack diamond = new ItemStack(Items.diamond);
        ItemStack diamondOre = new ItemStack(Blocks.diamond_ore);
        assertEquals(ResourceMaterialClass.CARBON_CRYSTAL, VanillaResourceClassification.getClassification(diamond));
        assertEquals(ResourceMaterialClass.CARBON_CRYSTAL, VanillaResourceClassification.getClassification(diamondOre));
        assertFalse(VanillaResourceClassification.isMeltable(diamond));
        assertNotNull(VanillaResourceClassification.getRejectionReason(diamond));
        assertTrue(
            VanillaResourceClassification.getRejectionReason(diamond)
                .contains("Diamond is crystalline carbon"));

        ItemStack emerald = new ItemStack(Items.emerald);
        ItemStack emeraldOre = new ItemStack(Blocks.emerald_ore);
        assertEquals(ResourceMaterialClass.BERYL_CRYSTAL, VanillaResourceClassification.getClassification(emerald));
        assertEquals(ResourceMaterialClass.BERYL_CRYSTAL, VanillaResourceClassification.getClassification(emeraldOre));
        assertFalse(VanillaResourceClassification.isMeltable(emerald));
        assertNotNull(VanillaResourceClassification.getRejectionReason(emerald));
        assertTrue(
            VanillaResourceClassification.getRejectionReason(emerald)
                .contains("Emerald is a gemstone crystal"));

        ItemStack quartz = new ItemStack(Items.quartz);
        ItemStack quartzOre = new ItemStack(Blocks.quartz_ore);
        assertEquals(ResourceMaterialClass.SILICATE_CRYSTAL, VanillaResourceClassification.getClassification(quartz));
        assertEquals(
            ResourceMaterialClass.SILICATE_CRYSTAL,
            VanillaResourceClassification.getClassification(quartzOre));
        assertFalse(VanillaResourceClassification.isMeltable(quartz));
        assertNotNull(VanillaResourceClassification.getRejectionReason(quartz));
        assertTrue(
            VanillaResourceClassification.getRejectionReason(quartz)
                .contains("Nether quartz is a crystalline mineral"));

        ItemStack lapis = new ItemStack(Items.dye, 1, 4);
        ItemStack lapisOre = new ItemStack(Blocks.lapis_ore);
        assertEquals(ResourceMaterialClass.MINERAL_ROCK, VanillaResourceClassification.getClassification(lapis));
        assertEquals(ResourceMaterialClass.MINERAL_ROCK, VanillaResourceClassification.getClassification(lapisOre));
        assertFalse(VanillaResourceClassification.isMeltable(lapis));
        assertNotNull(VanillaResourceClassification.getRejectionReason(lapis));
        assertTrue(
            VanillaResourceClassification.getRejectionReason(lapis)
                .contains("Lapis lazuli is a sedimentary mineral rock"));

        ItemStack redstone = new ItemStack(Items.redstone);
        ItemStack redstoneOre = new ItemStack(Blocks.redstone_ore);
        assertEquals(ResourceMaterialClass.REACTIVE_MINERAL, VanillaResourceClassification.getClassification(redstone));
        assertEquals(
            ResourceMaterialClass.REACTIVE_MINERAL,
            VanillaResourceClassification.getClassification(redstoneOre));
        assertFalse(VanillaResourceClassification.isMeltable(redstone));
        assertNotNull(VanillaResourceClassification.getRejectionReason(redstone));
        assertTrue(
            VanillaResourceClassification.getRejectionReason(redstone)
                .contains("Redstone dust is a reactive mineral substance"));
    }

    @Test
    void fuelsAreClassifiedAndRejectedFromCrucible() {
        ItemStack coal = new ItemStack(Items.coal);
        ItemStack coalOre = new ItemStack(Blocks.coal_ore);
        assertEquals(ResourceMaterialClass.ORGANIC_FUEL, VanillaResourceClassification.getClassification(coal));
        assertEquals(ResourceMaterialClass.ORGANIC_FUEL, VanillaResourceClassification.getClassification(coalOre));
        assertFalse(VanillaResourceClassification.isMeltable(coal));
        assertNotNull(VanillaResourceClassification.getRejectionReason(coal));
        assertTrue(
            VanillaResourceClassification.getRejectionReason(coal)
                .contains("Coal is a combustible fuel"));

        ItemStack blazeRod = new ItemStack(Items.blaze_rod);
        ItemStack blazePowder = new ItemStack(Items.blaze_powder);
        assertEquals(ResourceMaterialClass.MAGICAL_FUEL, VanillaResourceClassification.getClassification(blazeRod));
        assertEquals(ResourceMaterialClass.MAGICAL_FUEL, VanillaResourceClassification.getClassification(blazePowder));
        assertFalse(VanillaResourceClassification.isMeltable(blazeRod));
        assertNotNull(VanillaResourceClassification.getRejectionReason(blazeRod));
        assertTrue(
            VanillaResourceClassification.getRejectionReason(blazeRod)
                .contains("Blaze fuel is a high-temperature combustible"));
    }

    @Test
    void starterSmelterRejectsNonMeltableResources() {
        TileEntityStarterSmelter smelter = new TileEntityStarterSmelter();

        ItemStack diamond = new ItemStack(Items.diamond, 1);
        ItemStack emerald = new ItemStack(Items.emerald, 1);
        ItemStack quartz = new ItemStack(Items.quartz, 1);
        ItemStack lapis = new ItemStack(Items.dye, 1, 4);
        ItemStack redstone = new ItemStack(Items.redstone, 1);
        ItemStack coal = new ItemStack(Items.coal, 1);
        ItemStack blazeRod = new ItemStack(Items.blaze_rod, 1);

        for (ItemStack nonMeltable : new ItemStack[] { diamond, emerald, quartz, lapis, redstone, coal, blazeRod }) {
            assertFalse(
                TileEntityStarterSmelter.isSmeltableFeed(nonMeltable),
                "isSmeltableFeed must reject: " + nonMeltable.getItem()
                    .getUnlocalizedName());
            boolean inserted = smelter.insertFeed(nonMeltable);
            assertFalse(
                inserted,
                "insertFeed must reject non-meltable: " + nonMeltable.getItem()
                    .getUnlocalizedName());
            assertEquals(
                0.0,
                smelter.vesselState()
                    .totalMass()
                    .toGrams(),
                1e-6,
                "Smelter crucible mass must remain zero after rejected feed insertion");
            assertEquals(1, nonMeltable.stackSize, "Stack size must not decrease when rejected");
        }
    }
}
