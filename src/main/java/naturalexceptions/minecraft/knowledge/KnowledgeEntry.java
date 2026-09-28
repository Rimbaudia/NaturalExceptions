package naturalexceptions.minecraft.knowledge;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import net.minecraft.item.ItemStack;

import naturalexceptions.minecraft.multiblock.MultiblockDefinition;
import naturalexceptions.minecraft.multiblock.MultiblockMaturity;

/**
 * A comprehensive knowledge browser entry describing an item, process, machine,
 * multiblock, or material.
 */
public final class KnowledgeEntry {

    private final String id;
    private final String title;
    private final KnowledgeCategory category;
    private final ItemStack iconStack;
    private final String summary;
    private final String description;
    private final MultiblockMaturity maturity;
    private final String maturityNotice;
    private final MultiblockDefinition multiblock;
    private final Map<String, String> properties;
    private final List<ProcessStep> acquisitions;
    private final List<ProcessStep> usages;
    private final List<String> relatedEntryIds;

    public KnowledgeEntry(String id, String title, KnowledgeCategory category, ItemStack iconStack, String summary,
        String description, MultiblockMaturity maturity, String maturityNotice, MultiblockDefinition multiblock,
        Map<String, String> properties, List<ProcessStep> acquisitions, List<ProcessStep> usages,
        List<String> relatedEntryIds) {
        this.id = Objects.requireNonNull(id, "id");
        this.title = Objects.requireNonNull(title, "title");
        this.category = Objects.requireNonNull(category, "category");
        this.iconStack = iconStack != null ? iconStack.copy() : null;
        this.summary = summary != null ? summary : "";
        this.description = description != null ? description : "";
        this.maturity = maturity != null ? maturity : MultiblockMaturity.STABLE;
        this.maturityNotice = maturityNotice != null ? maturityNotice
            : (this.maturity.isExperimental() ? this.maturity.notice() : null);
        this.multiblock = multiblock;
        this.properties = properties != null ? new LinkedHashMap<>(properties) : new LinkedHashMap<>();
        this.acquisitions = acquisitions != null ? new ArrayList<>(acquisitions) : new ArrayList<>();
        this.usages = usages != null ? new ArrayList<>(usages) : new ArrayList<>();
        this.relatedEntryIds = relatedEntryIds != null ? new ArrayList<>(relatedEntryIds) : new ArrayList<>();
    }

    public String id() {
        return id;
    }

    public String title() {
        return title;
    }

    public KnowledgeCategory category() {
        return category;
    }

    public ItemStack iconStack() {
        return iconStack != null ? iconStack.copy() : null;
    }

    public String summary() {
        return summary;
    }

    public String description() {
        return description;
    }

    public MultiblockMaturity maturity() {
        return maturity;
    }

    public String maturityNotice() {
        return maturityNotice;
    }

    public boolean isExperimental() {
        return maturity.isExperimental();
    }

    public MultiblockDefinition multiblock() {
        return multiblock;
    }

    public boolean hasMultiblock() {
        return multiblock != null;
    }

    public Map<String, String> properties() {
        return Collections.unmodifiableMap(properties);
    }

    public List<ProcessStep> acquisitions() {
        return Collections.unmodifiableList(acquisitions);
    }

    public List<ProcessStep> usages() {
        return Collections.unmodifiableList(usages);
    }

    public List<String> relatedEntryIds() {
        return Collections.unmodifiableList(relatedEntryIds);
    }

    public static Builder builder(String id, String title, KnowledgeCategory category) {
        return new Builder(id, title, category);
    }

    public static final class Builder {

        private final String id;
        private final String title;
        private final KnowledgeCategory category;
        private ItemStack iconStack;
        private String summary;
        private String description;
        private MultiblockMaturity maturity = MultiblockMaturity.STABLE;
        private String maturityNotice;
        private MultiblockDefinition multiblock;
        private final Map<String, String> properties = new LinkedHashMap<>();
        private final List<ProcessStep> acquisitions = new ArrayList<>();
        private final List<ProcessStep> usages = new ArrayList<>();
        private final List<String> relatedEntryIds = new ArrayList<>();

        public Builder(String id, String title, KnowledgeCategory category) {
            this.id = id;
            this.title = title;
            this.category = category;
        }

        public Builder icon(ItemStack iconStack) {
            this.iconStack = iconStack;
            return this;
        }

        public Builder summary(String summary) {
            this.summary = summary;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder maturity(MultiblockMaturity maturity) {
            this.maturity = maturity;
            return this;
        }

        public Builder maturityNotice(String maturityNotice) {
            this.maturityNotice = maturityNotice;
            return this;
        }

        public Builder multiblock(MultiblockDefinition multiblock) {
            this.multiblock = multiblock;
            if (multiblock != null) {
                this.maturity = multiblock.maturity();
                this.maturityNotice = multiblock.maturityNotice();
            }
            return this;
        }

        public Builder addProperty(String key, String value) {
            this.properties.put(key, value);
            return this;
        }

        public Builder addAcquisition(ProcessStep step) {
            if (step != null) this.acquisitions.add(step);
            return this;
        }

        public Builder addUsage(ProcessStep step) {
            if (step != null) this.usages.add(step);
            return this;
        }

        public Builder addRelated(String entryId) {
            if (entryId != null) this.relatedEntryIds.add(entryId);
            return this;
        }

        public KnowledgeEntry build() {
            return new KnowledgeEntry(
                id,
                title,
                category,
                iconStack,
                summary,
                description,
                maturity,
                maturityNotice,
                multiblock,
                properties,
                acquisitions,
                usages,
                relatedEntryIds);
        }
    }
}
