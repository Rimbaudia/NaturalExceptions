package naturalexceptions.minecraft.item;

import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.List;
import naturalexceptions.core.composition.CanonicalComposition;
import naturalexceptions.core.id.ResourceId;
import naturalexceptions.core.material.CanonicalMaterialKey;
import naturalexceptions.core.material.MaterialRef;
import naturalexceptions.minecraft.testutil.BootstrapAllTestsExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import net.minecraft.item.ItemStack;

@ExtendWith(BootstrapAllTestsExtension.class)
final class MaterialSampleKnowledgeDisplayTest {
    @Test void actualGeneratedMaterialCarrierGetsUsefulCoarseDisplayWithoutCanonicalLeak() {
        CanonicalComposition mixture = CanonicalComposition.builder()
            .add(ResourceId.of("naturalexceptions", "copper"), 700)
            .add(ResourceId.of("naturalexceptions", "tin"), 300)
            .build();
        CanonicalMaterialKey key = CanonicalMaterialKey.of(mixture);
        MaterialRef ref = MaterialRef.generatedFrom(key);
        ItemStack stack = ItemMaterialSample.createStack(ref, key);
        ItemMaterialSample item = (ItemMaterialSample) stack.getItem();

        List<String> tooltip = new ArrayList<>();
        item.addInformation(stack, null, tooltip, false);

        assertEquals("Material Sample", item.getItemStackDisplayName(stack));
        String joined = String.join("\n", tooltip);
        assertTrue(joined.contains("Reddish") || joined.contains("grey") || joined.contains("dark"), joined);
        assertFalse(joined.contains(mixture.canonicalString()), joined);
        assertFalse(joined.contains(ref.asString()), joined);
        assertFalse(joined.contains("Cu70"), joined);
    }

    @Test void fieldAssayDoesNotSubstituteCopperPropertiesForUnresolvedOrIncompleteSamples() {
        ItemStack unresolved = ItemMaterialSample.createStack(
            MaterialRef.authored(ResourceId.of("futureaddon", "unknown_metal")));
        assertNull(ItemMaterialSample.propertiesForKnowledge(unresolved));

        ItemStack incomplete = new ItemStack(new ItemMaterialSample());
        assertNull(ItemMaterialSample.propertiesForKnowledge(incomplete));
    }

    @Test void unknownCyanMaterialReceivesUsefulColorDescriptionWithoutCanonicalReveal() {
        naturalexceptions.content.BuiltinContent.register();
        naturalexceptions.core.material.MaterialRef stellar = naturalexceptions.content.material.BuiltinMaterials
            .STELLAR_MATTER.ref();
        ItemStack stack = ItemMaterialSample.createStack(stellar,
            naturalexceptions.content.material.BuiltinMaterials.STELLAR_MATTER_COMPOSITION);
        List<String> tooltip = new ArrayList<>();

        ((ItemMaterialSample) stack.getItem()).addInformation(stack, null, tooltip, false);

        String joined = String.join("\n", tooltip).toLowerCase(java.util.Locale.ROOT);
        assertTrue(joined.contains("cyan-blue"), joined);
        assertFalse(joined.contains("stellar_matter"), joined);
        assertFalse(joined.contains("stellar matter"), joined);
    }
}
