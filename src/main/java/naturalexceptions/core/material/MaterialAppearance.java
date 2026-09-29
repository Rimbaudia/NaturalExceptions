package naturalexceptions.core.material;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import naturalexceptions.core.composition.CanonicalComposition;
import naturalexceptions.core.id.ResourceId;

/**
 * Authoritative appearance and color engine for Natural Exceptions materials.
 *
 * <p>
 * Provides continuous color and emissivity resolution for both solid and molten states:
 * <ul>
 * <li>Solid colors derive from authored element profiles and mass-weighted composition mixing.</li>
 * <li>Molten colors synthesize material-specific color with Planckian incandescence and anomalous emissions.</li>
 * </ul>
 */
public final class MaterialAppearance {

    private static final Map<ResourceId, Integer> ELEMENT_COLORS = new HashMap<>();

    // Standard baseline elemental and compound palette
    public static final ResourceId ID_CU = ResourceId.of("naturalexceptions", "copper");
    public static final ResourceId ID_SN = ResourceId.of("naturalexceptions", "tin");
    public static final ResourceId ID_ZN = ResourceId.of("naturalexceptions", "zinc");
    public static final ResourceId ID_PB = ResourceId.of("naturalexceptions", "lead");
    public static final ResourceId ID_FE = ResourceId.of("naturalexceptions", "iron");
    public static final ResourceId ID_AU = ResourceId.of("naturalexceptions", "gold");
    public static final ResourceId ID_AG = ResourceId.of("naturalexceptions", "silver");
    public static final ResourceId ID_BRONZE = ResourceId.of("naturalexceptions", "bronze");
    public static final ResourceId ID_WOOD = ResourceId.of("naturalexceptions", "wood");
    public static final ResourceId ID_SLAG = ResourceId.of("naturalexceptions", "molten_slag");
    public static final ResourceId ID_SLAG_ALT = ResourceId.of("naturalexceptions", "slag");
    public static final ResourceId ID_STELLAR = ResourceId.of("naturalexceptions", "stellar_matter");
    public static final ResourceId ID_IMPACT_CRUST = ResourceId.of("naturalexceptions", "impact_crust");
    public static final ResourceId ID_LUMENITE = ResourceId.of("naturalexceptions", "lumenite");
    public static final ResourceId ID_AVEL = ResourceId.of("naturalexceptions", "avel");
    public static final ResourceId ID_RHEL = ResourceId.of("naturalexceptions", "rhel");
    public static final ResourceId ID_THREN = ResourceId.of("naturalexceptions", "thren");
    public static final ResourceId ID_VEYR = ResourceId.of("naturalexceptions", "veyr");

    static {
        ELEMENT_COLORS.put(ID_CU, 0xD36C4C); // Warm reddish copper
        ELEMENT_COLORS.put(ID_SN, 0xD0D8DE); // Bright silvery-white tin
        ELEMENT_COLORS.put(ID_ZN, 0xBCC3C7); // Bluish-grey zinc
        ELEMENT_COLORS.put(ID_PB, 0x646672); // Dark slate-grey lead
        ELEMENT_COLORS.put(ID_FE, 0xD8D8D8); // Steel silver iron
        ELEMENT_COLORS.put(ID_AU, 0xFEE135); // Gleaming yellow gold
        ELEMENT_COLORS.put(ID_AG, 0xEEF2F6); // Lustrous silver
        ELEMENT_COLORS.put(ID_BRONZE, 0xC48443); // Classic reddish-gold bronze
        ELEMENT_COLORS.put(ID_WOOD, 0x8B5A2B); // Organic wood brown
        ELEMENT_COLORS.put(ID_SLAG, 0x3D352E); // Dark glassy silicate
        ELEMENT_COLORS.put(ID_SLAG_ALT, 0x3D352E);
        ELEMENT_COLORS.put(ID_STELLAR, 0x66CCFF); // Celestial blue star matter
        ELEMENT_COLORS.put(ID_IMPACT_CRUST, 0x3A3F44); // Dark fused glassy rock
        ELEMENT_COLORS.put(ID_LUMENITE, 0x9FD8F0); // Pale self-lit blue
        ELEMENT_COLORS.put(ID_AVEL, 0x38D6E8); // Fracture crystal cyan
        ELEMENT_COLORS.put(ID_RHEL, 0x9E2B52); // Biocrust crimson
        ELEMENT_COLORS.put(ID_THREN, 0x44A044); // Verdant amber-green
        ELEMENT_COLORS.put(ID_VEYR, 0x7B439B); // Regal violet
    }

