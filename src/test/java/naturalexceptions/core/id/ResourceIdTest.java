package naturalexceptions.core.id;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
    void rejectsMissingNamespace() {
        assertThrows(IllegalArgumentException.class, () -> ResourceId.parse("copper"));
    }

    @Test
    void rejectsUppercaseAndWhitespace() {
        assertThrows(IllegalArgumentException.class, () -> ResourceId.parse("NaturalExceptions:copper"));
        assertThrows(IllegalArgumentException.class, () -> ResourceId.parse("naturalexceptions:copper ore"));
    }
}
