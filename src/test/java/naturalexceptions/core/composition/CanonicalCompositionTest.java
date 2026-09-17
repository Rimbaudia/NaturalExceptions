package naturalexceptions.core.composition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import naturalexceptions.core.id.ResourceId;

final class CanonicalCompositionTest {

    private static final ResourceId CU = ResourceId.of("naturalexceptions", "copper");
    private static final ResourceId SN = ResourceId.of("naturalexceptions", "tin");
    private static final ResourceId ZN = ResourceId.of("naturalexceptions", "zinc");
    private static final ResourceId PB = ResourceId.of("naturalexceptions", "lead");

    @Test
    void pureMaterialYieldsFull10000Parts() {
        CanonicalComposition comp = CanonicalComposition.builder()
            .add(CU, 100)
            .build();

        assertEquals(1, comp.constituentCount());
        assertEquals(10_000, comp.parts(CU));
        assertEquals(1.0, comp.fraction(CU));
        assertEquals("naturalexceptions:copper@10000", comp.canonicalString());
    }

    @Test
    void binaryMixtureQuantizesAccurately() {
        // 70% Cu, 30% Sn
        CanonicalComposition comp = CanonicalComposition.builder()
            .add(CU, 70)
            .add(SN, 30)
            .build();

        assertEquals(2, comp.constituentCount());
        assertEquals(7_000, comp.parts(CU));
        assertEquals(3_000, comp.parts(SN));
        assertEquals(0.7, comp.fraction(CU));
        assertEquals(0.3, comp.fraction(SN));
        assertEquals(10_000, comp.totalParts());
    }

    @Test
    void ternaryMixtureMatchesPromptSpecification() {
        // 72.4% A, 18.1% B, 9.5% C
        CanonicalComposition comp = CanonicalComposition.builder()
            .add(CU, 72.4)
            .add(SN, 18.1)
            .add(ZN, 9.5)
            .build();

        assertEquals(3, comp.constituentCount());
        assertEquals(7_240, comp.parts(CU));
        assertEquals(1_810, comp.parts(SN));
        assertEquals(950, comp.parts(ZN));
        assertEquals(10_000, comp.totalParts());
    }

    @Test
    void permutationInvarianceYieldsIdenticalCanonicalOutput() {
        CanonicalComposition comp1 = CanonicalComposition.builder()
            .add(CU, 50)
            .add(SN, 30)
            .add(ZN, 20)
            .build();

        CanonicalComposition comp2 = CanonicalComposition.builder()
            .add(ZN, 20)
            .add(CU, 50)
            .add(SN, 30)
            .build();

        assertEquals(comp1, comp2);
        assertEquals(comp1.hashCode(), comp2.hashCode());
        assertEquals(comp1.canonicalString(), comp2.canonicalString());
        assertEquals(0, comp1.compareTo(comp2));
    }

    @Test
    void duplicateConstituentsFoldDeterministically() {
        // Adding Cu as 40 + 30 = 70, Sn as 30
        CanonicalComposition comp1 = CanonicalComposition.builder()
            .add(CU, 40)
            .add(SN, 30)
            .add(CU, 30)
            .build();

        CanonicalComposition comp2 = CanonicalComposition.builder()
            .add(CU, 70)
            .add(SN, 30)
            .build();

        assertEquals(comp2, comp1);
        assertEquals(7_000, comp1.parts(CU));
        assertEquals(3_000, comp1.parts(SN));
    }

    @Test
    void fractionalValuesRequiringRoundingSumToExact10000() {
        // 1/3 each: 33.333...%
        CanonicalComposition comp = CanonicalComposition.builder()
            .add(CU, 1)
            .add(SN, 1)
            .add(ZN, 1)
            .build();

        assertEquals(10_000, comp.totalParts());
        // 10000 / 3 = 3333 r 1
        // CU, SN, ZN remainder is 1 each.
        // Remainder units to allocate = 10000 - 3333*3 = 1 unit.
        // Deterministic tie-breaking assigns +1 to alphabetically first constituent (CU).
        assertEquals(3_334, comp.parts(CU));
        assertEquals(3_333, comp.parts(SN));
        assertEquals(3_333, comp.parts(ZN));
    }

