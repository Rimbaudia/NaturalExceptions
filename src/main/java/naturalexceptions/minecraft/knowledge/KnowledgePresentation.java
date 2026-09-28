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
        int rgb = naturalexceptions.core.material.MaterialAppearance.getSolidColor(ref, key);
        String appearance = classify(rgb);
        String state = key != null && key.phase() == MaterialPhase.LIQUID ? "liquid" : "solid";
        return KnowledgeDisplay.resolve(KnowledgeSubjectRef.material(ref),
            player == null ? null : PlayerKnowledgeProperties.get(player).knowledge(), appearance, state);
    }

    public static String appearance(int color) { return classify(color); }

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
