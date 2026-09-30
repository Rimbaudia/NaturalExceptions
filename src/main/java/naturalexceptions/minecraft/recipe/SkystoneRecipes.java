package naturalexceptions.minecraft.recipe;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import cpw.mods.fml.common.registry.GameRegistry;
import naturalexceptions.minecraft.block.BlockSkyslate;
import naturalexceptions.minecraft.block.BlockSkyslateSlab;
import naturalexceptions.minecraft.block.BlockSkystoneDecor;
import naturalexceptions.minecraft.block.BlockSkystoneSlab;
import naturalexceptions.minecraft.furniture.BlockWoodenTable;
import naturalexceptions.minecraft.registry.NEBlocks;

/**
 * Dressing Skystone Bricks into the rest of the family (tiles, polished, chiseled, pillars, stairs, slabs), darkening
 * them into Skyslate, and the furniture of the sky citadels.
 */
public final class SkystoneRecipes {

    private SkystoneRecipes() {}

    public static void register() {
        Block bricks = NEBlocks.skystoneBricks, decor = NEBlocks.skystoneDecor, slab = NEBlocks.skystoneSlab;
        if (bricks == null || decor == null
            || slab == null
            || NEBlocks.skystonePillar == null
            || NEBlocks.skystoneStairs == null) return;
        ItemStack brick = new ItemStack(bricks, 1, 0);
        ItemStack tiles = new ItemStack(decor, 1, BlockSkystoneDecor.TILES);
        ItemStack polished = new ItemStack(decor, 1, BlockSkystoneDecor.POLISHED);

        GameRegistry.addRecipe(new ItemStack(decor, 4, BlockSkystoneDecor.TILES), "BB", "BB", 'B', brick);
        GameRegistry.addSmelting(brick, polished.copy(), 0.1F);
        GameRegistry.addRecipe(
            new ItemStack(decor, 1, BlockSkystoneDecor.CHISELED),
            "S",
            "S",
            'S',
            new ItemStack(slab, 1, BlockSkystoneSlab.POLISHED));
        GameRegistry.addRecipe(new ItemStack(NEBlocks.skystonePillar, 2), "B", "B", 'B', polished);
        GameRegistry.addRecipe(new ItemStack(NEBlocks.skystoneStairs, 4), "B  ", "BB ", "BBB", 'B', brick);
        GameRegistry.addRecipe(new ItemStack(slab, 6, BlockSkystoneSlab.BRICK), "BBB", 'B', brick);
        GameRegistry.addRecipe(new ItemStack(slab, 6, BlockSkystoneSlab.TILES), "BBB", 'B', tiles);
        GameRegistry.addRecipe(new ItemStack(slab, 6, BlockSkystoneSlab.POLISHED), "BBB", 'B', polished);
        skyslate(brick);
        furniture();
    }

    private static void skyslate(ItemStack skystone) {
        Block slate = NEBlocks.skyslate, slab = NEBlocks.skyslateSlab;
        if (slate == null || slab == null || NEBlocks.skyslateStairs == null || NEBlocks.skyslateShingleStairs == null)
            return;
        ItemStack bricks = new ItemStack(slate, 1, BlockSkyslate.BRICKS);
        ItemStack shingles = new ItemStack(slate, 1, BlockSkyslate.SHINGLES);
        ItemStack coal = new ItemStack(Items.coal, 1, OreDictionary.WILDCARD_VALUE);
        GameRegistry
            .addRecipe(new ItemStack(slate, 4, BlockSkyslate.BRICKS), " B ", "BCB", " B ", 'B', skystone, 'C', coal);
        GameRegistry.addRecipe(new ItemStack(slate, 4, BlockSkyslate.SHINGLES), "BB", "BB", 'B', bricks);
        GameRegistry.addRecipe(new ItemStack(NEBlocks.skyslateStairs, 4), "B  ", "BB ", "BBB", 'B', bricks);
        GameRegistry.addRecipe(new ItemStack(NEBlocks.skyslateShingleStairs, 4), "B  ", "BB ", "BBB", 'B', shingles);
        GameRegistry.addRecipe(new ItemStack(slab, 6, BlockSkyslateSlab.BRICK), "BBB", 'B', bricks);
        GameRegistry.addRecipe(new ItemStack(slab, 6, BlockSkyslateSlab.SHINGLES), "BBB", 'B', shingles);
    }

