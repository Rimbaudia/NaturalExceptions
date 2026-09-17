package naturalexceptions.core.composition;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

import naturalexceptions.core.id.ResourceId;

/**
 * Immutable canonical representation of material composition.
 *
 * <p>
 * Quantization Specification:
 * <ul>
 * <li>Quantization resolution: 10,000 units (basis points, 0.01% resolution).</li>
 * <li>Total fraction: Stored constituent amounts strictly sum to 10,000.</li>
 * <li>Apportionment algorithm: Largest Remainder Method (Hare-Niemeyer) computed via exact
 * rational/integer arithmetic.</li>
 * <li>Maximum quantization error: &lt; 1 unit (&lt; 0.01% of total composition; typical &le; 0.005%).</li>
 * <li>Tie-breaking: Equal remainder values are broken deterministically by
 * {@link ResourceId#compareTo(ResourceId)}.</li>
 * <li>Trace constituents: Components rounding to 0 units are discarded.</li>
 * </ul>
 */
public final class CanonicalComposition implements Comparable<CanonicalComposition> {

    public static final int TOTAL_UNITS = 10_000;

    private final NavigableMap<ResourceId, Integer> constituents;
    private final String canonicalString;
    private final int hashCode;

    private CanonicalComposition(NavigableMap<ResourceId, Integer> constituents) {
        this.constituents = Collections.unmodifiableNavigableMap(constituents);
        this.canonicalString = buildCanonicalString(this.constituents);
        this.hashCode = Objects.hash(this.constituents);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static CanonicalComposition of(ResourceId constituent) {
        return builder().add(constituent, TOTAL_UNITS)
            .build();
    }

    public int totalParts() {
        return TOTAL_UNITS;
    }

    public int constituentCount() {
        return constituents.size();
    }

    public boolean contains(ResourceId constituent) {
        return constituents.containsKey(constituent);
    }

    public int parts(ResourceId constituent) {
        Integer val = constituents.get(constituent);
        return val != null ? val : 0;
    }

    public double fraction(ResourceId constituent) {
        return parts(constituent) / (double) TOTAL_UNITS;
    }

    public Set<ResourceId> constituents() {
        return constituents.keySet();
    }

    public Map<ResourceId, Integer> asMap() {
        return constituents;
    }

    public String canonicalString() {
        return canonicalString;
    }

    @Override
    public String toString() {
        return canonicalString;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CanonicalComposition)) return false;
        CanonicalComposition that = (CanonicalComposition) o;
        return constituents.equals(that.constituents);
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public int compareTo(CanonicalComposition other) {
        if (other == null) {
            throw new NullPointerException("Cannot compare CanonicalComposition to null");
        }
        Iterator<Map.Entry<ResourceId, Integer>> it1 = this.constituents.entrySet()
            .iterator();
        Iterator<Map.Entry<ResourceId, Integer>> it2 = other.constituents.entrySet()
            .iterator();

        while (it1.hasNext() && it2.hasNext()) {
            Map.Entry<ResourceId, Integer> e1 = it1.next();
            Map.Entry<ResourceId, Integer> e2 = it2.next();
            int cmpId = e1.getKey()
                .compareTo(e2.getKey());
            if (cmpId != 0) {
                return cmpId;
            }
            int cmpAmount = Integer.compare(e1.getValue(), e2.getValue());
            if (cmpAmount != 0) {
                return cmpAmount;
            }
        }
        if (it1.hasNext()) return 1;
        if (it2.hasNext()) return -1;
        return 0;
    }

