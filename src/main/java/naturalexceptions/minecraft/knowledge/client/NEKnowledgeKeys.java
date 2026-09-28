package naturalexceptions.minecraft.knowledge.client;

import net.minecraft.client.settings.KeyBinding;

import org.lwjgl.input.Keyboard;

import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Keybinding definitions for the Natural Exceptions Knowledge Browser.
 */
@SideOnly(Side.CLIENT)
public final class NEKnowledgeKeys {

    public static final String CATEGORY = "key.categories.naturalexceptions";

    public static KeyBinding keyOpenBrowser;
    public static KeyBinding keyRecipeLookup;
    public static KeyBinding keyUsageLookup;

    private static boolean registered = false;

    private NEKnowledgeKeys() {}

    public static synchronized void register() {
        if (registered) return;
        registered = true;

        keyOpenBrowser = new KeyBinding("key.naturalexceptions.open_knowledge_browser", Keyboard.KEY_K, CATEGORY);
        keyRecipeLookup = new KeyBinding("key.naturalexceptions.recipe_lookup", Keyboard.KEY_R, CATEGORY);
        keyUsageLookup = new KeyBinding("key.naturalexceptions.usage_lookup", Keyboard.KEY_U, CATEGORY);

        ClientRegistry.registerKeyBinding(keyOpenBrowser);
        ClientRegistry.registerKeyBinding(keyRecipeLookup);
        ClientRegistry.registerKeyBinding(keyUsageLookup);
    }
}
