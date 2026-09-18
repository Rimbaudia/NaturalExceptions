package naturalexceptions.minecraft.item;

import java.util.List;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import naturalexceptions.core.fabrication.ComponentGeometry;
import naturalexceptions.core.fabrication.GeometryId;
import naturalexceptions.minecraft.registry.NEItems;

/**
 * Minecraft item carrier representing a material-agnostic casting mold.
 */
public final class ItemCastingMold extends Item {

    public static final String KEY_GEOMETRY = "TargetGeometry";

    public ItemCastingMold() {
        setUnlocalizedName("castingMold");
        setTextureName("naturalexceptions:ingot_casting_mold");
        setMaxStackSize(16);
    }

    public static ItemStack createStack(GeometryId geometry) {
        Item item = NEItems.castingMold != null ? NEItems.castingMold : new ItemCastingMold();
        ItemStack stack = new ItemStack(item);
        if (geometry != null) {
            NBTTagCompound nbt = new NBTTagCompound();
            nbt.setString(KEY_GEOMETRY, geometry.id());
            stack.setTagCompound(nbt);
        }
        return stack;
    }

    public static GeometryId getGeometry(ItemStack stack) {
        if (stack == null || !stack.hasTagCompound()
            || !stack.getTagCompound()
                .hasKey(KEY_GEOMETRY, 8)) {
            return null;
        }
        return GeometryId.parse(
            stack.getTagCompound()
                .getString(KEY_GEOMETRY));
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        GeometryId geom = getGeometry(stack);
        if (geom == null) {
            return "Casting Mold";
        }
        return "Casting Mold (" + formatGeometryName(geom) + ")";
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean advanced) {
        GeometryId geom = getGeometry(stack);
        if (geom == null) {
            list.add("\u00a77Universal empty mold");
            return;
        }

        list.add("\u00a76Cavity: " + formatGeometryName(geom));
        double volume = getVolume(geom);
        list.add(String.format("  \u00a77Volume: %.1f cm\u00b3", volume));
        list.add("  \u00a77Accepts: Any compatible molten metal");
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void getSubItems(Item item, CreativeTabs tab, List list) {
        list.add(createStack(GeometryId.BLANK));
        list.add(createStack(GeometryId.PICK_HEAD));
        list.add(createStack(GeometryId.PLATE));
    }

    private static double getVolume(GeometryId geom) {
        switch (geom) {
            case PICK_HEAD:
                return ComponentGeometry.pickHead()
                    .volumeCm3();
            case PLATE:
                return ComponentGeometry.plate()
                    .volumeCm3();
            case ROD:
                return ComponentGeometry.rod()
                    .volumeCm3();
            case BLADE:
                return ComponentGeometry.blade()
                    .volumeCm3();
            case HANDLE:
                return ComponentGeometry.handle()
                    .volumeCm3();
            case BLANK:
            default:
                return ComponentGeometry.blank()
                    .volumeCm3();
        }
    }

    private static String formatGeometryName(GeometryId id) {
        if (id == null) return "Unknown";
        switch (id) {
            case PICK_HEAD:
                return "Pick Head";
            case HANDLE:
                return "Handle";
            case BLADE:
                return "Blade";
            case PLATE:
                return "Plate";
            case ROD:
                return "Rod";
            case BLANK:
                return "Ingot / Blank";
            default:
                return id.id();
        }
    }
}
