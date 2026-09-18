package naturalexceptions.core.metallurgy;

import java.util.Objects;

/**
 * Conservation-grade immutable physical mass quantity representation.
 *
 * <p>
 * Fundamental physical unit: 1 milligram (mg).
 * <ul>
 * <li>1 mg = 10⁻³ g = 10⁻⁶ kg.</li>
 * <li>All internal accounting is strictly integer-based to prevent floating-point drift.</li>
 * <li>Quantities are strictly non-negative; subtraction that would yield negative mass throws
 * {@link ArithmeticException}.</li>
 * <li>Arithmetic operations are overflow-checked via {@link Math#addExact(long, long)}.</li>
 * </ul>
 */
public final class MaterialQuantity implements Comparable<MaterialQuantity> {

    public static final long MILLIGRAMS_PER_GRAM = 1_000L;
    public static final long MILLIGRAMS_PER_KILOGRAM = 1_000_000L;

    public static final MaterialQuantity ZERO = new MaterialQuantity(0L);

    private final long milligrams;

    private MaterialQuantity(long milligrams) {
        if (milligrams < 0L) {
            throw new IllegalArgumentException("MaterialQuantity cannot be negative: " + milligrams + " mg");
        }
        this.milligrams = milligrams;
    }

    public static MaterialQuantity ofMilligrams(long milligrams) {
        if (milligrams == 0L) {
            return ZERO;
        }
        return new MaterialQuantity(milligrams);
    }

    public static MaterialQuantity ofGrams(double grams) {
        if (Double.isNaN(grams) || Double.isInfinite(grams) || grams < 0.0) {
            throw new IllegalArgumentException("Invalid grams quantity: " + grams);
        }
        long mg = Math.round(grams * MILLIGRAMS_PER_GRAM);
        return ofMilligrams(mg);
    }

    public static MaterialQuantity ofKilograms(double kilograms) {
        if (Double.isNaN(kilograms) || Double.isInfinite(kilograms) || kilograms < 0.0) {
            throw new IllegalArgumentException("Invalid kilograms quantity: " + kilograms);
        }
        long mg = Math.round(kilograms * MILLIGRAMS_PER_KILOGRAM);
        return ofMilligrams(mg);
    }

    public long milligrams() {
        return milligrams;
    }

    public double toGrams() {
        return (double) milligrams / MILLIGRAMS_PER_GRAM;
    }

    public double toKilograms() {
        return (double) milligrams / MILLIGRAMS_PER_KILOGRAM;
    }

    public boolean isZero() {
        return milligrams == 0L;
    }

    public boolean isPositive() {
        return milligrams > 0L;
    }

    public MaterialQuantity add(MaterialQuantity other) {
        Objects.requireNonNull(other, "other quantity");
        long sum = Math.addExact(this.milligrams, other.milligrams);
        return ofMilligrams(sum);
    }

    public MaterialQuantity subtract(MaterialQuantity other) {
        Objects.requireNonNull(other, "other quantity");
        if (this.milligrams < other.milligrams) {
            throw new ArithmeticException(
                "Underflow in mass subtraction: " + this.milligrams + " mg < " + other.milligrams + " mg");
        }
        return ofMilligrams(this.milligrams - other.milligrams);
    }

    public MaterialQuantity multiply(long factor) {
        if (factor < 0L) {
            throw new IllegalArgumentException("Multiplication factor cannot be negative: " + factor);
        }
        if (factor == 0L || milligrams == 0L) {
            return ZERO;
        }
        return ofMilligrams(Math.multiplyExact(this.milligrams, factor));
    }

    /**
     * Splits this quantity into a portion given by a fraction [0.0, 1.0], rounding deterministically to the nearest
     * milligram.
     */
    public MaterialQuantity portion(double fraction) {
        if (fraction < 0.0 || fraction > 1.0 || Double.isNaN(fraction)) {
            throw new IllegalArgumentException("Fraction must be within [0.0, 1.0]: " + fraction);
        }
        long portionMg = Math.round((double) milligrams * fraction);
        return ofMilligrams(Math.min(milligrams, portionMg));
    }

    @Override
    public int compareTo(MaterialQuantity o) {
        return Long.compare(this.milligrams, o.milligrams);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MaterialQuantity)) return false;
        MaterialQuantity that = (MaterialQuantity) o;
        return milligrams == that.milligrams;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(milligrams);
    }

    @Override
    public String toString() {
        if (milligrams >= MILLIGRAMS_PER_KILOGRAM) {
            return String.format("%.3f kg", toKilograms());
        } else if (milligrams >= MILLIGRAMS_PER_GRAM) {
            return String.format("%.3f g", toGrams());
        } else {
            return milligrams + " mg";
        }
    }
}
