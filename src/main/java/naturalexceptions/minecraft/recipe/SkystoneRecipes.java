package naturalexceptions.minecraft.recipe;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;

import cpw.mods.fml.common.registry.GameRegistry;
import naturalexceptions.minecraft.block.BlockSkystoneDecor;
import naturalexceptions.minecraft.block.BlockSkystoneSlab;
import naturalexceptions.minecraft.registry.NEBlocks;

/** Dressing Skystone Bricks into the rest of the family: tiles, polished, chiseled, pillars, stairs and slabs. */
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
    }
}
