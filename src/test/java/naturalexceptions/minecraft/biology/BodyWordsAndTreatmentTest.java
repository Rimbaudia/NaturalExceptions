package naturalexceptions.minecraft.biology;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import naturalexceptions.core.biology.BiologicalInjury;
import naturalexceptions.core.biology.BiologicalState;
import naturalexceptions.core.biology.BiologicalTreatment;
import naturalexceptions.core.biology.BodyPlanRegistry;
import naturalexceptions.core.biology.BuiltinBodyPlans;
import naturalexceptions.core.biology.ExposureRoute;
import naturalexceptions.core.id.DeterministicId;
import naturalexceptions.core.id.ResourceId;

final class BodyWordsAndTreatmentTest {

    private static final ResourceId LEFT_ARM = ResourceId.of("naturalexceptions", "left_arm");
    private static final ResourceId RIGHT_LEG = ResourceId.of("naturalexceptions", "right_leg");

    private static BiologicalState body() {
        return new BiologicalState(
            BodyPlanRegistry.standard()
                .require(BuiltinBodyPlans.HUMAN_ID),
            0L);
    }

    private static BiologicalInjury injury(String name, BiologicalInjury.Kind kind, ResourceId region, double severity,
        double bleeding) {
        return new BiologicalInjury(DeterministicId.derive("words", name), kind, region, severity, bleeding, 0L);
    }

    @Test
    void aBandageTreatsTheWorstUntreatedInjuryWhereItIsDropped() {
        BiologicalState state = body();
        BiologicalInjury graze = injury("graze", BiologicalInjury.Kind.CUT, LEFT_ARM, 0.1, 0.0);
        BiologicalInjury deep = injury("deep", BiologicalInjury.Kind.CUT, LEFT_ARM, 0.6, 2.0);
        BiologicalInjury leg = injury("leg", BiologicalInjury.Kind.STRUCTURAL, RIGHT_LEG, 0.9, 0.0);
        state.addInjury(graze);
        state.addInjury(deep);
        state.addInjury(leg);

        assertSame(deep, BodyTreatment.pick(state, "naturalexceptions:left_arm"));
        assertSame(leg, BodyTreatment.pick(state, "naturalexceptions:right_leg"));
        assertNull(BodyTreatment.pick(state, "naturalexceptions:head"), "nothing on the head");

        state.treatInjury(deep.id(), BodyTreatment.treatmentFor(deep.kind()));
        assertSame(graze, BodyTreatment.pick(state, "naturalexceptions:left_arm"), "treated wounds are skipped");
        assertEquals(BiologicalTreatment.SPLINT, BodyTreatment.treatmentFor(BiologicalInjury.Kind.STRUCTURAL));
        assertEquals(BiologicalTreatment.DRESS, BodyTreatment.treatmentFor(BiologicalInjury.Kind.BURN));

        BodyStatusSnapshot snapshot = BodyStatusSnapshot.from(state);
        assertTrue(BodyTreatment.treatable(snapshot, "naturalexceptions:left_arm"), "the graze is still open");
        assertFalse(BodyTreatment.treatable(snapshot, "naturalexceptions:head"));
    }

    @Test
    void injuriesReadAsPlainWords() {
        BiologicalState state = body();
        state.addInjury(injury("cut", BiologicalInjury.Kind.CUT, LEFT_ARM, 0.6, 2.0));
        state.addInjury(injury("burn", BiologicalInjury.Kind.BURN, LEFT_ARM, 0.3, 0.0));
        state.addInjury(injury("bone", BiologicalInjury.Kind.STRUCTURAL, RIGHT_LEG, 0.7, 0.0));
        BodyStatusSnapshot snapshot = BodyStatusSnapshot.from(state);

        assertEquals(
            "Deep cut on the left arm, bleeding",
            BodyWords.injury(
                snapshot,
                snapshot.injuries()
                    .get(0)));
        assertEquals(
            "Burn on the left arm",
            BodyWords.injury(
                snapshot,
                snapshot.injuries()
                    .get(1)));
        assertEquals(
            "Broken right leg",
            BodyWords.injury(
                snapshot,
                snapshot.injuries()
                    .get(2)));

        BiologicalInjury bone = state.injuries()
            .get(2);
        state.treatInjury(bone.id(), BiologicalTreatment.SPLINT);
        BodyStatusSnapshot treated = BodyStatusSnapshot.from(state);
        assertEquals(
            "Broken right leg, splinted",
            BodyWords.injury(
                treated,
                treated.injuries()
                    .get(2)));
        assertEquals("Hurt", BodyWords.status(treated));
    }

