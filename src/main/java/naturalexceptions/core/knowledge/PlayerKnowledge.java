package naturalexceptions.core.knowledge;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Per-player evidence and aliases. It intentionally contains no capability/unlock state. */
public final class PlayerKnowledge {
    public static final int MAX_SUBJECTS = 128;
    public static final int MAX_EVIDENCE_PER_SUBJECT = 16;
    public static final int MAX_TOTAL_EVIDENCE = 512;
    public static final int MAX_ALIAS_LENGTH = 48;
    private final Map<KnowledgeSubjectRef, SubjectRecord> subjects = new LinkedHashMap<>();

    public boolean addEvidence(KnowledgeSubjectRef subject, KnowledgeEvidence evidence) {
        if (subject == null || evidence == null) return false;
        if (evidenceCount() >= MAX_TOTAL_EVIDENCE) return false;
        SubjectRecord record = recordFor(subject, true);
        if (record == null || record.evidence.containsKey(evidence.evidenceId())) return false;
        if (record.evidence.size() >= MAX_EVIDENCE_PER_SUBJECT) {
            String first = record.evidence.keySet().iterator().next();
            record.evidence.remove(first);
        }
        record.evidence.put(evidence.evidenceId(), evidence);
        return true;
    }

    public void setAlias(KnowledgeSubjectRef subject, String alias) {
        SubjectRecord record = recordFor(subject, alias != null && !alias.trim().isEmpty());
        if (record == null) return;
        if (alias == null || alias.trim().isEmpty()) {
            record.alias = null;
            removeIfEmpty(subject, record);
            return;
        }
        String normalized = alias.trim();
        if (normalized.length() > MAX_ALIAS_LENGTH || containsControl(normalized)) {
            throw new IllegalArgumentException("Alias must be 1-48 printable characters");
        }
        record.alias = normalized;
    }

    public String aliasFor(KnowledgeSubjectRef subject) {
        SubjectRecord record = recordFor(subject, false);
        return record == null ? null : record.alias;
    }

    public List<KnowledgeEvidence> evidenceFor(KnowledgeSubjectRef subject) {
        SubjectRecord record = recordFor(subject, false);
        if (record == null) return Collections.emptyList();
        return Collections.unmodifiableList(new ArrayList<>(record.evidence.values()));
    }

    public KnowledgePrecision bestPrecision(KnowledgeSubjectRef subject) {
        KnowledgePrecision best = null;
        for (KnowledgeEvidence evidence : evidenceFor(subject)) {
            if (best == null || evidence.precision().ordinal() > best.ordinal()) best = evidence.precision();
        }
        return best;
    }

    public boolean hasEvidence(String evidenceId) {
        if (evidenceId == null) return false;
        for (SubjectRecord record : subjects.values()) {
            if (record.evidence.containsKey(evidenceId)) return true;
        }
        return false;
    }

    public List<KnowledgeSubjectRef> subjects() {
        return Collections.unmodifiableList(new ArrayList<>(subjects.keySet()));
    }

    public int evidenceCount() {
        int count = 0;
        for (SubjectRecord record : subjects.values()) count += record.evidence.size();
        return count;
    }

    private SubjectRecord recordFor(KnowledgeSubjectRef subject, boolean create) {
        if (subject == null) return null;
        SubjectRecord record = subjects.get(subject);
        if (record == null && create && subjects.size() < MAX_SUBJECTS) {
            record = new SubjectRecord();
            subjects.put(subject, record);
        }
        return record;
    }

    private void removeIfEmpty(KnowledgeSubjectRef subject, SubjectRecord record) {
        if (record.evidence.isEmpty() && record.alias == null) subjects.remove(subject);
    }

    private static boolean containsControl(String value) {
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            // Minecraft's section sign starts in-text formatting codes; personal text must stay literal.
            if (Character.isISOControl(c) || c == '\u00a7') return true;
        }
        return false;
    }

    public void readRecord(KnowledgeSubjectRef subject, String alias, List<KnowledgeEvidence> evidence) {
        if (subject == null) return;
        if (alias != null && !alias.trim().isEmpty()) {
            try { setAlias(subject, alias); } catch (IllegalArgumentException ignored) { }
        }
        if (evidence != null) for (KnowledgeEvidence item : evidence) addEvidence(subject, item);
    }

    private static final class SubjectRecord {
        private String alias;
        private final LinkedHashMap<String, KnowledgeEvidence> evidence = new LinkedHashMap<>();
    }
}
