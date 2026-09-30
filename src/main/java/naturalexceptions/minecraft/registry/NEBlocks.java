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
    public static Block stormglassCluster;
    public static Block stormglassVein;
    public static Block stormglassLantern;
    public static Block magmaRock;
    public static Block cloud;
    public static Block skystoneRock;
    public static Block skystoneBricks;
    public static Block skystoneDecor;
    public static Block skystonePillar;
    public static Block skystoneStairs;
    public static Block skystoneSlab;
    public static Block skystoneDoubleSlab;
    public static Block skyslate;
    public static Block skyslateStairs;
    public static Block skyslateShingleStairs;
    public static Block skyslateSlab;
    public static Block skyslateDoubleSlab;
    public static Block crate;
    public static Block barrel;
    public static Block pantryShelf;
    public static Block marketCounter;
    public static Block woodenTable;
    public static Block marketAwning;
    public static Block wallShelf;
    public static Block weaponRack;
    public static Block candelabra;
    public static Block brazier;
    public static Block stoneBench;
    public static Block pew;
    public static Block chair;
    public static Block skystoneSeam;
    public static Block masonsBench;
    public static Block sawmill;
    public static Block hoistWindlass;
    public static Block hoistBasket;
    public static Block cloudCondenser;
    public static Block stormcrystal;
    public static Block lighthouseLamp;
    public static Block fountain;
    public static Block orrery;
    public static Block chartTable;
    public static Block windbloom;
    public static Block wardedSkystone;
    public static Block vaultGate;
    public static Block triggerStone;
    public static Block crumblingSkystone;
    public static Block galeVent;
    public static Block sealedAlcove;
    public static Block wardenSeal;

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
        stormglassCluster = new naturalexceptions.minecraft.block.BlockStormglassCluster()
            .setCreativeTab(NECreativeTab.INSTANCE);
        stormglassVein = new naturalexceptions.minecraft.block.BlockStormglassVein()
            .setCreativeTab(NECreativeTab.INSTANCE);
        stormglassLantern = new naturalexceptions.minecraft.block.BlockStormglassLantern()
            .setCreativeTab(NECreativeTab.INSTANCE);
        magmaRock = new naturalexceptions.minecraft.block.BlockMagmaRock().setCreativeTab(NECreativeTab.INSTANCE);
        cloud = new naturalexceptions.minecraft.block.BlockCloud().setCreativeTab(NECreativeTab.INSTANCE);
        skystoneRock = new naturalexceptions.minecraft.block.BlockSkystoneRock().setCreativeTab(NECreativeTab.INSTANCE);
        skystoneBricks = new naturalexceptions.minecraft.block.BlockSkystoneBricks()
            .setCreativeTab(NECreativeTab.INSTANCE);
        skystoneDecor = new naturalexceptions.minecraft.block.BlockSkystoneDecor()
            .setCreativeTab(NECreativeTab.INSTANCE);
        skystonePillar = new naturalexceptions.minecraft.block.BlockSkystonePillar()
            .setCreativeTab(NECreativeTab.INSTANCE);
        skystoneStairs = new naturalexceptions.minecraft.block.BlockSkystoneStairs(skystoneBricks)
            .setCreativeTab(NECreativeTab.INSTANCE);
        skystoneSlab = new naturalexceptions.minecraft.block.BlockSkystoneSlab(false)
            .setCreativeTab(NECreativeTab.INSTANCE);
        skystoneDoubleSlab = new naturalexceptions.minecraft.block.BlockSkystoneSlab(true);
        skyslate = new naturalexceptions.minecraft.block.BlockSkyslate().setCreativeTab(NECreativeTab.INSTANCE);
        skyslateStairs = new naturalexceptions.minecraft.block.BlockSkystoneStairs(skyslate, 0, "skyslateStairs")
            .setCreativeTab(NECreativeTab.INSTANCE);
        skyslateShingleStairs = new naturalexceptions.minecraft.block.BlockSkystoneStairs(
            skyslate,
            1,
            "skyslateShingleStairs").setCreativeTab(NECreativeTab.INSTANCE);
        skyslateSlab = new naturalexceptions.minecraft.block.BlockSkyslateSlab(false)
            .setCreativeTab(NECreativeTab.INSTANCE);
        skyslateDoubleSlab = new naturalexceptions.minecraft.block.BlockSkyslateSlab(true);
        crate = new naturalexceptions.minecraft.furniture.BlockCrate().setCreativeTab(NECreativeTab.INSTANCE);
        barrel = new naturalexceptions.minecraft.furniture.BlockBarrel().setCreativeTab(NECreativeTab.INSTANCE);
        pantryShelf = new naturalexceptions.minecraft.furniture.BlockPantryShelf()
            .setCreativeTab(NECreativeTab.INSTANCE);
        marketCounter = new naturalexceptions.minecraft.furniture.BlockMarketCounter()
            .setCreativeTab(NECreativeTab.INSTANCE);
        woodenTable = new naturalexceptions.minecraft.furniture.BlockWoodenTable()
            .setCreativeTab(NECreativeTab.INSTANCE);
        marketAwning = new naturalexceptions.minecraft.furniture.BlockMarketAwning()
            .setCreativeTab(NECreativeTab.INSTANCE);
        wallShelf = new naturalexceptions.minecraft.furniture.BlockWallShelf().setCreativeTab(NECreativeTab.INSTANCE);
        weaponRack = new naturalexceptions.minecraft.furniture.BlockWeaponRack().setCreativeTab(NECreativeTab.INSTANCE);
        candelabra = new naturalexceptions.minecraft.furniture.BlockCandelabra().setCreativeTab(NECreativeTab.INSTANCE);
        brazier = new naturalexceptions.minecraft.furniture.BlockBrazier().setCreativeTab(NECreativeTab.INSTANCE);
        stoneBench = new naturalexceptions.minecraft.furniture.BlockBench().setCreativeTab(NECreativeTab.INSTANCE);
        pew = new naturalexceptions.minecraft.furniture.BlockPew().setCreativeTab(NECreativeTab.INSTANCE);
        chair = new naturalexceptions.minecraft.furniture.BlockChair().setCreativeTab(NECreativeTab.INSTANCE);
        skystoneSeam = new naturalexceptions.minecraft.block.BlockSkystoneSeam().setCreativeTab(NECreativeTab.INSTANCE);
        masonsBench = new naturalexceptions.minecraft.furniture.BlockCuttingBench(
            naturalexceptions.minecraft.furniture.CuttingRecipes.Kind.MASON).setCreativeTab(NECreativeTab.INSTANCE);
        sawmill = new naturalexceptions.minecraft.furniture.BlockCuttingBench(
            naturalexceptions.minecraft.furniture.CuttingRecipes.Kind.SAW).setCreativeTab(NECreativeTab.INSTANCE);
        hoistWindlass = new naturalexceptions.minecraft.furniture.BlockHoist(true).setCreativeTab(NECreativeTab.INSTANCE);
        hoistBasket = new naturalexceptions.minecraft.furniture.BlockHoist(false).setCreativeTab(NECreativeTab.INSTANCE);
        cloudCondenser = new naturalexceptions.minecraft.furniture.BlockCloudCondenser()
            .setCreativeTab(NECreativeTab.INSTANCE);
        stormcrystal = new naturalexceptions.minecraft.block.BlockStormcrystal().setCreativeTab(NECreativeTab.INSTANCE);
        lighthouseLamp = new naturalexceptions.minecraft.furniture.BlockLighthouseLamp()
            .setCreativeTab(NECreativeTab.INSTANCE);
        fountain = new naturalexceptions.minecraft.furniture.BlockFountain().setCreativeTab(NECreativeTab.INSTANCE);
        orrery = new naturalexceptions.minecraft.furniture.BlockOrrery().setCreativeTab(NECreativeTab.INSTANCE);
        chartTable = new naturalexceptions.minecraft.furniture.BlockChartTable().setCreativeTab(NECreativeTab.INSTANCE);
        windbloom = new naturalexceptions.minecraft.block.BlockWindbloom().setCreativeTab(NECreativeTab.INSTANCE);
        wardedSkystone = new naturalexceptions.minecraft.block.BlockWardedSkystone();
        vaultGate = new naturalexceptions.minecraft.block.BlockVaultGate();
        // Dungeon devices: in the creative tab so they can be built with and tried out.
        triggerStone = new naturalexceptions.minecraft.block.BlockTriggerStone().setCreativeTab(NECreativeTab.INSTANCE);
        crumblingSkystone = new naturalexceptions.minecraft.block.BlockCrumblingSkystone()
            .setCreativeTab(NECreativeTab.INSTANCE);
        galeVent = new naturalexceptions.minecraft.block.BlockGaleVent().setCreativeTab(NECreativeTab.INSTANCE);
        sealedAlcove = new naturalexceptions.minecraft.block.BlockSealedAlcove().setCreativeTab(NECreativeTab.INSTANCE);
        wardenSeal = new naturalexceptions.minecraft.block.BlockWardenSeal().setCreativeTab(NECreativeTab.INSTANCE);

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
            GameRegistry.registerBlock(stormglassCluster, "stormglass_cluster");
            GameRegistry.registerBlock(stormglassVein, "stormglass_vein");
            GameRegistry.registerBlock(stormglassLantern, "stormglass_lantern");
            GameRegistry.registerBlock(magmaRock, "magma_rock");
            GameRegistry.registerBlock(cloud, "cloud");
            GameRegistry.registerBlock(skystoneRock, "skystone");
            GameRegistry.registerBlock(
                skystoneBricks,
                naturalexceptions.minecraft.block.ItemBlockSkystoneBricks.class,
                "skystone_bricks");
            GameRegistry.registerBlock(
                skystoneDecor,
                naturalexceptions.minecraft.block.ItemBlockSkystoneDecor.class,
                "skystone_decor");
            GameRegistry.registerBlock(skystonePillar, "skystone_pillar");
            GameRegistry.registerBlock(skystoneStairs, "skystone_stairs");
            GameRegistry
                .registerBlock(skystoneSlab, naturalexceptions.minecraft.block.ItemSkystoneSlab.class, "skystone_slab");
            GameRegistry.registerBlock(
                skystoneDoubleSlab,
                naturalexceptions.minecraft.block.ItemSkystoneSlab.class,
                "skystone_double_slab");
            GameRegistry.registerBlock(skyslate, naturalexceptions.minecraft.block.ItemBlockSkyslate.class, "skyslate");
            GameRegistry.registerBlock(skyslateStairs, "skyslate_stairs");
            GameRegistry.registerBlock(skyslateShingleStairs, "skyslate_shingle_stairs");
            GameRegistry
                .registerBlock(skyslateSlab, naturalexceptions.minecraft.block.ItemSkyslateSlab.class, "skyslate_slab");
            GameRegistry.registerBlock(
                skyslateDoubleSlab,
                naturalexceptions.minecraft.block.ItemSkyslateSlab.class,
                "skyslate_double_slab");
            GameRegistry.registerBlock(crate, naturalexceptions.minecraft.furniture.ItemBlockCrate.class, "crate");
            GameRegistry.registerBlock(barrel, "barrel");
            GameRegistry.registerBlock(pantryShelf, "pantry_shelf");
            GameRegistry.registerBlock(
                marketCounter,
                naturalexceptions.minecraft.furniture.ItemBlockFurniture.class,
                "market_counter");
            GameRegistry.registerBlock(
                woodenTable,
                naturalexceptions.minecraft.furniture.ItemBlockFurniture.class,
                "wooden_table");
            GameRegistry.registerBlock(
                marketAwning,
                naturalexceptions.minecraft.furniture.ItemBlockFurniture.class,
                "market_awning");
            GameRegistry.registerBlock(wallShelf, "wall_shelf");
            GameRegistry.registerBlock(weaponRack, "weapon_rack");
            GameRegistry.registerBlock(candelabra, "candelabra");
            GameRegistry.registerBlock(brazier, "brazier");
            GameRegistry.registerBlock(stoneBench, "stone_bench");
            GameRegistry.registerBlock(pew, "pew");
            GameRegistry
                .registerBlock(chair, naturalexceptions.minecraft.furniture.ItemBlockFurniture.class, "wooden_chair");
            GameRegistry.registerBlock(skystoneSeam, "skystone_seam");
            GameRegistry.registerBlock(masonsBench, "masons_bench");
            GameRegistry.registerBlock(sawmill, "sawmill");
            GameRegistry.registerBlock(hoistWindlass, "hoist_windlass");
            GameRegistry.registerBlock(hoistBasket, "hoist_basket");
            GameRegistry.registerBlock(cloudCondenser, "cloud_condenser");
            GameRegistry.registerBlock(stormcrystal, "stormcrystal");
            GameRegistry.registerBlock(lighthouseLamp, "lighthouse_lamp");
            GameRegistry.registerBlock(fountain, "fountain");
            GameRegistry.registerBlock(orrery, "orrery");
            GameRegistry.registerBlock(chartTable, "chart_table");
            GameRegistry.registerBlock(wardedSkystone, "warded_skystone");
            GameRegistry.registerBlock(vaultGate, "vault_gate");
            GameRegistry.registerBlock(triggerStone, "trigger_stone");
            GameRegistry.registerBlock(crumblingSkystone, "crumbling_skystone");
            GameRegistry.registerBlock(galeVent, "gale_vent");
            GameRegistry.registerBlock(sealedAlcove, "sealed_alcove");
            GameRegistry.registerBlock(wardenSeal, "warden_seal");
            GameRegistry.registerBlock(windbloom, "windbloom");
        } catch (Throwable t) {
            System.out.println("DEBUG registerBlock caught: " + t);
            t.printStackTrace();
        }

        try {
            GameRegistry.registerTileEntity(TileEntityStarterSmelter.class, "starter_smelter");
        } catch (Throwable ignored) {}
        try {
            GameRegistry.registerTileEntity(
                naturalexceptions.minecraft.furniture.TileEntityCrate.class,
                "naturalexceptions_crate");
            GameRegistry.registerTileEntity(
                naturalexceptions.minecraft.furniture.TileEntityBarrel.class,
                "naturalexceptions_barrel");
            GameRegistry.registerTileEntity(
                naturalexceptions.minecraft.furniture.TileEntityPantryShelf.class,
                "naturalexceptions_pantry_shelf");
            GameRegistry.registerTileEntity(
                naturalexceptions.minecraft.furniture.TileEntityMarketCounter.class,
                "naturalexceptions_market_counter");
            GameRegistry.registerTileEntity(
                naturalexceptions.minecraft.furniture.TileEntityWoodenTable.class,
                "naturalexceptions_wooden_table");
            GameRegistry.registerTileEntity(
                naturalexceptions.minecraft.furniture.TileEntityWallShelf.class,
                "naturalexceptions_wall_shelf");
            GameRegistry.registerTileEntity(
                naturalexceptions.minecraft.furniture.TileEntityWeaponRack.class,
                "naturalexceptions_weapon_rack");
            GameRegistry.registerTileEntity(
                naturalexceptions.minecraft.furniture.TileEntityBrazier.class,
                "naturalexceptions_brazier");
            GameRegistry.registerTileEntity(
                naturalexceptions.minecraft.furniture.TileEntityOrrery.class,
                "naturalexceptions_orrery");
            GameRegistry.registerTileEntity(
                naturalexceptions.minecraft.furniture.TileEntityCuttingBench.class,
                "naturalexceptions_cutting_bench");
            GameRegistry.registerTileEntity(
                naturalexceptions.minecraft.furniture.TileEntityHoist.class,
                "naturalexceptions_hoist");
            GameRegistry.registerTileEntity(
                naturalexceptions.minecraft.furniture.TileEntityLighthouseLamp.class,
                "naturalexceptions_lighthouse_lamp");
        } catch (Throwable ignored) {}
        try {
            GameRegistry.registerTileEntity(
                naturalexceptions.minecraft.block.TileEntityGaleVent.class,
                "naturalexceptions_gale_vent");
            GameRegistry.registerTileEntity(
                naturalexceptions.minecraft.block.TileEntityWardenSeal.class,
                "naturalexceptions_warden_seal");
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
            naturalexceptions.minecraft.recipe.SkystoneRecipes.register();
            if (NEItems.lumeniteShard != null && NEItems.copperIngot != null) GameRegistry.addRecipe(
                new net.minecraft.item.ItemStack(lumeniteLamp),
                " S ",
                "SCS",
                " S ",
                'S',
                NEItems.lumeniteShard,
                'C',
                NEItems.copperIngot);
            if (NEItems.stormglassShard != null) GameRegistry.addRecipe(
                new net.minecraft.item.ItemStack(stormglassLantern, 2),
                "BSB",
                "SGS",
                "BSB",
                'B',
                skystoneBricks,
                'S',
                NEItems.stormglassShard,
                'G',
                net.minecraft.init.Blocks.glass);
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
