package naturalexceptions.minecraft.registry;

import net.minecraft.block.Block;

import cpw.mods.fml.common.registry.GameRegistry;
import naturalexceptions.content.material.BuiltinMaterials;
import naturalexceptions.minecraft.block.BlockCopperBlock;
import naturalexceptions.minecraft.block.BlockCopperOre;
import naturalexceptions.minecraft.fabrication.BlockCastingTable;
import naturalexceptions.minecraft.metallurgy.BlockStarterSmelter;
import naturalexceptions.minecraft.metallurgy.FeedstockItemRegistry;
import naturalexceptions.minecraft.metallurgy.ItemBlockStarterSmelter;
import naturalexceptions.minecraft.metallurgy.TileEntityStarterSmelter;

public final class NEBlocks {

    public static Block copperOre;
    public static Block copperBlock;
    public static Block refractoryBrick;
    public static Block starterSmelter;
    public static Block castingTable;
    public static Block smelterOutlet;
    public static Block smelterTuyere;
    public static Block forgingAnvil;
    public static Block assemblyTable;
    public static Block moldAssemblyTable;
    public static Block fluidCell;
    public static Block fallenStarCell;
    public static Block impactCrust;
    public static Block lumeniteOre;
    public static Block lumeniteCluster;
    public static Block lumeniteLamp;
    public static Block magmaRock;

    private NEBlocks() {}

