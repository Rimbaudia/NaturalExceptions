package naturalexceptions.core.knowledge;

import java.util.Objects;

import naturalexceptions.core.material.MaterialRef;

/** Durable subject address. Raw identities are retained even when their content is not installed. */
public final class KnowledgeSubjectRef implements Comparable<KnowledgeSubjectRef> {
    private static final int MAX_LENGTH = 256;
    private final String type;
    private final String identity;

    private KnowledgeSubjectRef(String type, String identity) {
        if (type == null || type.isEmpty() || type.length() > 32 || identity == null || identity.isEmpty()
            || identity.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("Invalid knowledge subject reference");
        }
        this.type = type;
        this.identity = identity;
    }

    public static KnowledgeSubjectRef material(MaterialRef ref) {
        Objects.requireNonNull(ref, "ref");
        return new KnowledgeSubjectRef("material", ref.asString());
    }

    public static KnowledgeSubjectRef parse(String encoded) {
        if (encoded == null || encoded.length() > MAX_LENGTH + 33) {
            throw new IllegalArgumentException("Invalid knowledge subject encoding");
        }
        int delimiter = encoded.indexOf('|');
        if (delimiter <= 0 || delimiter == encoded.length() - 1) {
            throw new IllegalArgumentException("Invalid knowledge subject encoding");
        }
        return new KnowledgeSubjectRef(encoded.substring(0, delimiter), encoded.substring(delimiter + 1));
    }

    public String type() { return type; }
    public String identity() { return identity; }
    public String asString() { return type + "|" + identity; }

    public boolean isResolvedMaterialRef() {
        if (!"material".equals(type)) return false;
        try {
            MaterialRef.parse(identity);
            return true;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    public MaterialRef materialRefOrNull() {
        if (!isResolvedMaterialRef()) return null;
        return MaterialRef.parse(identity);
    }

    @Override public int compareTo(KnowledgeSubjectRef other) { return asString().compareTo(other.asString()); }
    @Override public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof KnowledgeSubjectRef)) return false;
        KnowledgeSubjectRef that = (KnowledgeSubjectRef) other;
        return type.equals(that.type) && identity.equals(that.identity);
    }
    @Override public int hashCode() { return Objects.hash(type, identity); }
    @Override public String toString() { return asString(); }
}