    private MaterialAppearance() {}

    /**
     * Resolves the solid RGB color for a given material ref and state key.
     */
    public static int getSolidColor(MaterialRef ref, CanonicalMaterialKey key) {
        if (ref != null && ref.isAuthored()) {
            Integer color = ELEMENT_COLORS.get(ref.authoredId());
            if (color != null) {
                return color;
            }
        }

        if (key != null && key.composition() != null) {
            return getCompositionColor(key.composition());
        }

        if (ref != null) {
            String path = ref.isAuthored() ? ref.authoredId()
                .path()
                .toLowerCase()
                : ref.asString()
                    .toLowerCase();
            Integer color = ELEMENT_COLORS.get(ResourceId.of("naturalexceptions", path));
            if (color != null) {
                return color;
            }
        }

        return 0xC0C0C0; // Clean metallic grey default
    }

    /**
     * Resolves the composite solid RGB color from an elemental mass composition.
     */
    public static int getCompositionColor(CanonicalComposition comp) {
        Objects.requireNonNull(comp, "comp");
        double totalWeight = 0.0;
        double rAcc = 0.0;
        double gAcc = 0.0;
        double bAcc = 0.0;

        for (Map.Entry<ResourceId, Integer> entry : comp.asMap()
            .entrySet()) {
            ResourceId id = entry.getKey();
            double frac = entry.getValue() / (double) CanonicalComposition.TOTAL_UNITS;
            int col = ELEMENT_COLORS.getOrDefault(id, 0xA0A0A0);

            double r = (col >> 16) & 0xFF;
            double g = (col >> 8) & 0xFF;
            double b = col & 0xFF;

            rAcc += r * frac;
            gAcc += g * frac;
            bAcc += b * frac;
            totalWeight += frac;
        }

        if (totalWeight <= 0.0) {
            return 0xC0C0C0;
        }

        int rInt = Math.min(255, Math.max(0, (int) Math.round(rAcc / totalWeight)));
        int gInt = Math.min(255, Math.max(0, (int) Math.round(gAcc / totalWeight)));
        int bInt = Math.min(255, Math.max(0, (int) Math.round(bAcc / totalWeight)));

        return (rInt << 16) | (gInt << 8) | bInt;
    }

