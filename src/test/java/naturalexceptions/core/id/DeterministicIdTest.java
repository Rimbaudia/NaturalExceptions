package naturalexceptions.core.id;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

final class DeterministicIdTest {

    @Test
    void matchesKnownAnswerVector1() {
        DeterministicId id = DeterministicId.derive("naturalexceptions", "test/delayed_event", "100");
        assertEquals("92f04eb1bd5b7cdc50741859297d300d07314b1cf1b7b558a2a43cea47769274", id.hex());
        assertEquals(
            "92f04eb1-bd5b-5cdc-9074-1859297d300d",
            id.asUuid()
                .toString());
    }

    @Test
    void matchesKnownAnswerVector2() {
        DeterministicId id = DeterministicId.derive("seed-12345", "chunk-0-0", "anomaly");
        assertEquals("ad393f128e9a6319502c781d5b433a83ddf3a310d4d88cc7ed0ebf261fc69feb", id.hex());
    }

    @Test
    void matchesKnownAnswerVector3() {
        DeterministicId id = DeterministicId.derive("empty");
        assertEquals("03b56e23cce08d4bfd22426ee559ec18e75b429aa416760d45fabef028fa3ded", id.hex());
    }

    @Test
    void matchesKnownAnswerVector4Empty() {
        DeterministicId id = DeterministicId.derive();
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", id.hex());
    }

    @Test
    void framingPreventsAmbiguousConcatenationCollisions() {
        DeterministicId id1 = DeterministicId.derive("ab", "c");
        DeterministicId id2 = DeterministicId.derive("a", "bc");
        assertNotEquals(id1, id2);
        assertNotEquals(id1.hex(), id2.hex());
    }

    @Test
    void parsingRoundTrip() {
        DeterministicId id = DeterministicId.derive("sample-input");
        DeterministicId parsed = DeterministicId.parse(id.hex());
        assertEquals(id, parsed);
        assertEquals(id.hashCode(), parsed.hashCode());
        assertEquals(id.toString(), parsed.toString());
    }

    @Test
    void rejectsInvalidHex() {
        assertThrows(IllegalArgumentException.class, () -> DeterministicId.parse(null));
        assertThrows(IllegalArgumentException.class, () -> DeterministicId.parse(""));
        assertThrows(IllegalArgumentException.class, () -> DeterministicId.parse("tooshort"));
        assertThrows(
            IllegalArgumentException.class,
            () -> DeterministicId.parse("92f04eb1bd5b7cdc50741859297d300d07314b1cf1b7b558a2a43cea4776927Z"));
    }

    @Test
    void rejectsNullParts() {
        assertThrows(IllegalArgumentException.class, () -> DeterministicId.derive((String[]) null));
        assertThrows(IllegalArgumentException.class, () -> DeterministicId.derive("ok", null));
    }

    @Test
    void implementsComparableWithDeterministicSorting() {
        DeterministicId id1 = DeterministicId.parse("0000000000000000000000000000000000000000000000000000000000000001");
        DeterministicId id2 = DeterministicId.parse("0000000000000000000000000000000000000000000000000000000000000002");

        List<DeterministicId> list = new ArrayList<>();
        list.add(id2);
        list.add(id1);
        Collections.sort(list);

        assertEquals(id1, list.get(0));
        assertEquals(id2, list.get(1));
    }
}
