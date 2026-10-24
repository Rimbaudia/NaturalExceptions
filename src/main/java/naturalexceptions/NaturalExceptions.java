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
    acceptedMinecraftVersions = "[1.7.10]",
    // 1.7.10 is not playable without these (see README, Requirements). Angelica, client-only, is checked in preInit.
    dependencies = "required-after:gtnhlib;required-after:hodgepodge;required-after:lumi;required-after:chunkapi;"
        + "required-after:falsepatternlib")
public final class NaturalExceptions {

    public static final String MOD_ID = "naturalexceptions";
    public static final String MOD_NAME = "Natural Exceptions";

    @Mod.Instance(NaturalExceptions.MOD_ID)
    public static NaturalExceptions instance;

    private static NEWorldTickHandler tickHandler;
    private static NEGravityTickHandler gravityTickHandler;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        if (event.getSide()
            .isClient() && !cpw.mods.fml.common.Loader.isModLoaded("angelica"))
            // Forge shows its missing-mods screen for this, naming the mod.
            throw new cpw.mods.fml.common.MissingModsException(
                java.util.Collections
                    .singleton(cpw.mods.fml.common.versioning.VersionParser.parseVersionReference("angelica")));
        BuiltinContent.register();
        NEItems.register();
        NEBlocks.register();

