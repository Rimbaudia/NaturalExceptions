package naturalexceptions.content.knowledge;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import naturalexceptions.core.knowledge.KnowledgeEvidence;
import naturalexceptions.core.knowledge.KnowledgePrecision;

/** Small authored information set, kept outside item and GUI classes. */
public final class BuiltinKnowledgeSources {
    public static final String STARTING_MANUAL = "naturalexceptions:field_manual";
    public static final String FOUNDRY_NOTES = "naturalexceptions:foundry_notes";
    private static final Map<String, Source> SOURCES = new LinkedHashMap<>();

    static {
        SOURCES.put(STARTING_MANUAL, new Source("Field Manual", Arrays.asList(
            claim("process|naturalexceptions:starter_metallurgy", "method",
                "Heat copper-bearing feedstock in the starter smelter, then draw molten metal through its outlet.", "field_manual:starter_metallurgy", KnowledgePrecision.CLASSIFICATION),
            claim("process|naturalexceptions:basic_casting", "method",
                "A compatible mold shapes poured metal; allow the casting to cool before handling.", "field_manual:casting", KnowledgePrecision.CLASSIFICATION),
            claim("material|mat:authored:naturalexceptions:copper", "classification", "Copper", "field_manual:copper", KnowledgePrecision.CLASSIFICATION),
            claim("material|mat:authored:naturalexceptions:copper", "melting", "Copper melts at roughly 1,350 K.", "field_manual:copper_melting", KnowledgePrecision.APPROXIMATE),
            claim("entry|naturalexceptions:copper_ore", "process",
                "Copper-bearing ore occurs in stone deposits. Smelt it in the starter smelter with fuel; draw molten metal through the outlet and cast it in a compatible mold.",
                "field_manual:copper_ore", KnowledgePrecision.CLASSIFICATION),
            claim("entry|naturalexceptions:starter_smelter", "process",
                "Feed ore and fuel to the starter smelter. Hand bellows increase draft; the smelter can also operate without them.",
                "field_manual:starter_smelter", KnowledgePrecision.CLASSIFICATION))));
        SOURCES.put(FOUNDRY_NOTES, new Source("Foundry Notes", Arrays.asList(
            claim("material|mat:authored:naturalexceptions:copper", "processing",
                "Repeated remelting preserves copper identity unless other feedstock is introduced.", "foundry_notes:remelt", KnowledgePrecision.CLASSIFICATION),
            claim("entry|naturalexceptions:copper_ore", "observation",
                "Dark residue separated from molten copper is slag; skim it from the melt rather than casting it as metal.",
                "foundry_notes:slag", KnowledgePrecision.CLASSIFICATION),
            claim("process|naturalexceptions:starter_metallurgy", "observation",
                "A separated dark residue is slag, not usable metal.", "foundry_notes:slag", KnowledgePrecision.CLASSIFICATION))));
    }

    private BuiltinKnowledgeSources() { }

    public static Source get(String id) { return SOURCES.get(id); }
    public static Map<String, Source> all() { return Collections.unmodifiableMap(SOURCES); }

    private static Claim claim(String subject, String facet, String statement, String id,
        KnowledgePrecision precision) {
        return new Claim(subject, new KnowledgeEvidence(facet, statement, id,
            naturalexceptions.core.knowledge.KnowledgeProvenance.BOOK, precision));
    }

    public static final class Claim {
        private final String subject;
        private final KnowledgeEvidence evidence;
        public Claim(String subject, KnowledgeEvidence evidence) { this.subject = subject; this.evidence = evidence; }
        public String subject() { return subject; }
        public KnowledgeEvidence evidence() { return evidence; }
    }

    public static final class Source {
        private final String title;
        private final List<Claim> claims;
        private Source(String title, List<Claim> claims) { this.title = title; this.claims = Collections.unmodifiableList(claims); }
        public String title() { return title; }
        public List<Claim> claims() { return claims; }
    }
}
