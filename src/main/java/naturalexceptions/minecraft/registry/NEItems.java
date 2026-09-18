package naturalexceptions.minecraft.registry;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;

import cpw.mods.fml.common.registry.GameRegistry;
import naturalexceptions.content.material.BuiltinMaterials;
import naturalexceptions.minecraft.item.ItemMaterialSample;

public final class NEItems {

    public static Item copperOreChunk;
    public static Item copperIngot;
    public static ItemMaterialSample materialSample;
    public static naturalexceptions.minecraft.item.ItemGenericAssembly genericAssembly;

    private NEItems() {}

    public static void register() {
        if (copperOreChunk != null) return;

        copperOreChunk = new Item().setUnlocalizedName("copperOreChunk")
            .setTextureName("naturalexceptions:copper_ore_chunk")
            .setCreativeTab(CreativeTabs.tabMaterials);

        copperIngot = new Item().setUnlocalizedName("copperIngot")
            .setTextureName("naturalexceptions:copper_ingot")
            .setCreativeTab(CreativeTabs.tabMaterials);

        materialSample = new ItemMaterialSample();
        materialSample.setCreativeTab(CreativeTabs.tabMaterials);

        genericAssembly = new naturalexceptions.minecraft.item.ItemGenericAssembly();
        genericAssembly.setCreativeTab(CreativeTabs.tabTools);

        GameRegistry.registerItem(copperOreChunk, "copper_ore_chunk");
        GameRegistry.registerItem(copperIngot, "copper_ingot");
        GameRegistry.registerItem(materialSample, "material_sample");
        GameRegistry.registerItem(genericAssembly, "generic_assembly");

        MaterialBindingRegistry.global()
            .bind(copperOreChunk, BuiltinMaterials.COPPER.id());
        MaterialBindingRegistry.global()
            .bind(copperIngot, BuiltinMaterials.COPPER.id());
    }
}
