package naturalexceptions;

import net.minecraftforge.common.MinecraftForge;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import naturalexceptions.content.BuiltinContent;
import naturalexceptions.minecraft.gravity.CommandDebugGravity;
import naturalexceptions.minecraft.gravity.CommandDebugGravityBody;
import naturalexceptions.minecraft.gravity.NEGravityService;
import naturalexceptions.minecraft.gravity.NEGravityTickHandler;
import naturalexceptions.minecraft.registry.NEBlocks;
import naturalexceptions.minecraft.registry.NEItems;
import naturalexceptions.minecraft.tick.NEWorldTickHandler;
import naturalexceptions.minecraft.worldgen.NEChunkGenerationHistory;
import naturalexceptions.minecraft.worldgen.NEOverworldGenerator;

@Mod(
    modid = NaturalExceptions.MOD_ID,
    name = NaturalExceptions.MOD_NAME,
    version = Tags.VERSION,
    acceptedMinecraftVersions = "[1.7.10]")
public final class NaturalExceptions {

    public static final String MOD_ID = "naturalexceptions";
    public static final String MOD_NAME = "Natural Exceptions";

    @Mod.Instance(NaturalExceptions.MOD_ID)
    public static NaturalExceptions instance;

    private static NEWorldTickHandler tickHandler;
    private static NEGravityTickHandler gravityTickHandler;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        BuiltinContent.register();
        NEItems.register();
        NEBlocks.register();

