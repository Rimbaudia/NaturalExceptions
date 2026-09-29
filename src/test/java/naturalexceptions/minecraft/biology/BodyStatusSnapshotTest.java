package naturalexceptions.minecraft.biology;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import naturalexceptions.core.biology.BiologicalInjury;
import naturalexceptions.core.biology.BiologicalState;
import naturalexceptions.core.biology.BodyPlanRegistry;
import naturalexceptions.core.biology.BuiltinBodyPlans;
import naturalexceptions.core.biology.ExposureProtection;
import naturalexceptions.core.biology.ExposureRoute;
import naturalexceptions.core.id.DeterministicId;
import naturalexceptions.core.id.ResourceId;

final class BodyStatusSnapshotTest {

    private static final ResourceId LEFT_ARM = ResourceId.of("naturalexceptions", "left_arm");
    private static final ResourceId RIGHT_LEG = ResourceId.of("naturalexceptions", "right_leg");
    private static final ResourceId TORSO = ResourceId.of("naturalexceptions", "torso");

    private static BiologicalState hurtBody() {
        BiologicalState state = new BiologicalState(
            BodyPlanRegistry.standard()
                .require(BuiltinBodyPlans.HUMAN_ID),
            0L);
        state.addInjury(
            new BiologicalInjury(
                DeterministicId.derive("test", "cut"),
                BiologicalInjury.Kind.CUT,
                LEFT_ARM,
                0.35,
                1.4,
                0L));
        state.addInjury(
            new BiologicalInjury(
                DeterministicId.derive("test", "burn"),
                BiologicalInjury.Kind.BURN,
                TORSO,
                0.2,
                0.0,
                0L));
        state.region(RIGHT_LEG)
            .damageStructure(0.85);
        state.addExposureProtection(new ExposureProtection(ExposureRoute.INHALATION, 0.75));
        BiologicalEvents.applyCombustionSmoke(state, 40.0, 0L);
        return state;
    }

    @Test
    void capturesEveryRegionInjuryAndExposure() {
        BodyStatusSnapshot body = BodyStatusSnapshot.from(hurtBody());

        assertEquals(
            6,
            body.regions()
                .size(),
            "all six human regions");
        assertTrue(
            body.region("naturalexceptions:right_leg")
                .fractured(),
            "85% structural damage is a break");
        assertEquals(
            0.15F,
            body.region("naturalexceptions:right_leg")
                .integrity(),
            1.0e-4F);
        assertEquals(0.85F, body.regionHarm("naturalexceptions:right_leg"), 1.0e-4F);
        assertEquals(0.35F, body.regionHarm("naturalexceptions:left_arm"), 1.0e-4F, "injury severity counts as harm");
        assertEquals(0.0F, body.regionHarm("naturalexceptions:head"), 0.0F);
        assertTrue(body.regionBleeding("naturalexceptions:left_arm"));
        assertTrue(body.regionBurned("naturalexceptions:torso"));
        assertFalse(body.regionBleeding("naturalexceptions:torso"));

        assertEquals(
            2,
            body.injuries()
                .size());
        assertEquals(
            1,
            body.exposures()
                .size());
        BodyStatusSnapshot.Exposure smoke = body.exposures()
            .get(0);
        assertEquals("naturalexceptions:combustion_smoke", smoke.material());
        assertEquals(ExposureRoute.INHALATION, smoke.route());
        assertTrue(smoke.dose() > 0.0);
        assertEquals(0.75F, body.inhalationProtection(), 1.0e-6F);
        assertTrue(body.movement() < 1.0F, "a broken leg slows you");
    }

