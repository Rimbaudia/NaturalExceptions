package naturalexceptions.core.metallurgy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class MaterialQuantityTest {

    @Test
    void batchMassCannotBecomeNegative() {
        assertThrows(IllegalArgumentException.class, () -> MaterialQuantity.ofMilligrams(-1L));
        assertThrows(IllegalArgumentException.class, () -> MaterialQuantity.ofGrams(-0.001));
        assertThrows(IllegalArgumentException.class, () -> MaterialQuantity.ofKilograms(-1.0));

        MaterialQuantity q1 = MaterialQuantity.ofMilligrams(100L);
        MaterialQuantity q2 = MaterialQuantity.ofMilligrams(200L);

        // Subtraction that would yield negative mass throws ArithmeticException
        assertThrows(ArithmeticException.class, () -> q1.subtract(q2));
    }

    @Test
    void batchMergeConservesMass() {
        MaterialQuantity q1 = MaterialQuantity.ofMilligrams(15420L);
        MaterialQuantity q2 = MaterialQuantity.ofMilligrams(4580L);

        MaterialQuantity merged = q1.add(q2);
        assertEquals(20000L, merged.milligrams());
        assertEquals(20.0, merged.toGrams(), 1.0e-6);
        assertEquals(0.020, merged.toKilograms(), 1.0e-9);

        MaterialQuantity extracted = merged.subtract(q1);
        assertEquals(q2, extracted);
    }

    @Test
    void conversionsAndFormatting() {
        MaterialQuantity q = MaterialQuantity.ofGrams(123.456);
        assertEquals(123456L, q.milligrams());
        assertEquals(123.456, q.toGrams(), 1.0e-6);
        assertEquals(0.123456, q.toKilograms(), 1.0e-9);
        assertTrue(q.isPositive());
        assertFalse(q.isZero());

        MaterialQuantity zero = MaterialQuantity.ZERO;
        assertTrue(zero.isZero());
        assertFalse(zero.isPositive());
    }

    @Test
    void portioningConservesMassWithinRounding() {
        MaterialQuantity total = MaterialQuantity.ofMilligrams(1000L);
        MaterialQuantity p1 = total.portion(0.60);
        MaterialQuantity p2 = total.portion(0.35);
        MaterialQuantity p3 = total.portion(0.05);

        assertEquals(600L, p1.milligrams());
        assertEquals(350L, p2.milligrams());
        assertEquals(50L, p3.milligrams());

        MaterialQuantity sum = p1.add(p2)
            .add(p3);
        assertEquals(total, sum);
    }
}