        if (event.getSide()
            .isClient()) {
            registerResourcePacks();
            // Before the first texture stitch, so empty gear slots have their silhouettes.
            naturalexceptions.minecraft.gear.client.GearIcons.register();
        }
    }

    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    private void registerResourcePacks() {
        try {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getMinecraft();
            if (mc != null) {
                java.util.List<net.minecraft.client.resources.IResourcePack> packs = cpw.mods.fml.relauncher.ReflectionHelper
                    .getPrivateValue(
                        net.minecraft.client.Minecraft.class,
                        mc,
                        "defaultResourcePacks",
                        "field_110449_ao");
                if (packs != null) {
                    packs.add(new naturalexceptions.minecraft.client.resource.VanillaToolResourcePack());
                }
            }
        } catch (Throwable ignored) {
            // Headless or test environment without Minecraft client
        }
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        naturalexceptions.minecraft.network.NENetwork.init();
        MinecraftForge.EVENT_BUS.register(new naturalexceptions.minecraft.knowledge.PlayerKnowledgeEvents());
        MinecraftForge.EVENT_BUS.register(new naturalexceptions.minecraft.biology.BiologicalEvents());
        MinecraftForge.EVENT_BUS.register(new naturalexceptions.minecraft.gear.GearEvents());
        MinecraftForge.EVENT_BUS.register(new naturalexceptions.minecraft.gear.GearMovement());
        MinecraftForge.EVENT_BUS.register(new naturalexceptions.minecraft.event.StarRainWorldMaterializer());
        FMLCommonHandler.instance()
            .bus()
            .register(new naturalexceptions.minecraft.knowledge.PlayerKnowledgeEvents());
        FMLCommonHandler.instance()
            .bus()
            .register(new naturalexceptions.minecraft.knowledge.KnowledgeServerTasks());
        FMLCommonHandler.instance()
            .bus()
            .register(new naturalexceptions.minecraft.worldgen.DungeonBeacons());
        NEOverworldGenerator.register();
        cpw.mods.fml.common.registry.EntityRegistry.registerModEntity(
            naturalexceptions.minecraft.event.EntityFallingStar.class,
            "fallen_star",
            0,
            this,
            256,
            1,
            true);
        cpw.mods.fml.common.registry.EntityRegistry.registerModEntity(
            naturalexceptions.minecraft.weapon.EntitySkyfallStar.class,
            "skyfall_star",
            1,
            this,
            128,
            1,
            true);
        cpw.mods.fml.common.registry.EntityRegistry.registerModEntity(
            naturalexceptions.minecraft.weapon.EntityStormglassBolt.class,
            "stormglass_bolt",
            2,
            this,
            96,
            1,
            true);
        cpw.mods.fml.common.registry.EntityRegistry.registerModEntity(
            naturalexceptions.minecraft.entity.EntitySkyWarden.class,
            "sky_warden",
            3,
            this,
            80,
            3,
            true);
        MinecraftForge.EVENT_BUS.register(new NEChunkGenerationHistory());
        cpw.mods.fml.common.network.NetworkRegistry.INSTANCE
            .registerGuiHandler(this, new naturalexceptions.minecraft.gui.NEGuiHandler());

        tickHandler = new NEWorldTickHandler();
        FMLCommonHandler.instance()
            .bus()
            .register(tickHandler);

        gravityTickHandler = new NEGravityTickHandler(NEGravityService.global());
        MinecraftForge.EVENT_BUS.register(gravityTickHandler);
        FMLCommonHandler.instance()
            .bus()
            .register(gravityTickHandler);

        naturalexceptions.minecraft.knowledge.KnowledgeRegistry.instance()
            .init();

        if (event.getSide()
            .isClient()) {
            registerClientRenderers();
            MinecraftForge.EVENT_BUS.register(new naturalexceptions.minecraft.client.NEItemTooltipEventHandler());
            naturalexceptions.minecraft.client.P8ClientCues cues = new naturalexceptions.minecraft.client.P8ClientCues();
            MinecraftForge.EVENT_BUS.register(cues);
            FMLCommonHandler.instance()
                .bus()
                .register(cues);
            naturalexceptions.minecraft.knowledge.client.NEKnowledgeKeyHandler.register();
            naturalexceptions.minecraft.biology.client.BodyStatusPanel.register();
            naturalexceptions.minecraft.gui.client.EffectColumn.register();
            naturalexceptions.minecraft.gear.client.InventoryTabs.register();
            naturalexceptions.minecraft.gear.client.GearRenderer.register();
            naturalexceptions.minecraft.gear.client.GearClientEffects.register();
            naturalexceptions.minecraft.gear.client.VisorSight.register();
        }
    }

    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    private void registerClientRenderers() {
        cpw.mods.fml.client.registry.RenderingRegistry.registerEntityRenderingHandler(
            naturalexceptions.minecraft.event.EntityFallingStar.class,
            new naturalexceptions.minecraft.event.client.RenderFallingStar());
        cpw.mods.fml.client.registry.RenderingRegistry.registerEntityRenderingHandler(
            naturalexceptions.minecraft.weapon.EntitySkyfallStar.class,
            new naturalexceptions.minecraft.event.client.RenderFallingStar());
        cpw.mods.fml.client.registry.RenderingRegistry.registerEntityRenderingHandler(
            naturalexceptions.minecraft.weapon.EntityStormglassBolt.class,
            new naturalexceptions.minecraft.weapon.client.RenderStormglassBolt());
        cpw.mods.fml.client.registry.RenderingRegistry.registerEntityRenderingHandler(
            naturalexceptions.minecraft.entity.EntitySkyWarden.class,
            new naturalexceptions.minecraft.entity.client.RenderSkyWarden());
        cpw.mods.fml.client.registry.RenderingRegistry
            .registerBlockHandler(new naturalexceptions.minecraft.client.model.PartModelRenderer());
        cpw.mods.fml.client.registry.ClientRegistry.bindTileEntitySpecialRenderer(
            naturalexceptions.minecraft.fabrication.TileEntityCastingTable.class,
            new naturalexceptions.minecraft.fabrication.client.RenderCastingTable());
        cpw.mods.fml.client.registry.ClientRegistry.bindTileEntitySpecialRenderer(
            naturalexceptions.minecraft.metallurgy.TileEntitySmelterOutlet.class,
            new naturalexceptions.minecraft.metallurgy.client.RenderSmelterOutlet());
        cpw.mods.fml.client.registry.ClientRegistry.bindTileEntitySpecialRenderer(
            naturalexceptions.minecraft.fabrication.TileEntityForgingAnvil.class,
            new naturalexceptions.minecraft.fabrication.client.RenderForgingAnvil());
        cpw.mods.fml.client.registry.ClientRegistry.bindTileEntitySpecialRenderer(
            naturalexceptions.minecraft.metallurgy.TileEntityStarterSmelter.class,
            new naturalexceptions.minecraft.metallurgy.client.RenderStarterSmelter());
        cpw.mods.fml.client.registry.ClientRegistry.bindTileEntitySpecialRenderer(
            naturalexceptions.minecraft.fabrication.TileEntityAssemblyTable.class,
            new naturalexceptions.minecraft.fabrication.client.RenderAssemblyTable());
        naturalexceptions.minecraft.furniture.client.RenderFurnitureItems furniture = new naturalexceptions.minecraft.furniture.client.RenderFurnitureItems();
        cpw.mods.fml.client.registry.ClientRegistry.bindTileEntitySpecialRenderer(
            naturalexceptions.minecraft.furniture.TileEntityWoodenTable.class,
            furniture);
        cpw.mods.fml.client.registry.ClientRegistry.bindTileEntitySpecialRenderer(
            naturalexceptions.minecraft.furniture.TileEntityMarketCounter.class,
            furniture);
        cpw.mods.fml.client.registry.ClientRegistry.bindTileEntitySpecialRenderer(
            naturalexceptions.minecraft.furniture.TileEntityPantryShelf.class,
            furniture);
        cpw.mods.fml.client.registry.ClientRegistry
            .bindTileEntitySpecialRenderer(naturalexceptions.minecraft.furniture.TileEntityWallShelf.class, furniture);
        cpw.mods.fml.client.registry.ClientRegistry
            .bindTileEntitySpecialRenderer(naturalexceptions.minecraft.furniture.TileEntityWeaponRack.class, furniture);
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        if (event != null) {
            event.registerServerCommand(new CommandDebugGravityBody());
            event.registerServerCommand(new CommandDebugGravity());
            event.registerServerCommand(new naturalexceptions.minecraft.metallurgy.CommandSmelter());
            event.registerServerCommand(new naturalexceptions.minecraft.tick.CommandP8());
            event.registerServerCommand(new naturalexceptions.minecraft.tick.CommandP9());
            event.registerServerCommand(new naturalexceptions.minecraft.tick.CommandP11());
            event.registerServerCommand(new naturalexceptions.minecraft.biology.CommandBiology());
            event.registerServerCommand(new naturalexceptions.minecraft.worldgen.CommandLandmarks());
        }
    }

    public static NEWorldTickHandler tickHandler() {
        return tickHandler;
    }

    public static NEGravityTickHandler gravityTickHandler() {
        return gravityTickHandler;
    }
}