        if (event.getSide()
            .isClient()) {
            registerResourcePacks();
            registerGearIcons();
        }
    }

    /** Before the first texture stitch, so empty gear slots have their silhouettes. */
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    private void registerGearIcons() {
        naturalexceptions.minecraft.gear.client.GearIcons.register();
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
        MinecraftForge.EVENT_BUS.register(new naturalexceptions.minecraft.gear.HatAndShoeEffects());
        MinecraftForge.EVENT_BUS.register(new naturalexceptions.minecraft.worldgen.WindGates());
        cpw.mods.fml.common.FMLCommonHandler.instance()
            .bus()
            .register(new naturalexceptions.minecraft.power.PowerNetworks.Ticker());
        MinecraftForge.EVENT_BUS.register(new naturalexceptions.minecraft.event.StarRainWorldMaterializer());
        FMLCommonHandler.instance()
            .bus()
            .register(new naturalexceptions.minecraft.knowledge.PlayerKnowledgeEvents());
        FMLCommonHandler.instance()
            .bus()
            .register(new naturalexceptions.minecraft.network.MainThreadTasks());
        FMLCommonHandler.instance()
            .bus()
            .register(new naturalexceptions.minecraft.worldgen.DungeonBeacons());
        FMLCommonHandler.instance()
            .bus()
            .register(new naturalexceptions.minecraft.worldgen.LandPreparation());
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
            naturalexceptions.minecraft.entity.EntityCastellan.class,
            "sky_warden",
            3,
            this,
            80,
            3,
            true);
        cpw.mods.fml.common.registry.EntityRegistry.registerModEntity(
            naturalexceptions.minecraft.entity.EntityStormwisp.class,
            "stormwisp",
            4,
            this,
            64,
            2,
            true);
        cpw.mods.fml.common.registry.EntityRegistry.registerModEntity(
            naturalexceptions.minecraft.entity.EntitySkystoneSentinel.class,
            "skystone_sentinel",
            5,
            this,
            64,
            3,
            true);
        cpw.mods.fml.common.registry.EntityRegistry
            .registerModEntity(naturalexceptions.minecraft.furniture.EntitySeat.class, "seat", 6, this, 32, 20, false);
        cpw.mods.fml.common.registry.EntityRegistry.registerModEntity(
            naturalexceptions.minecraft.entity.EntityGaleHusk.class,
            "gale_husk",
            8,
            this,
            64,
            3,
            true);
        cpw.mods.fml.common.registry.EntityRegistry.registerModEntity(
            naturalexceptions.minecraft.entity.EntityStormboneArcher.class,
            "stormbone_archer",
            9,
            this,
            64,
            3,
            true);
        cpw.mods.fml.common.registry.EntityRegistry.registerModEntity(
            naturalexceptions.minecraft.entity.EntityStormArrow.class,
            "storm_arrow",
            10,
            this,
            64,
            20,
            true);
        cpw.mods.fml.common.registry.EntityRegistry.registerModEntity(
            naturalexceptions.minecraft.entity.EntityCitadelAutomaton.class,
            "citadel_guard",
            11,
            this,
            64,
            3,
            true);
        cpw.mods.fml.common.registry.EntityRegistry.registerModEntity(
            naturalexceptions.minecraft.entity.EntityFoundryColossus.class,
            "foundry_colossus",
            15,
            this,
            80,
            3,
            true);
        cpw.mods.fml.common.registry.EntityRegistry.registerModEntity(
            naturalexceptions.minecraft.entity.EntitySlagGlob.class,
            "slag_glob",
            16,
            this,
            64,
            10,
            true);
        cpw.mods.fml.common.registry.EntityRegistry.registerModEntity(
            naturalexceptions.minecraft.entity.EntityGathered.class,
            "gathered",
            17,
            this,
            80,
            3,
            true);
        cpw.mods.fml.common.registry.EntityRegistry.registerModEntity(
            naturalexceptions.minecraft.entity.EntityGatheredShade.class,
            "gathered_shade",
            18,
            this,
            64,
            3,
            true);
        cpw.mods.fml.common.registry.EntityRegistry.registerModEntity(
            naturalexceptions.minecraft.entity.EntityRepairedAutomaton.class,
            "repaired_automaton",
            19,
            this,
            64,
            3,
            true);
        cpw.mods.fml.common.registry.EntityRegistry.registerModEntity(
            naturalexceptions.minecraft.entity.EntityCloudweaver.class,
            "cloudweaver",
            12,
            this,
            64,
            3,
            true);
        cpw.mods.fml.common.registry.EntityRegistry.registerModEntity(
            naturalexceptions.minecraft.entity.EntityCloudsilk.class,
            "cloudsilk",
            13,
            this,
            64,
            10,
            true);
        MinecraftForge.EVENT_BUS.register(new naturalexceptions.minecraft.entity.SkyMobEvents());
        MinecraftForge.EVENT_BUS.register(new naturalexceptions.minecraft.worldgen.SkyIslandSpawns());
        cpw.mods.fml.common.registry.EntityRegistry.registerModEntity(
            naturalexceptions.minecraft.furniture.EntityHoistLoad.class,
            "hoist_load",
            7,
            this,
            96,
            1,
            false); // its rope length goes out every tick; the client hangs it from that, no positions
        cpw.mods.fml.common.registry.EntityRegistry.registerModEntity(
            naturalexceptions.minecraft.furniture.EntityHoistCounterweight.class,
            "hoist_counterweight",
            14,
            this,
            96,
            1,
            false);
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
            .isClient()) registerClient();
    }

    /** The client's renderers, input, overlays and cues (stripped from a dedicated server's copy of this class). */
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    private void registerClient() {
        registerClientRenderers();
        naturalexceptions.minecraft.client.CommandFps.register();
        naturalexceptions.minecraft.client.ArrivalHold.register();
        MinecraftForge.EVENT_BUS.register(new naturalexceptions.minecraft.client.NEItemTooltipEventHandler());
        naturalexceptions.minecraft.client.P8ClientCues cues = new naturalexceptions.minecraft.client.P8ClientCues();
        MinecraftForge.EVENT_BUS.register(cues);
        FMLCommonHandler.instance()
            .bus()
            .register(cues);
        naturalexceptions.minecraft.knowledge.client.NEKnowledgeKeyHandler.register();
        naturalexceptions.minecraft.biology.client.BodyStatusPanel.register();
        naturalexceptions.minecraft.worldgen.client.GuiDungeonChart.Opener.register();
        naturalexceptions.minecraft.gui.client.EffectColumn.register();
        naturalexceptions.minecraft.gear.client.InventoryTabs.register();
        naturalexceptions.minecraft.gear.client.GearRenderer.register();
        naturalexceptions.minecraft.gear.client.GearClientEffects.register();
        naturalexceptions.minecraft.gear.client.VisorSight.register();
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
            naturalexceptions.minecraft.entity.EntityCastellan.class,
            new naturalexceptions.minecraft.entity.client.RenderCastellan());
        cpw.mods.fml.client.registry.RenderingRegistry.registerEntityRenderingHandler(
            naturalexceptions.minecraft.entity.EntityStormwisp.class,
            new naturalexceptions.minecraft.entity.client.RenderStormwisp());
        cpw.mods.fml.client.registry.RenderingRegistry.registerEntityRenderingHandler(
            naturalexceptions.minecraft.entity.EntitySkystoneSentinel.class,
            new naturalexceptions.minecraft.entity.client.RenderSkystoneSentinel());
        cpw.mods.fml.client.registry.RenderingRegistry.registerEntityRenderingHandler(
            naturalexceptions.minecraft.entity.EntityGaleHusk.class,
            naturalexceptions.minecraft.entity.client.RenderSkyMob.galeHusk());
        cpw.mods.fml.client.registry.RenderingRegistry.registerEntityRenderingHandler(
            naturalexceptions.minecraft.entity.EntityStormboneArcher.class,
            naturalexceptions.minecraft.entity.client.RenderSkyMob.stormboneArcher());
        cpw.mods.fml.client.registry.RenderingRegistry.registerEntityRenderingHandler(
            naturalexceptions.minecraft.entity.EntityStormArrow.class,
            new net.minecraft.client.renderer.entity.RenderArrow());
        cpw.mods.fml.client.registry.RenderingRegistry.registerEntityRenderingHandler(
            naturalexceptions.minecraft.entity.EntityCitadelAutomaton.class,
            new naturalexceptions.minecraft.entity.client.RenderCitadelAutomaton());
        cpw.mods.fml.client.registry.RenderingRegistry.registerEntityRenderingHandler(
            naturalexceptions.minecraft.entity.EntityRepairedAutomaton.class,
            new naturalexceptions.minecraft.entity.client.RenderCitadelAutomaton());
        cpw.mods.fml.client.registry.RenderingRegistry.registerEntityRenderingHandler(
            naturalexceptions.minecraft.entity.EntityFoundryColossus.class,
            new naturalexceptions.minecraft.entity.client.RenderFoundryColossus());
        cpw.mods.fml.client.registry.RenderingRegistry.registerEntityRenderingHandler(
            naturalexceptions.minecraft.entity.EntityGathered.class,
            new naturalexceptions.minecraft.entity.client.RenderGathered());
        cpw.mods.fml.client.registry.RenderingRegistry.registerEntityRenderingHandler(
            naturalexceptions.minecraft.entity.EntityGatheredShade.class,
            new naturalexceptions.minecraft.entity.client.RenderGathered.Shade());
        cpw.mods.fml.client.registry.RenderingRegistry.registerEntityRenderingHandler(
            naturalexceptions.minecraft.entity.EntitySlagGlob.class,
            new net.minecraft.client.renderer.entity.RenderSnowball(
                naturalexceptions.minecraft.registry.NEItems.slagLump));
        cpw.mods.fml.client.registry.RenderingRegistry.registerEntityRenderingHandler(
            naturalexceptions.minecraft.entity.EntityCloudweaver.class,
            new naturalexceptions.minecraft.entity.client.RenderCloudweaver());
        cpw.mods.fml.client.registry.RenderingRegistry.registerEntityRenderingHandler(
            naturalexceptions.minecraft.entity.EntityCloudsilk.class,
            new net.minecraft.client.renderer.entity.RenderSnowball(
                naturalexceptions.minecraft.registry.NEItems.cloudsilk));
        cpw.mods.fml.client.registry.RenderingRegistry
            .registerBlockHandler(new naturalexceptions.minecraft.client.model.PartModelRenderer());
        cpw.mods.fml.client.registry.RenderingRegistry
            .registerBlockHandler(new naturalexceptions.minecraft.power.client.CableRenderer());
        cpw.mods.fml.client.registry.ClientRegistry.bindTileEntitySpecialRenderer(
            naturalexceptions.minecraft.power.TileWindmill.class,
            new naturalexceptions.minecraft.power.client.RenderWindmill());
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
        cpw.mods.fml.client.registry.ClientRegistry.bindTileEntitySpecialRenderer(
            naturalexceptions.minecraft.furniture.TileEntityTanningRack.class,
            furniture);
        cpw.mods.fml.client.registry.ClientRegistry.bindTileEntitySpecialRenderer(
            naturalexceptions.minecraft.furniture.TileEntityOrrery.class,
            new naturalexceptions.minecraft.furniture.client.RenderOrrery());
        cpw.mods.fml.client.registry.ClientRegistry.bindTileEntitySpecialRenderer(
            naturalexceptions.minecraft.furniture.TileEntityLighthouseLamp.class,
            new naturalexceptions.minecraft.furniture.client.RenderLighthouseLamp());
        cpw.mods.fml.client.registry.RenderingRegistry.registerEntityRenderingHandler(
            naturalexceptions.minecraft.furniture.EntityFloatingNumber.class,
            new naturalexceptions.minecraft.furniture.client.RenderFloatingNumber());
        cpw.mods.fml.client.registry.RenderingRegistry.registerEntityRenderingHandler(
            naturalexceptions.minecraft.furniture.EntitySeat.class,
            new net.minecraft.client.renderer.entity.Render() {

                @Override
                public void doRender(net.minecraft.entity.Entity entity, double x, double y, double z, float yaw,
                    float partialTicks) {}

                @Override
                protected net.minecraft.util.ResourceLocation getEntityTexture(net.minecraft.entity.Entity entity) {
                    return null;
                }
            });
        cpw.mods.fml.client.registry.RenderingRegistry.registerEntityRenderingHandler(
            naturalexceptions.minecraft.furniture.EntityHoistLoad.class,
            new naturalexceptions.minecraft.furniture.client.RenderHoistLoad());
        naturalexceptions.minecraft.furniture.client.HoistHandLine.register();
        cpw.mods.fml.client.registry.ClientRegistry.bindTileEntitySpecialRenderer(
            naturalexceptions.minecraft.furniture.TileEntityHoist.class,
            new naturalexceptions.minecraft.furniture.client.RenderWindlass());
        cpw.mods.fml.client.registry.ClientRegistry.bindTileEntitySpecialRenderer(
            naturalexceptions.minecraft.furniture.TileEntityDerrickPivot.class,
            new naturalexceptions.minecraft.furniture.client.RenderDerrickPivot());
        cpw.mods.fml.client.registry.RenderingRegistry.registerEntityRenderingHandler(
            naturalexceptions.minecraft.furniture.EntityHoistCounterweight.class,
            new naturalexceptions.minecraft.furniture.client.RenderHoistCounterweight());
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
            event.registerServerCommand(new naturalexceptions.minecraft.tick.CommandPerf());
        }
    }

    public static NEWorldTickHandler tickHandler() {
        return tickHandler;
    }

    public static NEGravityTickHandler gravityTickHandler() {
        return gravityTickHandler;
    }
}
