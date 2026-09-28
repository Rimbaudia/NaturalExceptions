package naturalexceptions.core.knowledge;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import naturalexceptions.core.material.MaterialRef;
import naturalexceptions.core.id.ResourceId;

class PlayerKnowledgeTest {

    private final MaterialRef unknown = MaterialRef.generatedFrom(
        naturalexceptions.core.composition.CanonicalComposition.of(ResourceId.of("naturalexceptions", "copper")));

    @Test
    void materialSubjectUsesStableMaterialIdentity() {
        KnowledgeSubjectRef subject = KnowledgeSubjectRef.material(unknown);
        assertEquals(subject, KnowledgeSubjectRef.parse(subject.asString()));
        assertTrue(subject.asString().contains(unknown.asString()));
    }

    @Test
    void aliasAndKnowledgeDoNotChangeCanonicalMaterialIdentityOrOtherPlayer() {
        PlayerKnowledge alice = new PlayerKnowledge();
        PlayerKnowledge bob = new PlayerKnowledge();
        KnowledgeSubjectRef subject = KnowledgeSubjectRef.material(unknown);

        alice.setAlias(subject, "Jeffium");

        assertEquals("Jeffium", alice.aliasFor(subject));
        assertNull(bob.aliasFor(subject));
        assertEquals(unknown, MaterialRef.parse(subject.identity()));
    }

    @Test
    void aliasesCannotInjectMinecraftFormattingOrLineBreaks() {
        PlayerKnowledge store = new PlayerKnowledge();
        KnowledgeSubjectRef subject = KnowledgeSubjectRef.material(unknown);
        assertThrows(IllegalArgumentException.class, () -> store.setAlias(subject, "\u00a7cHidden"));
        assertThrows(IllegalArgumentException.class, () -> store.setAlias(subject, "Two\nLines"));
    }

    @Test
    void evidenceAccumulatesAcrossDistinctSourcesAndDeduplicatesRepeats() {
        PlayerKnowledge store = new PlayerKnowledge();
        KnowledgeSubjectRef subject = KnowledgeSubjectRef.material(unknown);
        KnowledgeEvidence experiment = new KnowledgeEvidence(
            "phase",
            "Remains solid near a hot furnace",
            "inspection:solid",
            KnowledgeProvenance.EXPERIMENT,
            KnowledgePrecision.QUALITATIVE);
        KnowledgeEvidence book = new KnowledgeEvidence(
            "copper-bearing",
            "May contain copper-bearing matter",
            "book:starter_manual/copper",
            KnowledgeProvenance.BOOK,
            KnowledgePrecision.CLASSIFICATION);

        store.addEvidence(subject, experiment);
        store.addEvidence(subject, experiment);
        store.addEvidence(subject, book);

        assertEquals(2, store.evidenceFor(subject).size());
        assertEquals(KnowledgeProvenance.EXPERIMENT, store.evidenceFor(subject).get(0).provenance());
        assertEquals(KnowledgeProvenance.BOOK, store.evidenceFor(subject).get(1).provenance());
        assertEquals(KnowledgePrecision.CLASSIFICATION, store.bestPrecision(subject));
    }

    @Test
    void repeatedObservationGrowthIsBounded() {
        PlayerKnowledge store = new PlayerKnowledge();
        KnowledgeSubjectRef subject = KnowledgeSubjectRef.material(unknown);
        for (int i = 0; i < PlayerKnowledge.MAX_EVIDENCE_PER_SUBJECT + 20; i++) {
            store.addEvidence(subject, new KnowledgeEvidence(
                "observation-" + i,
                "Observation " + i,
                "experiment:" + i,
                KnowledgeProvenance.EXPERIMENT,
                KnowledgePrecision.QUALITATIVE));
        }
        assertEquals(PlayerKnowledge.MAX_EVIDENCE_PER_SUBJECT, store.evidenceFor(subject).size());
    }

    @Test
    void unknownDisplayUsesEarnedEvidenceWithoutExposingSubjectId() {
        PlayerKnowledge store = new PlayerKnowledge();
        KnowledgeSubjectRef subject = KnowledgeSubjectRef.material(unknown);
        assertEquals("Reddish material", KnowledgeDisplay.resolve(subject, store, "reddish", "solid"));
        store.addEvidence(subject, new KnowledgeEvidence(
            "appearance", "Reddish, metallic appearance", "inspection:appearance", KnowledgeProvenance.EXPERIMENT,
            KnowledgePrecision.QUALITATIVE));
        assertEquals("Reddish, metallic appearance", KnowledgeDisplay.resolve(subject, store, "reddish", "solid"));
        assertFalse(KnowledgeDisplay.resolve(subject, store, "reddish", "solid").contains(unknown.asString()));
    }

    @Test
    void instrumentEvidenceImprovesPrecisionWithoutAnyPhysicalCapabilityFlag() {
        PlayerKnowledge store = new PlayerKnowledge();
        KnowledgeSubjectRef subject = KnowledgeSubjectRef.material(unknown);
        store.addEvidence(subject, new KnowledgeEvidence(
            "density", "Feels unusually heavy", "inspection:weight", KnowledgeProvenance.EXPERIMENT,
            KnowledgePrecision.QUALITATIVE));
        store.addEvidence(subject, new KnowledgeEvidence(
            "density", "Density: 8.7-9.1 g/cm3", "instrument:field_assay", KnowledgeProvenance.INSTRUMENT,
            KnowledgePrecision.QUANTITATIVE));
        assertEquals(KnowledgePrecision.QUANTITATIVE, store.bestPrecision(subject));
        assertFalse(java.util.Arrays.stream(PlayerKnowledge.class.getDeclaredMethods())
            .anyMatch(method -> method.getName().toLowerCase().contains("capabilit")
                || method.getName().toLowerCase().contains("unlock")));
    }

    @Test
    void unknownDurableSubjectIdentityIsPreservedRatherThanRebound() {
        String futureSubject = "material|mat:future-addon:unknown_ore";
        KnowledgeSubjectRef parsed = KnowledgeSubjectRef.parse(futureSubject);
        assertEquals(futureSubject, parsed.asString());
        assertFalse(parsed.isResolvedMaterialRef());
    }
}
