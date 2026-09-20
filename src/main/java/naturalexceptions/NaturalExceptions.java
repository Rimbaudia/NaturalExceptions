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
import naturalexceptions.minecraft.worldgen.BootstrapCopperGenerator;

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
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        BootstrapCopperGenerator.register();
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

        if (event.getSide()
            .isClient()) {
            registerClientRenderers();
        }
    }

    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    private void registerClientRenderers() {
        cpw.mods.fml.client.registry.ClientRegistry.bindTileEntitySpecialRenderer(
            naturalexceptions.minecraft.fabrication.TileEntityCastingTable.class,
            new naturalexceptions.minecraft.fabrication.client.RenderCastingTable());
        cpw.mods.fml.client.registry.ClientRegistry.bindTileEntitySpecialRenderer(
            naturalexceptions.minecraft.metallurgy.TileEntitySmelterOutlet.class,
            new naturalexceptions.minecraft.metallurgy.client.RenderSmelterOutlet());
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        if (event != null) {
            event.registerServerCommand(new CommandDebugGravityBody());
            event.registerServerCommand(new CommandDebugGravity());
            event.registerServerCommand(new naturalexceptions.minecraft.metallurgy.CommandSmelter());
        }
    }

    public static NEWorldTickHandler tickHandler() {
        return tickHandler;
    }

    public static NEGravityTickHandler gravityTickHandler() {
        return gravityTickHandler;
    }
}
