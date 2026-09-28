package naturalexceptions.core.biology;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import naturalexceptions.core.id.DeterministicId;
import naturalexceptions.core.id.ResourceId;
import naturalexceptions.core.property.MaterialProperties;

final class BiologicalExposureTest {
    @Test void routesAccumulateQuantitativeDoseAndProtectionChangesRate() {
        BiologicalState body = new BiologicalState(BuiltinBodyPlans.humanLike(),
            DeterministicId.derive("organism", "exposure"), 0L);
        MaterialProperties hazard = MaterialProperties.builder().biologicalHazard(2.0).build();
        ResourceId smoke = ResourceId.of("test", "smoke");
        double first = body.expose(smoke, hazard, ExposureRoute.INHALATION, 10.0, 0L);
        double repeated = body.expose(smoke, hazard, ExposureRoute.INHALATION, 20.0, 0L);
        assertEquals(3.0 * first, repeated + first, 1e-9);
        assertEquals(42.0, body.exposures().get(0).dose(), 1e-9);
        body.addExposureProtection(new ExposureProtection(ExposureRoute.INHALATION, 0.8));
        double protectedDose = body.expose(smoke, hazard, ExposureRoute.INHALATION, 10.0, 0L);
        assertEquals(first * 0.2, protectedDose, 1e-9);
        assertEquals(1, body.exposures().size());
    }

    @Test void everyRequiredRouteIsRepresentableAndMaterialPotencyMatters() {
        BiologicalState body = new BiologicalState(BuiltinBodyPlans.humanLike(), 0L);
        MaterialProperties inert = MaterialProperties.builder().build();
        MaterialProperties hazardous = MaterialProperties.builder().biologicalHazard(4.0).build();
        ResourceId inertId = ResourceId.of("test", "inert");
        ResourceId hazardId = ResourceId.of("test", "hazard");
        for (ExposureRoute route : ExposureRoute.values()) {
            assertEquals(0.0, body.expose(inertId, inert, route, 10.0, 0L), 0.0);
            assertTrue(body.expose(hazardId, hazardous, route, 10.0, 0L) > 0.0);
        }
        assertEquals(ExposureRoute.values().length, body.exposures().size());
        BiologicalState crystal = new BiologicalState(BuiltinBodyPlans.crystalline(), 0L);
        assertTrue(crystal.expose(hazardId, hazardous, ExposureRoute.CONTACT, 10.0, 0L)
            < body.expose(hazardId, hazardous, ExposureRoute.CONTACT, 10.0, 0L));
    }
}
