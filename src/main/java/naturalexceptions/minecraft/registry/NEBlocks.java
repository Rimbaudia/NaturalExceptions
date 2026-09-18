package naturalexceptions.minecraft.registry;

import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;

import cpw.mods.fml.common.registry.GameRegistry;
import naturalexceptions.content.material.BuiltinMaterials;
import naturalexceptions.minecraft.block.BlockCopperBlock;
import naturalexceptions.minecraft.block.BlockCopperOre;

public final class NEBlocks {

    public static Block copperOre;
    public static Block copperBlock;
    public static Block starterSmelter;

    private NEBlocks() {}

    public static void register() {
        if (copperOre != null) return;

        copperOre = new BlockCopperOre().setCreativeTab(CreativeTabs.tabBlock);
        copperBlock = new BlockCopperBlock().setCreativeTab(CreativeTabs.tabBlock);
        starterSmelter = new naturalexceptions.minecraft.metallurgy.BlockStarterSmelter()
            .setCreativeTab(CreativeTabs.tabDecorations);

        GameRegistry.registerBlock(copperOre, "copper_ore");
        GameRegistry.registerBlock(copperBlock, "copper_block");
        GameRegistry.registerBlock(starterSmelter, "starter_smelter");

        GameRegistry.registerTileEntity(
            naturalexceptions.minecraft.metallurgy.TileEntityStarterSmelter.class,
            "starter_smelter");

        MaterialBindingRegistry.global()
            .bind(copperOre, BuiltinMaterials.COPPER.id());
        MaterialBindingRegistry.global()
            .bind(copperBlock, BuiltinMaterials.COPPER.id());
    }
}
