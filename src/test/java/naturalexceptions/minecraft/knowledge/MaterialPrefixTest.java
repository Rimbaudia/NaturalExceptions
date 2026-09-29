package naturalexceptions.minecraft.knowledge;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import naturalexceptions.content.BuiltinContent;
import naturalexceptions.content.material.BuiltinMaterials;
import naturalexceptions.core.material.CanonicalMaterialKey;

/** Tool and part names start with the material as this player knows it. */
final class MaterialPrefixTest {

    @BeforeAll
    static void init() {
        BuiltinContent.register();
    }

    @Test
    void woodIsCommonKnowledgeAndUnstudiedMetalGoesByItsLook() {
        assertEquals(
            "Wood",
            KnowledgePresentation.materialPrefix(
                null,
                BuiltinMaterials.WOOD.ref(),
                CanonicalMaterialKey.of(BuiltinMaterials.WOOD_COMPOSITION)));
        assertEquals(
            "Reddish",
            KnowledgePresentation.materialPrefix(
                null,
                BuiltinMaterials.COPPER.ref(),
                CanonicalMaterialKey.of(BuiltinMaterials.COPPER_COMPOSITION)),
            "no player, no knowledge: named by appearance, never by identity");
    }
}
