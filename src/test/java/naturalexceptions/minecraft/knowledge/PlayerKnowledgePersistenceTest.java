package naturalexceptions.minecraft.knowledge;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import net.minecraft.nbt.NBTTagCompound;
import naturalexceptions.core.knowledge.KnowledgeEvidence;
import naturalexceptions.core.knowledge.KnowledgePrecision;
import naturalexceptions.core.knowledge.KnowledgeProvenance;
import naturalexceptions.core.knowledge.KnowledgeSubjectRef;
import naturalexceptions.core.knowledge.PlayerKnowledge;
import naturalexceptions.core.material.MaterialRef;
import naturalexceptions.core.id.ResourceId;

final class PlayerKnowledgePersistenceTest {
    private static final KnowledgeSubjectRef COPPER = KnowledgeSubjectRef.material(
        MaterialRef.authored(ResourceId.of("naturalexceptions", "copper")));

    @Test void knowledgeAndAliasSurviveNbtSaveReload() {
        PlayerKnowledge before = new PlayerKnowledge();
        before.setAlias(COPPER, "Jeffium");
        before.addEvidence(COPPER, new KnowledgeEvidence("classification", "Copper", "book:manual:copper",
            KnowledgeProvenance.BOOK, KnowledgePrecision.CLASSIFICATION));

        PlayerKnowledge after = PlayerKnowledgeNbtCodec.read(PlayerKnowledgeNbtCodec.write(before));

        assertEquals("Jeffium", after.aliasFor(COPPER));
        assertEquals(before.evidenceFor(COPPER).get(0).evidenceId(), after.evidenceFor(COPPER).get(0).evidenceId());
        assertEquals(KnowledgeProvenance.BOOK, after.evidenceFor(COPPER).get(0).provenance());
    }

    @Test void playerPropertiesPersistKnowledgeAlongsideManualDeliveryState() {
        PlayerKnowledgeProperties before = new PlayerKnowledgeProperties();
        before.knowledge().setAlias(COPPER, "Jeffium");
        before.knowledge().addEvidence(COPPER, new KnowledgeEvidence("classification", "Copper", "book:manual:copper",
            KnowledgeProvenance.BOOK, KnowledgePrecision.CLASSIFICATION));
        before.markManualDelivered();
        NBTTagCompound playerData = new NBTTagCompound();
        before.saveNBTData(playerData);

        PlayerKnowledgeProperties after = new PlayerKnowledgeProperties();
        after.loadNBTData(playerData);

        assertTrue(after.manualDelivered());
        assertEquals("Jeffium", after.knowledge().aliasFor(COPPER));
        assertEquals(1, after.knowledge().evidenceFor(COPPER).size());
    }

    @Test void malformedRecordsDegradeAndUnknownSubjectIsNotRebound() {
        NBTTagCompound root = new NBTTagCompound();
        root.setInteger("Version", 1);
        net.minecraft.nbt.NBTTagList subjects = new net.minecraft.nbt.NBTTagList();
        NBTTagCompound future = new NBTTagCompound();
        future.setString("Subject", "material|mat:futureaddon:ore_xyz");
        future.setString("Alias", "test name");
        subjects.appendTag(future);
        NBTTagCompound corrupt = new NBTTagCompound();
        corrupt.setString("Subject", "malformed");
        subjects.appendTag(corrupt);
        root.setTag("Subjects", subjects);

        PlayerKnowledge result = PlayerKnowledgeNbtCodec.read(root);

        assertEquals(1, result.subjects().size());
        assertEquals("test name", result.aliasFor(KnowledgeSubjectRef.parse("material|mat:futureaddon:ore_xyz")));
        assertFalse(result.subjects().get(0).isResolvedMaterialRef());
    }

    @Test void transferableEvidenceImportsWithTransferredProvenance() {
        PlayerKnowledge source = new PlayerKnowledge();
        source.addEvidence(COPPER, new KnowledgeEvidence("melting", "Melts near 1,350 K", "book:manual:melting",
            KnowledgeProvenance.BOOK, KnowledgePrecision.APPROXIMATE));
        NBTTagCompound record = KnowledgeRecordNbtCodec.exportRecord(COPPER, source);
        PlayerKnowledge recipient = new PlayerKnowledge();

        assertTrue(KnowledgeRecordNbtCodec.importRecord(record, recipient));
        assertEquals(KnowledgeProvenance.TRANSFERRED_RECORD, recipient.evidenceFor(COPPER).get(0).provenance());
        assertNull(recipient.aliasFor(COPPER), "A record shares evidence, not its owner's personal alias");
        assertFalse(KnowledgeRecordNbtCodec.importRecord(record, recipient));
    }

    @Test void readingBookSourceAddsClaimsOnlyAndNeverChangesMaterialData() {
        MaterialRef identityBefore = MaterialRef.parse(COPPER.identity());
        PlayerKnowledge knowledge = new PlayerKnowledge();
        naturalexceptions.content.knowledge.BuiltinKnowledgeSources.Source manual = naturalexceptions.content.knowledge.BuiltinKnowledgeSources
            .get(naturalexceptions.content.knowledge.BuiltinKnowledgeSources.STARTING_MANUAL);

        for (naturalexceptions.content.knowledge.BuiltinKnowledgeSources.Claim claim : manual.claims()) {
            if (claim.subject().equals(COPPER.asString())) knowledge.addEvidence(COPPER, claim.evidence());
        }

        assertEquals(identityBefore, MaterialRef.parse(COPPER.identity()));
        assertEquals("Copper", knowledge.evidenceFor(COPPER).get(0).statement());
        assertTrue(java.util.Arrays.stream(PlayerKnowledge.class.getDeclaredFields())
            .noneMatch(field -> field.getName().toLowerCase().contains("unlock")
                || field.getName().toLowerCase().contains("capability")));
    }
}
