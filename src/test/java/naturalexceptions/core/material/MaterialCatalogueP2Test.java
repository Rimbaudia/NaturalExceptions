package naturalexceptions.core.material;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import naturalexceptions.content.material.BuiltinMaterials;
import naturalexceptions.core.composition.CanonicalComposition;
import naturalexceptions.core.id.ResourceId;

final class MaterialCatalogueP2Test {

    private static final ResourceId CU = ResourceId.of("naturalexceptions", "copper");
    private static final ResourceId SN = ResourceId.of("naturalexceptions", "tin");
    private static final ResourceId ZN = ResourceId.of("naturalexceptions", "zinc");

    @Test
    void resolvesAuthoredMaterialsByMaterialRef() {
        MaterialCatalogue catalogue = new MaterialCatalogue();
        catalogue.register(BuiltinMaterials.COPPER);

        MaterialRef copperRef = MaterialRef.authored(CU);
        assertTrue(catalogue.contains(copperRef));
        MaterialDefinition def = catalogue.getAuthored(copperRef);
        assertNotNull(def);
        assertEquals(BuiltinMaterials.COPPER.id(), def.id());
    }

    @Test
    void independentlyConstructedIdenticalCompositionsResolveToIdenticalIdentity() {
        MaterialCatalogue catalogue = new MaterialCatalogue();

        // Path A: add Cu 70, then Sn 30
        CanonicalComposition compA = CanonicalComposition.builder()
            .add(CU, 70)
            .add(SN, 30)
            .build();

        // Path B: add Sn 30, then Cu 40 + 30
        CanonicalComposition compB = CanonicalComposition.builder()
            .add(SN, 30)
            .add(CU, 40)
            .add(CU, 30)
            .build();

        GeneratedMaterial matA = catalogue.getOrRegisterGenerated(compA);
        GeneratedMaterial matB = catalogue.getOrRegisterGenerated(compB);

        assertSame(matA, matB, "Must resolve to identical interned object");
        assertEquals(matA.ref(), matB.ref());
        assertEquals(matA.composition(), matB.composition());
        assertEquals(1, catalogue.generatedCount());
    }

    @Test
    void creationOrderDoesNotAffectIdentityOrAssignment() {
        // Catalogue 1 registers Alloy X then Alloy Y
        CanonicalComposition alloyX = CanonicalComposition.builder()
            .add(CU, 80)
            .add(SN, 20)
            .build();
        CanonicalComposition alloyY = CanonicalComposition.builder()
            .add(CU, 60)
            .add(ZN, 40)
            .build();

        MaterialCatalogue cat1 = new MaterialCatalogue();
        GeneratedMaterial g1X = cat1.getOrRegisterGenerated(alloyX);
        GeneratedMaterial g1Y = cat1.getOrRegisterGenerated(alloyY);

        // Catalogue 2 registers Alloy Y then Alloy X (reversed order)
        MaterialCatalogue cat2 = new MaterialCatalogue();
        GeneratedMaterial g2Y = cat2.getOrRegisterGenerated(alloyY);
        GeneratedMaterial g2X = cat2.getOrRegisterGenerated(alloyX);

        assertEquals(g1X.ref(), g2X.ref(), "Alloy X must have identical identity regardless of registration order");
        assertEquals(g1Y.ref(), g2Y.ref(), "Alloy Y must have identical identity regardless of registration order");
        assertNotEquals(g1X.ref(), g1Y.ref(), "Distinct alloys must have distinct identities");
    }

    @Test
    void reconstructionInFreshInstanceYieldsExactSameIdentity() {
        CanonicalComposition comp = CanonicalComposition.builder()
            .add(CU, 72.4)
            .add(SN, 18.1)
            .add(ZN, 9.5)
            .build();

        MaterialCatalogue cat1 = new MaterialCatalogue();
        GeneratedMaterial original = cat1.getOrRegisterGenerated(comp);
        MaterialRef originalRef = original.ref();

        // Fresh catalogue instance simulating server reload
        MaterialCatalogue cat2 = new MaterialCatalogue();
        GeneratedMaterial reconstructed = cat2.getOrRegisterGenerated(comp);

        assertEquals(originalRef, reconstructed.ref());
        assertEquals(original.composition(), reconstructed.composition());
    }

    @Test
    void authoredAndGeneratedIdentitiesDoNotCollide() {
        MaterialCatalogue catalogue = new MaterialCatalogue();
        catalogue.register(BuiltinMaterials.COPPER);

        MaterialRef authoredRef = MaterialRef.authored(CU);

        // A 100% pure Cu generated composition
        CanonicalComposition pureCu = CanonicalComposition.of(CU);
        GeneratedMaterial generatedCu = catalogue.getOrRegisterGenerated(pureCu);

        assertNotEquals(authoredRef, generatedCu.ref());
        assertTrue(catalogue.contains(authoredRef));
        assertTrue(catalogue.contains(generatedCu.ref()));
        assertNotNull(catalogue.getAuthored(authoredRef));
        assertNotNull(catalogue.getGenerated(generatedCu.ref()));
        assertFalse(
            generatedCu.ref()
                .isAuthored());
        assertTrue(
            generatedCu.ref()
                .isGenerated());
    }
}
