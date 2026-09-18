package naturalexceptions.minecraft.registry;

import net.minecraft.item.Item;

import cpw.mods.fml.common.registry.GameRegistry;
import naturalexceptions.content.material.BuiltinMaterials;
import naturalexceptions.minecraft.item.ItemCastingMold;
import naturalexceptions.minecraft.item.ItemGenericAssembly;
import naturalexceptions.minecraft.item.ItemGenericComponent;
import naturalexceptions.minecraft.item.ItemMaterialSample;

public final class NEItems {

    public static Item copperOreChunk;
    public static Item copperIngot;
    public static Item slagLump;
    public static ItemMaterialSample materialSample;
    public static ItemCastingMold castingMold;
    public static ItemGenericComponent genericComponent;
    public static ItemGenericAssembly genericAssembly;

    private NEItems() {}

    public static void register() {
        if (copperOreChunk != null) return;

        copperOreChunk = new Item().setUnlocalizedName("copperOreChunk")
            .setTextureName("naturalexceptions:copper_ore_chunk")
            .setCreativeTab(NECreativeTab.INSTANCE);

        copperIngot = new Item().setUnlocalizedName("copperIngot")
            .setTextureName("naturalexceptions:copper_ingot")
            .setCreativeTab(NECreativeTab.INSTANCE);

        slagLump = new Item().setUnlocalizedName("slagLump")
            .setTextureName("naturalexceptions:molten_slag_lump")
            .setCreativeTab(NECreativeTab.INSTANCE);

        materialSample = new ItemMaterialSample();
        materialSample.setCreativeTab(NECreativeTab.INSTANCE);

        castingMold = new ItemCastingMold();
        castingMold.setCreativeTab(NECreativeTab.INSTANCE);

        genericComponent = new ItemGenericComponent();
        genericComponent.setCreativeTab(NECreativeTab.INSTANCE);

        genericAssembly = new ItemGenericAssembly();
        genericAssembly.setCreativeTab(NECreativeTab.INSTANCE);

        try {
            GameRegistry.registerItem(copperOreChunk, "copper_ore_chunk");
            GameRegistry.registerItem(copperIngot, "copper_ingot");
            GameRegistry.registerItem(slagLump, "slag_lump");
            GameRegistry.registerItem(materialSample, "material_sample");
            GameRegistry.registerItem(castingMold, "casting_mold");
            GameRegistry.registerItem(genericComponent, "generic_component");
            GameRegistry.registerItem(genericAssembly, "generic_assembly");
        } catch (Throwable t) {
            // Allows unit testing without LaunchClassLoader
        }

        MaterialBindingRegistry.global()
            .bind(copperOreChunk, BuiltinMaterials.COPPER.id());
        MaterialBindingRegistry.global()
            .bind(copperIngot, BuiltinMaterials.COPPER.id());
    }
}
