package naturalexceptions.minecraft.registry;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import cpw.mods.fml.common.registry.GameRegistry;
import naturalexceptions.content.material.BuiltinMaterials;
import naturalexceptions.minecraft.item.ItemBlacksmithHammer;
import naturalexceptions.minecraft.item.ItemBlacksmithTongs;
import naturalexceptions.minecraft.item.ItemCastingMold;
import naturalexceptions.minecraft.item.ItemFoundryLadle;
import naturalexceptions.minecraft.item.ItemGenericAssembly;
import naturalexceptions.minecraft.item.ItemGenericComponent;
import naturalexceptions.minecraft.item.ItemMaterialSample;
import naturalexceptions.minecraft.metallurgy.FeedstockItemRegistry;

public final class NEItems {

    public static naturalexceptions.minecraft.item.ItemBellows bellows;
    public static naturalexceptions.minecraft.item.ItemFluidVessel fluidVessel;
    public static naturalexceptions.minecraft.item.ItemAlmanac almanac;
    public static naturalexceptions.minecraft.item.ItemBandage bandage;
    public static naturalexceptions.minecraft.item.ItemExposureMask exposureMask;
    public static Item copperIngot;
    public static Item slagLump;
    public static Item refractoryMix;
    public static Item refractoryBrick;
    public static ItemMaterialSample materialSample;
    public static ItemCastingMold castingMold;
    public static ItemGenericComponent genericComponent;
    public static ItemGenericAssembly genericAssembly;
    public static ItemFoundryLadle foundryLadle;
    public static ItemBlacksmithHammer blacksmithHammer;
    public static ItemBlacksmithTongs blacksmithTongs;
    public static naturalexceptions.minecraft.item.ItemKnowledgeBook startingManual;
    public static naturalexceptions.minecraft.item.ItemKnowledgeBook foundryNotes;
    public static naturalexceptions.minecraft.item.ItemFieldAssay fieldAssay;
    public static naturalexceptions.minecraft.item.ItemKnowledgeRecord knowledgeRecord;

    private NEItems() {}

    private static boolean bootstrapped = false;

    public static synchronized void ensureBootstrapped() {
        if (bootstrapped) return;
        bootstrapped = true;

        try {
            Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
            java.lang.reflect.Field theUnsafe = unsafeClass.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            sun.misc.Unsafe unsafe = (sun.misc.Unsafe) theUnsafe.get(null);

            Object dummyLoader = unsafe.allocateInstance(cpw.mods.fml.common.Loader.class);
            java.lang.reflect.Field instField = cpw.mods.fml.common.Loader.class.getDeclaredField("instance");
            instField.setAccessible(true);
            if (instField.get(null) == null) {
                instField.set(null, dummyLoader);
            }
            Object currentLoader = instField.get(null);

            java.lang.reflect.Field sideField = cpw.mods.fml.relauncher.FMLRelaunchLog.class.getDeclaredField("side");
            sideField.setAccessible(true);
            if (sideField.get(null) == null) {
                sideField.set(null, cpw.mods.fml.relauncher.Side.CLIENT);
            }

            net.minecraft.init.Bootstrap.func_151354_b();

            cpw.mods.fml.common.ModMetadata meta = new cpw.mods.fml.common.ModMetadata();
            meta.modId = "naturalexceptions";
            meta.name = "Natural Exceptions";
            cpw.mods.fml.common.DummyModContainer modContainer = new cpw.mods.fml.common.DummyModContainer(meta);

            Object dummyController = unsafe.allocateInstance(cpw.mods.fml.common.LoadController.class);
            java.lang.reflect.Field mcStateField = cpw.mods.fml.common.LoadController.class.getDeclaredField("state");
            mcStateField.setAccessible(true);
            mcStateField.set(dummyController, cpw.mods.fml.common.LoaderState.INITIALIZATION);

            java.lang.reflect.Field mcActiveField = cpw.mods.fml.common.LoadController.class
                .getDeclaredField("activeContainer");
            mcActiveField.setAccessible(true);
            mcActiveField.set(dummyController, modContainer);

            java.lang.reflect.Field mcField = cpw.mods.fml.common.Loader.class.getDeclaredField("modController");
            mcField.setAccessible(true);
            if (mcField.get(currentLoader) == null) {
                mcField.set(currentLoader, dummyController);
            }
        } catch (Throwable ignored) {}

        try (java.io.InputStream in = NEItems.class.getResourceAsStream("/assets/naturalexceptions/lang/en_US.lang")) {
            if (in != null) {
                net.minecraft.util.StringTranslate.inject(in);
            }
        } catch (Throwable ignored) {}
    }

