package naturalexceptions.minecraft.item;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import naturalexceptions.core.fabrication.GeometryId;

/** Material-free creative carriers. Geometry alone never grants physical tool capabilities. */
public final class BlankGeometry {

    private static final String KEY = "BlankGeometry";

    private BlankGeometry() {}

    public static ItemStack create(Item item, GeometryId geometry) {
        ItemStack stack = new ItemStack(item);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString(KEY, geometry.id());
        stack.setTagCompound(tag);
        return stack;
    }

    public static GeometryId get(ItemStack stack) {
        if (stack == null || !stack.hasTagCompound()
            || !stack.getTagCompound()
                .hasKey(KEY, 8))
            return null;
        try {
            return GeometryId.parse(
                stack.getTagCompound()
                    .getString(KEY));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
