package naturalexceptions.core.material;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.util.StatCollector;

import naturalexceptions.content.BuiltinContent;
import naturalexceptions.content.material.BuiltinMaterials;
import naturalexceptions.core.assembly.AssemblyGraph;
import naturalexceptions.core.composition.CanonicalComposition;
import naturalexceptions.core.fabrication.ComponentInstance;
import naturalexceptions.core.fabrication.GeometryId;
import naturalexceptions.core.fabrication.ManufacturingState;
import naturalexceptions.core.id.ResourceId;

/**
 * Authoritative naming resolver implementing the Natural Exceptions material naming hierarchy:
 * <ol>
 * <li><b>Player-assigned alloy name</b> (from persistent {@link MaterialNameRegistry}).</li>
 * <li><b>Canonical / authored material</b> (localized name via {@link StatCollector} or definition).</li>
 * <li><b>Composition match to known authored material/alloy</b> (pure Cu -> Copper, 88Cu/12Sn -> Bronze).</li>
 * <li><b>Deterministic generated composition fallback</b> (e.g. "Fe70Cu30 Alloy").</li>
 * </ol>
 */
public final class MaterialNameResolver {

    private static final Map<ResourceId, String> CHEMICAL_SYMBOLS = new HashMap<>();

    static {
        CHEMICAL_SYMBOLS.put(ResourceId.of("naturalexceptions", "iron"), "Fe");
        CHEMICAL_SYMBOLS.put(ResourceId.of("naturalexceptions", "copper"), "Cu");
        CHEMICAL_SYMBOLS.put(ResourceId.of("naturalexceptions", "tin"), "Sn");
        CHEMICAL_SYMBOLS.put(ResourceId.of("naturalexceptions", "lead"), "Pb");
        CHEMICAL_SYMBOLS.put(ResourceId.of("naturalexceptions", "zinc"), "Zn");
        CHEMICAL_SYMBOLS.put(ResourceId.of("naturalexceptions", "gold"), "Au");
        CHEMICAL_SYMBOLS.put(ResourceId.of("naturalexceptions", "silver"), "Ag");
    }

    private MaterialNameResolver() {}

    public static String resolveMaterialName(MaterialRef ref) {
        return resolveMaterialName(ref, (CanonicalMaterialKey) null);
    }

    public static String resolveMaterialName(MaterialRef ref, CanonicalComposition composition) {
        return resolveMaterialName(ref, composition != null ? CanonicalMaterialKey.of(composition) : null);
    }

    public static String resolveDisplayName(MaterialRef ref, CanonicalComposition composition) {
        return resolveMaterialName(ref, composition);
    }

    /**
     * Resolves the canonical base material name (e.g. "Copper", "Bronze", "Redsteel", "Fe70Cu30 Alloy").
     */
    public static String resolveMaterialName(MaterialRef ref, CanonicalMaterialKey key) {
        // 1. Check Player-Assigned Custom Alloy Name
        String customName = MaterialNameRegistry.global()
            .getCustomName(ref, key);
        if (customName != null && !customName.isEmpty()) {
            return customName;
        }

        // 2. Authored Material Reference
        if (ref != null && ref.isAuthored()) {
            MaterialDefinition def = BuiltinContent.materials()
                .getAuthored(ref);
            if (def != null) {
                return getLocalizedOrCapitalized(
                    def.translationKey(),
                    ref.authoredId()
                        .path());
            }
            return capitalize(
                ref.authoredId()
                    .path());
        }

        // 3. Composition Match to Known Authored Material or Alloy
        if (key != null && key.composition() != null) {
            CanonicalComposition comp = key.composition();
            if (comp.constituentCount() == 1) {
                ResourceId singleRes = comp.constituents()
                    .iterator()
                    .next();
                MaterialDefinition def = BuiltinContent.materials()
                    .getAuthored(MaterialRef.authored(singleRes));
                if (def != null) {
                    return getLocalizedOrCapitalized(def.translationKey(), singleRes.path());
                }
                return capitalize(singleRes.path());
            }

            if (isBronzeComposition(comp)) {
                return getLocalizedOrCapitalized("material.naturalexceptions.bronze", "bronze");
            }

            // 4. Deterministic Generated Composition Fallback (e.g. "Fe70Cu30 Alloy")
            return formatCompositionFallback(comp);
        }

        // Fallback for empty or unknown
        if (ref != null) {
            String str = ref.asString();
            int colon = str.lastIndexOf(':');
            String sub = colon >= 0 ? str.substring(colon + 1) : str;
            return capitalize(sub);
        }

        return "Unknown Material";
    }

    private static boolean isBronzeComposition(CanonicalComposition comp) {
        if (comp == null) return false;
        return comp.equals(BuiltinMaterials.BRONZE_COMPOSITION);
    }

