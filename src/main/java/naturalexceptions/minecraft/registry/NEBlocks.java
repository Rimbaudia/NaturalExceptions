package naturalexceptions.minecraft.registry;

import net.minecraft.block.Block;

import cpw.mods.fml.common.registry.GameRegistry;
import naturalexceptions.content.material.BuiltinMaterials;
import naturalexceptions.minecraft.block.BlockCopperBlock;
import naturalexceptions.minecraft.block.BlockCopperOre;
import naturalexceptions.minecraft.fabrication.BlockCastingTable;
import naturalexceptions.minecraft.metallurgy.BlockStarterSmelter;
import naturalexceptions.minecraft.metallurgy.ItemBlockStarterSmelter;
import naturalexceptions.minecraft.metallurgy.TileEntityStarterSmelter;

public final class NEBlocks {

    public static Block copperOre;
    public static Block copperBlock;
    public static Block refractoryBrick;
    public static Block starterSmelter;
    public static Block castingTable;

    private NEBlocks() {}

    public static void register() {
        if (copperOre != null) return;

        copperOre = new BlockCopperOre().setCreativeTab(NECreativeTab.INSTANCE);
        copperBlock = new BlockCopperBlock().setCreativeTab(NECreativeTab.INSTANCE);

        refractoryBrick = new naturalexceptions.minecraft.block.BlockRefractoryBrick()
            .setCreativeTab(NECreativeTab.INSTANCE);

        starterSmelter = new BlockStarterSmelter().setCreativeTab(NECreativeTab.INSTANCE);

        castingTable = new BlockCastingTable().setCreativeTab(NECreativeTab.INSTANCE);

        try {
            net.minecraft.tileentity.TileEntity.addMapping(TileEntityStarterSmelter.class, "starter_smelter");
        } catch (Throwable ignored) {}
        try {
            net.minecraft.tileentity.TileEntity
                .addMapping(naturalexceptions.minecraft.fabrication.TileEntityCastingTable.class, "casting_table");
        } catch (Throwable ignored) {}

        try {
            GameRegistry.registerBlock(copperOre, "copper_ore");
            GameRegistry.registerBlock(copperBlock, "copper_block");
            GameRegistry.registerBlock(refractoryBrick, "refractory_brick");
            GameRegistry.registerBlock(starterSmelter, ItemBlockStarterSmelter.class, "starter_smelter");
            GameRegistry.registerBlock(castingTable, "casting_table");

            GameRegistry.registerTileEntity(TileEntityStarterSmelter.class, "starter_smelter");
            GameRegistry.registerTileEntity(
                naturalexceptions.minecraft.fabrication.TileEntityCastingTable.class,
                "casting_table");
        } catch (Throwable t) {
            // Allows unit testing without LaunchClassLoader
        }

        MaterialBindingRegistry.global()
            .bind(copperOre, BuiltinMaterials.COPPER.id());
        MaterialBindingRegistry.global()
            .bind(copperBlock, BuiltinMaterials.COPPER.id());
    }
}
