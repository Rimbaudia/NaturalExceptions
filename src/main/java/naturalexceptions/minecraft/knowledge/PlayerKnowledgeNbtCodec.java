package naturalexceptions.minecraft.knowledge;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import naturalexceptions.core.knowledge.KnowledgeEvidence;
import naturalexceptions.core.knowledge.KnowledgePrecision;
import naturalexceptions.core.knowledge.KnowledgeProvenance;
import naturalexceptions.core.knowledge.KnowledgeSubjectRef;
import naturalexceptions.core.knowledge.PlayerKnowledge;

/** Bounded NBT codec for player evidence. Unknown subject identities remain opaque and unresolved. */
public final class PlayerKnowledgeNbtCodec {
    private static final int VERSION = 1;
    private PlayerKnowledgeNbtCodec() { }

    public static NBTTagCompound write(PlayerKnowledge knowledge) {
        NBTTagCompound root = new NBTTagCompound();
        root.setInteger("Version", VERSION);
        NBTTagList subjects = new NBTTagList();
        if (knowledge != null) {
            int subjectCount = 0;
            for (KnowledgeSubjectRef subject : knowledge.subjects()) {
                if (subjectCount++ >= PlayerKnowledge.MAX_SUBJECTS) break;
                NBTTagCompound entry = new NBTTagCompound();
                entry.setString("Subject", subject.asString());
                String alias = knowledge.aliasFor(subject);
                if (alias != null) entry.setString("Alias", alias);
                NBTTagList evidenceList = new NBTTagList();
                for (KnowledgeEvidence evidence : knowledge.evidenceFor(subject)) {
                    NBTTagCompound fact = new NBTTagCompound();
                    fact.setString("Facet", evidence.facet());
                    fact.setString("Statement", evidence.statement());
                    fact.setString("EvidenceId", evidence.evidenceId());
                    fact.setString("Provenance", evidence.provenance().name());
                    fact.setString("Precision", evidence.precision().name());
                    evidenceList.appendTag(fact);
                }
                entry.setTag("Evidence", evidenceList);
                subjects.appendTag(entry);
            }
        }
        root.setTag("Subjects", subjects);
        return root;
    }

    public static PlayerKnowledge read(NBTTagCompound root) {
        PlayerKnowledge result = new PlayerKnowledge();
        if (root == null || !root.hasKey("Subjects", 9) || root.getInteger("Version") > VERSION) return result;
        NBTTagList subjects = root.getTagList("Subjects", 10);
        int limit = Math.min(subjects.tagCount(), PlayerKnowledge.MAX_SUBJECTS);
        for (int i = 0; i < limit; i++) {
            NBTTagCompound entry = subjects.getCompoundTagAt(i);
            try {
                KnowledgeSubjectRef subject = KnowledgeSubjectRef.parse(entry.getString("Subject"));
                List<KnowledgeEvidence> evidence = new ArrayList<>();
                if (entry.hasKey("Evidence", 9)) {
                    NBTTagList facts = entry.getTagList("Evidence", 10);
                    int factLimit = Math.min(facts.tagCount(), PlayerKnowledge.MAX_EVIDENCE_PER_SUBJECT);
                    for (int j = 0; j < factLimit; j++) {
                        NBTTagCompound fact = facts.getCompoundTagAt(j);
                        try {
                            evidence.add(new KnowledgeEvidence(
                                fact.getString("Facet"), fact.getString("Statement"), fact.getString("EvidenceId"),
                                KnowledgeProvenance.valueOf(fact.getString("Provenance")),
                                KnowledgePrecision.valueOf(fact.getString("Precision"))));
                        } catch (RuntimeException ignored) { }
                    }
                }
                result.readRecord(subject, entry.hasKey("Alias", 8) ? entry.getString("Alias") : null, evidence);
            } catch (RuntimeException ignored) {
                // Corrupt subject entries are skipped; well-formed unknown identities are retained above.
            }
        }
        return result;
    }
}
