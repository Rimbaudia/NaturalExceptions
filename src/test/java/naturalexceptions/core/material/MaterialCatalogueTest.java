package naturalexceptions.core.material;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import naturalexceptions.content.material.BuiltinMaterials;

final class MaterialCatalogueTest {

    @Test
    void copperHasStableIdentity() {
        assertEquals(
            "naturalexceptions:copper",
            BuiltinMaterials.COPPER.id()
                .toString());
    }

    @Test
    void catalogueReturnsSameDefinitionForStableId() {
        MaterialCatalogue catalogue = new MaterialCatalogue();
        catalogue.register(BuiltinMaterials.COPPER);
        assertSame(BuiltinMaterials.COPPER, catalogue.require(BuiltinMaterials.COPPER.id()));
    }

    @Test
    void duplicateStableIdIsRejected() {
        MaterialCatalogue catalogue = new MaterialCatalogue();
        catalogue.register(BuiltinMaterials.COPPER);
        assertThrows(IllegalStateException.class, () -> catalogue.register(BuiltinMaterials.COPPER));
    }
}
