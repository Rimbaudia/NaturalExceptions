package naturalexceptions.minecraft.registry;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;

import cpw.mods.fml.common.registry.GameRegistry;

public final class NEItems {

    public static Item copperOreChunk;
    public static Item copperIngot;

    private NEItems() {}

    public static void register() {
        if (copperOreChunk != null) return;

        copperOreChunk = new Item().setUnlocalizedName("copperOreChunk")
            .setTextureName("naturalexceptions:copper_ore_chunk")
            .setCreativeTab(CreativeTabs.tabMaterials);

        copperIngot = new Item().setUnlocalizedName("copperIngot")
            .setTextureName("naturalexceptions:copper_ingot")
            .setCreativeTab(CreativeTabs.tabMaterials);

        GameRegistry.registerItem(copperOreChunk, "copper_ore_chunk");
        GameRegistry.registerItem(copperIngot, "copper_ingot");
    }
}
