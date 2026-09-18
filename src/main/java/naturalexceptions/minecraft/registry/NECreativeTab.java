package naturalexceptions.minecraft.registry;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public final class NECreativeTab extends CreativeTabs {

    public static final NECreativeTab INSTANCE = new NECreativeTab();

    private NECreativeTab() {
        super("naturalExceptions");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public Item getTabIconItem() {
        return NEItems.copperIngot != null ? NEItems.copperIngot : net.minecraft.init.Items.iron_ingot;
    }
}
