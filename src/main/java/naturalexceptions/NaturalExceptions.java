package naturalexceptions;

import net.minecraftforge.common.MinecraftForge;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import naturalexceptions.content.BuiltinContent;
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
        tickHandler = new NEWorldTickHandler();
        FMLCommonHandler.instance()
            .bus()
            .register(tickHandler);

        gravityTickHandler = new NEGravityTickHandler(NEGravityService.global());
        MinecraftForge.EVENT_BUS.register(gravityTickHandler);
    }

    public static NEWorldTickHandler tickHandler() {
        return tickHandler;
    }

    public static NEGravityTickHandler gravityTickHandler() {
        return gravityTickHandler;
    }
}