    @Test
    void exactTieCaseUsesDeterministicTieBreaking() {
        // Two constituents with equal remainder: 5000.5 -> Cu and Sn both have remainder 1/2
        // Total 10000 / 2 = 5000 exact
        // Now consider 4 equal parts: 10000 / 4 = 2500 exact
        // Consider 6 equal parts: 10000 / 6 = 1666 r 4 (4 remainder units for 6 constituents)
        ResourceId a = ResourceId.of("naturalexceptions", "a");
        ResourceId b = ResourceId.of("naturalexceptions", "b");
        ResourceId c = ResourceId.of("naturalexceptions", "c");
        ResourceId d = ResourceId.of("naturalexceptions", "d");
        ResourceId e = ResourceId.of("naturalexceptions", "e");
        ResourceId f = ResourceId.of("naturalexceptions", "f");

        CanonicalComposition comp = CanonicalComposition.builder()
            .add(f, 10)
            .add(e, 10)
            .add(d, 10)
            .add(c, 10)
            .add(b, 10)
            .add(a, 10)
            .build();

        assertEquals(10_000, comp.totalParts());
        // 10000 / 6 = 1666 base. 4 remainder units distributed to a, b, c, d.
        assertEquals(1667, comp.parts(a));
        assertEquals(1667, comp.parts(b));
        assertEquals(1667, comp.parts(c));
        assertEquals(1667, comp.parts(d));
        assertEquals(1666, comp.parts(e));
        assertEquals(1666, comp.parts(f));
    }

    @Test
    void subQuantizationTraceConstituentDisappears() {
        // 100000 units of Cu, and 0.0001 units of Sn (far below 0.01% resolution)
        // 0.0001 / 100000.0001 * 10000 = 0.00001 parts -> 0 base, remainder tiny
        CanonicalComposition comp = CanonicalComposition.builder()
            .add(CU, 1_000_000)
            .add(SN, 1) // 1 in a million = 0.01 units. Cu gets remainder.
            .build();

        // 1 / 1000001 * 10000 = 0.009999 -> rounds to 0
        assertEquals(1, comp.constituentCount());
        assertTrue(comp.contains(CU));
        assertFalse(comp.contains(SN));
        assertEquals(10_000, comp.parts(CU));
    }

    @Test
    void rejectsInvalidNegativeOrEmpty() {
        assertThrows(
            IllegalArgumentException.class,
            () -> CanonicalComposition.builder()
                .add(CU, -5));
        assertThrows(
            IllegalArgumentException.class,
            () -> CanonicalComposition.builder()
                .add(null, 10));
        assertThrows(
            IllegalArgumentException.class,
            () -> CanonicalComposition.builder()
                .build());
        assertThrows(
            IllegalArgumentException.class,
            () -> CanonicalComposition.builder()
                .add(CU, 0)
                .build());
    }

    @Test
    void roundTripParsingMatchesOriginal() {
        CanonicalComposition comp = CanonicalComposition.builder()
            .add(CU, 72.4)
            .add(SN, 18.1)
            .add(ZN, 9.5)
            .build();

        String canonicalStr = comp.canonicalString();
        CanonicalComposition parsed = CanonicalComposition.parse(canonicalStr);

        assertEquals(comp, parsed);
        assertEquals(comp.hashCode(), parsed.hashCode());
        assertEquals(canonicalStr, parsed.canonicalString());
    }

    @Test
    void rejectsMalformedStringInParsing() {
        assertThrows(IllegalArgumentException.class, () -> CanonicalComposition.parse(null));
        assertThrows(IllegalArgumentException.class, () -> CanonicalComposition.parse(""));
        assertThrows(IllegalArgumentException.class, () -> CanonicalComposition.parse("invalid_format"));
        assertThrows(IllegalArgumentException.class, () -> CanonicalComposition.parse("naturalexceptions:copper@9000")); // sum
                                                                                                                         // !=
                                                                                                                         // 10000
        assertThrows(IllegalArgumentException.class, () -> CanonicalComposition.parse("naturalexceptions:copper@-100"));
    }
}
