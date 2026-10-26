package naturalexceptions.minecraft.recipe;

import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.world.World;

import naturalexceptions.content.material.BuiltinMaterials;
import naturalexceptions.core.assembly.AssemblyGraph;
import naturalexceptions.core.assembly.DerivedCapability;
import naturalexceptions.minecraft.item.ItemGenericAssembly;
import naturalexceptions.minecraft.item.ItemLumeniteShard;
import naturalexceptions.minecraft.item.ToolEdge;

/**
 * Inlays an edge along a tool's head: the tool and three shards of Lumenite, or three samples of Fused Impact Crust,
 * anywhere in the grid. Only a head that mines like diamond (steel's) is hard enough to hold an edge, and it takes
 * one only.
 */
public final class RecipeToolEdge implements IRecipe {

    public static final int PIECES = 3;

    /** The edge the grid makes, or null if it makes none. */
    static ToolEdge edge(InventoryCrafting inv) {
        ItemStack tool = null;
        int lumenite = 0, crust = 0, other = 0;
        for (int i = 0; i < inv.getSizeInventory(); i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (stack == null) continue;
            if (stack.getItem() instanceof ItemGenericAssembly) {
                if (tool != null) return null;
                tool = stack;
            } else if (stack.getItem() instanceof ItemLumeniteShard) lumenite++;
            else if (RecipeSkyfall.isSampleOf(stack, BuiltinMaterials.IMPACT_CRUST.ref())) crust++;
            else other++;
        }
        if (tool == null || other > 0 || ToolEdge.of(tool) != null || !holdsAnEdge(tool)) return null;
        if (lumenite == PIECES && crust == 0) return ToolEdge.LUMENITE;
        if (crust == PIECES && lumenite == 0) return ToolEdge.CRUST;
        return null;
    }

    /** Whether a tool's head is hard enough to take an edge. */
    public static boolean holdsAnEdge(ItemStack tool) {
        AssemblyGraph assembly = ItemGenericAssembly.getAssembly(tool);
        if (assembly == null || assembly.isBroken() || assembly.head() == null) return false;
        DerivedCapability capability = ItemGenericAssembly.getCapability(assembly);
        return capability != null && capability.harvestLevel() >= 3;
    }

    private static ItemStack tool(InventoryCrafting inv) {
        for (int i = 0; i < inv.getSizeInventory(); i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (stack != null && stack.getItem() instanceof ItemGenericAssembly) return stack;
        }
        return null;
    }

    @Override
    public boolean matches(InventoryCrafting inv, World world) {
        return edge(inv) != null;
    }

    @Override
    public ItemStack getCraftingResult(InventoryCrafting inv) {
        ToolEdge edge = edge(inv);
        if (edge == null) return null;
        ItemStack out = tool(inv).copy();
        out.stackSize = 1;
        edge.setOn(out);
        return out;
    }

    @Override
    public int getRecipeSize() {
        return PIECES + 1;
    }

    @Override
    public ItemStack getRecipeOutput() {
        return null;
    }
}
