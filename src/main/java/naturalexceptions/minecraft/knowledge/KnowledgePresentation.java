package naturalexceptions.minecraft.knowledge;

import net.minecraft.entity.player.EntityPlayer;
import naturalexceptions.core.knowledge.KnowledgeDisplay;
import naturalexceptions.core.knowledge.KnowledgeSubjectRef;
import naturalexceptions.core.material.CanonicalMaterialKey;
import naturalexceptions.core.material.MaterialPhase;
import naturalexceptions.core.material.MaterialRef;

/** Player-aware material naming for tooltip/UI surfaces without changing stack or simulation identity. */
public final class KnowledgePresentation {
    private KnowledgePresentation() { }

    public static String materialName(EntityPlayer player, MaterialRef ref, CanonicalMaterialKey key) {
        if (ref == null) return "Unidentified material";
        // Everyday materials need no study: anyone knows a stick is wood.
        if (naturalexceptions.content.material.BuiltinMaterials.WOOD.ref()
            .equals(ref)) return "Wood";
        int rgb = naturalexceptions.core.material.MaterialAppearance.getSolidColor(ref, key);
        String appearance = classify(rgb);
        String state = key != null && key.phase() == MaterialPhase.LIQUID ? "liquid" : "solid";
        return KnowledgeDisplay.resolve(KnowledgeSubjectRef.material(ref),
            player == null ? null : PlayerKnowledgeProperties.get(player).knowledge(), appearance, state);
    }

    public static String appearance(int color) { return classify(color); }

    /**
     * The material as a word before an object's name: "Copper" for a known material, "Reddish" for one only seen
     * ("Reddish material" without its trailing noun), so "Copper Pickaxe" or "Reddish Pickaxe".
     */
    public static String materialPrefix(EntityPlayer player, MaterialRef ref, CanonicalMaterialKey key) {
        String name = materialName(player, ref, key);
        String suffix = " material";
        if (name.toLowerCase(java.util.Locale.ROOT).endsWith(suffix) && name.length() > suffix.length())
            name = name.substring(0, name.length() - suffix.length());
        if (name.toLowerCase(java.util.Locale.ROOT).startsWith("unidentified")) return "Unidentified";
        return name;
    }

    /** Whose knowledge names things on this side: the client's own player, or nobody on the server. */
    public static EntityPlayer viewer() {
        if (cpw.mods.fml.common.FMLCommonHandler.instance()
            .getEffectiveSide()
            .isClient()) return naturalexceptions.minecraft.client.ClientPlayer.get();
        return null;
    }

    private static String classify(int color) {
        int r = color >> 16 & 255, g = color >> 8 & 255, b = color & 255;
        if (r > g * 1.18 && r > b * 1.18) return "reddish";
        if (r > 180 && g > 150 && b < 120) return "yellow";
        if (Math.abs(r - g) < 24 && Math.abs(g - b) < 24) return "grey";
        if (g > r * 1.18 && b > r * 1.18 && Math.abs(g - b) < 105) return "cyan-blue";
        if (b > r * 1.25 && b > g * 1.10) return "blue";
        return "dark";
    }
}
