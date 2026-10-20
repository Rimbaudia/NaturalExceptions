package naturalexceptions.coremod;

import java.util.Map;

import net.minecraft.launchwrapper.Launch;

import cpw.mods.fml.relauncher.IFMLLoadingPlugin;

/**
 * NE's coremod. It makes the mods NE requires (see the README) start together; it changes nothing of Minecraft's.
 * <p>
 * Hodgepodge's "optimize world updateLightByType" fix ({@code optimizeWorldUpdateLight}) injects into the lighting
 * method that Lumi overwrites, and the game stops at startup. Hodgepodge leaves the fix out when Angelica is loaded,
 * which hid this in single player, but not for Lumi, so a dedicated server could not start. The fix is redundant with
 * Lumi anyway (Lumi replaces the lighting engine it speeds up), so when Lumi is present NE registers
 * {@link HodgepodgeLumiTransformer}. That transformer makes Hodgepodge's own condition for the fix read false.
 * <p>
 * This is done with a transformer, not by changing the config value, because FML runs coremods in an order NE cannot
 * set: UniMixins asks Hodgepodge for its early mixins before NE's coremod gets a turn. The transformer is registered
 * here, while coremods are constructed, which is before Hodgepodge's class of mixins is loaded, whatever the order.
 */
@IFMLLoadingPlugin.MCVersion("1.7.10")
@IFMLLoadingPlugin.Name("NaturalExceptionsCore")
@IFMLLoadingPlugin.TransformerExclusions("naturalexceptions.coremod")
public final class NECoreMod implements IFMLLoadingPlugin {

    static final String LUMI_CORE = "com.falsepattern.lumi.internal.asm.ASMLoadingPlugin";

    public NECoreMod() {
        if (present(LUMI_CORE)) Launch.classLoader.registerTransformer(HodgepodgeLumiTransformer.class.getName());
    }

    static boolean present(String className) {
        try {
            return Launch.classLoader.getClassBytes(className) != null;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String[] getASMTransformerClass() {
        return new String[0];
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {}

    @Override
    public String getAccessTransformerClass() {
        return null;
    }
}
