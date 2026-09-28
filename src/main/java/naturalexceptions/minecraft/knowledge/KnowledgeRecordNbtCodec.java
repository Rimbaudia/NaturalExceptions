package naturalexceptions.minecraft.knowledge;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.NBTTagCompound;
import naturalexceptions.core.knowledge.KnowledgeEvidence;
import naturalexceptions.core.knowledge.KnowledgeProvenance;
import naturalexceptions.core.knowledge.KnowledgeSubjectRef;
import naturalexceptions.core.knowledge.PlayerKnowledge;

/** Item-carried evidence record codec. Import changes provenance to transferred-record. */
public final class KnowledgeRecordNbtCodec {
    private KnowledgeRecordNbtCodec() { }

    public static NBTTagCompound exportRecord(KnowledgeSubjectRef subject, PlayerKnowledge source) {
        NBTTagCompound record = new NBTTagCompound();
        if (subject == null || source == null) return record;
        PlayerKnowledge subset = new PlayerKnowledge();
        subset.readRecord(subject, null, source.evidenceFor(subject));
        record.setTag("KnowledgeRecord", PlayerKnowledgeNbtCodec.write(subset));
        return record;
    }

    public static boolean importRecord(NBTTagCompound itemTag, PlayerKnowledge destination) {
        if (itemTag == null || destination == null || !itemTag.hasKey("KnowledgeRecord", 10)) return false;
        PlayerKnowledge decoded = PlayerKnowledgeNbtCodec.read(itemTag.getCompoundTag("KnowledgeRecord"));
        boolean changed = false;
        for (KnowledgeSubjectRef subject : decoded.subjects()) {
            List<KnowledgeEvidence> transferred = new ArrayList<>();
            for (KnowledgeEvidence original : decoded.evidenceFor(subject)) {
                try {
                    transferred.add(new KnowledgeEvidence(original.facet(), original.statement(),
                        "transfer:" + original.evidenceId(), KnowledgeProvenance.TRANSFERRED_RECORD,
                        original.precision()));
                } catch (RuntimeException ignored) { }
            }
            for (KnowledgeEvidence evidence : transferred) changed |= destination.addEvidence(subject, evidence);
        }
        return changed;
    }

    public static boolean isRecord(NBTTagCompound itemTag) {
        return itemTag != null && itemTag.hasKey("KnowledgeRecord", 10);
    }
}
