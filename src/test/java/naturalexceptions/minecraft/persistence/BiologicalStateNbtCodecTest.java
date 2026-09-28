package naturalexceptions.minecraft.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import naturalexceptions.core.biology.BiologicalInteractionResolver;
import naturalexceptions.core.biology.BiologicalState;
import naturalexceptions.core.biology.ArmorLayer;
import naturalexceptions.core.biology.BodyPlanRegistry;
import naturalexceptions.core.biology.BuiltinBodyPlans;
import naturalexceptions.core.biology.ExposureRoute;
import naturalexceptions.core.biology.PhysicalInteraction;
import naturalexceptions.core.id.DeterministicId;
import naturalexceptions.core.id.ResourceId;
import naturalexceptions.core.composition.CanonicalComposition;
import naturalexceptions.core.fabrication.ComponentDamage;
import naturalexceptions.core.fabrication.ComponentInstance;
import naturalexceptions.core.fabrication.GeometryId;
import naturalexceptions.core.fabrication.ManufacturingState;
import naturalexceptions.core.material.CanonicalMaterialKey;
import naturalexceptions.core.material.MaterialPhase;
import naturalexceptions.core.material.MaterialRef;
import naturalexceptions.core.metallurgy.MaterialQuantity;
import naturalexceptions.core.property.MaterialProperties;

final class BiologicalStateNbtCodecTest {
    @Test void meaningfulInjuryReservesAndExposureSurviveReload() {
        BiologicalState before = new BiologicalState(BuiltinBodyPlans.humanLike(), DeterministicId.derive("p10", "persist"), 0L);
        CanonicalComposition iron = CanonicalComposition.of(ResourceId.of("minecraft", "iron"));
        ComponentInstance plate = new ComponentInstance(DeterministicId.derive("armor", "persist"), GeometryId.PLATE,
            MaterialRef.authored("minecraft", "iron"), CanonicalMaterialKey.of(iron, MaterialPhase.SOLID, null, null),
            MaterialQuantity.ofGrams(156), ManufacturingState.FINISHED, new ComponentDamage());
        before.equipArmor(new ArmorLayer(plate, MaterialProperties.builder().density(7.8).hardness(4).build(),
            0.01, java.util.Collections.singleton(ResourceId.of("naturalexceptions", "torso"))));
        BiologicalInteractionResolver.apply(before, new PhysicalInteraction(PhysicalInteraction.Type.CUT,
            ResourceId.of("naturalexceptions", "torso"), 300.0, 1.0, 0.9, 20.0), 10L);
        before.expose(ResourceId.of("test", "hazard"), MaterialProperties.builder().biologicalHazard(3.0).build(),
            ExposureRoute.WOUND, 5.0, 10L);
        BiologicalState after = BiologicalStateNbtCodec.read(BiologicalStateNbtCodec.write(before), BodyPlanRegistry.standard(), 0L);
        assertNotNull(after);
        assertEquals(before.organismId(), after.organismId());
        assertEquals(before.bodyPlan().id(), after.bodyPlan().id());
        assertEquals(before.energyReserveJoules(), after.energyReserveJoules(), 0.0);
        assertEquals(before.injuries().size(), after.injuries().size());
        assertEquals(before.injuries().get(0).severity(), after.injuries().get(0).severity(), 0.0);
        assertEquals(before.exposures().get(0).dose(), after.exposures().get(0).dose(), 0.0);
        assertTrue(after.injuries().get(0).bleedingMillilitresPerSecond() > 0.0);
        assertEquals(1, after.armor().size());
        assertEquals(plate.damage().cracking(), after.armor().get(0).component().damage().cracking(), 0.0);
        assertTrue(after.armor().get(0).component().damage().isFractured());
    }
    @Test void unknownBodyPlanFailsSafely() {
        net.minecraft.nbt.NBTTagCompound tag = new net.minecraft.nbt.NBTTagCompound();
        tag.setInteger("Version", 1); tag.setString("Plan", "missing:plan");
        tag.setString("Organism", DeterministicId.derive("unknown").hex());
        assertNull(BiologicalStateNbtCodec.read(tag, BodyPlanRegistry.standard(), 0L));
    }
}
