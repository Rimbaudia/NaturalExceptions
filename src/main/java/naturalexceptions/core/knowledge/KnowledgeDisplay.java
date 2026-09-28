package naturalexceptions.core.knowledge;

/** Small deterministic presentation policy; it only displays evidence held by this player. */
public final class KnowledgeDisplay {
    private KnowledgeDisplay() { }

    public static String resolve(KnowledgeSubjectRef subject, PlayerKnowledge knowledge, String appearance,
        String physicalState) {
        if (subject == null) return "Unidentified material";
        if (knowledge != null) {
            String alias = knowledge.aliasFor(subject);
            if (alias != null) return alias;
            for (KnowledgeEvidence evidence : knowledge.evidenceFor(subject)) {
                if ("classification".equals(evidence.facet())) return evidence.statement();
            }
            for (KnowledgeEvidence evidence : knowledge.evidenceFor(subject)) {
                if ("appearance".equals(evidence.facet())) return evidence.statement();
            }
        }
        if (appearance != null && !appearance.trim().isEmpty()) {
            return capitalize(appearance.trim()) + " material";
        }
        if (physicalState != null && !physicalState.trim().isEmpty()) {
            return "Unidentified " + physicalState.trim().toLowerCase() + " material";
        }
        return "Unidentified material";
    }

    private static String capitalize(String value) {
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }
}
