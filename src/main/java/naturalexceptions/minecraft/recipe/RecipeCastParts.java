package naturalexceptions.minecraft.recipe;

import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;

import naturalexceptions.content.material.BuiltinMaterials;
import naturalexceptions.core.fabrication.ComponentInstance;
import naturalexceptions.core.fabrication.GeometryId;
import naturalexceptions.minecraft.item.ItemGenericComponent;

/**
 * Shaped survival recipe whose ingredients may be cold cast metal components of a given geometry (any metal).
 * Cast parts carry material in NBT, which vanilla/ore recipes cannot match. The consumed parts are recorded on the
 * result under {@link #PARTS_TAG} so the equipment keeps the provenance of the metal it was made from.
 */
public final class RecipeCastParts implements IRecipe {

    public static final String PARTS_TAG = "NEMadeFrom";
    /** Parts above this temperature are too hot to handle in a crafting grid. */
    public static final double HANDLING_LIMIT_K = 323.15;

    /** One grid cell: a plain item, a cast metal part of a geometry, or empty. */
    public static final class Cell {

        final Item item;
        final GeometryId castGeometry;

        private Cell(Item item, GeometryId castGeometry) {
            this.item = item;
            this.castGeometry = castGeometry;
        }

        public static Cell item(Item item) {
            return new Cell(item, null);
        }

        public static Cell block(net.minecraft.block.Block block) {
            return new Cell(Item.getItemFromBlock(block), null);
        }

        public static Cell cast(GeometryId geometry) {
            return new Cell(null, geometry);
        }

        boolean matches(ItemStack stack) {
            if (stack == null) return false;
            if (castGeometry == null) return item != null && stack.getItem() == item;
            if (!(stack.getItem() instanceof ItemGenericComponent)) return false;
            ComponentInstance component = ItemGenericComponent.getComponent(stack);
            if (component == null || component.geometryId() != castGeometry) return false;
            if (BuiltinMaterials.WOOD.ref()
                .equals(component.materialRef())) return false;
            return ItemGenericComponent.getTemperatureKelvin(stack) <= HANDLING_LIMIT_K;
        }
    }

    private final int width;
    private final int height;
    private final Cell[] cells;
    private final ItemStack output;

    /** {@code cells} is row-major, {@code width * height} long; null entries must be empty. */
    public RecipeCastParts(int width, int height, Cell[] cells, ItemStack output) {
        if (cells.length != width * height) throw new IllegalArgumentException("Pattern size mismatch");
        this.width = width;
        this.height = height;
        this.cells = cells.clone();
        this.output = output;
    }

    @Override
    public boolean matches(InventoryCrafting inv, World world) {
        return offset(inv) != null;
    }

    private int[] offset(InventoryCrafting inv) {
        int gridWidth = gridWidth(inv);
        int gridHeight = inv.getSizeInventory() / gridWidth;
        for (int dx = 0; dx <= gridWidth - width; dx++) {
            for (int dy = 0; dy <= gridHeight - height; dy++) {
                if (matchesAt(inv, gridWidth, gridHeight, dx, dy)) return new int[] { dx, dy };
            }
        }
        return null;
    }

    private static int gridWidth(InventoryCrafting inv) {
        int size = inv.getSizeInventory();
        return size == 4 ? 2 : 3;
    }

    private boolean matchesAt(InventoryCrafting inv, int gridWidth, int gridHeight, int dx, int dy) {
        for (int x = 0; x < gridWidth; x++) {
            for (int y = 0; y < gridHeight; y++) {
                ItemStack stack = inv.getStackInSlot(x + y * gridWidth);
                int px = x - dx;
                int py = y - dy;
                Cell cell = px >= 0 && py >= 0 && px < width && py < height ? cells[px + py * width] : null;
                if (cell == null) {
                    if (stack != null) return false;
                } else if (!cell.matches(stack)) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public ItemStack getCraftingResult(InventoryCrafting inv) {
        if (offset(inv) == null) return null;
        ItemStack result = output.copy();
        NBTTagList parts = new NBTTagList();
        for (int i = 0; i < inv.getSizeInventory(); i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (stack != null && stack.getItem() instanceof ItemGenericComponent && stack.hasTagCompound()) {
                parts.appendTag(
                    stack.getTagCompound()
                        .copy());
            }
        }
        if (parts.tagCount() > 0) {
            NBTTagCompound tag = result.hasTagCompound() ? result.getTagCompound() : new NBTTagCompound();
            tag.setTag(PARTS_TAG, parts);
            result.setTagCompound(tag);
        }
        return result;
    }

    @Override
    public int getRecipeSize() {
        return width * height;
    }

    @Override
    public ItemStack getRecipeOutput() {
        return output;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    /** Pattern cell at (x, y), or null for an empty cell; used by the knowledge browser. */
    public Cell cellAt(int x, int y) {
        return cells[x + y * width];
    }

    public static GeometryId castGeometryOf(Cell cell) {
        return cell == null ? null : cell.castGeometry;
    }

    public static Item itemOf(Cell cell) {
        return cell == null ? null : cell.item;
    }
}
