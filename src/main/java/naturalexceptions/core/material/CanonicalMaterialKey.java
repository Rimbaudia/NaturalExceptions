package naturalexceptions.core.material;

import java.util.Objects;

import naturalexceptions.core.composition.CanonicalComposition;

/**
 * Host-independent canonical state-key defining the complete physical identity of a generated material.
 *
 * <p>
 * Combines:
 * <ul>
 * <li>Canonical quantized composition (at 10,000 basis points)</li>
 * <li>Materially relevant physical phase (e.g. solid, liquid, alpha-crystalline)</li>
 * <li>Materially relevant microstructure class (e.g. as-cast, annealed, quenched)</li>
 * <li>Materially relevant conditioning/history state (e.g. none, cold-worked)</li>
 * </ul>
 *
 * <p>
 * Explicitly excludes transient or sample-specific state (temperature, container, stack size, player alias, discovery
 * state).
 */
public final class CanonicalMaterialKey implements Comparable<CanonicalMaterialKey> {

    public static final MaterialPhase DEFAULT_PHASE = MaterialPhase.SOLID;
    public static final Microstructure DEFAULT_MICROSTRUCTURE = Microstructure.AS_CAST;
    public static final ConditioningHistory DEFAULT_CONDITIONING = ConditioningHistory.NONE;

    private final CanonicalComposition composition;
    private final MaterialPhase phase;
    private final Microstructure microstructure;
    private final ConditioningHistory conditioning;
    private final String canonicalString;

    public CanonicalMaterialKey(CanonicalComposition composition, MaterialPhase phase, Microstructure microstructure,
        ConditioningHistory conditioning) {
        this.composition = Objects.requireNonNull(composition, "composition");
        this.phase = phase != null ? phase : DEFAULT_PHASE;
        this.microstructure = microstructure != null ? microstructure : DEFAULT_MICROSTRUCTURE;
        this.conditioning = conditioning != null ? conditioning : DEFAULT_CONDITIONING;
        this.canonicalString = "comp=" + this.composition.canonicalString()
            + ";phase="
            + this.phase.id()
            + ";micro="
            + this.microstructure.id()
            + ";cond="
            + this.conditioning.id();
    }

    public static CanonicalMaterialKey of(CanonicalComposition composition) {
        return new CanonicalMaterialKey(composition, DEFAULT_PHASE, DEFAULT_MICROSTRUCTURE, DEFAULT_CONDITIONING);
    }

    public static CanonicalMaterialKey of(CanonicalComposition composition, MaterialPhase phase,
        Microstructure microstructure, ConditioningHistory conditioning) {
        return new CanonicalMaterialKey(composition, phase, microstructure, conditioning);
    }

    public static Builder builder() {
        return new Builder();
    }

    public CanonicalComposition composition() {
        return composition;
    }

    public MaterialPhase phase() {
        return phase;
    }

    public Microstructure microstructure() {
        return microstructure;
    }

    public ConditioningHistory conditioning() {
        return conditioning;
    }

    public String canonicalString() {
        return canonicalString;
    }

    public static CanonicalMaterialKey parse(String str) {
        if (str == null || str.trim()
            .isEmpty()) {
            throw new IllegalArgumentException("CanonicalMaterialKey string cannot be null or empty");
        }
        CanonicalComposition comp = null;
        MaterialPhase ph = DEFAULT_PHASE;
        Microstructure ms = DEFAULT_MICROSTRUCTURE;
        ConditioningHistory cond = DEFAULT_CONDITIONING;

        String[] tokens = str.split(";");
        for (String token : tokens) {
            token = token.trim();
            if (token.startsWith("comp=")) {
                comp = CanonicalComposition.parse(token.substring("comp=".length()));
            } else if (token.startsWith("phase=")) {
                ph = MaterialPhase.parse(token.substring("phase=".length()));
            } else if (token.startsWith("micro=")) {
                ms = Microstructure.parse(token.substring("micro=".length()));
            } else if (token.startsWith("cond=")) {
                cond = ConditioningHistory.parse(token.substring("cond=".length()));
            } else if (!token.isEmpty()) {
                // If legacy bare canonical composition string
                comp = CanonicalComposition.parse(token);
            }
        }

        if (comp == null) {
            throw new IllegalArgumentException("Missing composition in CanonicalMaterialKey: " + str);
        }

        return new CanonicalMaterialKey(comp, ph, ms, cond);
    }

    @Override
    public int compareTo(CanonicalMaterialKey o) {
        int cmp = this.canonicalString.compareTo(o.canonicalString);
        return cmp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CanonicalMaterialKey)) return false;
        CanonicalMaterialKey that = (CanonicalMaterialKey) o;
        return canonicalString.equals(that.canonicalString);
    }

    @Override
    public int hashCode() {
        return canonicalString.hashCode();
    }

    @Override
    public String toString() {
        return canonicalString;
    }

    public static final class Builder {

        private CanonicalComposition composition;
        private MaterialPhase phase = DEFAULT_PHASE;
        private Microstructure microstructure = DEFAULT_MICROSTRUCTURE;
        private ConditioningHistory conditioning = DEFAULT_CONDITIONING;

        private Builder() {}

        public Builder composition(CanonicalComposition composition) {
            this.composition = composition;
            return this;
        }

        public Builder phase(MaterialPhase phase) {
            this.phase = phase;
            return this;
        }

        public Builder microstructure(Microstructure microstructure) {
            this.microstructure = microstructure;
            return this;
        }

        public Builder conditioning(ConditioningHistory conditioning) {
            this.conditioning = conditioning;
            return this;
        }

        public CanonicalMaterialKey build() {
            if (composition == null) {
                throw new IllegalStateException("Composition is required");
            }
            return new CanonicalMaterialKey(composition, phase, microstructure, conditioning);
        }
    }
}