    @Test
    void roundTripsThroughTheNetworkEncoding() {
        BodyStatusSnapshot sent = BodyStatusSnapshot.from(hurtBody());
        ByteBuf buf = Unpooled.buffer();
        sent.write(buf);
        BodyStatusSnapshot received = BodyStatusSnapshot.read(buf);

        assertEquals(0, buf.readableBytes(), "reads exactly what was written");
        assertEquals(sent.fingerprint(), received.fingerprint());
        assertEquals(sent.condition(), received.condition(), 0.0F);
        assertEquals(
            sent.injuries()
                .size(),
            received.injuries()
                .size());
        assertEquals(
            BiologicalInjury.Kind.CUT,
            received.injuries()
                .get(0)
                .kind());
        assertEquals(
            1.4F,
            received.injuries()
                .get(0)
                .bleeding(),
            1.0e-4F);
        assertEquals(
            sent.exposures()
                .get(0)
                .dose(),
            received.exposures()
                .get(0)
                .dose(),
            0.0);
        assertTrue(
            received.region("naturalexceptions:right_leg")
                .fractured());
    }

    @Test
    void fingerprintChangesWithTheBodyAndBoundsLongLists() {
        BiologicalState state = hurtBody();
        long before = BodyStatusSnapshot.from(state)
            .fingerprint();
        state.region(LEFT_ARM)
            .damageStructure(0.1);
        assertNotEquals(
            before,
            BodyStatusSnapshot.from(state)
                .fingerprint());

        List<BodyStatusSnapshot.Injury> many = new ArrayList<>();
        for (int i = 0; i < 100; i++) many.add(
            new BodyStatusSnapshot.Injury(
                BiologicalInjury.Kind.CUT,
                "naturalexceptions:torso",
                0.1F,
                0.0F,
                0.0F,
                false));
        BodyStatusSnapshot big = new BodyStatusSnapshot(
            1,
            1,
            1,
            true,
            1,
            1,
            1,
            1,
            1,
            1,
            0,
            Collections.<BodyStatusSnapshot.Region>emptyList(),
            many,
            null);
        assertEquals(
            BodyStatusSnapshot.MAX_INJURIES,
            big.injuries()
                .size());
        ByteBuf buf = Unpooled.buffer();
        big.write(buf);
        assertEquals(
            BodyStatusSnapshot.MAX_INJURIES,
            BodyStatusSnapshot.read(buf)
                .injuries()
                .size());
    }

    @Test
    void wordsReadNaturally() {
        assertEquals("Left Arm", BodyStatusSnapshot.label("naturalexceptions:left_arm"));
        assertEquals("Combustion Smoke", BodyStatusSnapshot.label("naturalexceptions:combustion_smoke"));
        assertEquals(
            "Cut 35% \u00b7 bleeding 1.4 ml/s",
            BodyStatusSnapshot.injuryLine(
                new BodyStatusSnapshot.Injury(BiologicalInjury.Kind.CUT, "x:left_arm", 0.35F, 1.4F, 0.0F, false)));
        assertEquals(
            "Bone damage 60% \u00b7 splinted",
            BodyStatusSnapshot.injuryLine(
                new BodyStatusSnapshot.Injury(
                    BiologicalInjury.Kind.STRUCTURAL,
                    "x:right_leg",
                    0.6F,
                    0.0F,
                    0.5F,
                    false)));
        assertEquals(
            "inhaled \u00b7 dose 500 \u00b7 -5% condition",
            BodyStatusSnapshot
                .exposureDetail(new BodyStatusSnapshot.Exposure("x:smoke", ExposureRoute.INHALATION, 500.0)));
        assertEquals(
            "skin contact \u00b7 dose 0.20 \u00b7 no harm yet",
            BodyStatusSnapshot.exposureDetail(new BodyStatusSnapshot.Exposure("x:lead", ExposureRoute.CONTACT, 0.2)));
    }

    @Test
    void syncSendsOnChangeAndPeriodically() {
        assertTrue(BodyStatusSync.shouldSend(1L, 100L, 2L, 101L), "changed");
        assertFalse(BodyStatusSync.shouldSend(1L, 100L, 1L, 120L), "unchanged and recent");
        assertTrue(BodyStatusSync.shouldSend(1L, 100L, 1L, 100L + BodyStatusSync.REFRESH_TICKS), "refresh");
        assertTrue(BodyStatusSync.shouldSend(1L, 500L, 1L, 20L), "world time went back");
    }
}
