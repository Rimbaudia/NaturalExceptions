package naturalexceptions.minecraft.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import naturalexceptions.content.material.BuiltinMaterials;

final class MaterialBindingRegistryTest {

    @Test
    void representationCanBindToCanonicalMaterial() {
        Object representation = new Object();
        MaterialBindingRegistry registry = new MaterialBindingRegistry();
        registry.bind(representation, BuiltinMaterials.COPPER.id());
        assertEquals(BuiltinMaterials.COPPER.id(), registry.require(representation));
    }

    @Test
    void duplicateBindingIsRejected() {
        Object representation = new Object();
        MaterialBindingRegistry registry = new MaterialBindingRegistry();
        registry.bind(representation, BuiltinMaterials.COPPER.id());
        assertThrows(IllegalStateException.class, () -> registry.bind(representation, BuiltinMaterials.COPPER.id()));
    }
}