    /**
     * Resolves the molten RGB color for a material at a specific temperature.
     */
    public static int getMoltenColor(MaterialRef ref, CanonicalMaterialKey key, double temperatureKelvin) {
        // 1. Check anomalous signature emitters first
        if (ref != null) {
            String idStr = ref.asString()
                .toLowerCase();
            if (idStr.contains("avel")) {
                return 0x55FFFF; // Radiant cyan
            } else if (idStr.contains("rhel")) {
                return 0xFF2255; // Incandescent crimson
            } else if (idStr.contains("thren")) {
                return 0x66FF33; // Radiant emerald
            } else if (idStr.contains("veyr")) {
                return 0xC044FF; // Electric violet
            } else if (idStr.contains("stellar")) {
                return 0x99FFFF; // Plasma cyan-white
            } else if (idStr.contains("slag")) {
                return 0x8B3214; // Molten lava-slag crust
            }
        }

        int solidCol = getSolidColor(ref, key);
        double sR = ((solidCol >> 16) & 0xFF) / 255.0;
        double sG = ((solidCol >> 8) & 0xFF) / 255.0;
        double sB = (solidCol & 0xFF) / 255.0;

        // 2. Compute Planckian blackbody thermal radiance color
        double temp = Math.max(300.0, temperatureKelvin);
        double hR, hG, hB;
        if (temp < 800.0) {
            // Low-temperature melt (e.g. tin 505K, lead 601K)
            // Silvery liquid metal with warm incandescent underglow
            hR = 0.95;
            hG = 0.65;
            hB = 0.40;
        } else if (temp < 1100.0) {
            // Dull cherry red
            hR = 0.95;
            hG = 0.35;
            hB = 0.15;
        } else if (temp < 1400.0) {
            // Bright molten copper / bronze orange
            hR = 1.0;
            hG = 0.55;
            hB = 0.15;
        } else if (temp < 1700.0) {
            // Bright golden-yellow heat
            hR = 1.0;
            hG = 0.80;
            hB = 0.30;
        } else {
            // White-hot steel / high superheat
            hR = 1.0;
            hG = 0.95;
            hB = 0.70;
        }

        // Blend 50% material character with 50% thermal radiance
        double mR = sR * 0.40 + hR * 0.60;
        double mG = sG * 0.35 + hG * 0.65;
        double mB = sB * 0.30 + hB * 0.70;

        int rInt = Math.min(255, Math.max(0, (int) Math.round(mR * 255.0)));
        int gInt = Math.min(255, Math.max(0, (int) Math.round(mG * 255.0)));
        int bInt = Math.min(255, Math.max(0, (int) Math.round(mB * 255.0)));

        return (rInt << 16) | (gInt << 8) | bInt;
    }

    /**
     * Returns the molten color as normalized floats [R, G, B] for OpenGL rendering.
     */
    public static float[] getMoltenRgbFloat(MaterialRef ref, CanonicalMaterialKey key, double temperatureKelvin) {
        int color = getMoltenColor(ref, key, temperatureKelvin);
        float r = ((color >> 16) & 0xFF) / 255.0F;
        float g = ((color >> 8) & 0xFF) / 255.0F;
        float b = (color & 0xFF) / 255.0F;
        return new float[] { r, g, b };
    }

    /**
     * Resolves continuous thermal incandescence (blackbody glow) as [R, G, B, Alpha] for OpenGL overlay rendering.
     * Alpha smoothly transitions from 0.0 at dark/ambient (<= 550 K) to dull red (550-750 K), cherry red (750-950 K),
     * glowing orange (950-1250 K), and dazzling yellow-white (1250-1800+ K).
     */
    public static float[] getThermalIncandescenceRgba(double temperatureKelvin) {
        if (temperatureKelvin <= 550.0) {
            return new float[] { 0.0F, 0.0F, 0.0F, 0.0F };
        }
        double temp = Math.min(1800.0, temperatureKelvin);
        float r, g, b, a;
        if (temp < 750.0) {
            // Dull deep crimson / cherry red glow just becoming visible
            float t = (float) ((temp - 550.0) / 200.0);
            r = 0.70F + 0.25F * t;
            g = 0.05F + 0.10F * t;
            b = 0.02F + 0.05F * t;
            a = 0.05F + 0.30F * t; // 0.05 to 0.35 alpha
        } else if (temp < 950.0) {
            // Cherry red to bright red-orange
            float t = (float) ((temp - 750.0) / 200.0);
            r = 0.95F + 0.05F * t;
            g = 0.15F + 0.35F * t;
            b = 0.07F + 0.05F * t;
            a = 0.35F + 0.25F * t; // 0.35 to 0.60 alpha
        } else if (temp < 1250.0) {
            // Glowing orange to bright golden yellow
            float t = (float) ((temp - 950.0) / 300.0);
            r = 1.0F;
            g = 0.50F + 0.35F * t;
            b = 0.12F + 0.25F * t;
            a = 0.60F + 0.20F * t; // 0.60 to 0.80 alpha
        } else {
            // Dazzling yellow-white heat
            float t = (float) ((temp - 1250.0) / 550.0);
            r = 1.0F;
            g = 0.85F + 0.15F * t;
            b = 0.37F + 0.45F * t;
            a = 0.80F + 0.10F * t; // 0.80 to 0.90 alpha
        }
        return new float[] { r, g, b, a };
    }
}