    /**
     * Formats a multi-constituent composition as a clean chemical/elemental formula (e.g. "Fe70Cu30 Alloy").
     */
    public static String formatCompositionFallback(CanonicalComposition comp) {
        if (comp == null || comp.constituentCount() == 0) {
            return "Generated Alloy";
        }

        List<ResourceId> sorted = new ArrayList<>(comp.constituents());
        sorted.sort((a, b) -> {
            int cmp = Double.compare(comp.fraction(b), comp.fraction(a));
            return cmp != 0 ? cmp : a.compareTo(b);
        });

        StringBuilder sb = new StringBuilder();
        for (ResourceId res : sorted) {
            String symbol = CHEMICAL_SYMBOLS.get(res);
            if (symbol == null) {
                symbol = capitalize(res.path());
            }
            int pct = (int) Math.round(comp.fraction(res) * 100.0);
            if (pct > 0) {
                sb.append(symbol)
                    .append(pct);
            }
        }
        if (sb.length() == 0) {
            return "Generated Alloy";
        }
        sb.append(" Alloy");
        return sb.toString();
    }

    /**
     * Resolves the full display name for a dynamic tool assembly (e.g. "Copper Pickaxe", "Redsteel Pickaxe").
     */
    public static String resolveToolDisplayName(AssemblyGraph assembly) {
        if (assembly == null) {
            return "Tool Assembly";
        }
        try {
            ComponentInstance head = assembly.head();
            if (head == null) {
                return "Incomplete Tool";
            }

            String headMat = resolveMaterialName(head.materialRef(), head.stateKey());
            String geomName = formatToolGeometry(head.geometryId());

            if (assembly.isBroken()) {
                return "Broken " + headMat + " " + geomName;
            }
            return headMat + " " + geomName;
        } catch (Exception e) {
            return "Tool Assembly";
        }
    }

    /**
     * Resolves the full display name for an individual manufactured component.
     */
    public static String resolveComponentDisplayName(ComponentInstance comp) {
        if (comp == null) {
            return "Component";
        }
        try {
            String matName = resolveMaterialName(comp.materialRef(), comp.stateKey());
            String geomName = formatComponentGeometry(comp.geometryId());

            if (comp.manufacturingState() == ManufacturingState.COLD_WORKED) {
                return "Cold-Worked " + matName + " " + geomName;
            }
            if (comp.manufacturingState() == ManufacturingState.ANNEALED) {
                return "Annealed " + matName + " " + geomName;
            }
            return matName + " " + geomName;
        } catch (Exception e) {
            return "Component";
        }
    }

    /**
     * Resolves the contained metal name inside a mold (e.g. "Molten Copper", "Redsteel").
     */
    public static String resolveMoldContainedName(MaterialRef ref, CanonicalMaterialKey key, boolean isLiquid) {
        String matName = resolveMaterialName(ref, key);
        return isLiquid ? "Molten " + matName : matName;
    }

    /**
     * Resolves the display name for a material sample.
     */
    public static String resolveSampleDisplayName(MaterialRef ref, CanonicalMaterialKey key, boolean isLiquid) {
        String matName = resolveMaterialName(ref, key);
        if (key != null && MaterialNameRegistry.global()
            .getCustomName(ref, key) == null
            && matName.equals(formatCompositionFallback(key.composition()))
            && matName.endsWith(" Alloy")) {
            matName = matName.substring(0, matName.length() - " Alloy".length());
        }
        return (isLiquid ? "Molten " : "") + matName + " Sample";
    }

    public static String formatToolGeometry(GeometryId id) {
        if (id == null) return "Tool";
        switch (id) {
            case PICK_HEAD:
                return "Pickaxe";
            case AXE_HEAD:
                return "Axe";
            case SHOVEL_HEAD:
                return "Shovel";
            case HOE_HEAD:
                return "Hoe";
            case BLADE:
                return "Sword";
            case PLATE:
                return "Shield";
            case ROD:
                return "Rod";
            case HANDLE:
                return "Handle";
            case BLANK:
            default:
                return "Tool";
        }
    }

    public static String formatComponentGeometry(GeometryId id) {
        if (id == null) return "Part";
        switch (id) {
            case PICK_HEAD:
                return "Pick Head";
            case AXE_HEAD:
                return "Axe Head";
            case SHOVEL_HEAD:
                return "Shovel Head";
            case HOE_HEAD:
                return "Hoe Head";
            case BLADE:
                return "Blade";
            case PLATE:
                return "Plate";
            case ROD:
                return "Rod";
            case HANDLE:
                return "Handle";
            case BLANK:
                return "Blank";
            default:
                return "Part";
        }
    }

    private static String getLocalizedOrCapitalized(String locKey, String fallback) {
        try {
            if (StatCollector.canTranslate(locKey)) {
                return StatCollector.translateToLocal(locKey);
            }
        } catch (Throwable ignored) {}
        return capitalize(fallback);
    }

    public static String capitalize(String str) {
        if (str == null || str.isEmpty()) return "";
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }
}