    private static void furniture() {
        ItemStack planks = new ItemStack(Blocks.planks, 1, OreDictionary.WILDCARD_VALUE);
        ItemStack slabs = new ItemStack(Blocks.wooden_slab, 1, OreDictionary.WILDCARD_VALUE);
        if (NEBlocks.crate != null)
            GameRegistry.addRecipe(new ItemStack(NEBlocks.crate), "PSP", "S S", "PSP", 'P', planks, 'S', Items.stick);
        if (NEBlocks.barrel != null) GameRegistry
            .addRecipe(new ItemStack(NEBlocks.barrel), "PIP", "P P", "PPP", 'P', planks, 'I', Items.iron_ingot);
        if (NEBlocks.pantryShelf != null) GameRegistry
            .addRecipe(new ItemStack(NEBlocks.pantryShelf), "PPP", "S S", "PPP", 'P', planks, 'S', Items.stick);
        if (NEBlocks.marketCounter != null) {
            Block counter = NEBlocks.marketCounter;
            GameRegistry.addRecipe(new ItemStack(counter, 1, 0), "SSS", "P P", "PPP", 'S', slabs, 'P', planks);
            Object[] goods = { Items.apple, Items.fish, Items.flower_pot,
                new ItemStack(Blocks.wool, 1, OreDictionary.WILDCARD_VALUE) };
            for (int kind = 0; kind < goods.length; kind++) GameRegistry.addShapelessRecipe(
                new ItemStack(counter, 1, kind),
                new ItemStack(counter, 1, OreDictionary.WILDCARD_VALUE),
                goods[kind]);
        }
        if (NEBlocks.woodenTable != null)
            for (int wood = 0; wood < BlockWoodenTable.PLANKS.length; wood++) GameRegistry.addRecipe(
                new ItemStack(NEBlocks.woodenTable, 1, wood),
                "SSS",
                " F ",
                " F ",
                'S',
                new ItemStack(Blocks.wooden_slab, 1, BlockWoodenTable.PLANKS[wood]),
                'F',
                Blocks.fence);
        Object skystone = NEBlocks.skystoneBricks;
        if (NEBlocks.candelabra != null) GameRegistry.addRecipe(
            new ItemStack(NEBlocks.candelabra),
            "SSS",
            "GGG",
            " G ",
            'S',
            Items.string,
            'G',
            Items.gold_ingot);
        if (NEBlocks.brazier != null && skystone != null) GameRegistry
            .addRecipe(new ItemStack(NEBlocks.brazier), "SCS", " S ", "S S", 'S', skystone, 'C', Items.coal);
        if (NEBlocks.stoneBench != null && NEBlocks.skystoneSlab != null && skystone != null) GameRegistry.addRecipe(
            new ItemStack(NEBlocks.stoneBench, 2),
            "LLL",
            "S S",
            'L',
            new ItemStack(NEBlocks.skystoneSlab, 1, OreDictionary.WILDCARD_VALUE),
            'S',
            skystone);
        if (NEBlocks.pew != null)
            GameRegistry.addRecipe(new ItemStack(NEBlocks.pew, 2), "P  ", "PPP", "T T", 'P', planks, 'T', Items.stick);
        if (NEBlocks.fountain != null && skystone != null) GameRegistry
            .addRecipe(new ItemStack(NEBlocks.fountain), "S S", "SWS", "SSS", 'S', skystone, 'W', Items.water_bucket);
        if (NEBlocks.orrery != null) GameRegistry.addRecipe(
            new ItemStack(NEBlocks.orrery),
            " C ",
            "GGG",
            " P ",
            'C',
            Items.clock,
            'G',
            Items.gold_ingot,
            'P',
            planks);
        if (NEBlocks.chartTable != null) GameRegistry.addRecipe(
            new ItemStack(NEBlocks.chartTable),
            " M ",
            "SSS",
            "F F",
            'M',
            Items.map,
            'S',
            slabs,
            'F',
            Blocks.fence);
        if (naturalexceptions.minecraft.registry.NEItems.surveyorsChart != null
            && naturalexceptions.minecraft.registry.NEItems.stormglassShard != null)
            GameRegistry.addShapelessRecipe(
                new ItemStack(naturalexceptions.minecraft.registry.NEItems.surveyorsChart),
                Items.map,
                naturalexceptions.minecraft.registry.NEItems.stormglassShard);
        if (NEBlocks.wallShelf != null)
            GameRegistry.addRecipe(new ItemStack(NEBlocks.wallShelf, 3), "SSS", "T T", 'S', slabs, 'T', Items.stick);
        if (NEBlocks.weaponRack != null) GameRegistry
            .addRecipe(new ItemStack(NEBlocks.weaponRack), "T T", "PTP", "T T", 'T', Items.stick, 'P', planks);
        if (NEBlocks.marketAwning != null) {
            int[] wool = { 14, 11, 13, 4 };
            for (int colour = 0; colour < wool.length; colour++) GameRegistry.addRecipe(
                new ItemStack(NEBlocks.marketAwning, 4, colour << 2),
                "CWC",
                "S S",
                'C',
                new ItemStack(Blocks.wool, 1, wool[colour]),
                'W',
                new ItemStack(Blocks.wool, 1, 0),
                'S',
                Items.stick);
        }
    }
}
