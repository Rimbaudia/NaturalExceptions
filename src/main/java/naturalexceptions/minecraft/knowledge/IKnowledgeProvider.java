package naturalexceptions.minecraft.knowledge;

/**
 * Provider interface for contributing knowledge entries to the central KnowledgeRegistry.
 */
public interface IKnowledgeProvider {

    /**
     * Registers all entries and process steps provided by this contributor.
     */
    void registerEntries(KnowledgeRegistry registry);
}
