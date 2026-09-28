package naturalexceptions.core.biology;

import java.util.Arrays;
import java.util.List;

import naturalexceptions.core.id.ResourceId;
import naturalexceptions.core.property.MaterialProperties;

/** Authored starter physiology fixtures shared by players and organism tests. */
public final class BuiltinBodyPlans {
    public static final ResourceId HUMAN_ID = ResourceId.of("naturalexceptions", "human");
    public static final ResourceId CRYSTALLINE_ID = ResourceId.of("naturalexceptions", "crystalline");
    private BuiltinBodyPlans() { }

    public static void registerInto(BodyPlanRegistry registry) {
        registry.register(humanLike());
        registry.register(crystalline());
    }

    public static BodyPlan humanLike() {
        return new BodyPlan(HUMAN_ID, humanRegions(), PhysiologyProfile.humanLike(),
            MaterialProperties.builder().density(1.05).hardness(0.15).specificHeatSolid(3500).build());
    }

    public static BodyPlan crystalline() {
        List<BodyRegionDefinition> regions = Arrays.asList(
            region("head", 90, 38, 0.0, 0.05, 0.05), region("torso", 120, 55, 0.0, 0.0, 0.0),
            region("left_arm", 95, 36, 0.0, 0.0, 0.22), region("right_arm", 95, 36, 0.0, 0.0, 0.22),
            region("left_leg", 100, 42, 0.0, 0.45, 0.0), region("right_leg", 100, 42, 0.0, 0.45, 0.0));
        return new BodyPlan(CRYSTALLINE_ID, regions, PhysiologyProfile.crystalline(),
            MaterialProperties.builder().density(2.8).hardness(7.0).specificHeatSolid(760).build());
    }

    private static List<BodyRegionDefinition> humanRegions() {
        return Arrays.asList(region("head", 8, 75, 0.55, 0.05, 0.05), region("torso", 12, 105, 1.0, 0.0, 0.0),
            region("left_arm", 9, 55, 0.35, 0.0, 0.25), region("right_arm", 9, 55, 0.35, 0.0, 0.25),
            region("left_leg", 10, 65, 0.25, 0.45, 0.0), region("right_leg", 10, 65, 0.25, 0.45, 0.0));
    }

    private static BodyRegionDefinition region(String path, double cutJ, double fractureJ,
        double bleed, double mobility, double handling) {
        return new BodyRegionDefinition(ResourceId.of("naturalexceptions", path), cutJ, fractureJ,
            bleed, mobility, handling);
    }
}