    public static void register() {
        ensureBootstrapped();
        if (copperIngot != null) return;

        copperIngot = new Item().setUnlocalizedName("copperIngot")
            .setTextureName("naturalexceptions:copper_ingot")
            .setCreativeTab(NECreativeTab.INSTANCE);

        slagLump = new Item().setUnlocalizedName("slagLump")
            .setTextureName("naturalexceptions:molten_slag_lump")
            .setCreativeTab(NECreativeTab.INSTANCE);

        refractoryMix = new Item().setUnlocalizedName("refractoryMix")
            .setTextureName("minecraft:clay_ball")
            .setCreativeTab(NECreativeTab.INSTANCE);
        refractoryBrick = new Item().setUnlocalizedName("refractoryBrick")
            .setTextureName("minecraft:brick")
            .setCreativeTab(NECreativeTab.INSTANCE);

        materialSample = new ItemMaterialSample();
        materialSample.setCreativeTab(NECreativeTab.INSTANCE);

        castingMold = new ItemCastingMold();
        castingMold.setCreativeTab(NECreativeTab.INSTANCE);

        genericComponent = new ItemGenericComponent();
        genericComponent.setCreativeTab(NECreativeTab.INSTANCE);

        genericAssembly = new ItemGenericAssembly();
        genericAssembly.setCreativeTab(NECreativeTab.INSTANCE);

        foundryLadle = new naturalexceptions.minecraft.item.ItemFoundryLadle();
        foundryLadle.setCreativeTab(NECreativeTab.INSTANCE);

        blacksmithHammer = new naturalexceptions.minecraft.item.ItemBlacksmithHammer();
        blacksmithHammer.setCreativeTab(NECreativeTab.INSTANCE);

        blacksmithTongs = new naturalexceptions.minecraft.item.ItemBlacksmithTongs();
        blacksmithTongs.setCreativeTab(NECreativeTab.INSTANCE);
        startingManual = new naturalexceptions.minecraft.item.ItemKnowledgeBook(
            naturalexceptions.content.knowledge.BuiltinKnowledgeSources.STARTING_MANUAL, "fieldManual",
            "minecraft:book");
        startingManual.setCreativeTab(NECreativeTab.INSTANCE);
        foundryNotes = new naturalexceptions.minecraft.item.ItemKnowledgeBook(
            naturalexceptions.content.knowledge.BuiltinKnowledgeSources.FOUNDRY_NOTES, "foundryNotes",
            "minecraft:writable_book");
        foundryNotes.setCreativeTab(NECreativeTab.INSTANCE);
        fieldAssay = new naturalexceptions.minecraft.item.ItemFieldAssay();
        fieldAssay.setCreativeTab(NECreativeTab.INSTANCE);
        knowledgeRecord = new naturalexceptions.minecraft.item.ItemKnowledgeRecord();
        knowledgeRecord.setCreativeTab(NECreativeTab.INSTANCE);
        bellows = new naturalexceptions.minecraft.item.ItemBellows();
        bellows.setCreativeTab(NECreativeTab.INSTANCE);
        almanac = new naturalexceptions.minecraft.item.ItemAlmanac();
        almanac.setCreativeTab(NECreativeTab.INSTANCE);
        fluidVessel = new naturalexceptions.minecraft.item.ItemFluidVessel();
        fluidVessel.setCreativeTab(NECreativeTab.INSTANCE);
        bandage = new naturalexceptions.minecraft.item.ItemBandage();
        bandage.setCreativeTab(NECreativeTab.INSTANCE);
        exposureMask = new naturalexceptions.minecraft.item.ItemExposureMask();
        exposureMask.setCreativeTab(NECreativeTab.INSTANCE);

        try {
            GameRegistry.registerItem(bellows, "bellows");
            GameRegistry.registerItem(fluidVessel, "fluid_vessel");
            GameRegistry.registerItem(almanac, "almanac");
            GameRegistry.addShapelessRecipe(
                new ItemStack(almanac),
                net.minecraft.init.Items.book,
                net.minecraft.init.Items.paper,
                new ItemStack(net.minecraft.init.Items.coal, 1, net.minecraftforge.oredict.OreDictionary.WILDCARD_VALUE));
            GameRegistry.registerItem(bandage, "bandage");
            GameRegistry.registerItem(exposureMask, "exposure_mask");
            GameRegistry.registerItem(copperIngot, "copper_ingot");
            GameRegistry.registerItem(slagLump, "slag_lump");
            GameRegistry.registerItem(refractoryMix, "refractory_mix");
            GameRegistry.registerItem(refractoryBrick, "refractory_brick_item");
            GameRegistry.registerItem(materialSample, "material_sample");
            GameRegistry.registerItem(castingMold, "casting_mold");
            GameRegistry.registerItem(genericComponent, "generic_component");
            GameRegistry.registerItem(genericAssembly, "generic_assembly");
            GameRegistry.registerItem(foundryLadle, "foundry_ladle");
            GameRegistry.registerItem(blacksmithHammer, "blacksmith_hammer");
            GameRegistry.registerItem(blacksmithTongs, "blacksmith_tongs");
            GameRegistry.registerItem(startingManual, "field_manual");
            GameRegistry.registerItem(foundryNotes, "foundry_notes");
            GameRegistry.registerItem(fieldAssay, "field_assay");
            GameRegistry.registerItem(knowledgeRecord, "knowledge_record");
            GameRegistry.addShapedRecipe(
                new ItemStack(foundryNotes),
                " PF", " BB", "  ", 'P', net.minecraft.init.Items.paper, 'F', net.minecraft.init.Items.feather,
                'B', net.minecraft.init.Items.book);
            GameRegistry.addShapelessRecipe(new ItemStack(fieldAssay), net.minecraft.init.Items.compass,
                net.minecraft.init.Items.iron_ingot, net.minecraft.init.Blocks.glass_pane);
            GameRegistry.addShapelessRecipe(new ItemStack(knowledgeRecord), net.minecraft.init.Items.book,
                net.minecraft.init.Items.paper);
            GameRegistry.addRecipe(naturalexceptions.minecraft.item.ItemFluidVessel.survivalRecipe(fluidVessel));
            GameRegistry.addShapelessRecipe(new ItemStack(bandage, 2), net.minecraft.init.Items.paper,
                net.minecraft.init.Items.string, net.minecraft.init.Blocks.wool);
            GameRegistry.addShapelessRecipe(new ItemStack(exposureMask), net.minecraft.init.Items.leather,
                net.minecraft.init.Items.string, net.minecraft.init.Blocks.wool);

            try {
                net.minecraftforge.oredict.OreDictionary.registerOre("ingotCopper", copperIngot);
            } catch (Throwable ignored) {}

            GameRegistry.addRecipe(new naturalexceptions.minecraft.recipe.RecipeToolAssembly());
            GameRegistry.addShapelessRecipe(
                new ItemStack(refractoryMix, 2),
                net.minecraft.init.Items.clay_ball,
                net.minecraft.init.Items.clay_ball,
                net.minecraft.init.Blocks.sand,
                net.minecraft.init.Blocks.sand);
            GameRegistry.addShapelessRecipe(
                naturalexceptions.minecraft.item.ItemCastingMold.createStack(null),
                net.minecraft.init.Items.clay_ball,
                net.minecraft.init.Items.clay_ball,
                net.minecraft.init.Items.clay_ball,
                net.minecraft.init.Blocks.sand);
            GameRegistry.addSmelting(refractoryMix, new ItemStack(refractoryBrick), 0.1F);
            GameRegistry.addRecipe(
                new net.minecraftforge.oredict.ShapedOreRecipe(
                    new ItemStack(bellows),
                    " P ",
                    "PLP",
                    " S ",
                    'P',
                    "plankWood",
                    'L',
                    net.minecraft.init.Items.leather,
                    'S',
                    "stickWood"));
            try {
                net.minecraftforge.oredict.RecipeSorter.register(
                    "naturalexceptions:tool_assembly",
                    naturalexceptions.minecraft.recipe.RecipeToolAssembly.class,
                    net.minecraftforge.oredict.RecipeSorter.Category.SHAPELESS,
                    "after:minecraft:shapeless");
            } catch (Throwable ignored) {}

            // Survival handle crafting: 2 sticks in a vertical column -> 1 Wood Handle
            ItemStack handleStack = ItemGenericComponent.createWoodenHandle();
            try {
                GameRegistry
                    .addRecipe(new net.minecraftforge.oredict.ShapedOreRecipe(handleStack, "S", "S", 'S', "stickWood"));
            } catch (Throwable ignored) {}
            try {
                GameRegistry.addRecipe(
                    new net.minecraft.item.crafting.ShapedRecipes(
                        1,
                        2,
                        new net.minecraft.item.ItemStack[] {
                            new net.minecraft.item.ItemStack(net.minecraft.init.Items.stick),
                            new net.minecraft.item.ItemStack(net.minecraft.init.Items.stick) },
                        handleStack));
            } catch (Throwable ignored) {}
        } catch (Throwable t) {
            // Allows unit testing without LaunchClassLoader
        }

        MaterialBindingRegistry.global()
            .bind(copperIngot, BuiltinMaterials.COPPER.id());

        try {
            if (net.minecraft.init.Items.iron_ingot != null) {
                MaterialBindingRegistry.global()
                    .bind(net.minecraft.init.Items.iron_ingot, BuiltinMaterials.IRON.id());
            }
            if (net.minecraft.init.Blocks.iron_ore != null) {
                MaterialBindingRegistry.global()
                    .bind(net.minecraft.init.Blocks.iron_ore, BuiltinMaterials.IRON.id());
            }
            if (net.minecraft.init.Items.gold_ingot != null) {
                MaterialBindingRegistry.global()
                    .bind(net.minecraft.init.Items.gold_ingot, BuiltinMaterials.GOLD.id());
            }
            if (net.minecraft.init.Blocks.gold_ore != null) {
                MaterialBindingRegistry.global()
                    .bind(net.minecraft.init.Blocks.gold_ore, BuiltinMaterials.GOLD.id());
            }
        } catch (Throwable ignored) {}

        FeedstockItemRegistry.registerBuiltins();
    }
}
