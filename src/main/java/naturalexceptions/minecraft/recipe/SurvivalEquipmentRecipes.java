package naturalexceptions.minecraft.recipe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import cpw.mods.fml.common.registry.GameRegistry;
import naturalexceptions.core.fabrication.GeometryId;
import naturalexceptions.minecraft.recipe.RecipeCastParts.Cell;
import naturalexceptions.minecraft.registry.NEBlocks;
import naturalexceptions.minecraft.registry.NEItems;

/**
 * Survival routes for the P4/P5 equipment that previously existed only in the creative tab. Each uses parts the
 * player has already produced at that point in the loop: refractory brick before the smelter, cast parts after the
 * first pour.
 */
public final class SurvivalEquipmentRecipes {

    private static final List<RecipeCastParts> REGISTERED = new ArrayList<>();

    private SurvivalEquipmentRecipes() {}

    /** Recipes built by {@link #register()}, exposed for tests and the knowledge browser. */
    public static List<RecipeCastParts> recipes() {
        if (REGISTERED.isEmpty()) REGISTERED.addAll(build());
        return Collections.unmodifiableList(REGISTERED);
    }

    static List<RecipeCastParts> build() {
        List<RecipeCastParts> recipes = new ArrayList<>();
        Cell ingot = Cell.cast(GeometryId.INGOT);
        Cell rod = Cell.cast(GeometryId.ROD);
        Cell plate = Cell.cast(GeometryId.PLATE);
        Cell stick = Cell.item(Items.stick);
        Cell brickItem = Cell.item(NEItems.refractoryBrick);
        Cell brickBlock = Cell.block(NEBlocks.refractoryBrick);
        Cell stone = Cell.block(Blocks.stone);

        // Blacksmith Hammer: a cast ingot as the head on a stick.
        if (NEItems.blacksmithHammer != null) {
            recipes
                .add(new RecipeCastParts(1, 2, new Cell[] { ingot, stick }, new ItemStack(NEItems.blacksmithHammer)));
        }
        // Blacksmith Tongs: two cast rods crossed.
        if (NEItems.blacksmithTongs != null) {
            recipes.add(
                new RecipeCastParts(2, 2, new Cell[] { rod, null, null, rod }, new ItemStack(NEItems.blacksmithTongs)));
        }
        // Foundry Ladle: a fired refractory bowl on a stick; usable before any metal has been cast.
        if (NEItems.foundryLadle != null) {
            recipes.add(
                new RecipeCastParts(
                    3,
                    2,
                    new Cell[] { brickItem, null, brickItem, null, brickItem, stick },
                    new ItemStack(NEItems.foundryLadle)));
        }
        // Forging Anvil: three cast plates faced onto a stone block body.
        if (NEBlocks.forgingAnvil != null) {
            recipes.add(
                new RecipeCastParts(
                    3,
                    2,
                    new Cell[] { plate, plate, plate, stone, stone, stone },
                    new ItemStack(NEBlocks.forgingAnvil)));
        }
        // Smelter Tuyere: a cast rod as the blast pipe through refractory.
        if (NEBlocks.smelterTuyere != null) {
            recipes.add(
                new RecipeCastParts(
                    3,
                    1,
                    new Cell[] { brickBlock, rod, brickBlock },
                    new ItemStack(NEBlocks.smelterTuyere)));
        }
        return recipes;
    }

    public static void register() {
        for (RecipeCastParts recipe : recipes()) {
            GameRegistry.addRecipe(recipe);
        }
        try {
            net.minecraftforge.oredict.RecipeSorter.register(
                "naturalexceptions:cast_parts",
                RecipeCastParts.class,
                net.minecraftforge.oredict.RecipeSorter.Category.SHAPED,
                "after:minecraft:shaped");
        } catch (Throwable ignored) {}
    }
}
