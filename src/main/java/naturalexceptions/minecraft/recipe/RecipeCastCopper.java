package naturalexceptions.minecraft.recipe;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.oredict.ShapedOreRecipe;

import naturalexceptions.content.material.BuiltinMaterials;
import naturalexceptions.core.fabrication.ComponentInstance;
import naturalexceptions.core.fabrication.GeometryId;
import naturalexceptions.minecraft.item.ItemGenericComponent;
import naturalexceptions.minecraft.registry.NEItems;

/**
 * A shaped recipe calling for copper ingots that also takes the copper ingots a player casts: a cast copper ingot
 * (an ingot mold's casting of pure copper) wherever the recipe shows NE's copper ingot, once it is cool enough to hold.
 * The two are different items (a casting keeps its mass and heat), and turning one into the other would not conserve
 * metal (a casting is about 0.22 kg, an ingot melts as 1 kg), so the recipes take either rather than convert.
 */
public final class RecipeCastCopper extends ShapedOreRecipe {

    /** Warm to the hand, no hotter: a casting fresh from the mold must cool first. */
    public static final double HANDLEABLE_KELVIN = 330.0;

    public RecipeCastCopper(ItemStack result, Object... recipe) {
        super(result, recipe);
    }

    @Override
    public boolean matches(InventoryCrafting grid, World world) {
        return super.matches(asIngots(grid), world);
    }

    @Override
    public ItemStack getCraftingResult(InventoryCrafting grid) {
        return super.getCraftingResult(asIngots(grid));
    }

    /** A cast ingot of pure copper, cool enough to hold. */
    public static boolean isCastCopperIngot(ItemStack stack) {
        if (stack == null || !(stack.getItem() instanceof ItemGenericComponent)) return false;
        ComponentInstance part = ItemGenericComponent.getComponent(stack);
        if (part == null || part.geometryId() != GeometryId.INGOT) return false;
        boolean copper = BuiltinMaterials.COPPER.ref()
            .equals(part.materialRef())
            || part.stateKey() != null && BuiltinMaterials.COPPER_COMPOSITION.equals(
                part.stateKey()
                    .composition());
        return copper && ItemGenericComponent.getTemperatureKelvin(stack) <= HANDLEABLE_KELVIN;
    }

    /** The grid as the recipe sees it: each cast copper ingot read as a copper ingot. */
    private static InventoryCrafting asIngots(InventoryCrafting grid) {
        boolean any = false;
        for (int i = 0; i < grid.getSizeInventory(); i++) if (isCastCopperIngot(grid.getStackInSlot(i))) any = true;
        if (!any || NEItems.copperIngot == null) return grid;
        int side = (int) Math.round(Math.sqrt(grid.getSizeInventory()));
        InventoryCrafting read = new InventoryCrafting(NO_CONTAINER, side, side);
        for (int i = 0; i < grid.getSizeInventory(); i++) {
            ItemStack stack = grid.getStackInSlot(i);
            read.setInventorySlotContents(
                i,
                isCastCopperIngot(stack) ? new ItemStack(NEItems.copperIngot) : stack == null ? null : stack.copy());
        }
        return read;
    }

    /** InventoryCrafting tells its container of every change; this one has nothing to tell. */
    private static final Container NO_CONTAINER = new Container() {

        @Override
        public boolean canInteractWith(EntityPlayer player) {
            return false;
        }

        @Override
        public void onCraftMatrixChanged(net.minecraft.inventory.IInventory inventory) {}
    };
}
