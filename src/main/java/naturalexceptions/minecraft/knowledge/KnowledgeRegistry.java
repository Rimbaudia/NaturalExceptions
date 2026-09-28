package naturalexceptions.minecraft.knowledge;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import net.minecraft.item.ItemStack;

import naturalexceptions.core.fabrication.GeometryId;
import naturalexceptions.minecraft.item.ItemCastingMold;
import naturalexceptions.minecraft.item.ItemGenericAssembly;
import naturalexceptions.minecraft.item.ItemGenericComponent;
import naturalexceptions.minecraft.item.ItemMaterialSample;
import naturalexceptions.minecraft.metallurgy.TileEntityStarterSmelter;
import naturalexceptions.minecraft.registry.NEBlocks;
import naturalexceptions.minecraft.registry.NEItems;

/**
 * Central registry for all knowledge entries, searchable recipes, processes,
 * and authoritative multiblock definitions.
 */
public final class KnowledgeRegistry {

    private static final KnowledgeRegistry INSTANCE = new KnowledgeRegistry();

    private final Map<String, KnowledgeEntry> entriesById = new LinkedHashMap<>();
    private final List<KnowledgeEntry> allEntries = new ArrayList<>();
    private final Map<KnowledgeCategory, List<KnowledgeEntry>> entriesByCategory = new EnumMap<>(
        KnowledgeCategory.class);
    private boolean initialized = false;

    private KnowledgeRegistry() {
        for (KnowledgeCategory cat : KnowledgeCategory.values()) {
            entriesByCategory.put(cat, new ArrayList<>());
        }
    }

    public static KnowledgeRegistry instance() {
        return INSTANCE;
    }

    public synchronized void registerEntry(KnowledgeEntry entry) {
        if (entry == null) return;
        if (entriesById.containsKey(entry.id())) {
            // Replace existing entry
            KnowledgeEntry existing = entriesById.get(entry.id());
            int index = allEntries.indexOf(existing);
            if (index >= 0) {
                allEntries.set(index, entry);
            }
            List<KnowledgeEntry> catList = entriesByCategory.get(existing.category());
            if (catList != null) catList.remove(existing);
        } else {
            allEntries.add(entry);
        }

        entriesById.put(entry.id(), entry);
        entriesByCategory.get(entry.category())
            .add(entry);
    }

    public KnowledgeEntry getEntry(String id) {
        return entriesById.get(id);
    }

    public List<KnowledgeEntry> getAllEntries() {
        return Collections.unmodifiableList(allEntries);
    }

    public List<KnowledgeEntry> getEntriesByCategory(KnowledgeCategory category) {
        if (category == null) return getAllEntries();
        List<KnowledgeEntry> list = entriesByCategory.get(category);
        return list != null ? Collections.unmodifiableList(list) : Collections.emptyList();
    }

    public synchronized void init() {
        if (initialized) return;
        initialized = true;

        NEItems.ensureBootstrapped();
        NEBlocks.register();
        NEItems.register();

        new naturalexceptions.minecraft.knowledge.provider.CraftingKnowledgeProvider().registerEntries(this);
        new naturalexceptions.minecraft.knowledge.provider.MetallurgyKnowledgeProvider().registerEntries(this);
        new naturalexceptions.minecraft.knowledge.provider.CastingKnowledgeProvider().registerEntries(this);
        new naturalexceptions.minecraft.knowledge.provider.AssemblyKnowledgeProvider().registerEntries(this);
        new naturalexceptions.minecraft.knowledge.provider.MaterialKnowledgeProvider().registerEntries(this);
    }

    public synchronized void reset() {
        entriesById.clear();
        allEntries.clear();
        for (KnowledgeCategory cat : KnowledgeCategory.values()) {
            entriesByCategory.get(cat)
                .clear();
        }
        initialized = false;
    }

    /**
     * Performs a multi-field search across titles, summaries, descriptions, categories, and properties.
     */
    public List<KnowledgeEntry> search(String query) {
        if (query == null || query.trim()
            .isEmpty()) {
            return getAllEntries();
        }

        String lowerQuery = query.toLowerCase(Locale.ROOT)
            .trim();
        String[] tokens = lowerQuery.split("\\s+");
        List<KnowledgeEntry> results = new ArrayList<>();

        for (KnowledgeEntry entry : allEntries) {
            String title = entry.title()
                .toLowerCase(Locale.ROOT);
            String summary = entry.summary()
                .toLowerCase(Locale.ROOT);
            String desc = entry.description()
                .toLowerCase(Locale.ROOT);
            String cat = entry.category()
                .displayName()
                .toLowerCase(Locale.ROOT);

            boolean match = true;
            for (String token : tokens) {
                boolean tokenMatch = title.contains(token) || summary.contains(token)
                    || desc.contains(token)
                    || cat.contains(token);

                if (!tokenMatch) {
                    for (Map.Entry<String, String> prop : entry.properties()
                        .entrySet()) {
                        if (prop.getKey()
                            .toLowerCase(Locale.ROOT)
                            .contains(token)
                            || prop.getValue()
                                .toLowerCase(Locale.ROOT)
                                .contains(token)) {
                            tokenMatch = true;
                            break;
                        }
                    }
                }

                if (!tokenMatch) {
                    match = false;
                    break;
                }
            }

            if (match) {
                results.add(entry);
            }
        }

        return results;
    }