    @Test
    void overallStateAndExposureWords() {
        BodyStatusSnapshot healthy = BodyStatusSnapshot.from(body());
        assertEquals("Healthy", BodyWords.status(healthy));
        assertEquals("Critical", BodyWords.status(snapshotWithCondition(0.2F)));
        assertEquals("Badly hurt", BodyWords.status(snapshotWithCondition(0.5F)));

        BodyStatusSnapshot.Exposure smoke = new BodyStatusSnapshot.Exposure(
            "naturalexceptions:combustion_smoke",
            ExposureRoute.INHALATION,
            200.0);
        assertEquals("Smoke in your lungs", BodyWords.exposure(smoke, "Combustion Smoke"));
        assertEquals("mild", BodyWords.level(smoke));
        BodyStatusSnapshot.Exposure lead = new BodyStatusSnapshot.Exposure(
            "naturalexceptions:lead",
            ExposureRoute.CONTACT,
            5000.0);
        assertEquals("Lead on your skin", BodyWords.exposure(lead, "Lead"));
        assertEquals("severe", BodyWords.level(lead));
        assertEquals(
            "Swallowed lead",
            BodyWords.exposure(new BodyStatusSnapshot.Exposure("x:lead", ExposureRoute.INGESTION, 1.0), "Lead"));
    }

    @Test
    void repeatedBurnsReadAsOneLineAndOneDressingCoversThem() {
        BiologicalState state = body();
        for (int i = 0; i < 4; i++)
            state.addInjury(injury("burn" + i, BiologicalInjury.Kind.BURN, LEFT_ARM, 0.5 + i * 0.1, 0.0));
        state.addInjury(injury("cut", BiologicalInjury.Kind.CUT, LEFT_ARM, 0.3, 1.0));
        BodyStatusSnapshot snapshot = BodyStatusSnapshot.from(state);
        assertEquals(
            5,
            snapshot.injuries()
                .size());
        assertEquals(
            2,
            snapshot.groupedInjuries()
                .size(),
            "four burns and a cut");
        BodyStatusSnapshot.Injury burns = snapshot.groupedInjuries()
            .get(0);
        assertEquals(4, burns.count());
        assertEquals(0.8F, burns.severity(), 1.0e-4F, "the worst of them");
        assertEquals("Bad burns on the left arm", BodyWords.injury(snapshot, burns));

        assertEquals(4, BodyTreatment.treatGroup(state, "naturalexceptions:left_arm"), "the burns, not the cut");
        BodyStatusSnapshot after = BodyStatusSnapshot.from(state);
        assertEquals(
            "Bad burns on the left arm, dressed",
            BodyWords.injury(
                after,
                after.groupedInjuries()
                    .get(0)));
        assertEquals(
            "Cut on the left arm, bleeding",
            BodyWords.injury(
                after,
                after.groupedInjuries()
                    .get(1)));
        assertEquals(1, BodyTreatment.treatGroup(state, "naturalexceptions:left_arm"), "next bandage: the cut");
        assertEquals(0, BodyTreatment.treatGroup(state, "naturalexceptions:left_arm"), "nothing left to treat");
    }

    @Test
    void everyGuiTextureIsTheSizeMinecraftAssumes() throws Exception {
        java.io.File dir = new java.io.File("src/main/resources/assets/naturalexceptions/textures/gui");
        java.io.File[] files = dir.listFiles((d, name) -> name.endsWith(".png"));
        assertTrue(files != null && files.length > 0);
        for (java.io.File file : files) {
            java.awt.image.BufferedImage image = javax.imageio.ImageIO.read(file);
            // drawTexturedModalRect maps UVs as pixels / 256; any other size stretches or misplaces every sprite.
            assertEquals(256, image.getWidth(), file.getName());
            assertEquals(256, image.getHeight(), file.getName());
        }
    }

    private static BodyStatusSnapshot snapshotWithCondition(float condition) {
        return new BodyStatusSnapshot(
            condition,
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
            null,
            null);
    }
}
