package naturalexceptions.core.knowledge;

import java.util.Objects;

/** One bounded player-facing observation or documented claim about a subject. */
public final class KnowledgeEvidence {
    public static final int MAX_FIELD_LENGTH = 256;
    private final String facet;
    private final String statement;
    private final String evidenceId;
    private final KnowledgeProvenance provenance;
    private final KnowledgePrecision precision;

    public KnowledgeEvidence(String facet, String statement, String evidenceId, KnowledgeProvenance provenance,
        KnowledgePrecision precision) {
        this.facet = bounded(facet, "facet", 64);
        this.statement = bounded(statement, "statement", MAX_FIELD_LENGTH);
        this.evidenceId = bounded(evidenceId, "evidenceId", 128);
        this.provenance = Objects.requireNonNull(provenance, "provenance");
        this.precision = Objects.requireNonNull(precision, "precision");
    }

    private static String bounded(String value, String label, int max) {
        if (value == null || value.trim().isEmpty() || value.length() > max) {
            throw new IllegalArgumentException("Invalid knowledge " + label);
        }
        return value.trim();
    }

    public String facet() { return facet; }
    public String statement() { return statement; }
    public String evidenceId() { return evidenceId; }
    public KnowledgeProvenance provenance() { return provenance; }
    public KnowledgePrecision precision() { return precision; }

    @Override public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof KnowledgeEvidence)) return false;
        KnowledgeEvidence that = (KnowledgeEvidence) other;
        return evidenceId.equals(that.evidenceId);
    }
    @Override public int hashCode() { return evidenceId.hashCode(); }
}
