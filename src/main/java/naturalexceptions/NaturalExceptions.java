package naturalexceptions;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

@Mod(
    modid = NaturalExceptions.MOD_ID,
    name = NaturalExceptions.MOD_NAME,
    version = Tags.VERSION,
    acceptedMinecraftVersions = "[1.7.10]")
public final class NaturalExceptions {

    public static final String MOD_ID = "naturalexceptions";
    public static final String MOD_NAME = "Natural Exceptions";

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {}

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {}
}
