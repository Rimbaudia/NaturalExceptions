package naturalexceptions.core.id;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

final class ResourceIdTest {

    @Test
    void parsesNamespacedId() {
        ResourceId id = ResourceId.parse("naturalexceptions:copper");
        assertEquals("naturalexceptions", id.namespace());
        assertEquals("copper", id.path());
        assertEquals("naturalexceptions:copper", id.toString());
    }

    @Test
    void supportsHierarchicalPathWithSlashes() {
        ResourceId id = ResourceId.parse("naturalexceptions:test/delayed_event");
        assertEquals("naturalexceptions", id.namespace());
        assertEquals("test/delayed_event", id.path());
        assertEquals("naturalexceptions:test/delayed_event", id.toString());
    }

    @Test
    void roundTripParsingPreservesIdentity() {
        ResourceId id = ResourceId.of("naturalexceptions", "deep_copper.ore-1");
        ResourceId parsed = ResourceId.parse(id.toString());
        assertEquals(id, parsed);
        assertEquals(id.hashCode(), parsed.hashCode());
    }

    @Test
    void rejectsMissingNamespaceOrPath() {
        assertThrows(IllegalArgumentException.class, () -> ResourceId.parse("copper"));
        assertThrows(IllegalArgumentException.class, () -> ResourceId.parse(":copper"));
        assertThrows(IllegalArgumentException.class, () -> ResourceId.parse("copper:"));
        assertThrows(IllegalArgumentException.class, () -> ResourceId.parse(null));
        assertThrows(IllegalArgumentException.class, () -> ResourceId.of(null, "copper"));
        assertThrows(IllegalArgumentException.class, () -> ResourceId.of("naturalexceptions", null));
    }

    @Test
    void rejectsMultipleColons() {
        assertThrows(IllegalArgumentException.class, () -> ResourceId.parse("a:b:c"));
    }

    @Test
    void rejectsSlashInNamespace() {
        assertThrows(IllegalArgumentException.class, () -> ResourceId.parse("natural/exceptions:copper"));
    }

    @Test
    void rejectsUppercaseAndWhitespaceAndSymbols() {
        assertThrows(IllegalArgumentException.class, () -> ResourceId.parse("NaturalExceptions:copper"));
        assertThrows(IllegalArgumentException.class, () -> ResourceId.parse("naturalexceptions:Copper"));
        assertThrows(IllegalArgumentException.class, () -> ResourceId.parse("naturalexceptions:copper ore"));
        assertThrows(IllegalArgumentException.class, () -> ResourceId.parse("natural exceptions:copper"));
        assertThrows(IllegalArgumentException.class, () -> ResourceId.parse("naturalexceptions:copper$block"));
    }

    @Test
    void supportsCollectionsAndHashing() {
        ResourceId id1 = ResourceId.of("naturalexceptions", "copper");
        ResourceId id2 = ResourceId.parse("naturalexceptions:copper");
        ResourceId id3 = ResourceId.of("naturalexceptions", "tin");

        assertEquals(id1, id2);
        assertNotEquals(id1, id3);

        Set<ResourceId> set = new HashSet<>();
        set.add(id1);
        set.add(id2);
        assertEquals(1, set.size());
        assertTrue(set.contains(id1));

        Map<ResourceId, String> map = new HashMap<>();
        map.put(id1, "red_metal");
        assertEquals("red_metal", map.get(id2));
    }

    @Test
    void implementsComparableWithDeterministicSorting() {
        ResourceId idA = ResourceId.of("a", "z");
        ResourceId idB = ResourceId.of("b", "a");
        ResourceId idC = ResourceId.of("b", "b");
        ResourceId idD = ResourceId.of("b", "c");

        List<ResourceId> list = new ArrayList<>();
        list.add(idD);
        list.add(idA);
        list.add(idC);
        list.add(idB);

        Collections.sort(list);

        assertEquals(idA, list.get(0));
        assertEquals(idB, list.get(1));
        assertEquals(idC, list.get(2));
        assertEquals(idD, list.get(3));
    }
}