    public static void register() {
        NEItems.ensureBootstrapped();
        NEItems.register();
        if (copperOre != null) return;

        copperOre = new BlockCopperOre().setCreativeTab(NECreativeTab.INSTANCE);
        copperBlock = new BlockCopperBlock().setCreativeTab(NECreativeTab.INSTANCE);

        refractoryBrick = new naturalexceptions.minecraft.block.BlockRefractoryBrick()
            .setCreativeTab(NECreativeTab.INSTANCE);

        starterSmelter = new BlockStarterSmelter().setCreativeTab(NECreativeTab.INSTANCE);

        castingTable = new BlockCastingTable().setCreativeTab(NECreativeTab.INSTANCE);

        smelterOutlet = new naturalexceptions.minecraft.metallurgy.BlockSmelterOutlet()
            .setCreativeTab(NECreativeTab.INSTANCE);

        smelterTuyere = new naturalexceptions.minecraft.metallurgy.BlockSmelterTuyere()
            .setCreativeTab(NECreativeTab.INSTANCE);

        forgingAnvil = new naturalexceptions.minecraft.fabrication.BlockForgingAnvil()
            .setCreativeTab(NECreativeTab.INSTANCE);

        assemblyTable = new naturalexceptions.minecraft.fabrication.BlockAssemblyTable()
            .setCreativeTab(NECreativeTab.INSTANCE);
        moldAssemblyTable = new naturalexceptions.minecraft.fabrication.BlockMoldAssemblyTable()
            .setCreativeTab(NECreativeTab.INSTANCE);
        fluidCell = new naturalexceptions.minecraft.fluid.BlockFluidCell();
        // A Fallen Star host without its canonical parcel would be fake matter; sites come only from impacts.
        fallenStarCell = new naturalexceptions.minecraft.fluid.BlockFallenStar();
        impactCrust = new naturalexceptions.minecraft.fluid.BlockImpactCrust();
        lumeniteOre = new naturalexceptions.minecraft.block.BlockLumeniteOre().setCreativeTab(NECreativeTab.INSTANCE);
        lumeniteCluster = new naturalexceptions.minecraft.block.BlockLumeniteCluster()
            .setCreativeTab(NECreativeTab.INSTANCE);
        lumeniteLamp = new naturalexceptions.minecraft.block.BlockLumeniteLamp().setCreativeTab(NECreativeTab.INSTANCE);
        magmaRock = new naturalexceptions.minecraft.block.BlockMagmaRock().setCreativeTab(NECreativeTab.INSTANCE);

        try {
            net.minecraft.tileentity.TileEntity.addMapping(TileEntityStarterSmelter.class, "starter_smelter");
        } catch (Throwable ignored) {}
        try {
            net.minecraft.tileentity.TileEntity
                .addMapping(naturalexceptions.minecraft.fluid.TileEntityFluidCell.class, "fluid_cell");
        } catch (Throwable ignored) {}
        try {
            net.minecraft.tileentity.TileEntity.addMapping(
                naturalexceptions.minecraft.fabrication.TileEntityMoldAssemblyTable.class,
                "mold_assembly_table");
        } catch (Throwable ignored) {}
        try {
            net.minecraft.tileentity.TileEntity
                .addMapping(naturalexceptions.minecraft.fabrication.TileEntityCastingTable.class, "casting_table");
        } catch (Throwable ignored) {}
        try {
            net.minecraft.tileentity.TileEntity
                .addMapping(naturalexceptions.minecraft.metallurgy.TileEntitySmelterOutlet.class, "smelter_outlet");
        } catch (Throwable ignored) {}
        try {
            net.minecraft.tileentity.TileEntity
                .addMapping(naturalexceptions.minecraft.fabrication.TileEntityForgingAnvil.class, "forging_anvil");
        } catch (Throwable ignored) {}
        try {
            net.minecraft.tileentity.TileEntity
                .addMapping(naturalexceptions.minecraft.fabrication.TileEntityAssemblyTable.class, "assembly_table");
        } catch (Throwable ignored) {}

        try {
            GameRegistry.registerBlock(copperOre, "copper_ore");
            GameRegistry.registerBlock(copperBlock, "copper_block");
            GameRegistry.registerBlock(refractoryBrick, "refractory_brick");
            GameRegistry.registerBlock(starterSmelter, ItemBlockStarterSmelter.class, "starter_smelter");
            GameRegistry.registerBlock(castingTable, "casting_table");
            GameRegistry.registerBlock(smelterOutlet, "smelter_outlet");
            GameRegistry.registerBlock(smelterTuyere, "smelter_tuyere");
            GameRegistry.registerBlock(forgingAnvil, "forging_anvil");
            GameRegistry.registerBlock(assemblyTable, "assembly_table");
            GameRegistry.registerBlock(moldAssemblyTable, "mold_assembly_table");
            GameRegistry.registerBlock(fluidCell, "fluid_cell");
            GameRegistry.registerBlock(fallenStarCell, "fallen_star_site");
            GameRegistry.registerBlock(impactCrust, "impact_crust");
            GameRegistry.registerBlock(lumeniteOre, "lumenite_ore");
            GameRegistry.registerBlock(lumeniteCluster, "lumenite_cluster");
            GameRegistry.registerBlock(lumeniteLamp, "lumenite_lamp");
            GameRegistry.registerBlock(magmaRock, "magma_rock");
        } catch (Throwable t) {
            System.out.println("DEBUG registerBlock caught: " + t);
            t.printStackTrace();
        }

        try {
            GameRegistry.registerTileEntity(TileEntityStarterSmelter.class, "starter_smelter");
        } catch (Throwable ignored) {}
        try {
            GameRegistry.registerTileEntity(naturalexceptions.minecraft.fluid.TileEntityFluidCell.class, "fluid_cell");
        } catch (Throwable ignored) {}
        try {
            GameRegistry.registerTileEntity(
                naturalexceptions.minecraft.fabrication.TileEntityMoldAssemblyTable.class,
                "mold_assembly_table");
        } catch (Throwable ignored) {}
        try {
            GameRegistry.registerTileEntity(
                naturalexceptions.minecraft.fabrication.TileEntityCastingTable.class,
                "casting_table");
        } catch (Throwable ignored) {}
        try {
            GameRegistry.registerTileEntity(
                naturalexceptions.minecraft.metallurgy.TileEntitySmelterOutlet.class,
                "smelter_outlet");
        } catch (Throwable ignored) {}
        try {
            GameRegistry.registerTileEntity(
                naturalexceptions.minecraft.fabrication.TileEntityForgingAnvil.class,
                "forging_anvil");
        } catch (Throwable ignored) {}
        try {
            GameRegistry.registerTileEntity(
                naturalexceptions.minecraft.fabrication.TileEntityAssemblyTable.class,
                "assembly_table");
        } catch (Throwable ignored) {}

        try {
            net.minecraftforge.oredict.OreDictionary.registerOre("oreCopper", copperOre);
            net.minecraftforge.oredict.OreDictionary.registerOre("blockCopper", copperBlock);
        } catch (Throwable ignored) {}

        try {
            GameRegistry.addRecipe(new net.minecraft.item.ItemStack(smelterOutlet), "B B", " B ", 'B', refractoryBrick);
            naturalexceptions.minecraft.recipe.TreasureRecipes.register();
            if (NEItems.lumeniteShard != null && NEItems.copperIngot != null) GameRegistry.addRecipe(
                new net.minecraft.item.ItemStack(lumeniteLamp),
                " S ",
                "SCS",
                " S ",
                'S',
                NEItems.lumeniteShard,
                'C',
                NEItems.copperIngot);
            GameRegistry
                .addRecipe(new net.minecraft.item.ItemStack(starterSmelter), "BBB", "B B", "BBB", 'B', refractoryBrick);
            GameRegistry.addRecipe(new net.minecraft.item.ItemStack(castingTable), "BBB", "B B", 'B', refractoryBrick);
            GameRegistry.addRecipe(
                new net.minecraft.item.ItemStack(assemblyTable),
                "PPP",
                "B B",
                "B B",
                'P',
                net.minecraft.init.Blocks.planks,
                'B',
                refractoryBrick);
            GameRegistry.addRecipe(
                new net.minecraft.item.ItemStack(moldAssemblyTable),
                "PPP",
                " B ",
                "B B",
                'P',
                net.minecraft.init.Blocks.planks,
                'B',
                refractoryBrick);

            GameRegistry
                .addRecipe(new net.minecraft.item.ItemStack(refractoryBrick), "BB", "BB", 'B', NEItems.refractoryBrick);
        } catch (Throwable ignored) {}
        try {
            naturalexceptions.minecraft.recipe.SurvivalEquipmentRecipes.register();
        } catch (Throwable ignored) {}

        MaterialBindingRegistry.global()
            .bind(copperOre, BuiltinMaterials.COPPER.id());
        MaterialBindingRegistry.global()
            .bind(copperBlock, BuiltinMaterials.COPPER.id());

        FeedstockItemRegistry.registerBuiltins();
    }
}
