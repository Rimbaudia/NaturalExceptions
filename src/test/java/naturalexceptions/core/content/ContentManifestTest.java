package naturalexceptions.core.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import naturalexceptions.content.BuiltinContent;
import naturalexceptions.core.id.ResourceId;

final class ContentManifestTest {

    @Test
    void builtinManifestContainsCopper() {
        ContentManifest manifest = BuiltinContent.manifest();
        ResourceId copperId = ResourceId.of("naturalexceptions", "copper");

        assertTrue(manifest.contains(copperId));
        ContentManifestEntry entry = manifest.get(copperId);
        assertEquals(copperId, entry.id());
        assertEquals("material", entry.type());
        assertEquals(1L, entry.version());
    }

    @Test
    void rejectsDuplicateSemanticIds() {
        ContentManifest.Builder builder = ContentManifest.builder(1, "N0.01.0-test");
        ResourceId id = ResourceId.of("naturalexceptions", "copper");
        builder.add(id, "material", 1L);

        assertThrows(IllegalArgumentException.class, () -> builder.add(id, "ore", 1L));
    }

    @Test
    void iteratesInDeterministicSortedOrderRegardlessOfInsertionOrder() {
        ResourceId idZ = ResourceId.of("naturalexceptions", "zinc");
        ResourceId idA = ResourceId.of("naturalexceptions", "aluminum");
        ResourceId idC = ResourceId.of("naturalexceptions", "copper");

        // Insert in reverse/random order
        ContentManifest manifest = ContentManifest.builder(1, "test-build")
            .add(idZ, "material", 1L)
            .add(idA, "material", 1L)
            .add(idC, "material", 1L)
            .build();

        List<ResourceId> ordered = new ArrayList<>();
        for (ContentManifestEntry entry : manifest) {
            ordered.add(entry.id());
        }

        assertEquals(3, ordered.size());
        assertEquals(idA, ordered.get(0));
        assertEquals(idC, ordered.get(1));
        assertEquals(idZ, ordered.get(2));
    }

    @Test
    void distinguishesManifestFormatVersionAndContentVersion() {
        ContentManifest manifest = ContentManifest.builder(1, "N0.01.0-custom")
            .build();
        assertEquals(1, manifest.formatVersion());
        assertEquals("N0.01.0-custom", manifest.buildVersion());
        assertNotEquals(String.valueOf(manifest.formatVersion()), manifest.buildVersion());
    }

    @Test
    void rejectsInvalidVersions() {
        assertThrows(IllegalArgumentException.class, () -> ContentManifest.builder(0, "ver"));
        assertThrows(IllegalArgumentException.class, () -> ContentManifest.builder(-1, "ver"));
        assertThrows(IllegalArgumentException.class, () -> ContentManifest.builder(1, null));
        assertThrows(IllegalArgumentException.class, () -> ContentManifest.builder(1, "  "));
    }
}
