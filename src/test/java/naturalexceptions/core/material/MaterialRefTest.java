package naturalexceptions.core.material;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import naturalexceptions.core.composition.CanonicalComposition;
import naturalexceptions.core.id.DeterministicId;
import naturalexceptions.core.id.ResourceId;

final class MaterialRefTest {

    private static final ResourceId CU = ResourceId.of("naturalexceptions", "copper");
    private static final ResourceId SN = ResourceId.of("naturalexceptions", "tin");

    @Test
    void authoredRefPropertiesAndRoundTrip() {
        MaterialRef ref = MaterialRef.authored(CU);

        assertTrue(ref.isAuthored());
        assertFalse(ref.isGenerated());
        assertEquals(MaterialRefKind.AUTHORED, ref.kind());
        assertEquals(CU, ref.authoredId());
        assertEquals("mat:authored:naturalexceptions:copper", ref.asString());
        assertEquals("mat:authored:naturalexceptions:copper", ref.toString());

        MaterialRef parsed = MaterialRef.parse("mat:authored:naturalexceptions:copper");
        assertEquals(ref, parsed);
        assertEquals(ref.hashCode(), parsed.hashCode());
    }

    @Test
    void generatedRefPropertiesAndRoundTrip() {
        DeterministicId detId = DeterministicId.derive("test-material-input");
        MaterialRef ref = MaterialRef.generated(detId);

        assertTrue(ref.isGenerated());
        assertFalse(ref.isAuthored());
        assertEquals(MaterialRefKind.GENERATED, ref.kind());
        assertEquals(detId, ref.generatedId());
        assertEquals("mat:generated:" + detId.hex(), ref.asString());

        MaterialRef parsed = MaterialRef.parse(ref.asString());
        assertEquals(ref, parsed);
        assertEquals(ref.hashCode(), parsed.hashCode());
    }

    @Test
    void generatedFromCompositionIsDeterministic() {
        CanonicalComposition comp1 = CanonicalComposition.builder()
            .add(CU, 70)
            .add(SN, 30)
            .build();

        CanonicalComposition comp2 = CanonicalComposition.builder()
            .add(SN, 30)
            .add(CU, 70)
            .build();

        MaterialRef ref1 = MaterialRef.generatedFrom(comp1);
        MaterialRef ref2 = MaterialRef.generatedFrom(comp2);

        assertEquals(ref1, ref2);
        assertEquals(ref1.hashCode(), ref2.hashCode());
        assertEquals(ref1.asString(), ref2.asString());
    }

    @Test
    void authoredAndGeneratedNeverCollide() {
        MaterialRef authored = MaterialRef.authored(CU);
        DeterministicId detId = DeterministicId.derive("naturalexceptions", "copper");
        MaterialRef generated = MaterialRef.generated(detId);

        assertNotEquals(authored, generated);
        assertNotEquals(authored.hashCode(), generated.hashCode());
        assertNotEquals(authored.asString(), generated.asString());
    }

    @Test
    void orderingSortsAuthoredBeforeGeneratedAndPreservesDeterministicOrder() {
        MaterialRef authA = MaterialRef.authored("a", "mat");
        MaterialRef authB = MaterialRef.authored("b", "mat");
        MaterialRef gen1 = MaterialRef.generated(DeterministicId.derive("1"));
        MaterialRef gen2 = MaterialRef.generated(DeterministicId.derive("2"));

        assertTrue(authA.compareTo(authB) < 0);
        assertTrue(authB.compareTo(gen1) < 0);
        assertTrue(gen1.compareTo(gen2) != 0);
    }

    @Test
    void rejectsMalformedStringInParsing() {
        assertThrows(IllegalArgumentException.class, () -> MaterialRef.parse(null));
        assertThrows(IllegalArgumentException.class, () -> MaterialRef.parse(""));
        assertThrows(IllegalArgumentException.class, () -> MaterialRef.parse("copper"));
        assertThrows(IllegalArgumentException.class, () -> MaterialRef.parse("mat:unknown:copper"));
        assertThrows(IllegalArgumentException.class, () -> MaterialRef.parse("mat:authored:"));
        assertThrows(IllegalArgumentException.class, () -> MaterialRef.parse("mat:generated:invalidhex"));
    }

    @Test
    void rejectsNullConstruction() {
        assertThrows(IllegalArgumentException.class, () -> MaterialRef.authored((ResourceId) null));
        assertThrows(IllegalArgumentException.class, () -> MaterialRef.generated((DeterministicId) null));
        assertThrows(IllegalArgumentException.class, () -> MaterialRef.generatedFrom(null));
    }
}