    private static String buildCanonicalString(NavigableMap<ResourceId, Integer> map) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Map.Entry<ResourceId, Integer> entry : map.entrySet()) {
            if (!first) {
                sb.append(',');
            }
            sb.append(
                entry.getKey()
                    .toString())
                .append('@')
                .append(entry.getValue());
            first = false;
        }
        return sb.toString();
    }

    public static CanonicalComposition parse(String str) {
        if (str == null || str.trim()
            .isEmpty()) {
            throw new IllegalArgumentException("Canonical composition string cannot be null or empty");
        }
        NavigableMap<ResourceId, Integer> map = new TreeMap<>();
        String[] tokens = str.split(",");
        int sum = 0;
        for (String token : tokens) {
            int atIdx = token.indexOf('@');
            if (atIdx <= 0 || atIdx >= token.length() - 1) {
                throw new IllegalArgumentException("Invalid composition token format: " + token);
            }
            ResourceId id = ResourceId.parse(token.substring(0, atIdx));
            int parts;
            try {
                parts = Integer.parseInt(token.substring(atIdx + 1));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid constituent amount in token: " + token, e);
            }
            if (parts <= 0) {
                throw new IllegalArgumentException("Constituent amount must be positive: " + parts);
            }
            if (map.containsKey(id)) {
                throw new IllegalArgumentException("Duplicate constituent in canonical string: " + id);
            }
            map.put(id, parts);
            sum += parts;
        }
        if (sum != TOTAL_UNITS) {
            throw new IllegalArgumentException(
                "Canonical composition parts sum (" + sum + ") does not equal " + TOTAL_UNITS);
        }
        return new CanonicalComposition(map);
    }

    public static final class Builder {

        private final Map<ResourceId, BigDecimal> weights = new TreeMap<>();

        private Builder() {}

        public Builder add(ResourceId constituent, BigDecimal weight) {
            if (constituent == null) {
                throw new IllegalArgumentException("Constituent ResourceId cannot be null");
            }
            if (weight == null || weight.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Constituent weight cannot be null or negative: " + weight);
            }
            if (weight.compareTo(BigDecimal.ZERO) > 0) {
                weights.merge(constituent, weight, BigDecimal::add);
            }
            return this;
        }

        public Builder add(ResourceId constituent, long weight) {
            if (weight < 0) {
                throw new IllegalArgumentException("Constituent weight cannot be negative: " + weight);
            }
            return add(constituent, BigDecimal.valueOf(weight));
        }

        public Builder add(ResourceId constituent, double weight) {
            if (Double.isNaN(weight) || Double.isInfinite(weight) || weight < 0.0) {
                throw new IllegalArgumentException(
                    "Constituent weight must be a non-negative finite number: " + weight);
            }
            return add(constituent, BigDecimal.valueOf(weight));
        }

        public CanonicalComposition build() {
            if (weights.isEmpty()) {
                throw new IllegalArgumentException("Composition must have at least one non-zero constituent");
            }

            // Find maximum scale to convert all BigDecimals to exact BigIntegers
            int maxScale = 0;
            for (BigDecimal bd : weights.values()) {
                if (bd.scale() > maxScale) {
                    maxScale = bd.scale();
                }
            }
            // Add safety scale for precision
            int scale = Math.max(maxScale, 6);

            List<ConstituentWeight> items = new ArrayList<>();
            BigInteger totalWeight = BigInteger.ZERO;

            for (Map.Entry<ResourceId, BigDecimal> entry : weights.entrySet()) {
                BigInteger intWeight = entry.getValue()
                    .scaleByPowerOfTen(scale)
                    .toBigInteger();
                if (intWeight.compareTo(BigInteger.ZERO) > 0) {
                    items.add(new ConstituentWeight(entry.getKey(), intWeight));
                    totalWeight = totalWeight.add(intWeight);
                }
            }

            if (totalWeight.compareTo(BigInteger.ZERO) <= 0) {
                throw new IllegalArgumentException("Total composition weight must be positive");
            }

            BigInteger targetTotal = BigInteger.valueOf(TOTAL_UNITS);
            int allocatedSum = 0;

            for (ConstituentWeight item : items) {
                BigInteger product = item.weight.multiply(targetTotal);
                BigInteger[] divRem = product.divideAndRemainder(totalWeight);
                item.allocated = divRem[0].intValue();
                item.remainder = divRem[1];
                allocatedSum += item.allocated;
            }

            int remainderUnits = TOTAL_UNITS - allocatedSum;

            // Sort by remainder descending, tie-break by ResourceId ascending
            items.sort((a, b) -> {
                int cmpRem = b.remainder.compareTo(a.remainder);
                if (cmpRem != 0) {
                    return cmpRem;
                }
                return a.id.compareTo(b.id);
            });

            for (int i = 0; i < remainderUnits && i < items.size(); i++) {
                items.get(i).allocated++;
            }

            NavigableMap<ResourceId, Integer> result = new TreeMap<>();
            for (ConstituentWeight item : items) {
                if (item.allocated > 0) {
                    result.put(item.id, item.allocated);
                }
            }

            if (result.isEmpty()) {
                throw new IllegalArgumentException("Composition quantization resulted in zero constituents");
            }

            return new CanonicalComposition(result);
        }

        private static final class ConstituentWeight {

            final ResourceId id;
            final BigInteger weight;
            int allocated;
            BigInteger remainder = BigInteger.ZERO;

            ConstituentWeight(ResourceId id, BigInteger weight) {
                this.id = id;
                this.weight = weight;
            }
        }
    }
}