    /**
     * Finds entries explaining how to obtain the given ItemStack ("R" key).
     */
    public List<KnowledgeEntry> findAcquisitions(ItemStack query) {
        if (query == null) return Collections.emptyList();
        Set<KnowledgeEntry> matches = new LinkedHashSet<>();

        // 1. Direct entry icon match
        for (KnowledgeEntry entry : allEntries) {
            if (ProcessStep.isMatching(entry.iconStack(), query)) {
                matches.add(entry);
            }
        }

        // 2. Process step output match
        for (KnowledgeEntry entry : allEntries) {
            for (ProcessStep step : entry.acquisitions()) {
                if (step.matchesOutput(query)) {
                    matches.add(entry);
                    break;
                }
            }
        }

        // 3. Dynamic contextual resolution
        if (query.getItem() instanceof ItemGenericComponent) {
            GeometryId geom = ItemGenericComponent.getGeometry(query);
            if (geom == GeometryId.HANDLE) {
                KnowledgeEntry handleEntry = getEntry("wooden_handle");
                if (handleEntry != null) matches.add(handleEntry);
            } else if (geom != null) {
                KnowledgeEntry moldEntry = getEntry("mold_" + geom.id());
                if (moldEntry != null) matches.add(moldEntry);
                KnowledgeEntry castingTable = getEntry("casting_table");
                if (castingTable != null) matches.add(castingTable);
            }
        } else if (query.getItem() instanceof ItemGenericAssembly) {
            KnowledgeEntry assemblyEntry = getEntry("tool_assembly");
            if (assemblyEntry != null) matches.add(assemblyEntry);
            KnowledgeEntry tableEntry = getEntry("assembly_table");
            if (tableEntry != null) matches.add(tableEntry);
        } else if (query.getItem() instanceof ItemCastingMold) {
            GeometryId geom = ItemCastingMold.getGeometry(query);
            if (geom != null) {
                KnowledgeEntry moldEntry = getEntry("mold_" + geom.id());
                if (moldEntry != null) matches.add(moldEntry);
            }
            KnowledgeEntry blankMold = getEntry("unshaped_blank_mold");
            if (blankMold != null) matches.add(blankMold);
        } else if (query.getItem() instanceof ItemMaterialSample) {
            KnowledgeEntry matEntry = getEntry("material_copper");
            if (matEntry != null) matches.add(matEntry);
        }

        return new ArrayList<>(matches);
    }

    /**
     * Finds entries explaining what the given ItemStack can be used for ("U" key).
     */
    public List<KnowledgeEntry> findUsages(ItemStack query) {
        if (query == null) return Collections.emptyList();
        Set<KnowledgeEntry> matches = new LinkedHashSet<>();

        // 1. Process step input match in usages or acquisitions
        for (KnowledgeEntry entry : allEntries) {
            for (ProcessStep step : entry.usages()) {
                if (step.matchesInput(query)) {
                    matches.add(entry);
                    break;
                }
            }
            for (ProcessStep step : entry.acquisitions()) {
                if (step.matchesInput(query)) {
                    matches.add(entry);
                    break;
                }
            }
        }

        // 2. Fuel check
        if (TileEntityStarterSmelter.getFuelBurnTime(query) > 0) {
            KnowledgeEntry smelterEntry = getEntry("starter_smelter");
            if (smelterEntry != null) matches.add(smelterEntry);
        }

        // 3. Dynamic contextual resolution
        if (query.getItem() instanceof ItemGenericComponent) {
            GeometryId geom = ItemGenericComponent.getGeometry(query);
            if (geom == GeometryId.HANDLE || (geom != null && geom.isToolHead())) {
                KnowledgeEntry assemblyEntry = getEntry("tool_assembly");
                if (assemblyEntry != null) matches.add(assemblyEntry);
                KnowledgeEntry tableEntry = getEntry("assembly_table");
                if (tableEntry != null) matches.add(tableEntry);
            }
            // All components can be remelted
            KnowledgeEntry smelterEntry = getEntry("starter_smelter");
            if (smelterEntry != null) matches.add(smelterEntry);
        } else if (query.getItem() instanceof ItemCastingMold) {
            KnowledgeEntry castingTable = getEntry("casting_table");
            if (castingTable != null) matches.add(castingTable);
            KnowledgeEntry outlet = getEntry("smelter_outlet");
            if (outlet != null) matches.add(outlet);
        } else if (query.getItem() instanceof ItemGenericAssembly) {
            KnowledgeEntry assemblyEntry = getEntry("tool_assembly");
            if (assemblyEntry != null) matches.add(assemblyEntry);
        }

        return new ArrayList<>(matches);
    }
}
