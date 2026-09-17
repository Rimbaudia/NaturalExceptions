package naturalexceptions.minecraft.registry;

import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;

import cpw.mods.fml.common.registry.GameRegistry;
import naturalexceptions.minecraft.block.BlockCopperBlock;
import naturalexceptions.minecraft.block.BlockCopperOre;

public final class NEBlocks {

    public static Block copperOre;
    public static Block copperBlock;

    private NEBlocks() {}

    public static void register() {
        if (copperOre != null) return;

        copperOre = new BlockCopperOre().setCreativeTab(CreativeTabs.tabBlock);
        copperBlock = new BlockCopperBlock().setCreativeTab(CreativeTabs.tabBlock);

        GameRegistry.registerBlock(copperOre, "copper_ore");
        GameRegistry.registerBlock(copperBlock, "copper_block");
    }
}
