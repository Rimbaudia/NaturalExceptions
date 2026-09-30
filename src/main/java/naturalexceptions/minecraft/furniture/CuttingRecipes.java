package naturalexceptions.minecraft.furniture;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import naturalexceptions.minecraft.block.BlockSkystoneDecor;
import naturalexceptions.minecraft.block.BlockSkystoneSlab;
import naturalexceptions.minecraft.registry.NEBlocks;

/**
 * What a cutting bench makes, one piece at a time with none of the crafting grid's waste: a Mason's Bench dresses
 * Skystone, a Sawmill cuts logs. Each recipe takes {@code count} of an input and gives its output.
 */
public final class CuttingRecipes {

    /** The kinds of bench. */
    public enum Kind {
        MASON,
        SAW
    }

    public static final class Recipe {

        final Item input;
        /** The input's metadata, or -1 for any. */
        final int inputMeta;
        final int count;
        final ItemStack output;

        Recipe(Item input, int inputMeta, int count, ItemStack output) {
            this.input = input;
            this.inputMeta = inputMeta;
            this.count = count;
            this.output = output;
        }

        boolean takes(ItemStack stack) {
            return stack != null && stack.getItem() == input
                && (inputMeta < 0 || stack.getItemDamage() == inputMeta)
                && stack.stackSize >= count;
        }

        public ItemStack output() {
            return output.copy();
        }

        public int count() {
            return count;
        }
    }

    private CuttingRecipes() {}

    /** The recipes of this kind of bench that take {@code input} (as many as it has room for), in order. */
    public static List<Recipe> forInput(Kind kind, ItemStack input) {
        List<Recipe> out = new ArrayList<>();
        if (input == null) return out;
        for (Recipe r : all(kind)) if (r.takes(input)) out.add(r);
        return out;
    }

    /** Whether anything at this kind of bench is made from {@code stack}. */
    public static boolean accepts(Kind kind, ItemStack stack) {
        if (stack == null) return false;
        for (Recipe r : all(kind))
            if (stack.getItem() == r.input && (r.inputMeta < 0 || stack.getItemDamage() == r.inputMeta)) return true;
        return false;
    }

    private static List<Recipe> mason, saw;

    static List<Recipe> all(Kind kind) {
        if (kind == Kind.MASON) {
            if (mason == null) mason = masonry();
            return mason;
        }
        if (saw == null) saw = sawing();
        return saw;
    }

    /** Skystone (the rock) or whole Skystone Bricks, dressed into every Skystone piece, one for one. */
    private static List<Recipe> masonry() {
        List<Recipe> out = new ArrayList<>();
        Block rock = NEBlocks.skystoneRock, bricks = NEBlocks.skystoneBricks, decor = NEBlocks.skystoneDecor;
        if (rock == null || bricks == null || decor == null) return out;
        List<ItemStack> pieces = new ArrayList<>();
        pieces.add(new ItemStack(bricks, 1, 0));
        pieces.add(new ItemStack(bricks, 1, 1));
        pieces.add(new ItemStack(decor, 1, BlockSkystoneDecor.TILES));
        pieces.add(new ItemStack(decor, 1, BlockSkystoneDecor.POLISHED));
        pieces.add(new ItemStack(decor, 1, BlockSkystoneDecor.CHISELED));
        if (NEBlocks.skystonePillar != null) pieces.add(new ItemStack(NEBlocks.skystonePillar, 1, 0));
        if (NEBlocks.skystoneStairs != null) pieces.add(new ItemStack(NEBlocks.skystoneStairs, 1, 0));
        if (NEBlocks.skystoneSlab != null) {
            pieces.add(new ItemStack(NEBlocks.skystoneSlab, 2, BlockSkystoneSlab.BRICK));
            pieces.add(new ItemStack(NEBlocks.skystoneSlab, 2, BlockSkystoneSlab.TILES));
            pieces.add(new ItemStack(NEBlocks.skystoneSlab, 2, BlockSkystoneSlab.POLISHED));
        }
        for (ItemStack piece : pieces) out.add(new Recipe(Item.getItemFromBlock(rock), -1, 1, piece));
        for (ItemStack piece : pieces) {
            if (piece.getItem() == Item.getItemFromBlock(bricks) && piece.getItemDamage() == 0) continue;
            out.add(new Recipe(Item.getItemFromBlock(bricks), 0, 1, piece));
        }
        return out;
    }

    /** Logs of each wood into planks, slabs, stairs, sticks and fencing, more than the grid gives. */
    private static List<Recipe> sawing() {
        List<Recipe> out = new ArrayList<>();
        Block[] stairs = { Blocks.oak_stairs, Blocks.spruce_stairs, Blocks.birch_stairs, Blocks.jungle_stairs,
            Blocks.acacia_stairs, Blocks.dark_oak_stairs };
        for (int wood = 0; wood < 6; wood++) {
            Item log = Item.getItemFromBlock(wood < 4 ? Blocks.log : Blocks.log2);
            int logMeta = wood < 4 ? wood : wood - 4;
            out.add(new Recipe(log, logMeta, 1, new ItemStack(Blocks.planks, 6, wood)));
            out.add(new Recipe(log, logMeta, 1, new ItemStack(Blocks.wooden_slab, 8, wood)));
            out.add(new Recipe(log, logMeta, 1, new ItemStack(stairs[wood], 4)));
            out.add(new Recipe(log, logMeta, 1, new ItemStack(Items.stick, 12)));
            out.add(new Recipe(log, logMeta, 1, new ItemStack(Blocks.fence, 3)));
            out.add(new Recipe(log, logMeta, 1, new ItemStack(Blocks.trapdoor, 2)));
        }
        return out;
    }
}
